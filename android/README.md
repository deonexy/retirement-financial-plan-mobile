# Android MVP foundation

Implementasi awal Android native offline-first sekarang tersedia di direktori `android/`.

## Arsitektur

- `domain/`: modul pure Kotlin/JVM untuk model domain dan kalkulator deterministik (`SavingsCalculator`, `GoldCalculator`, `RetirementCalculator`).
- `app/`: aplikasi Android berbasis Jetpack Compose + Material 3.
- Penyimpanan lokal memakai Room untuk `RetirementProfile`, `FinancialUpdate`, `AssetPurchase`, dan `GoldPriceSnapshot`.
- Pengaturan perangkat memakai DataStore untuk tema, pengingat, dan toggle refresh harga emas.
- WorkManager aktif untuk pengingat bulanan dan refresh harga emas berkala saat jaringan tersedia.
- Summary membaca satu `Flow<FinancialSummary>` dari repository/use case, bukan menghitung ulang di composable.

## Menjalankan build dan test

Dari direktori `android/`:

```bash
./gradlew -PskipAndroidApp=true :domain:test
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Catatan: build modul `app` membutuhkan akses ke Google Maven untuk Android Gradle Plugin dan dependensi AndroidX/Compose. Pada sandbox ini, validasi penuh modul `app` dapat gagal jika repository tersebut tidak bisa dijangkau.

## Offline/online boundary

- Semua data finansial inti dibaca/ditulis ke Room lebih dulu.
- Auth/server sengaja tidak dipindahkan ke penyimpanan lokal dan tetap dianggap integrasi terpisah.
- Refresh harga emas otomatis berjalan via worker dan tetap fallback ke status `harga terakhir diperbarui ...` atau `belum tersedia` saat offline.

## Keamanan

- Uang dan kuantitas decimal disimpan aman sebagai `String` lalu dipetakan ke `BigDecimal`.
- Tidak ada logging data sensitif pada implementasi ini.
- Android Keystore dan BiometricPrompt belum diaktifkan, tetapi tetap menjadi arah lanjutan yang didokumentasikan.

## Backup dan migrasi JSON terenkripsi

- Settings menyediakan ekspor backup JSON terenkripsi (`.json.enc`) ke direktori app external files dengan passphrase pengguna.
- Impor backup terakhir mendukung mode `merge` dan `replace` secara atomik dengan transaksi Room, memakai passphrase yang sama.
- Restorasi berfokus pada data finansial lokal; snapshot settings disertakan untuk audit/portabilitas backup.
- Payload backup menyertakan `backupVersion`, `exportedAt`, profile, update bulanan, pembelian aset, snapshot harga emas, dan snapshot settings.

## Batasan dan TODO terukur

- Sinkronisasi cloud masih belum tersedia (tetap local-first).
- Proteksi biometric app lock belum diaktifkan.
- Validasi instrumentation/CI Android penuh masih bergantung akses Google Maven pada environment runner.
