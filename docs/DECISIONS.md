# Keputusan Arsitektur & Rekayasa (Architecture Decision Records) — ForFH Android

Dokumen ini mencatat seluruh keputusan teknis, investigasi akar masalah (*root cause analysis*), dan perubahan arsitektur penting pada project `ForFH-Android`.

---

## ADR-001: Penghapusan Hardcoded `fillMaxWidth()` pada `ForfhComponents.kt`
* **Tanggal:** 19 Agustus 2026
* **Status:** Diterapkan (v2.5.0)
* **Konteks & Masalah:**
  Pada halaman Pengaturan (*SettingsScreen*), teks di section *"TENTANG APLIKASI & PEMBARUAN"* ter-render vertikal (1 huruf per baris).
* **Akar Masalah (*Root Cause*):**
  Komponen `PrimaryButton`, `TonalButton`, dan `OutlineButton` di `ForfhComponents.kt` memiliki modifier hardcoded `.fillMaxWidth()` di dalam body composable alih-alih sebagai default parameter modifier. Ketika diletakkan di dalam `Row` berdampingan dengan `Column(modifier = Modifier.weight(1f))`, tombol menyerap seluruh lebar layar dan menekan kolom teks hingga lebarnya hanya beberapa piksel, memaksa teks *character-wrap* ke bawah.
* **Solusi & Keputusan:**
  1. Hapus `.fillMaxWidth()` dari body internal fungsi komponen.
  2. Jadikan `modifier: Modifier = Modifier.fillMaxWidth()` sebagai default parameter sehingga pemanggil dapat menimpa modifier dengan `wrapContentWidth()`, `weight()`, atau custom sizing.
  3. Tambahkan `Modifier.wrapContentWidth()` pada tombol "Cek Update" di `PengaturanScreen.kt`.

---

## ADR-002: Resource Logo Raster PNG untuk Jetpack Compose (`ic_forfh_logo.png`)
* **Tanggal:** 19 Agustus 2026
* **Status:** Diterapkan (v2.5.0)
* **Konteks & Masalah:**
  Pemanggilan `painterResource(R.mipmap.ic_launcher)` pada API 26+ melempar runtime exception `IllegalArgumentException: Only VectorDrawables and rasterized asset types are supported in Compose` karena `ic_launcher` di API 26+ bertipe `<adaptive-icon>` XML.
* **Solusi & Keputusan:**
  Dibuat aset rasterized khusus `res/drawable/ic_forfh_logo.png` dengan logo timbangan ForFH dan diakses secara aman di `LoginScreen.kt`.

---

## ADR-003: Penyesuaian OkHttpClient Timeouts (45 Detik) untuk Ketahanan Cloud Serverless
* **Tanggal:** 19 Agustus 2026
* **Status:** Diterapkan (v2.5.0)
* **Konteks & Masalah:**
  Pengguna sering mengalami error *"Gangguan koneksi, coba lagi"* saat login di jaringan kampus lambat atau saat server backend Koyeb / UNAIR KampusKita mengalami cold-start.
* **Akar Masalah (*Root Cause*):**
  Default timeout Retrofit sebelumnya adalah 20 detik (`connectTimeout(20, TimeUnit.SECONDS)`, `readTimeout(20, TimeUnit.SECONDS)`). `SocketTimeoutException` merupakan turunan dari `IOException`, sehingga memicu mapping generic network error.
* **Solusi & Keputusan:**
  Tingkatkan `connectTimeout`, `readTimeout`, dan `writeTimeout` menjadi **45 detik** di `ApiClient.kt`.

---

## ADR-004: Standar Rilis & Upload Aset Produksi Resmi
* **Tanggal:** 19 Agustus 2026
* **Status:** Diterapkan
* **Aturan Baku Rilis:**
  1. Setiap build APK untuk pengujian perangkat nyata wajib menggunakan varian **Release** bertanda tangan resmi (`app-release.apk`), bukan Debug.
  2. Rilis GitHub Release wajib meng-upload `app-release.apk` sebagai binary asset yang dapat diunduh (terverifikasi HTTP 200 OK), bukan sekadar git tag source code.
  3. Portal website `/unduh` harus selalu sinkron dengan versi rilis terbaru dan tautan download langsung GitHub Release yang valid.
