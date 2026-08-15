# ForFH Android

App Android V1 klien resmi ForFH — jadwal kuliah, tugas, dan alarm bangun kuliah
dengan offset 3/2/1 jam, sinkronisasi dari server ForFH UNAIR FH (jalur
email+password, bukan NIM).

## Build

Prasyarat: JDK 21 (Temurin), Android SDK (cmdline-tools + platforms;android-36 +
build-tools;36.0.0), `ANDROID_HOME` di-set, Gradle wrapper 9.5.0 sudah ada di repo.

```powershell
.\gradlew.bat :app:assembleDebug          # debug APK
.\gradlew.bat :app:assembleRelease        # release APK (butuh keystore.properties)
.\gradlew.bat :app:testDebugUnitTest      # unit test JVM
```

Release signing: buat keystore sekali (lihat task T13) lalu isi `keystore.properties`
di root repo — file ini dan `.jks` tidak pernah di-commit.

## Install di Redmi (HyperOS)

1. `app/build/outputs/apk/release/app-release.apk` → kirim ke ponsel, izinkan "install dari sumber tidak dikenal".
2. Buka app → login dengan email+password kampus FH UNAIR.
3. Beri izin: notifikasi, "Alarm & pengingat" (MIUI/HyperOS), alarm presisi (Android 12+), layar penuh (Android 14+).
4. Nonaktifkan penghemat baterai untuk ForFH bila alarm tidak muncul.

## Arsitektur

Room (mirror `schedules`/`tasks` + state `scheduled_alarms`) sebagai sumber
kebenaran; WorkManager ±6 jam safety net; AlarmManager `RTC_WAKEUP` exact dengan
fallback `setWindow` (jendela mulai dari trigger, hingga 10 menit); receiver = validator + executor; sesi cookie
`__Host-forfh-session` dienkripsi AES-256-GCM (Android Keystore). Tanpa foreground
service. Detail desain: `docs/superpowers/specs/2026-08-15-forfh-android-design.md`.
