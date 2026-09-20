# retirement-financial-plan-mobile

Dashboard pribadi untuk membantu menyiapkan masa pensiun yang sehat secara finansial melalui pencatatan kondisi keuangan, pembaruan bulanan, proyeksi dana pensiun, dan rekomendasi tindakan yang relevan.

Dokumentasi ini menjadi acuan arsitektur **mobile Android offline-first** dengan pendekatan **frontend React yang sudah ada dikemas ke Android via Capacitor**, sehingga migrasi dapat dilakukan cepat tanpa rewrite total UI.

## Ruang lingkup fitur

- Ringkasan kesehatan finansial: status jalur pensiun, total aset bersih, dana darurat, dana kesehatan, dan napas finansial.
- Profil rencana pensiun: usia, kebutuhan hidup, pendapatan pasca-pensiun, aset, utang, tabungan, inflasi, dan target warisan.
- Proyeksi DPLK dan JHT.
- Pelacakan emas dalam gram, termasuk emas tergadai dan kewajiban gadai.
- Harga emas live saat online, dengan fallback ke snapshot lokal saat offline.
- Update kondisi keuangan bulanan.
- Akumulasi tabungan otomatis dari saldo awal + surplus positif - pembelian aset dari tabungan.
- Transaksi pembelian aset.
- Saran personal berbasis data terbaru.
- Autentikasi per akun agar identitas user tetap terpisah.

## Aturan perhitungan utama

### Surplus dan tabungan

```text
surplusBulan = pendapatan
             - kebutuhanWajib
             - gayaHidup
             - kesehatan
             - pembayaranUtang

surplusBulan = max(0, surplusBulan)
tabunganSaatIni = saldoAwalProfile
                + Σ surplusBulan
                - Σ pembelianDenganSumberTabungan
```

- Defisit bulanan **tidak** mengurangi tabungan otomatis.
- Penarikan saat defisit harus dicatat eksplisit lewat transaksi/koreksi profile.

### Emas

```text
totalHargaJualEmas = (gramDiTangan + gramDigadaikan) × hargaEmasPerGram
nilaiBersihEmas = max(0, totalHargaJualEmas - kewajibanGadai)
```

- Saat online, aplikasi mengambil harga live dari API eksternal.
- Saat offline, aplikasi memakai snapshot lokal terakhir dan menandainya sebagai **stale**.
- Jika belum ada snapshot, UI menampilkan status harga belum tersedia; jangan diam-diam mengisi nol.

### Total aset bersih

```text
totalAsetBersih = tabunganSaatIni
                + nilaiBersihEmas
                + saham
                + investasiLain
                + properti
                + asetPensiunDPLKJHT
```

Pembelian aset yang dibiayai dari tabungan hanya memindahkan nilai dari tabungan ke aset, bukan menambah kekayaan bersih.

## Target arsitektur mobile

Target implementasi mobile adalah:

- **UI tetap berbasis React** agar logika dan pengalaman aplikasi web dapat dipertahankan.
- **Android shell menggunakan Capacitor** untuk membungkus frontend menjadi aplikasi Android native.
- **Data finansial utama berpindah ke SQLite lokal di perangkat** untuk mewujudkan offline-first.
- **Autentikasi dan identitas user tetap server/cloud**.

Arsitektur target:

```text
React UI
  -> data repository abstraction
    -> auth adapter (server/cloud)
    -> financial data adapter (local SQLite on device)
    -> gold price adapter (online with local snapshot fallback)

Capacitor Android shell
  -> SQLite plugin / local database
  -> secure storage / biometric bridge (opsional)
```

## Pemisahan domain data

### Tetap server/cloud

- `users`
- autentikasi
- identitas akun aktif

### Lokal di perangkat

- `retirement_profiles`
- `financial_updates`
- `asset_purchases`
- snapshot harga emas
- pengaturan perangkat yang relevan untuk mode offline

Semua data finansial lokal harus direlasikan ke user aktif melalui identifier seperti `ownerOpenId`, tanpa menyimpan kredensial sensitif.

## Kontrak data lokal

Skema SQLite lokal harus ekuivalen dengan struktur finansial yang sudah ada agar logika bisnis utama tetap konsisten.

Contoh kebutuhan minimum tabel lokal:

### `retirement_profiles`

- `id`
- `ownerOpenId`
- field profil pensiun, aset, utang, tabungan, DPLK/JHT, emas, dan asumsi proyeksi
- `updatedAt`

### `financial_updates`

- `id`
- `ownerOpenId`
- bulan/periode
- pendapatan, kebutuhan wajib, gaya hidup, kesehatan, pembayaran utang, sisa utang
- catatan
- `updatedAt`

### `asset_purchases`

- `id`
- `ownerOpenId`
- `assetType`
- `assetName`
- `quantity`
- `unit`
- `purchasePriceRupiah`
- `purchaseValueRupiah`
- `purchaseDate`
- `fundingSource`
- `notes`
- `updatedAt`

### `gold_price_snapshots`

- `id`
- `pricePerGramRupiah`
- `currency`
- `source`
- `fetchedAt`
- `isStale`

## Alur data offline-first

1. Aplikasi memuat user aktif dari auth server jika tersedia.
2. Semua baca/tulis data finansial dilakukan ke database lokal lebih dulu.
3. Ringkasan dihitung dari data lokal yang sama.
4. Harga emas live hanya dicoba saat online.
5. Jika offline, aplikasi memakai snapshot harga emas lokal terakhir.
6. Jika user belum login, aplikasi harus menampilkan mode terbatas atau memblokir akses data finansial sesuai kebijakan produk.

## Refactor lapisan akses data

UI tidak boleh lagi bergantung langsung ke tRPC untuk data finansial. Lapisan akses data perlu dipisah menjadi:

- `auth repository/service`: tetap memakai adapter server.
- `financial repository/service`: memakai adapter lokal.
- `gold price service`: online-first dengan cache lokal.

Dengan bentuk ini, React UI tetap sama, tetapi sumber data dapat ditukar sesuai platform.

## Fitur yang tetap bergantung internet

- login dan validasi sesi user
- refresh harga emas live
- backup/sinkronisasi cloud jika nanti diaktifkan

Perilaku offline:

- data finansial tetap bisa CRUD penuh
- ringkasan dan proyeksi tetap berjalan
- harga emas memakai snapshot lokal terakhir dengan indikator waktu pembaruan

## Keamanan data lokal

- Enkripsi database lokal untuk data finansial.
- Jangan menyimpan PIN, OTP, token, atau nomor rekening di log.
- Proteksi aplikasi dengan biometric/app lock bersifat opsional.
- Logout harus membersihkan data lokal user aktif sesuai kebijakan clear data.

## Rencana migrasi bertahap

### Fase 1

- Android build dengan Capacitor.
- Auth tetap server/cloud.
- UI React mulai dijalankan dalam shell Android.

### Fase 2

- `retirement_profiles`, `financial_updates`, dan `asset_purchases` pindah ke SQLite lokal.
- Repository finansial lokal menjadi source of truth.
- Tambahkan snapshot harga emas lokal.

### Fase 3

- Hardening error handling.
- Recovery dan migrasi data.
- Penguatan keamanan lokal.
- Validasi perilaku offline/online dan multi-user device.

## Konfigurasi build Android

Saat implementasi dimulai, konfigurasi minimal yang perlu disiapkan:

- project React tetap menjadi sumber UI utama
- Capacitor Android project untuk packaging dan bridge native
- plugin SQLite lokal untuk penyimpanan finansial offline
- secure storage untuk metadata sensitif non-finansial seperlunya
- konfigurasi network hanya untuk auth, harga emas, dan backup opsional

## Skenario validasi kualitas

- login user
- CRUD profile/update/pembelian aset
- restart app dan verifikasi data tetap ada
- perpindahan offline ke online
- harga emas live dan fallback snapshot lokal
- logout lalu login user berbeda
- performa query lokal pada perangkat Android

## Batasan implementasi

- Logika bisnis utama web tidak boleh berubah makna.
- Istilah aset seperti `liquidAssets`, `stockAssets`, `otherInvestmentAssets`, dan `propertyAssets` harus tetap konsisten.
- Sinkronisasi cloud bukan prasyarat pemakaian aplikasi.
- Offline-first berlaku untuk data finansial, bukan untuk autentikasi live.
