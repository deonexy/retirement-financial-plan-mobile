# Android MVP foundation

Implementasi awal Android native offline-first sekarang tersedia di direktori `android/`.

## Arsitektur

- `domain/`: modul pure Kotlin/JVM untuk model domain dan kalkulator deterministik (`SavingsCalculator`, `GoldCalculator`, `RetirementCalculator`).
- `app/`: aplikasi Android berbasis Jetpack Compose + Material 3.
- Penyimpanan lokal memakai Room untuk `RetirementProfile`, `FinancialUpdate`, `AssetPurchase`, dan `GoldPriceSnapshot`.
- Pengaturan perangkat memakai DataStore untuk tema, pengingat, dan toggle refresh harga emas.
- WorkManager disiapkan sebagai placeholder untuk pengingat bulanan dan refresh harga emas saat jaringan tersedia.
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
- Refresh harga emas masih placeholder; aplikasi tetap dapat menampilkan status `harga terakhir diperbarui ...` atau `belum tersedia` saat offline.

## Keamanan

- Uang dan kuantitas decimal disimpan aman sebagai `String` lalu dipetakan ke `BigDecimal`.
- Tidak ada logging data sensitif pada implementasi ini.
- Android Keystore dan BiometricPrompt belum diaktifkan, tetapi tetap menjadi arah lanjutan yang didokumentasikan.

## Migrasi web JSON (lanjutan)

Format migrasi JSON dari README belum diimplementasikan pada PR pertama ini. Struktur Room dan model domain sengaja dibuat dekat dengan istilah bisnis README agar fase ekspor/impor berikutnya lebih mudah.

## Batasan dan TODO terukur

- Worker masih placeholder/no-op sampai integrasi notifikasi dan refresh harga emas dipilih.
- Migration instrumentation test Room belum ditambahkan pada PR pertama ini. TODO berikutnya: tambahkan skema tersemat dan test `MigrationTestHelper` saat pipeline/emulator Android instrumentation tersedia.
- Belum ada sinkronisasi cloud, ekspor/impor terenkripsi, atau proteksi biometric aktif.
