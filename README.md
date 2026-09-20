# retirement-financial-plan-mobile
Versi mobile Rencana Keuangan Pensiun
# Pensiun Sehat Finansial

Dashboard pribadi untuk membantu menyiapkan masa pensiun yang sehat secara finansial melalui pencatatan kondisi keuangan, pembaruan bulanan, proyeksi dana pensiun, dan rekomendasi tindakan yang relevan.

Dokumentasi ini juga menjadi acuan untuk membuat **versi Android native offline-first** dengan database lokal, tanpa mengubah logika bisnis utama aplikasi web.

## Fitur utama

- **Ringkasan kesehatan finansial** dengan status jalur pensiun, total aset bersih, dana darurat, dana kesehatan, dan napas finansial sampai usia rencana.
- **Profil rencana pensiun** untuk usia sekarang, usia pensiun, kebutuhan hidup, pendapatan setelah pensiun, aset, utang, tabungan, inflasi, dan target warisan.
- **Proyeksi DPLK dan JHT** berdasarkan saldo awal, setoran bulanan, dan asumsi imbal hasil tahunan.
- **Pelacakan emas** dalam satuan gram, meliputi emas di tangan, emas yang digadaikan, kewajiban gadai, dan tambahan gram setiap bulan.
- **Harga emas live saat dashboard dimuat atau direfresh.** Nilai jual emas dihitung dari jumlah gram dikalikan harga emas per gram. Nilai bersih aset mengurangi kewajiban gadai.
- **Update kondisi keuangan bulanan** untuk pendapatan, pengeluaran, pembayaran utang, dan sisa utang. Posisi aset/investasi utama dikelola dari Profile dan transaksi pembelian aset.
- **Akumulasi tabungan otomatis** dari saldo awal Profile ditambah surplus positif setiap bulan dan dikurangi pembelian aset yang sumber dananya berasal dari tabungan.
- **Transaksi pembelian aset** dengan jenis aset, jumlah, satuan, harga beli, tanggal, dan sumber dana.
- **Saran personal** yang dihitung berdasarkan data terbaru, misalnya penguatan dana darurat, pengendalian cicilan, pengurangan kewajiban gadai, atau pengisian update bulan berjalan.
- **Autentikasi per akun** sehingga data profil dan riwayat keuangan dipisahkan antar pengguna.

## Cara kerja perhitungan utama

### Surplus dan tabungan

Surplus bulanan dihitung dari arus kas yang dicatat pada Update bulanan:

```text
surplusBulan = pendapatan
             − kebutuhanWajib
             − gayaHidup
             − kesehatan
             − pembayaranUtang

surplusBulan = max(0, surplusBulan)
tabunganSaatIni = saldoAwalProfile
                + Σ surplusBulan
                − Σ pembelianDenganSumberTabungan
```

Defisit bulanan tidak mengurangi tabungan otomatis pada versi saat ini. Jika terjadi defisit, pengguna dapat mencatat penarikan tabungan melalui fitur transaksi yang akan ditambahkan kemudian atau mengoreksi saldo melalui Profile.

### Emas

Pada setiap pemuatan dashboard, server meminta harga spot emas dari sumber eksternal. Sumber utama adalah GoldPrice.dev untuk harga XAU/IDR. Jika sumber utama tidak tersedia, sistem menggunakan Gold-API untuk harga XAU/USD dan ExchangeRate-API untuk kurs USD/IDR.

```text
Total harga jual emas = (gram di tangan + gram digadaikan) × harga emas per gram
Nilai bersih emas = max(0, total harga jual emas − kewajiban gadai)
```

Harga live adalah harga spot indikatif, bukan jaminan harga buyback dari toko emas, Antam, Pegadaian, atau lembaga gadai tertentu. Proyeksi masa depan menggunakan asumsi pertumbuhan emas pada Profile, sedangkan ringkasan saat ini menggunakan harga live.

### Total aset

```text
Total aset bersih = tabungan saat ini
                  + nilai bersih emas
                  + saham dari Profile dan transaksi
                  + investasi lain dari Profile dan transaksi
                  + properti dari Profile dan transaksi
                  + aset pensiun sesuai perhitungan DPLK/JHT
```

Setiap pembelian yang dibiayai dari tabungan memindahkan nilai dari kantong tabungan ke aset, sehingga tidak menaikkan kekayaan bersih secara artifisial.

## Penggunaan dashboard web

### 1. Tab Rencana

Isi data dasar rencana pensiun, saldo awal tabungan, posisi aset, DPLK/JHT, emas, utang, kebutuhan hidup, dan asumsi proyeksi. Input aset/investasi utama berada di Profile, bukan di Update bulanan.

Untuk emas, gunakan satuan **gram** pada kolom emas di tangan, emas yang digadaikan, dan tambahan emas per bulan. Kewajiban gadai tetap diisi dalam rupiah. Asumsi imbal hasil emas diisi sebagai persentase per tahun.

Tab Rencana juga menyediakan formulir **Catat pembelian aset**. Masukkan jenis aset, nama, jumlah, satuan, harga beli, tanggal pembelian, dan sumber dana. Jika sumber dana adalah tabungan, saldo tabungan otomatis berkurang sebesar nilai pembelian.

### 2. Tab Update bulanan

Pada awal setiap bulan, masukkan angka aktual untuk:

- Pendapatan bersih.
- Kebutuhan wajib.
- Gaya hidup.
- Pengeluaran kesehatan.
- Pembayaran cicilan atau utang.
- Sisa utang.
- Catatan tambahan.

Surplus positif dari setiap Update akan menambah saldo tabungan otomatis. Aset/investasi tidak perlu diinput ulang pada Update bulanan karena dikelola dari Profile dan transaksi pembelian.

### 3. Tab Ringkasan

Ringkasan menampilkan tabungan saat ini, surplus bulan terakhir, akumulasi surplus seluruh Update, pembelian dari tabungan, total aset bersih, harga emas saat refresh, nilai bersih setelah gadai, proyeksi DPLK/JHT, proyeksi emas, dan saran tindakan yang disesuaikan dengan kondisi keuangan.

## Rencana versi Android native offline-first

### Tujuan dan prinsip

Versi Android ditujukan untuk penggunaan pribadi yang tetap berfungsi tanpa koneksi internet. Semua pencatatan utama harus dapat dilakukan secara offline dan tersimpan di perangkat. Koneksi internet hanya diperlukan untuk sinkronisasi opsional, login, backup, dan pembaruan harga emas live.

Prinsip implementasi:

1. **Local-first:** UI membaca dan menulis ke database lokal terlebih dahulu.
2. **Single source of truth di perangkat:** seluruh ringkasan dan proyeksi dihitung dari data lokal yang sama.
3. **Perhitungan deterministik:** rumus surplus, saldo tabungan, pembelian aset, dan proyeksi dipindahkan ke modul domain Kotlin yang dapat diuji tanpa Android framework.
4. **Tidak menyimpan rahasia finansial di log:** nomor rekening, PIN, OTP, dan token tidak boleh ditulis ke log atau catatan bebas.
5. **Sinkronisasi bukan prasyarat penggunaan:** aplikasi tetap dapat dipakai tanpa akun cloud.

### Stack Android yang direkomendasikan

| Lapisan | Teknologi | Tanggung jawab |
|---|---|---|
| UI | Kotlin + Jetpack Compose + Material 3 | Ringkasan, Profile, Update bulanan, transaksi aset, pengaturan |
| State | ViewModel + Kotlin Coroutines + Flow | State layar, loading, error, dan event pengguna |
| Database lokal | Room + SQLite | Penyimpanan Profile, Update, transaksi, dan snapshot harga |
| Preferensi | DataStore | Tema, pengaturan, waktu pengingat, dan opsi sinkronisasi |
| Keamanan | Android Keystore; opsi BiometricPrompt | Kunci enkripsi lokal dan penguncian aplikasi |
| Background | WorkManager | Pengingat bulanan dan refresh harga saat kondisi jaringan tersedia |
| Jaringan opsional | Retrofit/OkHttp | Harga emas dan sinkronisasi/backup jika diaktifkan |
| Backup | File JSON terenkripsi atau cloud sync opsional | Ekspor, impor, dan pemulihan data pribadi |

### Struktur proyek Android yang disarankan

```text
android/
  app/
    src/main/java/com/pensiunsehat/finansial/
      data/
        local/
          AppDatabase.kt
          dao/
          entity/
        remote/
          GoldPriceApi.kt
          SyncApi.kt                 # opsional
        repository/
      domain/
        model/
        calculator/
          RetirementCalculator.kt
          SavingsCalculator.kt
          GoldCalculator.kt
        usecase/
      presentation/
        navigation/
        summary/
        profile/
        monthlyupdate/
        assetpurchase/
        settings/
      worker/
        MonthlyReminderWorker.kt
        GoldPriceRefreshWorker.kt
```

### Pemetaan database lokal

Nama kolom sebaiknya mempertahankan istilah dan tipe bisnis dari schema web agar migrasi lebih mudah. Room menggunakan `Long` untuk ID, `String` untuk tanggal ISO `yyyy-MM-dd`, dan `BigDecimal` atau `Long` rupiah sesuai keputusan implementasi. Untuk akurasi rupiah, jangan gunakan `Double` sebagai penyimpanan uang.

| Entity Room | Sumber web | Isi |
|---|---|---|
| `UserEntity` | `users` | Identitas lokal, jika login/sinkronisasi digunakan |
| `RetirementProfileEntity` | `retirement_profiles` | Usia, target, pendapatan, aset, tabungan, DPLK/JHT, emas, utang, asumsi |
| `FinancialUpdateEntity` | `financial_updates` | Snapshot arus kas per bulan dan sisa utang |
| `AssetPurchaseEntity` | `asset_purchases` | Pembelian emas, saham, investasi lain, atau properti |
| `GoldPriceSnapshotEntity` | Sumber baru lokal | Harga per gram, mata uang, sumber, waktu pengambilan, dan status validitas |
| `AppSettingEntity` | DataStore atau tabel lokal | Pengingat, tema, dan preferensi perangkat |

Setiap entity yang akan disinkronkan harus memiliki `id`, `updatedAt`, dan `syncState` (`LOCAL_ONLY`, `PENDING_UPLOAD`, `SYNCED`, `CONFLICT`) atau mekanisme setara. Jika aplikasi hanya lokal, `syncState` dapat ditunda.

Contoh entity transaksi aset:

```kotlin
@Entity(tableName = "asset_purchases")
data class AssetPurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assetType: String,
    val assetName: String,
    val quantity: String,             // decimal disimpan sebagai String
    val unit: String,
    val purchasePriceRupiah: Long,
    val purchaseValueRupiah: Long,
    val purchaseDate: String,         // yyyy-MM-dd
    val fundingSource: String,
    val notes: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
```

### Alur kalkulasi Android

Repository mengambil Profile, seluruh Update yang tanggalnya tidak melewati hari ini, transaksi pembelian yang sudah terjadi, dan harga emas terakhir yang valid. Use case kemudian menjalankan kalkulator domain:

```text
surplus setiap update = max(0, pendapatan − pengeluaran − pembayaran utang)
akumulasi surplus = jumlah seluruh surplus setiap update
saldo tabungan = saldo awal Profile + akumulasi surplus − pembelian dari tabungan
nilai emas = total gram × harga emas lokal terbaru
nilai bersih emas = max(0, nilai emas − kewajiban gadai)
```

Jika harga emas tidak tersedia, aplikasi harus tetap menampilkan ringkasan dengan harga terakhir yang tersimpan dan label **harga terakhir diperbarui pada ...**. Jika belum pernah ada harga, tampilkan status belum tersedia; jangan mengisi angka nol secara diam-diam tanpa penjelasan.

### Navigasi dan layar Android

Minimal versi pertama terdiri dari:

1. **Ringkasan:** saldo tabungan, surplus terakhir, akumulasi surplus, aset bersih, harga emas, dan saran.
2. **Update bulanan:** formulir arus kas, pembayaran utang, sisa utang, dan catatan.
3. **Rencana/Profile:** target pensiun, aset awal, emas, DPLK/JHT, kebutuhan, asumsi, dan koreksi data.
4. **Pembelian aset:** input transaksi dan histori pembelian.
5. **Pengaturan:** pengingat bulanan, ekspor/impor, keamanan aplikasi, dan sinkronisasi opsional.

Gunakan satu `NavHost` Compose dan satu `ViewModel` per feature. Ringkasan sebaiknya mengamati `Flow<FinancialSummary>` dari repository, bukan menghitung angka terpisah di setiap composable.

## Strategi migrasi dari web ke Android

### Fase 1 — Bekukan kontrak domain

- Dokumentasikan semua field Profile, Update, dan transaksi pembelian.
- Pindahkan rumus ke spesifikasi bersama dan buat contoh input-output tetap.
- Pastikan istilah `liquidAssets`, `stockAssets`, `otherInvestmentAssets`, dan `propertyAssets` tidak berubah maknanya.
- Tetapkan bahwa surplus negatif tidak mengurangi tabungan otomatis sampai fitur penarikan/defisit dibuat.

### Fase 2 — Bangun database lokal

- Buat project Android Kotlin dengan Compose.
- Buat entity, DAO, Room database, dan migration test.
- Sediakan seed data kosong yang aman untuk pengguna baru.
- Implementasikan repository lokal sebelum menambahkan jaringan.

### Fase 3 — Pindahkan kalkulator dan UI inti

- Implementasikan `SavingsCalculator`, `GoldCalculator`, dan `RetirementCalculator` sebagai pure Kotlin.
- Tambahkan unit test untuk surplus positif, surplus negatif, pembelian dari tabungan, pembelian dari pendapatan, emas tergadai, dan harga emas yang tidak tersedia.
- Bangun layar Ringkasan, Profile, Update bulanan, dan Pembelian aset.

### Fase 4 — Pengingat dan keamanan

- Gunakan WorkManager untuk pengingat bulanan, bukan service yang selalu hidup.
- Simpan konfigurasi pengingat di DataStore.
- Enkripsi backup ekspor dan lindungi akses aplikasi dengan BiometricPrompt jika pengguna mengaktifkannya.
- Sediakan fitur hapus semua data lokal dengan konfirmasi yang jelas.

### Fase 5 — Harga emas dan backup opsional

- Tambahkan refresh harga emas hanya saat jaringan tersedia.
- Simpan snapshot harga beserta timestamp dan sumbernya.
- Tambahkan ekspor/impor JSON terenkripsi terlebih dahulu sebagai backup sederhana.
- Sinkronisasi cloud dapat ditambahkan kemudian dengan strategi konflik yang jelas; jangan menjadikan server web sebagai ketergantungan versi lokal.

### Fase 6 — Migrasi data dari web

Format ekspor yang disarankan:

```text
backupVersion
exportedAt
profile
financialUpdates[]
assetPurchases[]
goldPriceSnapshots[]
settings
```

Alur migrasi:

1. Pengguna mengekspor data dari web dalam JSON.
2. Aplikasi Android memvalidasi `backupVersion` dan tipe data.
3. Aplikasi menampilkan preview jumlah Profile, Update, transaksi, dan tanggal terakhir.
4. Pengguna memilih merge atau replace data lokal.
5. Aplikasi menjalankan Room transaction agar migrasi bersifat atomic.
6. Aplikasi mencatat hasil migrasi tanpa menyimpan salinan data sensitif di log.

### Checklist Definition of Done Android MVP

- [ ] Aplikasi dapat dibuka dan digunakan tanpa koneksi internet.
- [ ] Profile, Update, pembelian aset, dan ringkasan tersimpan di Room.
- [ ] Saldo tabungan menghitung saldo awal + surplus positif − pembelian dari tabungan.
- [ ] Kalkulator Android menghasilkan angka yang sama dengan contoh uji web.
- [ ] Harga emas terakhir tetap terlihat saat offline dengan timestamp yang jelas.
- [ ] Pengingat bulanan dijalankan oleh WorkManager.
- [ ] Backup JSON terenkripsi dapat diekspor dan diimpor kembali.
- [ ] Database migration test tersedia untuk setiap perubahan schema.
- [ ] Tidak ada PIN, OTP, token, atau nomor rekening yang dicatat ke log.
- [ ] Test unit, test database, dan smoke test UI lulus pada perangkat/emulator Android.

## Menjalankan web secara lokal

Persyaratan:

- Node.js 22 atau versi kompatibel.
- pnpm.
- Database MySQL/TiDB yang dapat diakses aplikasi.
- Environment variables untuk autentikasi Manus dan database.

Instal dependensi dan jalankan server pengembangan:

```bash
pnpm install
pnpm dev
```

Perintah validasi dan produksi:

```bash
pnpm check       # Pemeriksaan TypeScript
pnpm test        # Menjalankan Vitest
pnpm build       # Membuat bundle produksi
pnpm start       # Menjalankan bundle produksi
```

## Migrasi database web

Model database berada di `drizzle/schema.ts`. Untuk membuat migrasi:

```bash
pnpm drizzle-kit generate
```

Tinjau SQL yang dibuat di `drizzle/` sebelum menerapkannya ke database. Perubahan data utama disimpan pada tabel berikut:

| Tabel | Isi |
|---|---|
| `users` | Identitas akun dan peran pengguna. |
| `retirement_profiles` | Profil target pensiun, DPLK, JHT, emas awal, aset, tabungan, dan asumsi proyeksi. |
| `financial_updates` | Riwayat arus kas dan pembayaran utang bulanan. |
| `asset_purchases` | Transaksi pembelian aset beserta sumber dana dan nilai pembelian. |

## Struktur proyek web

```text
client/src/pages/Home.tsx   # Antarmuka dashboard dan formulir
server/db.ts                # Query database dan pengambilan harga emas
server/asset-purchases.ts   # Kalkulator surplus dan transaksi aset
server/routers.ts           # Kontrak API tRPC dan validasi input
drizzle/schema.ts           # Model tabel database
drizzle/                   # File migrasi database
```

1. Tetapkan target arsitektur mobile: gunakan basis frontend React yang sudah ada lalu kemas ke Android (via Capacitor) agar migrasi cepat tanpa rewrite total UI.

2. Pisahkan data menjadi 2 domain:
- **Tetap server/cloud**: autentikasi dan identitas user (`users`).
- **Pindah lokal perangkat**: `retirement_profiles`, `financial_updates`, dan `asset_purchases`.

3. Definisikan kontrak data lokal Android:
- Buat skema SQLite lokal yang ekuivalen dengan struktur data finansial saat ini.
- Tetapkan aturan relasi data lokal ke user aktif (mis. `ownerOpenId`) tanpa menyimpan kredensial sensitif.

4. Ubah alur data aplikasi menjadi offline-first:
- Semua baca/tulis data finansial dilakukan ke database lokal.
- Data profil user tetap diambil dari endpoint auth seperti saat ini.
- Siapkan fallback jika user belum login (mode terbatas atau blokir akses data finansial).

5. Refactor lapisan akses data:
- Abstraksikan repository/service data agar UI tidak bergantung langsung ke tRPC untuk data finansial.
- Sediakan implementasi “local storage adapter” untuk Android dan pertahankan adapter server untuk autentikasi.

6. Sesuaikan fitur yang masih bergantung internet:
- Harga emas live tetap via API eksternal saat online.
- Saat offline, gunakan nilai terakhir yang tersimpan lokal + indikator “stale”.

7. Siapkan migrasi bertahap:
- Fase 1: Android build + auth tetap server.
- Fase 2: Pindah profile/update/purchase ke lokal.
- Fase 3: hardening (error handling, recovery, data migration).

8. Tambahkan keamanan data lokal:
- Enkripsi database lokal untuk data finansial.
- Proteksi akses aplikasi (biometric/app lock opsional).
- Kebijakan clear data saat logout user.

9. Validasi kualitas:
- Uji skenario utama: login, CRUD data finansial, restart app, offline/online switch, logout/login user berbeda.
- Uji kompatibilitas perangkat Android dan performa query lokal.

10. Perbarui dokumentasi proyek:
- Arsitektur baru (auth server + finansial lokal).
- Konfigurasi Android build.
- Batasan fitur online/offline dan alur data.

  ## Catatan keamanan dan penggunaan

Jangan memasukkan nomor rekening, PIN, kata sandi, kode OTP, atau informasi rahasia lainnya ke dalam catatan dashboard. Data keuangan yang ditampilkan adalah alat bantu perencanaan, bukan nasihat investasi, pajak, hukum, atau jaminan hasil. Verifikasi harga jual aktual dan kewajiban gadai pada lembaga terkait sebelum mengambil keputusan keuangan.

Untuk versi Android lokal, gunakan enkripsi perangkat/backup, Android Keystore, kunci aplikasi opsional, dan jangan mencetak payload finansial ke log debug.

## Status validasi

Build dokumentasi ini telah melewati pemeriksaan TypeScript, tes backend, dan build produksi. Peringatan ukuran chunk frontend dari Vite tidak menghalangi proses build. Fondasi Android native offline-first awal sekarang tersedia di direktori `android/` dengan modul domain pure Kotlin, skeleton Compose/Room/DataStore/WorkManager, dan dokumentasi menjalankan build/test pada `android/README.md`.

