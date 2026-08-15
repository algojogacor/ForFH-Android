# ForFH Android V1 Implementation Plan

> **REQUIRED SUB-SKILL:** `superpowers:subagent-driven-development` (recommended) atau `superpowers:executing-plans`.
> Setiap task dikerjakan sebagai unit sendiri (test → gagal → implementasi → lulus → commit). Jangan pernah mengerjakan dua task sekaligus.
> Semua langkah memakai checkbox — centang saat selesai. Test runner: `.\gradlew.bat :app:testDebugUnitTest`.

## Goal

Aplikasi Android native pendamping ForFH V1 (Kotlin + Compose + Material 3, minSdk 26 / targetSdk 36) yang menyediakan jadwal, tugas, dan alarm bangun kuliah on-device (Room + WorkManager sync + AlarmManager exact + recovery boot, tanpa foreground service), sebagai klien murni dari 1 deploy Koyeb web ForFH tanpa perubahan API server apa pun, dan didistribusikan via sideload APK.

## Architecture

Pendekatan 1 yang terkunci di spec: Room lokal sebagai satu-satunya source of truth state alarm (3 tabel: `schedules` + `tasks` mirror, `scheduled_alarms` state eksplisit), sync via WorkManager (±6 jam network-constrained + one-shot setelah login/manual), dan AlarmManager exact (`USE_EXACT_ALARM`, `RTC_WAKEUP`) dengan fallback `setWindow` bila exact tidak tersedia. Receiver = validator + executor dengan guard berlapis (login, jadwal enabled, identity + `triggerAtMillis` cocok, `now < startDateTime`, POST_NOTIFICATIONS), sehingga alarm basi tidak pernah tampil dan seluruh alarm dapat di-cancel/rebuild/recover secara deterministic dari Room (reconcile idempotent + rescheduleAll preserve sesi snooze, recovery boot/package-replaced/exact-restored). Auth via session cookie `__Host-forfh-session` yang dibungkus AES-256-GCM di Android Keystore (alias `forfh_session_key`), disuntikkan ke OkHttp CookieJar hanya dalam memori; 401 di `/api/*` selain login memicu auto-logout. Desain visual mengikuti DNA web ForFH (warna + tipografi serif italic/mono uppercase, dark/light ikut sistem) dengan layout Material 3.

## Tech Stack (versi dari riset platform, 2026-08)

| Komponen | Versi | Catatan |
|---|---|---|
| AGP | 9.3.0 | Butuh Gradle 9.5.0, JDK 17+, Build Tools 36.0.0 |
| Gradle wrapper | 9.5.0 | Bootstrap via gradle binary (lihat T1) |
| Kotlin | 2.4.0 | Built-in Kotlin AGP 9 — plugin `org.jetbrains.kotlin.android` TIDAK dipakai |
| KSP | 2.3.10 | Rilis dengan fix kompatibilitas Kotlin 2.4.0 (docs resmi sebut 2.3.9) |
| Compose BOM | 2026.08.00 | compose 1.12.0, material3 1.4.0, icons-core 1.7.8 |
| Room | 2.8.4 | minSdk 23+; KSP compiler |
| Retrofit | 3.0.0 | + converter-kotlinx-serialization resmi |
| OkHttp | 5.1.0 | + logging-interceptor (debug saja) |
| kotlinx-serialization | 1.11.0 | json |
| WorkManager | 2.11.2 | minSdk 23+; pakai `work-runtime` (ktx kosong sejak 2.9) |
| DataStore | 1.2.1 | preferences |
| androidx.core | 1.19.0 | core-ktx sudah merger, cukup `core` |
| activity-compose | 1.13.0 | rilis stable Maret 2026 |
| lifecycle | 2.11.0 | runtime-compose + viewmodel-compose |
| navigation-compose | 2.9.8 | stable April 2026 |
| kotlinx-coroutines | 1.10.2 | android (runtime) + test |
| JUnit | 4.13.2 | unit test JVM |

## Spec

Source of truth: `D:\Projects\ForFH-Android\docs\superpowers\specs\2026-08-15-forfh-android-design.md` (bahasa Indonesia, status terkunci).

## Global Constraints

Konstrain berikut SALINAN VERBATIM dari spec — pelanggaran = gagal:

- **minSdk 26 (Android 8.0) / targetSdk 36 (Android 16); edge-to-edge WAJIB** — semua layar memakai `Scaffold` + `WindowInsets`; konten tidak pernah tersembunyi di balik status/navigation bar; system bars transparan/dynamic (§3).
- **Izin exact alarm = `USE_EXACT_ALARM` (bukan `SCHEDULE_EXACT_ALARM`)** — declared di manifest, tanpa dialog special access (§3, §8.3).
- **Semua alarm kuliah dan snooze selalu `RTC_WAKEUP`** — membangunkan device dari sleep; snooze bukan `RTC` (§8.2, §8.6).
- **Identity deterministic**: `"class|scheduleId|offsetMinutes|occurrenceDate"` (kuliah), `"task|slot|date"` (tugas); **requestCode PendingIntent & notificationId = stableHash(id)**; **PendingIntent `FLAG_IMMUTABLE`**; extras membawa `scheduleId, offsetMinutes, occurrenceDate, triggerAtMillis` — receiver memvalidasi dari extras + Room, tidak menebak-nebak (§7).
- **`scheduled_alarms` TIDAK PERNAH di-wipe** — wipe-and-replace hanya menyentuh tabel mirror `schedules` & `tasks`, dan hanya saat response sukses & valid; perubahan `scheduled_alarms` selalu lewat `AlarmRescheduler` (§7, §9).
- **Preserve sesi snooze aktif**: row dengan `snoozeCount > 0` dan `triggerAtMillis` masih future tidak di-cancel dan tidak di-reset oleh sync/reconcile/boot; jika rebuild penuh terpaksa menyentuhnya, schedule ulang langsung ke `triggerAtMillis` tersimpan. Invariant: `snoozeCount` hanya naik lewat aksi snooze user, tidak pernah turun oleh proses lain; reset hanya saat row baru (§7, §8.1).
- **Snooze maks 5× per occurrence, +3 menit** (`triggerAtMillis += 180_000`); setelah snooze ke-5 tidak ada schedule ulang lagi dan tombol "Tidur lagi 3 menit" hilang (§8.6).
- **Guard receiver berlapis (kuliah)**: masih login (tidak → cancel alarm ini) → jadwal masih ada & enabled → row identity masih ada dan `row.triggerAtMillis == extras.triggerAtMillis` (stale → skip) → `now < startDateTime` (hitung ulang dari Room + occurrenceDate, WIB) → POST_NOTIFICATIONS (ditolak → silent, tidak crash). Salah satu gagal → **skip, tidak menampilkan apa pun** (§8.4, §10).
- **Slot tugas 09:00 / 15:00 / 20:00 WIB, one-shot (bukan setRepeating)** — pola: one-shot alarm → receiver → query Room → tampil notif → schedule occurrence berikutnya; "tidak ada tugas" hanya slot 09:00 menampilkan "🎉 Tidak ada tugas hari ini — selamat beraktivitas!", slot 15:00 & 20:00 silent (§8.7).
- **Exact alarm**: cek `canScheduleExactAlarms()` sebelum tiap schedule → exact `setExactAndAllowWhileIdle(RTC_WAKEUP, ...)` atau fallback `setWindow(RTC_WAKEUP, triggerAtMillis, windowLengthMs = 10 menit, ...)`; istilah "±10 menit" DILARANG di UX — di Pengaturan cukup: *"waktu alarm dapat sedikit bergeser"* (§8.3).
- **Full-screen intent**: `canUseFullScreenIntent()` → FullScreenAlarmActivity di atas lock screen (`setShowWhenLocked(true)` + `setTurnScreenOn(true)` API 27+, fallback flags API 26); tidak tersedia → fallback notif heads-up (channel "Alarm Kuliah" importance HIGH + sound + vibration + `setCategory(CATEGORY_ALARM)`); app tidak pernah bergantung pada FSI; tanpa izin `INTERRUPTION_FILTER` (tidak mem-bypass DND) (§8.5).
- **Recovery**: `BOOT_COMPLETED` (non-directBootAware — deferred path: tanpa unlock pertama pasca-reboot alarm tidak di-rebuild dan tidak ada yang tampil, begitu unlock reconcile berjalan otomatis) → `AlarmRescheduler.reconcile()`; `ACTION_MY_PACKAGE_REPLACED` → sama; exact access kembali tersedia (`ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`) → `rescheduleAll()`; habis sync sukses / login → `rescheduleAll()` (§8.9).
- **Logout**: (1) cancel seluruh alarm (iterate `scheduled_alarms` → `AlarmManager.cancel`); (2) hapus tabel `scheduled_alarms`; (3) hapus Room + DataStore (cookie, status sync); (4) kembali ke Login; (5) defense-in-depth: receiver apa pun yang jalan setelah logout → `isLoggedIn == false` → skip/cancel (§8.10).
- **Keamanan kredensial**: cookie sesi TIDAK pernah plaintext — SecureCookieStore AES-256-GCM, kunci non-exportable di Android Keystore (alias `forfh_session_key`), ciphertext + IV di DataStore; cookie ke OkHttp CookieJar hanya dalam memori; password kampus tidak pernah disimpan; fail-safe key hilang → auto-logout (§7).
- **Timezone**: SEMUA perhitungan memakai `ZoneId.of("Asia/Jakarta")` eksplisit — next occurrence kuliah, 3 slot tugas, snooze, recovery, guard `now < startDateTime`; tidak pernah mengandalkan timezone device implisit (§3, §8.8).
- **API server: nol perubahan** — `POST /api/auth/login {email, password}` (Set-Cookie `__Host-forfh-session`, 30 hari), `GET /api/schedules`, `GET /api/tasks`, `PUT /api/tasks/{id}` body `{status: "DONE"}` (app hanya pakai status); `401` di `/api/*` selain login = sesi habis → auto-logout; 401 login dibedakan → "Email atau password salah." (§4, §10).
- **WorkManager ±6 jam = safety net reconciliation, BUKAN timing guarantee** — waktu alarm ditentukan AlarmManager semata; kalau reschedule di receiver gagal, alarm besok tetap terpasang oleh reconcile berikutnya (§8.7, §9).
- **Wipe-and-replace hanya schedules+tasks saat sukses; sync gagal → Room tidak disentuh dan alarm tetap jalan dari data lokal**; "Tandai selesai" tidak pernah mengubah data lokal sebelum response sukses (gagal → toast, tanpa perubahan lokal); server tetap sumber kebenaran (§6, §9, §10).
- **HyperOS compatibility**: mekanisme murni AlarmManager, tanpa foreground service permanen dan tanpa service penjaga hidup; optimisasi background/power HyperOS = compatibility concern; saran di Pengaturan: *"aktifkan Autostart & nonaktifkan Battery Restriction untuk ForFH di Pengaturan → Aplikasi → ForFH"* (§8.11).
- **Distribusi**: build `./gradlew assembleRelease` di repo ForFH-Android (lokal, tanpa CI/deploy); APK ditandatangani keystore milik user (dibuat sekali saat rilis pertama; **tidak pernah di-commit ke git**; salinan cadangan terpisah); tanpa Play Store, tanpa auto-update di V1 (§13).
- **Non-goals V1 yang tidak diimplementasikan**: edit/CRUD jadwal/tugas/matkul di app, integrasi KRS/presensi/nilai/HE-BAT/Kampus Kita, push notification, Play Store/auto-update/analytics/multi-user, foreground service (§14).
## File Structure

```
D:\Projects\ForFH-Android\
├── .gitignore
├── README.md                          (T13)
├── settings.gradle.kts
├── build.gradle.kts                   (root, plugin alias apply false)
├── gradle.properties
├── gradle/libs.versions.toml
├── gradle/wrapper/gradle-wrapper.{jar,properties} + gradlew, gradlew.bat
├── keystore.properties                (T13, TIDAK di-commit; template di README)
├── forfh-release.jks                  (T13, TIDAK di-commit)
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── res/
        │   │   ├── values/strings.xml, colors.xml, themes.xml
        │   │   ├── drawable/ic_launcher_foreground.xml, ic_stat_alarm.xml
        │   │   └── mipmap-anydpi-v26/ic_launcher.xml
        │   └── java/com/aryariap/forfh/
        │       ├── ForfhApp.kt                    (Application + AppContainer lazy)
        │       ├── AppContainer.kt                (DI manual: db, prefs, network, sync, alarm)
        │       ├── MainActivity.kt                (T12: enableEdgeToEdge + NavHost)
        │       ├── ui/
        │       │   ├── theme/Theme.kt, Color.kt, Type.kt   (DNA web ForFH)
        │       │   ├── ForfhAppRoot.kt            (navigasi + sesi + notif intent)
        │       │   ├── UiFormat.kt                (format deadline/jam WIB — pure)
        │       │   ├── login/LoginScreen.kt, LoginViewModel.kt
        │       │   ├── jadwal/JadwalScreen.kt, JadwalViewModel.kt
        │       │   ├── tugas/TugasListScreen.kt, TugasDetailScreen.kt, TugasViewModel.kt
        │       │   └── pengaturan/PengaturanScreen.kt, PengaturanViewModel.kt
        │       ├── data/
        │       │   ├── db/AppDatabase.kt, ScheduleEntity.kt, TaskEntity.kt,
        │       │   │      ScheduledAlarmEntity.kt, SchedulesDao.kt, TasksDao.kt,
        │       │   │      ScheduledAlarmsDao.kt, DueDateParser.kt
        │       │   └── prefs/Preferences.kt, AlarmOffsets.kt,
        │       │          SecureCookieStore.kt, CookiePayloadCodec.kt,
        │       │          SessionManager.kt
        │       ├── network/
        │       │   ├── Dtos.kt, Mappers.kt, ForfhApiService.kt, ApiClient.kt,
        │       │   │   PersistentCookieJar.kt, SessionExpiryInterceptor.kt,
        │       │   │   LoginErrorMapper.kt
        │       ├── sync/
        │       │   ├── SyncRepository.kt, SyncStateStore.kt, SyncWorker.kt,
        │       │   │   ReconcilePlanner.kt, AlarmRescheduler.kt,
        │       │   │   BootReceiver.kt, ExactAlarmPermissionReceiver.kt
        │       └── alarm/
        │           ├── AlarmPlanner.kt, AlarmScheduler.kt (AlarmApi),
        │           │   AndroidAlarmApi.kt, StableHash.kt, ReceiverGuard.kt,
        │           │   SnoozeCounter.kt, TaskReminderText.kt, ClassAlarmText.kt,
        │           │   AlarmReceiver.kt, AlarmFlowHandler.kt, ForfhNotifications.kt,
        │           │   FullScreenAlarmActivity.kt, FullScreenAlarmViewModel.kt
        └── test/java/com/aryariap/forfh/
            ├── data/db/DueDateParserTest.kt
            ├── data/prefs/CookiePayloadCodecTest.kt, AlarmOffsetsTest.kt
            ├── network/DtoDecodeTest.kt, MappersTest.kt, LoginErrorMapperTest.kt
            ├── alarm/AlarmPlannerTest.kt, AlarmSchedulerTest.kt,
            │       ReceiverGuardTest.kt, TaskReminderTextTest.kt,
            │       SnoozeCounterTest.kt, ClassAlarmTextTest.kt, StableHashTest.kt
            ├── sync/ReconcilePlannerTest.kt, SyncRepositoryTest.kt, RecoveryPlanTest.kt
            └── ui/UiFormatTest.kt
```

## Task List

| Task | Isi | File utama |
|---|---|---|
| T1 | Scaffold Gradle + manifest + bootstrap env | build files, AndroidManifest.xml, ForfhApp |
| T2 | Room: 3 entity + 3 DAO + AppDatabase + DueDateParser | data/db/* |
| T3 | DataStore + SecureCookieStore + SessionManager | data/prefs/* |
| T4 | Network: DTO, Retrofit, CookieJar, error mapping | network/* |
| T5 | AlarmPlanner (math next occurrence WIB) | alarm/AlarmPlanner.kt |
| T6 | AlarmScheduler + stableHash + AndroidAlarmApi | alarm/AlarmScheduler.kt |
| T7 | ReceiverGuard + Snooze + TaskReminderText + AlarmReceiver + Notifications | alarm/* |
| T8 | ReconcilePlanner + AlarmRescheduler | sync/* |
| T9 | FullScreenAlarmActivity | alarm/FullScreenAlarm* |
| T10 | SyncRepository + SyncWorker + login flow + AppContainer | sync/*, AppContainer |
| T11 | Boot/package-replaced/exact-restored receivers | sync/BootReceiver.kt |
| T12 | UI Compose 4 layar + navigasi + tema | ui/* |
| T13 | Release build + keystore + README | README.md, build.gradle.kts |

---

## T1 — Scaffold Gradle + manifest + bootstrap environment

**Files:**
- Create `D:\Projects\ForFH-Android\settings.gradle.kts`
- Create `D:\Projects\ForFH-Android\build.gradle.kts`
- Create `D:\Projects\ForFH-Android\gradle.properties`
- Create `D:\Projects\ForFH-Android\gradle\libs.versions.toml`
- Create `D:\Projects\ForFH-Android\.gitignore`
- Create `D:\Projects\ForFH-Android\app\build.gradle.kts`
- Create `D:\Projects\ForFH-Android\app\proguard-rules.pro`
- Create `D:\Projects\ForFH-Android\app\src\main\AndroidManifest.xml`
- Create `D:\Projects\ForFH-Android\app\src\main\res\values\{strings,colors,themes}.xml`
- Create `D:\Projects\ForFH-Android\app\src\main\res\drawable\{ic_launcher_foreground,ic_stat_alarm}.xml`
- Create `D:\Projects\ForFH-Android\app\src\main\res\mipmap-anydpi-v26\ic_launcher.xml`
- Create `D:\Projects\ForFH-Android\app\src\main\java\com\aryariap\forfh\ForfhApp.kt`
- Create `D:\Projects\ForFH-Android\app\src\main\java\com\aryariap\forfh\MainActivity.kt` (minimal, ditulis ulang di T12)

**Interfaces:**
- Consumes: — (task pertama)
- Produces: proyek yang buildable (`assembleDebug` hijau) — prasyarat seluruh task berikutnya.

**Langkah:**

- [ ] **1. Bootstrap environment SDK + Gradle** (sekali jalan, PowerShell):
```powershell
# SDK Android SUDAH terpasang di D:\Android\Sdk (platforms;android-36, build-tools;36.0.0,
# platform-tools, license diterima). Verifikasi + pastikan env var di sesi ini:
$env:ANDROID_HOME = "D:\Android\Sdk"
$env:ANDROID_SDK_ROOT = "D:\Android\Sdk"
# Bila SDK dipindah ke mesin baru: setx ANDROID_HOME "D:\Android\Sdk" sekali,
# lalu sdkmanager.bat --licenses. local.properties (langkah 4) menunjuk sdk.dir=D\:\\Android\\Sdk.

# Bootstrap Gradle wrapper 9.5.0 (CLI global tidak ada; unduh distribusi sekali)
Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-9.5.0-bin.zip" -OutFile "$env:TEMP\gradle-9.5.0-bin.zip"
New-Item -ItemType Directory -Force "D:\tools"
Expand-Archive "$env:TEMP\gradle-9.5.0-bin.zip" "D:\tools"
```
- [ ] **2. Tulis `settings.gradle.kts`**:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "ForFH"
include(":app")
```
- [ ] **3. Tulis `gradle/libs.versions.toml`** (versi dari riset platform — jangan diubah tanpa alasan):
```toml
[versions]
agp = "9.3.0"
kotlin = "2.4.0"
ksp = "2.3.10"
composeBom = "2026.08.00"
room = "2.8.4"
retrofit = "3.0.0"
okhttp = "5.1.0"
kotlinxSerialization = "1.11.0"
work = "2.11.2"
datastore = "1.2.1"
core = "1.19.0"
activityCompose = "1.13.0"
lifecycle = "2.11.0"
navigation = "2.9.8"
coroutines = "1.10.2"
junit = "4.13.2"

[libraries]
androidx-core = { group = "androidx.core", name = "core", version.ref = "core" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-icons-core = { group = "androidx.compose.material", name = "material-icons-core" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
work-runtime = { group = "androidx.work", name = "work-runtime", version.ref = "work" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
junit = { group = "junit", name = "junit", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```
- [ ] **4. Tulis `build.gradle.kts` (root)** — catatan: AGP 9 punya built-in Kotlin, plugin `org.jetbrains.kotlin.android` DILARANG (build error bila dipakai):
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
```
- [ ] **5. Tulis `gradle.properties`**:
```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.configuration-cache=true
android.useAndroidX=true
```
- [ ] **6. Tulis `app/build.gradle.kts`** — signing release dibaca dari `keystore.properties` (dibuat di T13; bila tidak ada, release build tetap bisa dijalankan tanpa signature):
```kotlin
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}

android {
    namespace = "com.aryariap.forfh"
    compileSdk = 37 // library plan sendiri (Compose BOM 2026.08.00 / core 1.19.0 / lifecycle 2.11.0) menuntut >= 37 — diverifikasi di T1 (checkDebugAarMetadata)
    // targetSdk tetap 36 (edge-to-edge + izin sesuai spec)

    defaultConfig {
        applicationId = "com.aryariap.forfh"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false // V1: biarkan debug-size; R8 kandidat versi berikutnya (konsisten dgn T13)
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1")
    }
}

// Built-in Kotlin AGP 9: blok kotlin{} top-level (bukan android.kotlinOptions)
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.core)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.work.runtime)
    implementation(libs.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
```
- [ ] **7. Tulis `app/proguard-rules.pro`** — kotlinx-serialization + Retrofit/OkHttp:
```proguard
# kotlinx-serialization (Reflection-less serializer lookup)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.aryariap.forfh.**$$serializer { *; }
-keepclassmembers class com.aryariap.forfh.** { *** Companion; }
-keepclasseswithmembers class com.aryariap.forfh.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Retrofit
-keepattributes Signature, Exceptions
# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
```
- [ ] **8. Tulis `app/src/main/AndroidManifest.xml`** — izin persis dari spec: `POST_NOTIFICATIONS`, `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `USE_FULL_SCREEN_INTENT`, `VIBRATE`, `INTERNET`; receiver + FSI activity dideklarasi (activity receiver akan dilengkapi body di task masing-masing):
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <application
        android:name=".ForfhApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.ForFH">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- T9: layer alarm di atas lock screen -->
        <activity
            android:name=".alarm.FullScreenAlarmActivity"
            android:excludeFromRecents="true"
            android:exported="false"
            android:launchMode="singleTask"
            android:taskAffinity=""
            android:theme="@style/Theme.ForFH.Alarm" />

        <!-- T7: trigger alarm kuliah + tugas (intent eksplisit dari PendingIntent) -->
        <receiver
            android:name=".alarm.AlarmReceiver"
            android:exported="false" />

        <!-- T11: recovery boot & package-replaced (non-directBootAware; exported=false aman utk protected broadcast sistem) -->
        <receiver
            android:name=".sync.BootReceiver"
            android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
            </intent-filter>
        </receiver>

        <!-- T11: exact access dikembalikan setelah dicabut -->
        <receiver
            android:name=".sync.ExactAlarmPermissionReceiver"
            android:exported="false">
            <intent-filter>
                <action android:name="android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```
- [ ] **9. Tulis resource** — `res/values/strings.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">ForFH</string>
    <string name="tagline">JADWAL · TUGAS · ALARM</string>
    <string name="channel_alarm_kuliah">Alarm Kuliah</string>
    <string name="channel_reminder_tugas">Reminder Tugas</string>
</resources>
```
- `res/values/colors.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#3D5A80</color>
</resources>
```
- `res/values/themes.xml` (parent platform murni, tanpa AppCompat — layar 100% Compose):
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.ForFH" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">true</item>
    </style>
    <style name="Theme.ForFH.Alarm" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@android:color/black</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
    </style>
</resources>
```
- `res/drawable/ic_launcher_foreground.xml` (monogram F, 108dp canvas adaptive):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M40,28 L40,80 L47,80 L47,56 L64,56 L64,49 L47,49 L47,28 Z" />
</vector>
```
- `res/drawable/ic_stat_alarm.xml` (ikon small notification — siluet F putih):
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M7,3 L7,21 L9.5,21 L9.5,13.5 L15,13.5 L15,10.5 L9.5,10.5 L9.5,3 Z" />
</vector>
```
- `res/mipmap-anydpi-v26/ic_launcher.xml` (minSdk 26, jadi anydpi-v26 mencakup semua device):
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```
- [ ] **10. Tulis `.gitignore`** (keystore dan kredensial TIDAK pernah di-commit — §13):
```gitignore
*.iml
.gradle/
.idea/
.kotlin/
build/
local.properties
captures/
*.jks
*.keystore
keystore.properties
.DS_Store
```
- [ ] **11. Tulis `ForfhApp.kt`** (container diisi bertahap mulai T2):
```kotlin
package com.aryariap.forfh

import android.app.Application

class ForfhApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```
- [ ] **12. Tulis `AppContainer.kt`** (stub — bertambah di T2/T3/T4/T8/T10):
```kotlin
package com.aryariap.forfh

class AppContainer(private val app: ForfhApp)
```
- [ ] **13. Tulis `MainActivity.kt`** (minimal; diganti penuh di T12):
```kotlin
package com.aryariap.forfh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // edge-to-edge wajib (targetSdk 36)
        setContent {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "ForFH",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                    ),
                )
            }
        }
    }
}
```
- [ ] **14. Bootstrap wrapper + buktikan build hijau** (test "T1" = `assembleDebug` sukses):
```powershell
cd D:\Projects\ForFH-Android
& "D:\tools\gradle-9.5.0\bin\gradle.bat" wrapper --gradle-version 9.5.0
.\gradlew.bat :app:assembleDebug
```
  Ekspektasi: `BUILD SUCCESSFUL` dan `app/build/outputs/apk/debug/app-debug.apk` ada. Bila ada error `SDK location not found`, buat `local.properties` berisi `sdk.dir=C\:\\Users\\Arya Rizky\\AppData\\Local\\Android\\Sdk`.
- [ ] **15. Commit**: `git add -A && git commit -m "T1: scaffold gradle 9.5 + AGP 9.3 + manifest izin + bootstrap env"` (jangan push ke web repo — repo ini terpisah, §2).

---
## T2 — Room: 3 entity + 3 DAO + AppDatabase + DueDateParser

**Files:**
- Test `app/src/test/java/com/aryariap/forfh/data/db/DueDateParserTest.kt`
- Create `app/src/main/java/com/aryariap/forfh/data/db/DueDateParser.kt`
- Create `app/src/main/java/com/aryariap/forfh/data/db/ScheduleEntity.kt`
- Create `app/src/main/java/com/aryariap/forfh/data/db/TaskEntity.kt`
- Create `app/src/main/java/com/aryariap/forfh/data/db/ScheduledAlarmEntity.kt`
- Create `app/src/main/java/com/aryariap/forfh/data/db/SchedulesDao.kt`
- Create `app/src/main/java/com/aryariap/forfh/data/db/TasksDao.kt`
- Create `app/src/main/java/com/aryariap/forfh/data/db/ScheduledAlarmsDao.kt`
- Create `app/src/main/java/com/aryariap/forfh/data/db/AppDatabase.kt`
- Modify `app/src/main/java/com/aryariap/forfh/AppContainer.kt` (isi field database)

**Interfaces:**
- Consumes: — (murni baru)
- Produces:
  - `SchedulesDao`: `getAll(): Flow<List<ScheduleEntity>>`; `getAllOnce(): List<ScheduleEntity>`; `getEnabledOnce(): List<ScheduleEntity>`; `getByIdOnce(id: String): ScheduleEntity?`; `replaceAll(items: List<ScheduleEntity>)` (clear+insert, @Transaction) — dipakai T5/T8/T10.
  - `TasksDao`: `getAll(): Flow<List<TaskEntity>>`; `getById(id: String): Flow<TaskEntity?>`; `getActiveByDeadline(): List<TaskEntity>`; `updateStatus(id: String, status: String, computedStatus: String?)`; `replaceAll(items: List<TaskEntity>)` — dipakai T7/T10/T12.
  - `ScheduledAlarmsDao`: `getAll(): Flow<List<ScheduledAlarmEntity>>`; `getAllOnce()`; `getByIdOnce(id)`; `upsert(row)`; `deleteById(id)`; `clearAll()` — dipakai T7/T8/T9/T10.
  - `DueDateParser.parseToEpochMillis(value: String?): Long?` — dipakai T4 (mappers).

**Langkah:**

- [ ] **1. Tulis test DULU (harus gagal)** — `DueDateParserTest.kt`:
```kotlin
package com.aryariap.forfh.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DueDateParserTest {

    @Test
    fun `ISO string dikonversi ke epoch millis`() {
        assertEquals(1_787_281_200_000L, DueDateParser.parseToEpochMillis("2026-08-21T03:00:00.000Z")) // anchor terverifikasi .NET: 21 Agu 03:00Z
    }

    @Test
    fun `epoch ms numerik diterima apa adanya`() {
        assertEquals(1_787_281_200_000L, DueDateParser.parseToEpochMillis("1787281200000"))
    }

    @Test
    fun `null tetap null`() {
        assertNull(DueDateParser.parseToEpochMillis(null))
    }

    @Test
    fun `string tak valid menghasilkan null bukan crash`() {
        assertNull(DueDateParser.parseToEpochMillis("bukan-tanggal"))
        assertNull(DueDateParser.parseToEpochMillis("2026-13-99T99:00:00Z"))
    }
}
```
- [ ] **2. Jalankan, buktikan gagal**: `.\gradlew.bat :app:testDebugUnitTest --tests "*DueDateParserTest*"` → `Compilation error: unresolved reference DueDateParser`.
- [ ] **3. Implementasi `DueDateParser.kt`** (server mengirim ISO-8601, kadang epoch ms — lihat API contract riset):
```kotlin
package com.aryariap.forfh.data.db

import java.time.Instant

object DueDateParser {
    /** Server mengirim timestamp sebagai ISO-8601 (mis. "2026-08-20T03:00:00.000Z") atau epoch ms. Keduanya → epoch millis. */
    fun parseToEpochMillis(value: String?): Long? {
        if (value == null) return null
        return runCatching {
            if (value.all { it.isDigit() }) value.toLong() else Instant.parse(value).toEpochMilli()
        }.getOrNull()
    }
}
```
- [ ] **4. Jalankan, buktikan lulus**: perintah sama dengan langkah 2 → `BUILD SUCCESSFUL`, 4 test hijau.
- [ ] **5. Tulis 3 entity** — kolom persis spec §7 (mirror + state alarm):
`ScheduleEntity.kt`:
```kotlin
package com.aryariap.forfh.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tabel mirror GET /api/schedules — wipe-and-replace saat sync sukses. */
@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val courseName: String,
    val courseCode: String?,
    val courseColor: String,   // default "#3b82f6"
    val lecturer: String?,
    val credits: Int,          // default 2
    val dayOfWeek: Int,        // 0=Sunday .. 6=Saturday (konvensi API)
    val startTime: String,     // "HH:MM"
    val endTime: String,       // "HH:MM"
    val room: String?,
    val onlineUrl: String?,
    val enabled: Boolean,
)
```
`TaskEntity.kt`:
```kotlin
package com.aryariap.forfh.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabel mirror GET /api/tasks — wipe-and-replace saat sync sukses.
 * Kolom mengikuti spec §7; courseId & courseColor ditambah untuk filter/warna di UI (REQ-18).
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val courseId: String?,
    val courseName: String?,
    val courseCode: String?,
    val title: String,
    val description: String?,
    val dueAt: Long?,                    // epoch ms | null
    val status: String,                  // NOT_STARTED|IN_PROGRESS|REVISION|DONE|OVERDUE
    val computedStatus: String?,         // "OVERDUE" | null
    val priority: String,                // low|medium|high|urgent
    val courseColor: String?,            // dari course row API, utk badge warna
    val subtasksJson: String?,           // JSON encode List<SubtaskDto> — spec §7: detail tugas WAJIB tampilkan subtasks
)
```
`ScheduledAlarmEntity.kt`:
```kotlin
package com.aryariap.forfh.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * State alarm eksplisit — identity deterministic:
 *   "class|scheduleId|offsetMinutes|occurrenceDate"  (kuliah)
 *   "task|slot|date"                                  (tugas, slot = "09"|"15"|"20")
 * TIDAK pernah di-wipe; perubahannya hanya lewat AlarmRescheduler.
 */
@Entity(tableName = "scheduled_alarms")
data class ScheduledAlarmEntity(
    @PrimaryKey val id: String,
    val kind: String,              // "CLASS_ALARM" | "TASK_REMINDER"
    val scheduleId: String?,       // null utk task slot
    val offsetMinutes: Int,        // 0 utk task slot
    val occurrenceDate: String,    // "2026-08-17" (LocalDate WIB)
    val triggerAtMillis: Long,     // berubah saat snooze
    val snoozeCount: Int,          // reset saat row baru
)
```
- [ ] **6. Tulis 3 DAO** (wipe-and-replace via @Transaction di interface — aman untuk unit-test fake nanti):
`SchedulesDao.kt`:
```kotlin
package com.aryariap.forfh.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SchedulesDao {
    @Query("SELECT * FROM schedules ORDER BY dayOfWeek, startTime")
    fun getAll(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules")
    fun getAllOnce(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE enabled = 1")
    fun getEnabledOnce(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE id = :id")
    fun getByIdOnce(id: String): ScheduleEntity?

    @Query("DELETE FROM schedules")
    fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: List<ScheduleEntity>)

    /** wipe-and-replace — HANYA tabel mirror, HANYA saat sync sukses (invariant spec). */
    @Transaction
    suspend fun replaceAll(items: List<ScheduleEntity>) {
        clearAll()
        insertAll(items)
    }
}
```
`TasksDao.kt`:
```kotlin
package com.aryariap.forfh.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TasksDao {
    @Query("SELECT * FROM tasks ORDER BY CASE status WHEN 'DONE' THEN 1 ELSE 0 END, (dueAt IS NULL), dueAt ASC")
    fun getAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks")
    fun getAllOnce(): List<TaskEntity>

    /** Reminder tugas: status != DONE, urut deadline terdekat (dueAt ASC NULLS LAST). */
    @Query("SELECT * FROM tasks WHERE status != 'DONE' ORDER BY (dueAt IS NULL), dueAt ASC")
    fun getActiveByDeadline(): List<TaskEntity>

    /** Dipanggil HANYA setelah PUT /api/tasks/{id} sukses (invariant: server sumber kebenaran). */
    @Query("UPDATE tasks SET status = :status, computedStatus = :computedStatus WHERE id = :id")
    fun updateStatus(id: String, status: String, computedStatus: String?)

    @Query("DELETE FROM tasks")
    fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: List<TaskEntity>)

    @Transaction
    suspend fun replaceAll(items: List<TaskEntity>) {
        clearAll()
        insertAll(items)
    }
}
```
`ScheduledAlarmsDao.kt`:
```kotlin
package com.aryariap.forfh.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledAlarmsDao {
    @Query("SELECT * FROM scheduled_alarms")
    fun getAll(): Flow<List<ScheduledAlarmEntity>>

    @Query("SELECT * FROM scheduled_alarms")
    fun getAllOnce(): List<ScheduledAlarmEntity>

    @Query("SELECT * FROM scheduled_alarms WHERE id = :id")
    fun getByIdOnce(id: String): ScheduledAlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(row: ScheduledAlarmEntity)

    @Query("DELETE FROM scheduled_alarms WHERE id = :id")
    fun deleteById(id: String)

    @Query("DELETE FROM scheduled_alarms")
    fun clearAll()
}
```
- [ ] **7. Tulis `AppDatabase.kt`** — fallback destruktif sederhana (V1 masih version 1):
```kotlin
package com.aryariap.forfh.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScheduleEntity::class, TaskEntity::class, ScheduledAlarmEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schedulesDao(): SchedulesDao
    abstract fun tasksDao(): TasksDao
    abstract fun scheduledAlarmsDao(): ScheduledAlarmsDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "forfh.db")
                .fallbackToDestructiveMigration(true)
                .build()
    }
}
```
- [ ] **8. Update `AppContainer.kt`**:
```kotlin
package com.aryariap.forfh

import com.aryariap.forfh.data.db.AppDatabase

class AppContainer(private val app: ForfhApp) {
    val database: AppDatabase by lazy { AppDatabase.build(app) }
}
```
- [ ] **9. Verifikasi**: `.\gradlew.bat :app:assembleDebug` hijau (Room + KSP tercompile).
- [ ] **10. Commit**: `git add -A && git commit -m "T2: Room 3 entity + DAO wipe-and-replace + DueDateParser"`.

---

## T3 — DataStore Preferences + SecureCookieStore (Keystore AES-GCM) + SessionManager

**Files:**
- Test `app/src/test/java/com/aryariap/forfh/data/prefs/CookiePayloadCodecTest.kt`
- Test `app/src/test/java/com/aryariap/forfh/data/prefs/AlarmOffsetsTest.kt`
- Create `app/src/main/java/com/aryariap/forfh/data/prefs/AlarmOffsets.kt`
- Create `app/src/main/java/com/aryariap/forfh/data/prefs/CookiePayloadCodec.kt`
- Create `app/src/main/java/com/aryariap/forfh/data/prefs/Preferences.kt`
- Create `app/src/main/java/com/aryariap/forfh/data/prefs/SecureCookieStore.kt`
- Create `app/src/main/java/com/aryariap/forfh/data/prefs/SessionManager.kt`
- Modify `app/src/main/java/com/aryariap/forfh/AppContainer.kt`

**Interfaces:**
- Consumes: `AppDatabase` (T2)
- Produces:
  - `AlarmOffsets(offset3h, offset2h, offset1h).activeOffsets(): List<Int>` — dipakai T8 (ReconcilePlanner) dan T12 (toggles).
  - `Preferences`: `offsets: Flow<AlarmOffsets>`; `suspend fun setOffsets(o: AlarmOffsets)`; `lastSyncAt: Flow<Long>`; `lastSyncStatus: Flow<String>`; `suspend fun setLastSync(epochMillis: Long, status: String)` — dipakai T8/T10/T12.
  - `SecureCookieStore`: `suspend fun writeAll(map: Map<String, String>)`; `suspend fun readAll(): Map<String, String>?` (null = tak terbaca → auto-logout); `suspend fun clear()` — dipakai T4 (CookieJar), T10 (logout).
  - `SessionManager`: `events: SharedFlow<SessionEvent>` (`LoggedIn` / `LoggedOut(message)`); `suspend fun isLoggedIn(): Boolean`; `fun onLoggedIn()`; `fun onSessionExpired()` — dipakai T4/T10/T12.
  - `SyncStateStore` (interface, implementasi Preferences): `suspend fun setLastSync(epochMillis, status)`; `suspend fun lastSyncAt(): Long`; `suspend fun lastSyncStatus(): String` — dipakai T10 (SyncRepository testable).

**Langkah:**

- [ ] **1. Tulis test DULU (harus gagal)** — `CookiePayloadCodecTest.kt`:
```kotlin
package com.aryariap.forfh.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CookiePayloadCodecTest {

    @Test
    fun `roundtrip peta cookie`() {
        val map = mapOf(
            "usual-olwen-algojogacorbgt-a2be655b.koyeb.app" to "__Host-forfh-session=abc123; Path=/",
            "koyeb.app" to "session=xyz",
        )
        assertEquals(map, CookiePayloadCodec.decode(CookiePayloadCodec.encode(map)))
    }

    @Test
    fun `encode peta kosong menghasilkan string kosong`() {
        assertEquals("", CookiePayloadCodec.encode(emptyMap()))
    }

    @Test
    fun `decode string kosong menghasilkan peta kosong`() {
        assertTrue(CookiePayloadCodec.decode("").isEmpty())
        assertTrue(CookiePayloadCodec.decode("\n\n").isEmpty())
    }

    @Test
    fun `baris tanpa tab dibuang`() {
        val decoded = CookiePayloadCodec.decode("baris-sampah\nhost\tvalue")
        assertEquals(mapOf("host" to "value"), decoded)
    }
}
```
- [ ] **2. Tulis test DULU — `AlarmOffsetsTest.kt`**:
```kotlin
package com.aryariap.forfh.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmOffsetsTest {

    @Test
    fun `semua aktif menghasilkan 180 120 60`() {
        assertEquals(listOf(180, 120, 60), AlarmOffsets(true, true, true).activeOffsets())
    }

    @Test
    fun `hanya 2 jam aktif`() {
        assertEquals(listOf(120), AlarmOffsets(false, true, false).activeOffsets())
    }

    @Test
    fun `semua nonaktif kosong`() {
        assertEquals(emptyList<Int>(), AlarmOffsets(false, false, false).activeOffsets())
    }
}
```
- [ ] **3. Jalankan, buktikan gagal**: `.\gradlew.bat :app:testDebugUnitTest --tests "*CookiePayloadCodecTest*" --tests "*AlarmOffsetsTest*"` → unresolved reference.
- [ ] **4. Implementasi `AlarmOffsets.kt`** (spec: `alarm_offsets` 3j/2j/1j boolean):
```kotlin
package com.aryariap.forfh.data.prefs

data class AlarmOffsets(
    val offset3h: Boolean,
    val offset2h: Boolean,
    val offset1h: Boolean,
) {
    /** Offsets aktif dalam menit, urutan terbesar dulu (3j → 2j → 1j). */
    fun activeOffsets(): List<Int> = buildList {
        if (offset3h) add(180)
        if (offset2h) add(120)
        if (offset1h) add(60)
    }
}
```
- [ ] **5. Implementasi `CookiePayloadCodec.kt`**:
```kotlin
package com.aryariap.forfh.data.prefs

object CookiePayloadCodec {
    /** Encode peta host→cookie jadi satu string (baris "host\tvalue"), sebelum dienkripsi. */
    fun encode(map: Map<String, String>): String =
        map.entries.joinToString("\n") { "${it.key}\t${it.value}" }

    fun decode(payload: String): Map<String, String> = payload
        .split('\n')
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val i = line.indexOf('\t')
            if (i <= 0) null else line.substring(0, i) to line.substring(i + 1)
        }
        .toMap()
}
```
- [ ] **6. Jalankan, buktikan lulus** (4 + 3 test hijau).
- [ ] **7. Implementasi `Preferences.kt`** — DataStore Preferences + implementasi `SyncStateStore`:
```kotlin
package com.aryariap.forfh.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.aryariap.forfh.sync.SyncStateStore

class Preferences(private val dataStore: DataStore<Preferences>) : SyncStateStore {

    private val keyOffset3h = booleanPreferencesKey("alarm_offset_3h")
    private val keyOffset2h = booleanPreferencesKey("alarm_offset_2h")
    private val keyOffset1h = booleanPreferencesKey("alarm_offset_1h")
    private val keyLastSyncAt = longPreferencesKey("last_sync_at")
    private val keyLastSyncStatus = stringPreferencesKey("last_sync_status")

    val offsets: Flow<AlarmOffsets> = dataStore.data.map { p ->
        AlarmOffsets(
            offset3h = p[keyOffset3h] ?: true,
            offset2h = p[keyOffset2h] ?: true,
            offset1h = p[keyOffset1h] ?: true,
        )
    }

    suspend fun setOffsets(o: AlarmOffsets) {
        dataStore.edit { p ->
            p[keyOffset3h] = o.offset3h
            p[keyOffset2h] = o.offset2h
            p[keyOffset1h] = o.offset1h
        }
    }

    val lastSyncAt: Flow<Long> = dataStore.data.map { it[keyLastSyncAt] ?: 0L }
    val lastSyncStatus: Flow<String> = dataStore.data.map { it[keyLastSyncStatus] ?: "" }

    override suspend fun setLastSync(epochMillis: Long, status: String) {
        dataStore.edit { p ->
            p[keyLastSyncAt] = epochMillis
            p[keyLastSyncStatus] = status
        }
    }

    override suspend fun lastSyncAt(): Long = dataStore.data.first()[keyLastSyncAt] ?: 0L
    override suspend fun lastSyncStatus(): String = dataStore.data.first()[keyLastSyncStatus] ?: ""
}
```
- [ ] **8. Implementasi `SecureCookieStore.kt`** — AES-256-GCM, kunci non-exportable di Android Keystore (alias `forfh_session_key`), ciphertext + IV di DataStore; password kampus tidak pernah disimpan; fail-safe: key hilang → decrypt gagal → null → auto-logout:
```kotlin
package com.aryariap.forfh.data.prefs

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Bungkus cookie sesi: AES-256-GCM, kunci non-exportable di Android Keystore.
 * Cookie diberikan ke OkHttp CookieJar HANYA dalam memori saat runtime (§7).
 */
class SecureCookieStore(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope,
) {
    private val keyCipher = stringPreferencesKey("cookie_cipher")
    private val keyIv = stringPreferencesKey("cookie_iv")

    private val keyAlias = "forfh_session_key"

    suspend fun writeAll(map: Map<String, String>) {
        if (map.isEmpty()) { clear(); return }
        val (iv, ct) = runCatching { encrypt(CookiePayloadCodec.encode(map)) }
            .getOrElse { return } // key bermasalah → jangan simpan plaintext, jaga invariant
        dataStore.edit { p -> p[keyIv] = iv; p[keyCipher] = ct }
    }

    /** null = cookie tidak ada ATAU tak terbaca (key Keystore hilang) → alur auto-logout menangani. */
    suspend fun readAll(): Map<String, String>? {
        val p = dataStore.data.first()
        val iv = p[keyIv] ?: return null
        val ct = p[keyCipher] ?: return null
        val plain = runCatching { decrypt(iv, ct) }.getOrNull() ?: return null
        return CookiePayloadCodec.decode(plain)
    }

    suspend fun clear() {
        dataStore.edit { p -> p.remove(keyCipher); p.remove(keyIv) }
    }

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): Pair<String, String> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) to
            Base64.encodeToString(ct, Base64.NO_WRAP)
    }

    private fun decrypt(ivB64: String, ctB64: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, Base64.decode(ivB64, Base64.NO_WRAP)),
        )
        return String(cipher.doFinal(Base64.decode(ctB64, Base64.NO_WRAP)), Charsets.UTF_8)
    }
}
```
- [ ] **9. Implementasi `SessionManager.kt`**:
```kotlin
package com.aryariap.forfh.data.prefs

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

sealed interface SessionEvent {
    data object LoggedIn : SessionEvent
    data class LoggedOut(val message: String) : SessionEvent
}

class SessionManager(private val cookieStore: SecureCookieStore) {
    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<SessionEvent> = _events

    /** isLoggedIn = cookie sesi tersimpan (terdekripsi). */
    suspend fun isLoggedIn(): Boolean = cookieStore.readAll().isNullOrEmpty().not()

    fun onLoggedIn() { _events.tryEmit(SessionEvent.LoggedIn) }

    /** 401 di /api/* selain login → auto-logout (data dibersihkan oleh AppContainer.logout). */
    fun onSessionExpired() { _events.tryEmit(SessionEvent.LoggedOut("Sesi berakhir, masuk lagi.")) }
}
```
- [ ] **10. Update `AppContainer.kt`**:
```kotlin
package com.aryariap.forfh

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.aryariap.forfh.data.db.AppDatabase
import com.aryariap.forfh.data.prefs.Preferences
import com.aryariap.forfh.data.prefs.SecureCookieStore
import com.aryariap.forfh.data.prefs.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(private val app: ForfhApp) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.build(app) }

    private val dataStore: DataStore<Preferences> by lazy {
        androidx.datastore.preferences.preferenceDataStoreFactory.create(
            scope = applicationScope,
            produceFile = { app.preferencesDataStoreFile("forfh_prefs") },
        )
    }

    val prefs: Preferences by lazy { Preferences(dataStore) }
    val secureCookieStore: SecureCookieStore by lazy { SecureCookieStore(dataStore, applicationScope) }
    val sessionManager: SessionManager by lazy { SessionManager(secureCookieStore) }
}
```
- [ ] **11. Verifikasi**: `.\gradlew.bat :app:assembleDebug` + `.\gradlew.bat :app:testDebugUnitTest` hijau.
- [ ] **12. Commit**: `git add -A && git commit -m "T3: DataStore + SecureCookieStore AES-GCM Keystore + SessionManager"`.

---
## T4 — Network: DTO + Retrofit service + ApiClient (CookieJar persist, error mapping)

**Files:**
- Test `app/src/test/java/com/aryariap/forfh/network/DtoDecodeTest.kt`
- Test `app/src/test/java/com/aryariap/forfh/network/MappersTest.kt`
- Test `app/src/test/java/com/aryariap/forfh/network/LoginErrorMapperTest.kt`
- Create `app/src/main/java/com/aryariap/forfh/network/Dtos.kt`
- Create `app/src/main/java/com/aryariap/forfh/network/Mappers.kt`
- Create `app/src/main/java/com/aryariap/forfh/network/ForfhApiService.kt`
- Create `app/src/main/java/com/aryariap/forfh/network/PersistentCookieJar.kt`
- Create `app/src/main/java/com/aryariap/forfh/network/SessionExpiryInterceptor.kt`
- Create `app/src/main/java/com/aryariap/forfh/network/LoginErrorMapper.kt`
- Create `app/src/main/java/com/aryariap/forfh/network/ApiClient.kt`
- Create `app/src/main/java/com/aryariap/forfh/sync/SyncStateStore.kt` (interface, dipakai Preferences — dipindah referensi dari T3)
- Modify `app/src/main/java/com/aryariap/forfh/AppContainer.kt`

**Interfaces:**
- Consumes: `SecureCookieStore` (T3), `SessionManager` (T3), `DueDateParser` (T2), `Preferences` (T3)
- Produces:
  - `ForfhApiService` (Retrofit): `suspend fun login(@Body body: LoginRequest): Response<LoginResponse>`; `suspend fun schedules(): Response<SchedulesResponse>`; `suspend fun tasks(): Response<TasksResponse>`; `suspend fun markDone(@Path("id") id: String, @Body body: MarkDoneRequest): Response<SuccessResponse>` — dipakai T7/T10/T12.
  - `PersistentCookieJar(secureCookieStore)` (OkHttp CookieJar, cookie dari SecureCookieStore, disimpan kembali terenkripsi) — dipakai ApiClient.
  - `LoginErrorMapper.map(code: Int?, serverMessage: String?): String` + `mapNetwork(): String` — dipakai T12 (LoginScreen).
  - `SessionExpiryInterceptor` — 401 authed → `sessionManager.onSessionExpired()`.
  - Mapper DTO→Entity: `ScheduleDto.toEntity()`, `TaskDto.toEntity(nowMs: Long)`, `computeComputedStatus(...)` — dipakai T10 (SyncRepository).

**Langkah:**

- [ ] **1. Tulis test DULU (harus gagal)** — `DtoDecodeTest.kt` (fixture dari kontrak API riset):
```kotlin
package com.aryariap.forfh.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DtoDecodeTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `login response terdecode - user dan success`() {
        val r = json.decodeFromString<LoginResponse>(
            """{"success":true,"user":{"id":"u1","username":"nama","displayName":"Nama","nim":"123"}}""",
        )
        assertEquals(true, r.success)
        assertEquals("u1", r.user.id)
        assertEquals("123", r.user.nim)
    }

    @Test
    fun `schedule dto - enabled int 0-1, color default, nullable field`() {
        val r = json.decodeFromString<SchedulesResponse>(
            """{"schedules":[
                {"id":"s1","courseId":"c1","courseName":"Hukum","courseColor":"#c9a84c",
                 "lecturer":null,"credits":3,"dayOfWeek":1,"startTime":"08:00","endTime":"09:40",
                 "room":"A101","onlineUrl":null,"enabled":1},
                {"id":"s2","courseId":"c2","courseName":"Tata Negara","courseCode":"TN101",
                 "dayOfWeek":3,"startTime":"10:00","endTime":"11:40","enabled":0}
            ]}""",
        )
        assertEquals(2, r.schedules.size)
        assertEquals(1, r.schedules[0].enabled)
        assertEquals(0, r.schedules[1].enabled)
        assertEquals("#3b82f6", r.schedules[1].courseColor) // default
        assertNull(r.schedules[0].lecturer)
        assertEquals("TN101", r.schedules[1].courseCode)
    }

    @Test
    fun `task dto - dueAt ISO string, computedStatus, course dan subtasks`() {
        val r = json.decodeFromString<TasksResponse>(
            """{"tasks":[{
                "id":"t1","userId":"u1","courseId":"c1","title":"Makalah",
                "description":null,"type":"assignment","dueAt":"2026-08-20T03:00:00.000Z",
                "internalTargetAt":null,"priority":"high","estimatedMinutes":120,
                "status":"NOT_STARTED","progress":0,"source":"manual","completedAt":null,
                "deletedAt":null,"version":1,"externalId":null,
                "createdAt":"2026-08-01T03:00:00.000Z","updatedAt":"2026-08-01T03:00:00.000Z",
                "computedStatus":"OVERDUE",
                "course":{"id":"c1","userId":"u1","name":"Hukum","code":"HK101","color":"#c9a84c"},
                "subtasks":[{"id":"st1","userId":"u1","taskId":"t1","title":"Bab 1",
                             "completed":0,"orderIndex":1,"estimatedMinutes":60,"dueAt":null,
                             "deletedAt":null,"version":0,"createdAt":"2026-08-01T03:00:00.000Z",
                             "updatedAt":"2026-08-01T03:00:00.000Z"}]
            }]}""",
        )
        val t = r.tasks.single()
        assertEquals("OVERDUE", t.computedStatus)
        assertEquals("2026-08-20T03:00:00.000Z", t.dueAt)
        assertEquals("c9a84c", t.course?.color)
        assertEquals(1, t.subtasks.size)
        assertNotNull(t.course)
    }

    @Test
    fun `mark done response dan error body terdecode`() {
        val ok = json.decodeFromString<SuccessResponse>("""{"success":true}""")
        assertEquals(true, ok.success)
        val err = json.decodeFromString<ErrorBody>("""{"error":"Judul tugas wajib diisi."}""")
        assertEquals("Judul tugas wajib diisi.", err.error)
    }
}
```
- [ ] **2. Tulis test DULU — `MappersTest.kt`**:
```kotlin
package com.aryariap.forfh.network

import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {

    @Test
    fun `schedule dto ke entity - enabled int ke boolean`() {
        val dto = ScheduleDto(
            id = "s1", courseId = "c1", courseName = "Hukum", courseCode = null,
            courseColor = "", lecturer = null, credits = 2, dayOfWeek = 1,
            startTime = "08:00", endTime = "09:40", room = null, onlineUrl = null, enabled = 1,
        )
        val e: ScheduleEntity = dto.toEntity()
        assertTrue(e.enabled)
        assertEquals("#3b82f6", e.courseColor) // blank → default
    }

    @Test
    fun `task dto ke entity - computedStatus dari server, dueAt epoch`() {
        val dto = TaskDto(
            id = "t1", userId = "u1", courseId = "c1", title = "Makalah", description = null,
            type = "assignment", dueAt = "2026-08-20T03:00:00.000Z", internalTargetAt = null,
            priority = "high", estimatedMinutes = 60, status = "NOT_STARTED", progress = 0,
            source = "manual", completedAt = null, deletedAt = null, version = 1,
            externalId = null, createdAt = "2026-08-01T03:00:00.000Z",
            updatedAt = "2026-08-01T03:00:00.000Z", computedStatus = "OVERDUE",
            course = null,
            subtasks = listOf(
                SubtaskDto(
                    id = "st1", userId = "u1", taskId = "t1", title = "Bab 1",
                    createdAt = "2026-08-01T03:00:00.000Z", updatedAt = "2026-08-01T03:00:00.000Z",
                ),
            ),
        )
        val e: TaskEntity = dto.toEntity(nowMs = 1_000_000_000_000L)
        assertEquals("OVERDUE", e.computedStatus)
        assertEquals(1_787_194_800_000L, e.dueAt) // "2026-08-20T03:00:00.000Z" = 1_787_194_800_000 (terverifikasi .NET)
        // spec §7: subtasks tidak boleh hilang — disimpan JSON utk detail tugas
        assertNotNull(e.subtasksJson)
        assertTrue(e.subtasksJson!!.contains("Bab 1"))
    }

    @Test
    fun `computedStatus dihitung ulang bila server tak kirim - overdue hanya bila lewat`() {
        assertNull(computeComputedStatus("NOT_STARTED", "2026-08-20T03:00:00.000Z", nowMs = 1_786_000_000_000L))
        assertEquals("OVERDUE", computeComputedStatus("NOT_STARTED", "2026-08-20T03:00:00.000Z", nowMs = 1_788_000_000_000L))
        assertNull(computeComputedStatus("DONE", "2026-08-01T03:00:00.000Z", nowMs = 1_788_000_000_000L))
        assertNull(computeComputedStatus("NOT_STARTED", null, nowMs = 1_788_000_000_000L))
    }
}
```
- [ ] **3. Tulis test DULU — `LoginErrorMapperTest.kt`** (spec §10: 401 login → "Email atau password salah.", 429 → pesan server, network → "Gangguan koneksi, coba lagi."):
```kotlin
package com.aryariap.forfh.network

import org.junit.Assert.assertEquals
import org.junit.Test

class LoginErrorMapperTest {

    @Test
    fun `429 menampilkan pesan rate limit dari server`() {
        assertEquals(
            "Terlalu banyak percobaan. Coba lagi dalam 214 detik.",
            LoginErrorMapper.map(429, "Terlalu banyak percobaan. Coba lagi dalam 214 detik."),
        )
    }

    @Test
    fun `401 login = email atau password salah`() {
        assertEquals("Email atau password salah.", LoginErrorMapper.map(401, null))
    }

    @Test
    fun `502 dari verifikasi kampus = email atau password salah`() {
        assertEquals("Email atau password salah.", LoginErrorMapper.map(502, "Gagal verifikasi UNAIR"))
    }

    @Test
    fun `network error = gangguan koneksi`() {
        assertEquals("Gangguan koneksi, coba lagi.", LoginErrorMapper.mapNetwork())
    }
}
```
- [ ] **4. Jalankan, buktikan gagal**: `.\gradlew.bat :app:testDebugUnitTest --tests "*DtoDecodeTest*" --tests "*MappersTest*" --tests "*LoginErrorMapperTest*"` → unresolved reference.
- [ ] **5. Implementasi `Dtos.kt`** — field mengikuti kontrak API riset (perhatikan: timestamp JSON = ISO string; `enabled`/`completed` int; field tambahan diabaikan via `ignoreUnknownKeys`):
```kotlin
package com.aryariap.forfh.network

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val success: Boolean, val user: UserDto)

@Serializable
data class UserDto(val id: String, val username: String, val displayName: String, val nim: String)

@Serializable
data class SchedulesResponse(val schedules: List<ScheduleDto>)

@Serializable
data class ScheduleDto(
    val id: String,
    val courseId: String,
    val courseName: String,
    val courseCode: String? = null,
    val courseColor: String = "#3b82f6",
    val lecturer: String? = null,
    val credits: Int = 2,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val room: String? = null,
    val onlineUrl: String? = null,
    val enabled: Int, // 0|1
)

@Serializable
data class TasksResponse(val tasks: List<TaskDto>)

@Serializable
data class TaskDto(
    val id: String,
    val userId: String,
    val courseId: String? = null,
    val title: String,
    val description: String? = null,
    val type: String = "assignment",
    val dueAt: String? = null,               // ISO-8601 string dari server
    val internalTargetAt: String? = null,
    val priority: String = "medium",
    val estimatedMinutes: Int? = null,
    val status: String = "NOT_STARTED",
    val progress: Int = 0,
    val source: String = "manual",
    val completedAt: String? = null,
    val deletedAt: String? = null,
    val version: Int = 0,
    val externalId: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val computedStatus: String? = null,      // hanya ada di list endpoint
    val course: CourseDto? = null,
    val subtasks: List<SubtaskDto> = emptyList(),
)

@Serializable
data class CourseDto(
    val id: String,
    val userId: String = "",
    val name: String,
    val code: String? = null,
    val lecturer: String? = null,
    val credits: Int = 2,
    val color: String = "#3b82f6",
)

@Serializable
data class SubtaskDto(
    val id: String,
    val userId: String,
    val taskId: String,
    val title: String,
    val completed: Int = 0,
    val orderIndex: Int = 0,
    val estimatedMinutes: Int? = null,
    val dueAt: String? = null,
    val deletedAt: String? = null,
    val version: Int = 0,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class MarkDoneRequest(val status: String) // body PUT /api/tasks/{id} — app hanya pakai status (REQ-13)

@Serializable
data class SuccessResponse(val success: Boolean, val taskId: String? = null, val scheduleId: String? = null)

@Serializable
data class ErrorBody(val error: String? = null)
```
- [ ] **6. Implementasi `Mappers.kt`**:
```kotlin
package com.aryariap.forfh.network

import com.aryariap.forfh.data.db.DueDateParser
import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.TaskEntity
import kotlinx.serialization.json.Json

/** Encode subtasks ringkas (tanpa default fields) utk kolom subtasksJson. */
private val subtasksJsonCodec = Json { encodeDefaults = false }

fun ScheduleDto.toEntity(): ScheduleEntity = ScheduleEntity(
    id = id,
    courseId = courseId,
    courseName = courseName,
    courseCode = courseCode,
    courseColor = courseColor.ifBlank { "#3b82f6" },
    lecturer = lecturer,
    credits = credits,
    dayOfWeek = dayOfWeek,
    startTime = startTime,
    endTime = endTime,
    room = room,
    onlineUrl = onlineUrl,
    enabled = enabled != 0,
)

fun TaskDto.toEntity(nowMs: Long): TaskEntity = TaskEntity(
    id = id,
    courseId = courseId,
    courseName = course?.name,
    courseCode = course?.code,
    title = title,
    description = description,
    dueAt = DueDateParser.parseToEpochMillis(dueAt),
    status = status,
    computedStatus = computedStatus ?: computeComputedStatus(status, dueAt, nowMs),
    priority = priority,
    courseColor = course?.color,
    subtasksJson = subtasks.takeIf { it.isNotEmpty() }?.let { subtasksJsonCodec.encodeToString(it) },
)

/** OVERDUE dinamis: status != DONE dan dueAt di masa lalu (spesifikasi list endpoint). */
fun computeComputedStatus(status: String, dueAtIso: String?, nowMs: Long): String? {
    if (status == "DONE") return null
    val due = DueDateParser.parseToEpochMillis(dueAtIso) ?: return null
    return if (due < nowMs) "OVERDUE" else null
}
```
- [ ] **7. Implementasi `ForfhApiService.kt`**:
```kotlin
package com.aryariap.forfh.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ForfhApiService {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @GET("api/schedules")
    suspend fun schedules(): Response<SchedulesResponse>

    @GET("api/tasks")
    suspend fun tasks(): Response<TasksResponse>

    @PUT("api/tasks/{id}")
    suspend fun markDone(@Path("id") id: String, @Body body: MarkDoneRequest): Response<SuccessResponse>
}
```
- [ ] **8. Implementasi `SyncStateStore.kt`** (interface agar SyncRepository bisa di-unit-test):
```kotlin
package com.aryariap.forfh.sync

interface SyncStateStore {
    suspend fun setLastSync(epochMillis: Long, status: String)
    suspend fun lastSyncAt(): Long
    suspend fun lastSyncStatus(): String
}
```
- [ ] **9. Implementasi `PersistentCookieJar.kt`** — cookie hanya hidup di memori saat runtime; persistensi via SecureCookieStore (terenkripsi):
```kotlin
package com.aryariap.forfh.network

import com.aryariap.forfh.data.prefs.SecureCookieStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

class PersistentCookieJar(
    private val secureCookieStore: SecureCookieStore,
    private val scope: CoroutineScope,
) : CookieJar {

    // host -> string cookie (header value) — HANYA dalam memori saat runtime
    private val cookies = ConcurrentHashMap<String, String>()

    init {
        scope.launch { secureCookieStore.readAll()?.let { cookies.putAll(it) } }
    }

    override fun saveFromResponse(url: HttpUrl, cookieList: List<Cookie>) {
        if (cookieList.isEmpty()) return
        val value = cookieList.joinToString("; ") { it.toString() }
        cookies[url.host] = value
        scope.launch { secureCookieStore.writeAll(cookies.toMap()) }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val value = cookies[url.host] ?: return emptyList()
        return runCatching { Cookie.parseAll(url, value) }.getOrDefault(emptyList())
    }
}
```
- [ ] **10. Implementasi `SessionExpiryInterceptor.kt`** — 401 di `/api/*` SELAIN login = sesi habis → auto-logout (spec §4, §10):
```kotlin
package com.aryariap.forfh.network

import com.aryariap.forfh.data.prefs.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class SessionExpiryInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code == 401 && !request.url.encodedPath.startsWith("/api/auth/login")) {
            sessionManager.onSessionExpired()
        }
        return response
    }
}
```
- [ ] **11. Implementasi `LoginErrorMapper.kt`**:
```kotlin
package com.aryariap.forfh.network

object LoginErrorMapper {
    const val WRONG_CREDENTIALS = "Email atau password salah."
    const val NETWORK = "Gangguan koneksi, coba lagi."

    /** HTTP error login: 429 → pesan rate limit server; lainnya (400/401/502 kampus) → kredensial salah. */
    fun map(code: Int?, serverMessage: String?): String = when (code) {
        429 -> serverMessage?.takeIf { it.isNotBlank() } ?: "Terlalu banyak percobaan. Coba lagi nanti."
        else -> WRONG_CREDENTIALS
    }

    fun mapNetwork(): String = NETWORK
}
```
- [ ] **12. Implementasi `ApiClient.kt`**:
```kotlin
package com.aryariap.forfh.network

import com.aryariap.forfh.data.prefs.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinxserialization.asConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // 1 deploy Koyeb yang ada — nol deploy baru, nol perubahan server (spec §2, §4)
    private const val BASE_URL = "https://usual-olwen-algojogacorbgt-a2be655b.koyeb.app/"

    fun build(cookieJar: PersistentCookieJar, sessionManager: SessionManager): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(SessionExpiryInterceptor(sessionManager))
            .addInterceptor(logging)
            .build()
    }

    fun retrofit(okHttpClient: OkHttpClient): ForfhApiService {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ForfhApiService::class.java)
    }
}
```
- [ ] **13. Update `AppContainer.kt`**:
```kotlin
package com.aryariap.forfh

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.aryariap.forfh.data.db.AppDatabase
import com.aryariap.forfh.data.prefs.Preferences
import com.aryariap.forfh.data.prefs.SecureCookieStore
import com.aryariap.forfh.data.prefs.SessionManager
import com.aryariap.forfh.network.ApiClient
import com.aryariap.forfh.network.ForfhApiService
import com.aryariap.forfh.network.PersistentCookieJar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(private val app: ForfhApp) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.build(app) }

    private val dataStore: DataStore<Preferences> by lazy {
        androidx.datastore.preferences.preferenceDataStoreFactory.create(
            scope = applicationScope,
            produceFile = { app.preferencesDataStoreFile("forfh_prefs") },
        )
    }

    val prefs: Preferences by lazy { Preferences(dataStore) }
    val secureCookieStore: SecureCookieStore by lazy { SecureCookieStore(dataStore, applicationScope) }
    val sessionManager: SessionManager by lazy { SessionManager(secureCookieStore) }

    private val cookieJar: PersistentCookieJar by lazy {
        PersistentCookieJar(secureCookieStore, applicationScope)
    }

    val apiService: ForfhApiService by lazy {
        ApiClient.retrofit(ApiClient.build(cookieJar, sessionManager))
    }
}
```
- [ ] **14. Jalankan test, buktikan lulus**: `.\gradlew.bat :app:testDebugUnitTest` → DtoDecode (4) + Mappers (3) + LoginErrorMapper (4) hijau; `assembleDebug` hijau.
- [ ] **15. Commit**: `git add -A && git commit -m "T4: network DTO + Retrofit + CookieJar persist + error mapping"`.

---

## T5 — AlarmPlanner (math next occurrence WIB, murni)

**Files:**
- Test `app/src/test/java/com/aryariap/forfh/alarm/AlarmPlannerTest.kt`
- Create `app/src/main/java/com/aryariap/forfh/alarm/AlarmPlanner.kt`

**Interfaces:**
- Consumes: — (java.time; minSdk 26 sudah punya java.time native)
- Produces:
  - `AlarmPlanner(zone: ZoneId = Asia/Jakarta)` dengan:
    - `nextClassOccurrence(scheduleId, dayOfWeek 0..6, startTime "HH:MM", offsetMinutes, now: ZonedDateTime): ClassOccurrence(identity, occurrenceDate: LocalDate, startDateTime, triggerAtMillis)`
    - `nextTaskSlot(slotHour: Int, now: ZonedDateTime): Pair<LocalDate, Long>` (hari ini bila future, besok bila sudah lewat)
    - `startDateTimeFor(occurrenceDate: String, startTime: String): ZonedDateTime`
    - companion: `classIdentity(...)`, `taskIdentity(slotHour, date)` — dipakai T7/T8/T9.

**Langkah:**

- [ ] **1. Tulis test DULU (harus gagal)** — kasus persis spec §12:
```kotlin
package com.aryariap.forfh.alarm

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AlarmPlannerTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val planner = AlarmPlanner(zone)

    private fun wib(s: String): ZonedDateTime =
        LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")).atZone(zone)

    private fun wibEpoch(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `Senin 0800 offset 2 jam - trigger Senin 0600`() {
        val now = wib("2026-08-15T10:00") // Sabtu
        val occ = planner.nextClassOccurrence("s1", dayOfWeek = 1, startTime = "08:00", offsetMinutes = 120, now = now)
        assertEquals(LocalDate.of(2026, 8, 17), occ.occurrenceDate)
        assertEquals("class|s1|120|2026-08-17", occ.identity)
        assertEquals(wibEpoch(2026, 8, 17, 6, 0), occ.triggerAtMillis) // Senin 06:00 WIB (start 08:00 − 120 mnt)
    }

    @Test
    fun `trigger hari ini sudah lewat - lompat ke minggu berikutnya`() {
        val now = wib("2026-08-17T07:00") // Senin 07:00, trigger-nya Senin 06:00 sudah lewat
        val occ = planner.nextClassOccurrence("s1", dayOfWeek = 1, startTime = "08:00", offsetMinutes = 120, now = now)
        assertEquals(LocalDate.of(2026, 8, 24), occ.occurrenceDate)
        assertEquals(wibEpoch(2026, 8, 24, 6, 0), occ.triggerAtMillis) // Senin 24 06:00 WIB
    }

    @Test
    fun `persis sama dengan now dianggap lewat`() {
        val now = wib("2026-08-17T06:00") // Senin 06:00 == trigger
        val occ = planner.nextClassOccurrence("s1", dayOfWeek = 1, startTime = "08:00", offsetMinutes = 120, now = now)
        assertEquals(LocalDate.of(2026, 8, 24), occ.occurrenceDate)
    }

    @Test
    fun `lintas minggu - jadwal Selasa dihitung dari Kamis`() {
        val now = wib("2026-08-13T09:00") // Kamis
        val occ = planner.nextClassOccurrence("s1", dayOfWeek = 2, startTime = "07:00", offsetMinutes = 60, now = now)
        assertEquals(LocalDate.of(2026, 8, 18), occ.occurrenceDate) // Selasa berikutnya
        assertEquals(wibEpoch(2026, 8, 18, 6, 0), occ.triggerAtMillis)
    }

    @Test
    fun `pergantian tanggal - malam Senin jadwal Selasa pagi`() {
        val now = wib("2026-08-17T23:30") // Senin 23:30
        val occ = planner.nextClassOccurrence("s1", dayOfWeek = 2, startTime = "07:00", offsetMinutes = 60, now = now)
        assertEquals(LocalDate.of(2026, 8, 18), occ.occurrenceDate)
        assertEquals(wibEpoch(2026, 8, 18, 6, 0), occ.triggerAtMillis)
    }

    @Test
    fun `hari Minggu = dayOfWeek 0`() {
        val now = wib("2026-08-15T10:00") // Sabtu
        val occ = planner.nextClassOccurrence("s0", dayOfWeek = 0, startTime = "09:00", offsetMinutes = 0, now = now)
        assertEquals(LocalDate.of(2026, 8, 16), occ.occurrenceDate)
        assertEquals(wibEpoch(2026, 8, 16, 9, 0), occ.triggerAtMillis)
    }

    @Test
    fun `slot tugas - 09 pagi sudah lewat berarti besok, 15 siang masih hari ini`() {
        val now = wib("2026-08-15T10:00")
        val (date09, t09) = planner.nextTaskSlot(9, now)
        val (date15, t15) = planner.nextTaskSlot(15, now)
        assertEquals(LocalDate.of(2026, 8, 16), date09)
        assertEquals(wibEpoch(2026, 8, 16, 9, 0), t09)
        assertEquals(LocalDate.of(2026, 8, 15), date15)
        assertEquals(wibEpoch(2026, 8, 15, 15, 0), t15)
    }

    @Test
    fun `identity tugas - format task|slot|date`() {
        assertEquals("task|09|2026-08-15", AlarmPlanner.taskIdentity(9, LocalDate.of(2026, 8, 15)))
        assertEquals("task|20|2026-08-15", AlarmPlanner.taskIdentity(20, LocalDate.of(2026, 8, 15)))
    }
}
```
- [ ] **2. Jalankan, buktikan gagal**: `.\gradlew.bat :app:testDebugUnitTest --tests "*AlarmPlannerTest*"` → unresolved reference.
- [ ] **3. Implementasi `AlarmPlanner.kt`** (WIB eksplisit — spec §8.8):
```kotlin
package com.aryariap.forfh.alarm

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class ClassOccurrence(
    val identity: String,
    val occurrenceDate: LocalDate,
    val startDateTime: ZonedDateTime,
    val triggerAtMillis: Long,
)

/** Matematika next occurrence — murni, tanpa Android, bisa unit-test. Semua perhitungan WIB eksplisit. */
class AlarmPlanner(private val zone: ZoneId = ZoneId.of("Asia/Jakarta")) {

    companion object {
        const val CLASS_PREFIX = "class"
        const val TASK_PREFIX = "task"
        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        fun classIdentity(scheduleId: String, offsetMinutes: Int, occurrenceDate: LocalDate): String =
            "$CLASS_PREFIX|$scheduleId|$offsetMinutes|${occurrenceDate.format(DATE_FMT)}"

        fun taskIdentity(slotHour: Int, date: LocalDate): String =
            "$TASK_PREFIX|${slotHour.toString().padStart(2, '0')}|${date.format(DATE_FMT)}"
    }

    /**
     * Next occurrence kuliah: LocalDate + DayOfWeek(dayOfWeek) + LocalTime(startTime) → ZonedDateTime WIB.
     * Jika trigger (start − offset) <= now → lompat ke minggu berikutnya (spec §8.2).
     */
    fun nextClassOccurrence(
        scheduleId: String,
        dayOfWeek: Int,      // 0=Sunday .. 6=Saturday (konvensi API ForFH)
        startTime: String,   // "HH:MM"
        offsetMinutes: Int,
        now: ZonedDateTime,
    ): ClassOccurrence {
        require(dayOfWeek in 0..6) { "dayOfWeek harus 0..6" }
        val start = LocalTime.parse(startTime)
        val javaDay = DayOfWeek.of((dayOfWeek + 6) % 7 + 1)
        var date = now.toLocalDate().plusDays(((javaDay.value - now.dayOfWeek.value + 7) % 7).toLong())
        var trigger = date.atTime(start).atZone(zone).toInstant().toEpochMilli() - offsetMinutes * 60_000L
        if (trigger <= now.toInstant().toEpochMilli()) {
            date = date.plusWeeks(1)
            trigger = date.atTime(start).atZone(zone).toInstant().toEpochMilli() - offsetMinutes * 60_000L
        }
        return ClassOccurrence(
            identity = classIdentity(scheduleId, offsetMinutes, date),
            occurrenceDate = date,
            startDateTime = date.atTime(start).atZone(zone),
            triggerAtMillis = trigger,
        )
    }

    /** Slot tugas one-shot: hari ini bila trigger masih future, kalau tidak besok. */
    fun nextTaskSlot(slotHour: Int, now: ZonedDateTime): Pair<LocalDate, Long> {
        var date = now.toLocalDate()
        var trigger = date.atTime(slotHour, 0).atZone(zone).toInstant().toEpochMilli()
        if (trigger <= now.toInstant().toEpochMilli()) {
            date = date.plusDays(1)
            trigger = date.atTime(slotHour, 0).atZone(zone).toInstant().toEpochMilli()
        }
        return date to trigger
    }

    /** Rekonstruksi waktu mulai dari Room + occurrenceDate (WIB) — dipakai guard receiver. */
    fun startDateTimeFor(occurrenceDate: String, startTime: String): ZonedDateTime =
        LocalDate.parse(occurrenceDate, DATE_FMT).atTime(LocalTime.parse(startTime)).atZone(zone)
}
```
- [ ] **4. Jalankan, buktikan lulus** (9 test hijau).
- [ ] **5. Commit**: `git add -A && git commit -m "T5: AlarmPlanner next occurrence WIB + test lengkap"`.

---
## T6 — AlarmScheduler + StableHash + AndroidAlarmApi (exact/fallback)

**Files:**
- Test `app/src/test/java/com/aryariap/forfh/alarm/StableHashTest.kt`
- Test `app/src/test/java/com/aryariap/forfh/alarm/AlarmSchedulerTest.kt`
- Create `app/src/main/java/com/aryariap/forfh/alarm/StableHash.kt`
- Create `app/src/main/java/com/aryariap/forfh/alarm/AlarmScheduler.kt` (interface `AlarmApi`)
- Create `app/src/main/java/com/aryariap/forfh/alarm/AndroidAlarmApi.kt` (impl Android, reference `AlarmReceiver` — file dibuat di T7)

**Interfaces:**
- Consumes: `ScheduledAlarmEntity` (T2)
- Produces:
  - `AlarmScheduler(alarmApi: AlarmApi, stableHash: (String) -> Int)`: `schedule(row: ScheduledAlarmEntity)` (cek `canScheduleExact()` → `setExactAndAllowWhileIdle` / `setWindow(trigger, 10 menit)`; extras `scheduleId, offsetMinutes, occurrenceDate, triggerAtMillis`; requestCode `stableHash(row.id)`); `cancel(row)` — dipakai T7/T8/T9/T10.
  - `AndroidAlarmApi(context)` — impl AlarmManager + PendingIntent `FLAG_IMMUTABLE`, `RTC_WAKEUP`, target `AlarmReceiver`.
  - `StableHash.of(identity: String): Int` — deterministic (String.hashCode dijamin Java), non-negatif (aman untuk notificationId).

**Langkah:**

- [ ] **1. Tulis test DULU (harus gagal)** — `StableHashTest.kt`:
```kotlin
package com.aryariap.forfh.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StableHashTest {

    @Test
    fun `deterministik - nilai sama utk identity sama`() {
        val id = "class|s1|120|2026-08-17"
        assertEquals(StableHash.of(id), StableHash.of(id))
    }

    @Test
    fun `identity berbeda menghasilkan requestCode berbeda`() {
        assertNotEquals(
            StableHash.of("class|s1|120|2026-08-17"),
            StableHash.of("class|s1|120|2026-08-24"),
        )
    }

    @Test
    fun `selalu non-negatif - aman utk notificationId`() {
        assertTrue(StableHash.of("task|09|2026-08-15") >= 0)
        assertTrue(StableHash.of("class|s1|180|2026-08-17") >= 0)
    }
}
```
- [ ] **2. Tulis test DULU — `AlarmSchedulerTest.kt`** (fake AlarmApi; exact revoke/restore persis spec §12):
```kotlin
package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSchedulerTest {

    class FakeAlarmApi(var exactAvailable: Boolean) : AlarmApi {
        val calls = mutableListOf<String>()
        var lastExtras: Map<String, String>? = null

        override fun canScheduleExact(): Boolean = exactAvailable
        override fun setExactAndAllowWhileIdle(triggerAtMillis: Long, requestCode: Int, extras: Map<String, String>) {
            calls += "exact:$requestCode:$triggerAtMillis"
            lastExtras = extras
        }
        override fun setWindow(triggerAtMillis: Long, windowLengthMillis: Long, requestCode: Int, extras: Map<String, String>) {
            calls += "window:$requestCode:$triggerAtMillis:$windowLengthMillis"
            lastExtras = extras
        }
        override fun cancel(requestCode: Int) {
            calls += "cancel:$requestCode"
        }
    }

    private fun classRow(trigger: Long, snoozeCount: Int = 0) = ScheduledAlarmEntity(
        id = "class|s1|120|2026-08-17",
        kind = "CLASS_ALARM",
        scheduleId = "s1",
        offsetMinutes = 120,
        occurrenceDate = "2026-08-17",
        triggerAtMillis = trigger,
        snoozeCount = snoozeCount,
    )

    @Test
    fun `exact tersedia - setExactAndAllowWhileIdle dipanggil dengan requestCode stableHash`() {
        val api = FakeAlarmApi(exactAvailable = true)
        val scheduler = AlarmScheduler(api)
        val row = classRow(trigger = 1_750_000_000_000L)
        scheduler.schedule(row)
        assertEquals(
            listOf("exact:${StableHash.of(row.id)}:1750000000000"),
            api.calls,
        )
    }

    @Test
    fun `exact dicabut - setWindow dengan windowLength 10 menit dari trigger`() {
        val api = FakeAlarmApi(exactAvailable = false)
        val scheduler = AlarmScheduler(api)
        val row = classRow(trigger = 1_750_000_000_000L)
        scheduler.schedule(row)
        assertEquals(
            listOf("window:${StableHash.of(row.id)}:1750000000000:600000"),
            api.calls,
        )
    }

    @Test
    fun `restore exact - kembali setExactAndAllowWhileIdle dengan trigger tersimpan (snooze)`() {
        val api = FakeAlarmApi(exactAvailable = false)
        val scheduler = AlarmScheduler(api)
        val row = classRow(trigger = 1_750_000_000_000L, snoozeCount = 2)
        scheduler.schedule(row)
        api.exactAvailable = true
        scheduler.schedule(row) // rescheduleAll setelah exact dikembalikan — trigger snooze dipertahankan
        assertEquals("exact:${StableHash.of(row.id)}:1750000000000", api.calls.last())
    }

    @Test
    fun `extras membawa identity fields - receiver tidak menebak-nebak`() {
        val api = FakeAlarmApi(exactAvailable = true)
        AlarmScheduler(api).schedule(classRow(trigger = 1_750_000_000_000L))
        val extras = api.lastExtras!!
        assertEquals("s1", extras["scheduleId"])
        assertEquals("120", extras["offsetMinutes"])
        assertEquals("2026-08-17", extras["occurrenceDate"])
        assertEquals("1750000000000", extras["triggerAtMillis"])
    }

    @Test
    fun `cancel memakai requestCode yang sama dengan schedule`() {
        val api = FakeAlarmApi(exactAvailable = true)
        val scheduler = AlarmScheduler(api)
        val row = classRow(trigger = 1_750_000_000_000L)
        scheduler.schedule(row)
        scheduler.cancel(row)
        assertEquals("cancel:${StableHash.of(row.id)}", api.calls.last())
    }

    @Test
    fun `fallback window tidak lebih kecil dari 10 menit`() {
        val api = FakeAlarmApi(exactAvailable = false)
        AlarmScheduler(api).schedule(classRow(trigger = 1_750_000_000_000L))
        assertTrue(api.calls.last().endsWith(":600000"))
    }
}
```
- [ ] **3. Jalankan, buktikan gagal**: `.\gradlew.bat :app:testDebugUnitTest --tests "*StableHashTest*" --tests "*AlarmSchedulerTest*"` → unresolved reference.
- [ ] **4. Implementasi `StableHash.kt`**:
```kotlin
package com.aryariap.forfh.alarm

object StableHash {
    /**
     * RequestCode PendingIntent & notificationId — deterministic lintas proses
     * (String.hashCode dijamin spesifikasi Java), non-negatif (notificationId wajib >= 0).
     */
    fun of(identity: String): Int = identity.hashCode() and 0x7FFFFFFF
}
```
- [ ] **5. Implementasi `AlarmScheduler.kt`** (satu-satunya pintu pasang/cancel AlarmManager — spec §8.1):
```kotlin
package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduledAlarmEntity

/** Abstraksi AlarmManager agar bisa di-unit-test dengan fake (tanpa Robolectric). */
interface AlarmApi {
    fun canScheduleExact(): Boolean
    fun setExactAndAllowWhileIdle(triggerAtMillis: Long, requestCode: Int, extras: Map<String, String>)
    fun setWindow(triggerAtMillis: Long, windowLengthMillis: Long, requestCode: Int, extras: Map<String, String>)
    fun cancel(requestCode: Int)
}

class AlarmScheduler(
    private val alarmApi: AlarmApi,
    private val stableHash: (String) -> Int = StableHash::of,
) {
    companion object {
        /** Fallback window: jendela mulai DARI triggerAtMillis (bukan "±10 menit" — istilah itu dilarang, §8.3). */
        const val FALLBACK_WINDOW_MS = 10 * 60 * 1000L
    }

    /** Exact bila tersedia, fallback setWindow bila tidak. RTC_WAKEUP di sisi impl (AndroidAlarmApi). */
    fun schedule(row: ScheduledAlarmEntity) {
        val requestCode = stableHash(row.id)
        val extras = mapOf(
            "scheduleId" to (row.scheduleId ?: ""),
            "offsetMinutes" to row.offsetMinutes.toString(),
            "occurrenceDate" to row.occurrenceDate,
            "triggerAtMillis" to row.triggerAtMillis.toString(),
        )
        if (alarmApi.canScheduleExact()) {
            alarmApi.setExactAndAllowWhileIdle(row.triggerAtMillis, requestCode, extras)
        } else {
            alarmApi.setWindow(row.triggerAtMillis, FALLBACK_WINDOW_MS, requestCode, extras)
        }
    }

    fun cancel(row: ScheduledAlarmEntity) {
        alarmApi.cancel(stableHash(row.id))
    }
}
```
- [ ] **6. Implementasi `AndroidAlarmApi.kt`** (impl produksi; PendingIntent `FLAG_IMMUTABLE`, extras dibawa ke `AlarmReceiver`):
```kotlin
package com.aryariap.forfh.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class AndroidAlarmApi(private val context: Context) : AlarmApi {

    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun canScheduleExact(): Boolean = am.canScheduleExactAlarms()

    override fun setExactAndAllowWhileIdle(triggerAtMillis: Long, requestCode: Int, extras: Map<String, String>) {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent(requestCode, extras))
    }

    override fun setWindow(triggerAtMillis: Long, windowLengthMillis: Long, requestCode: Int, extras: Map<String, String>) {
        am.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMillis, windowLengthMillis, pendingIntent(requestCode, extras))
    }

    override fun cancel(requestCode: Int) {
        am.cancel(pendingIntent(requestCode, emptyMap()))
    }

    private fun pendingIntent(requestCode: Int, extras: Map<String, String>): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        extras.forEach { (k, v) -> intent.putExtra(k, v) }
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}
```
  > Catatan: import `Build` tidak dipakai di file ini — jangan disalin (tidak ada placeholder; hapus import bila terlanjur).
- [ ] **7. Jalankan, buktikan lulus** (9 test hijau). `AlarmReceiver` belum ada → buat stub sementara dengan isi:
```kotlin
package com.aryariap.forfh.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit // diisi penuh di T7
}
```
- [ ] **8. Commit**: `git add -A && git commit -m "T6: AlarmScheduler exact/fallback + stableHash + AndroidAlarmApi"`.

---

## T7 — ReceiverGuard + Snooze + TaskReminderText + AlarmReceiver + Notifications

**Files:**
- Test `app/src/test/java/com/aryariap/forfh/alarm/ReceiverGuardTest.kt`
- Test `app/src/test/java/com/aryariap/forfh/alarm/SnoozeCounterTest.kt`
- Test `app/src/test/java/com/aryariap/forfh/alarm/TaskReminderTextTest.kt`
- Test `app/src/test/java/com/aryariap/forfh/alarm/ClassAlarmTextTest.kt`
- Create `app/src/main/java/com/aryariap/forfh/alarm/SnoozeCounter.kt`
- Create `app/src/main/java/com/aryariap/forfh/alarm/TaskReminderText.kt`
- Create `app/src/main/java/com/aryariap/forfh/alarm/ClassAlarmText.kt`
- Create `app/src/main/java/com/aryariap/forfh/alarm/ReceiverGuard.kt`
- Create `app/src/main/java/com/aryariap/forfh/alarm/ForfhNotifications.kt`
- Create `app/src/main/java/com/aryariap/forfh/alarm/AlarmFlowHandler.kt`
- Create (ganti stub) `app/src/main/java/com/aryariap/forfh/alarm/AlarmReceiver.kt`

**Interfaces:**
- Consumes: `AlarmPlanner` (T5), `AlarmScheduler` (T6), `ScheduledAlarmsDao`/`SchedulesDao`/`TasksDao` (T2), `Preferences` (T3), `SessionManager` (T3)
- Produces:
  - `ReceiverGuard.evaluate(input: GuardInput): GuardResult` — pure, dipakai AlarmFlowHandler + test.
  - `SnoozeCounter.{MAX_SNOOZE, SNOOZE_MS, canSnooze, nextTrigger, nextCount}` — dipakai T9.
  - `TaskReminderText.build(tasks: List<TaskEntity>, slotHour: Int): String?` — dipakai AlarmFlowHandler.
  - `ClassAlarmText.buildTitle/buildBody(schedule): String` — dipakai ForfhNotifications + T9.
  - `ForfhNotifications(context)`: `hasPermission(): Boolean`; `ensureChannels()`; `showClassAlarm(display, snoozeAvailable)` (FSI atau heads-up fallback); `showTaskReminder(text)` — dipakai T9/T12.
  - `AlarmFlowHandler` (dieksekusi dari receiver): `handleClassAlarm(intent)`, `handleTaskReminder(intent)`, `snooze(identity): Boolean` — dipakai T9 (FSI activity).

**Langkah:**

- [ ] **1. Tulis test DULU (harus gagal)** — `SnoozeCounterTest.kt` (spec §8.6):
```kotlin
package com.aryariap.forfh.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnoozeCounterTest {

    @Test
    fun `maksimal 5 kali - snooze 0 sampai 4 diizinkan`() {
        assertTrue(SnoozeCounter.canSnooze(0))
        assertTrue(SnoozeCounter.canSnooze(4))
        assertFalse(SnoozeCounter.canSnooze(5))
        assertFalse(SnoozeCounter.canSnooze(6))
    }

    @Test
    fun `tiap snooze menambah tepat 3 menit`() {
        assertEquals(1_000_000L + 180_000L, SnoozeCounter.nextTrigger(1_000_000L))
        assertEquals(1, SnoozeCounter.nextCount(0))
        assertEquals(5, SnoozeCounter.nextCount(4))
    }

    @Test
    fun `konstanta - 5 kali dan 3 menit dalam millis`() {
        assertEquals(5, SnoozeCounter.MAX_SNOOZE)
        assertEquals(3 * 60 * 1000L, SnoozeCounter.SNOOZE_MS)
    }
}
```
- [ ] **2. Tulis test DULU — `TaskReminderTextTest.kt`** (spec §8.7: format, ≤2 judul, "+K lagi", slot 09:00 saja utk "tidak ada tugas"):
```kotlin
package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskReminderTextTest {

    private fun task(id: String, title: String, dueAt: Long?) = TaskEntity(
        id = id, courseId = null, courseName = null, courseCode = null, title = title,
        description = null, dueAt = dueAt, status = "NOT_STARTED", computedStatus = null,
        priority = "medium", courseColor = null,
    )

    @Test
    fun `nol tugas - hanya slot 09 yang menampilkan perayaan`() {
        assertEquals("🎉 Tidak ada tugas hari ini — selamat beraktivitas!", TaskReminderText.build(emptyList(), 9))
        assertNull(TaskReminderText.build(emptyList(), 15))
        assertNull(TaskReminderText.build(emptyList(), 20))
    }

    @Test
    fun `satu tugas`() {
        assertEquals("📚 1 tugas belum selesai: Makalah Hukum", TaskReminderText.build(listOf(task("t1", "Makalah Hukum", null)), 9))
    }

    @Test
    fun `dua tugas`() {
        assertEquals(
            "📚 2 tugas belum selesai: Makalah Hukum, Kuis Bab 2",
            TaskReminderText.build(listOf(task("t1", "Makalah Hukum", 1L), task("t2", "Kuis Bab 2", 2L)), 15),
        )
    }

    @Test
    fun `tiga tugas atau lebih - format K lagi`() {
        val tasks = listOf(
            task("t1", "A", 1L), task("t2", "B", 2L), task("t3", "C", 3L), task("t4", "D", 4L),
        )
        assertEquals("📚 4 tugas belum selesai: A, B, +2 lagi", TaskReminderText.build(tasks, 9))
    }

    @Test
    fun `urutan deadline terdekat - dueAt null paling akhir`() {
        val late = task("late", "Tanpa deadline", null)
        val soon = task("soon", "Deadline besok", 1L)
        assertEquals("📚 2 tugas belum selesai: Deadline besok, Tanpa deadline", TaskReminderText.build(listOf(late, soon), 9))
    }
}
```
- [ ] **3. Tulis test DULU — `ReceiverGuardTest.kt`** (spec §8.4 + §12 — semua cabang guard):
```kotlin
package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiverGuardTest {

    private val zone = java.time.ZoneId.of("Asia/Jakarta")

    private fun schedule(enabled: Boolean = true, startTime: String = "08:00") = ScheduleEntity(
        id = "s1", courseId = "c1", courseName = "Hukum", courseCode = null,
        courseColor = "#c9a84c", lecturer = null, credits = 2, dayOfWeek = 1,
        startTime = startTime, endTime = "09:40", room = "A101", onlineUrl = null, enabled = enabled,
    )

    // Senin 2026-08-17 08:00 WIB
    private val startEpoch = java.time.ZonedDateTime.of(2026, 8, 17, 8, 0, 0, 0, zone).toInstant().toEpochMilli()
    private val triggerEpoch = startEpoch - 120 * 60_000L

    private fun row(trigger: Long = triggerEpoch, snoozeCount: Int = 0) = ScheduledAlarmEntity(
        id = "class|s1|120|2026-08-17", kind = "CLASS_ALARM", scheduleId = "s1",
        offsetMinutes = 120, occurrenceDate = "2026-08-17", triggerAtMillis = trigger, snoozeCount = snoozeCount,
    )

    private fun input(
        loggedIn: Boolean = true, sched: ScheduleEntity? = schedule(), r: ScheduledAlarmEntity? = row(),
        extraTrigger: Long = triggerEpoch, now: Long = triggerEpoch - 60_000L, notifOk: Boolean = true,
    ) = GuardInput(
        isLoggedIn = loggedIn, schedule = sched, row = r,
        extrasTriggerAtMillis = extraTrigger, nowEpochMillis = now, hasNotificationPermission = notifOk,
    )

    @Test
    fun `guard lengkap lulus - Show dengan startDateTime`() {
        val result = ReceiverGuard.evaluate(input())
        assertEquals(GuardResult.Show(schedule(), startEpoch, row()), result)
    }

    @Test
    fun `tidak login - SkipCancel (cancel alarm ini)`() {
        assertEquals(GuardResult.SkipCancel, ReceiverGuard.evaluate(input(loggedIn = false)))
    }

    @Test
    fun `jadwal hilang atau dinonaktifkan - skip silent`() {
        assertEquals(GuardResult.SkipSilent, ReceiverGuard.evaluate(input(sched = null)))
        assertEquals(GuardResult.SkipSilent, ReceiverGuard.evaluate(input(sched = schedule(enabled = false))))
    }

    @Test
    fun `row identity hilang - skip silent (jadwal diubah setelah alarm terpasang)`() {
        assertEquals(GuardResult.SkipSilent, ReceiverGuard.evaluate(input(r = null)))
    }

    @Test
    fun `triggerAtMillis tidak cocok dengan extras - skip silent (stale)`() {
        assertEquals(GuardResult.SkipSilent, ReceiverGuard.evaluate(input(extraTrigger = triggerEpoch + 180_000L)))
    }

    @Test
    fun `now >= startDateTime - skip silent (alarm basi keluar dari Doze)`() {
        assertEquals(GuardResult.SkipSilent, ReceiverGuard.evaluate(input(now = startEpoch + 1L)))
    }

    @Test
    fun `POST_NOTIFICATIONS ditolak - silent, tidak crash`() {
        assertEquals(GuardResult.SkipSilent, ReceiverGuard.evaluate(input(notifOk = false)))
    }
}
```
- [ ] **4. Tulis test DULU — `ClassAlarmTextTest.kt`**:
```kotlin
package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduleEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ClassAlarmTextTest {

    private fun sched(room: String?, onlineUrl: String?, start: String, end: String, name: String) = ScheduleEntity(
        id = "s1", courseId = "c1", courseName = name, courseCode = null,
        courseColor = "#c9a84c", lecturer = null, credits = 2, dayOfWeek = 1,
        startTime = start, endTime = end, room = room, onlineUrl = onlineUrl, enabled = true,
    )

    @Test
    fun `dengan ruang - ruang dan jam`() {
        val s = sched(room = "A101", onlineUrl = null, start = "08:00", end = "09:40", name = "Hukum")
        assertEquals("Hukum", ClassAlarmText.title(s))
        assertEquals("A101 · 08:00–09:40", ClassAlarmText.body(s))
    }

    @Test
    fun `tanpa ruang tapi daring - label Daring`() {
        val s = sched(room = null, onlineUrl = "https://zoom.us/xyz", start = "10:00", end = "11:40", name = "Hukum")
        assertEquals("Daring · 10:00–11:40", ClassAlarmText.body(s))
    }

    @Test
    fun `tanpa ruang dan tanpa daring - hanya jam`() {
        val s = sched(room = null, onlineUrl = null, start = "08:00", end = "09:40", name = "Hukum")
        assertEquals("08:00–09:40", ClassAlarmText.body(s))
    }
}
```
- [ ] **5. Jalankan, buktikan gagal**: `.\gradlew.bat :app:testDebugUnitTest --tests "*SnoozeCounterTest*" --tests "*TaskReminderTextTest*" --tests "*ReceiverGuardTest*" --tests "*ClassAlarmTextTest*"` → unresolved reference.
- [ ] **6. Implementasi `SnoozeCounter.kt`**:
```kotlin
package com.aryariap.forfh.alarm

/** State snooze ada di Room (snoozeCount row identity) — di sini hanya aturan murni (spec §8.6). */
object SnoozeCounter {
    const val MAX_SNOOZE = 5
    const val SNOOZE_MS = 3 * 60 * 1000L // +3 menit, WIB (epoch tak bergantung zona)

    fun canSnooze(snoozeCount: Int): Boolean = snoozeCount < MAX_SNOOZE

    fun nextTrigger(currentTriggerAtMillis: Long): Long = currentTriggerAtMillis + SNOOZE_MS

    fun nextCount(current: Int): Int = current + 1
}
```
- [ ] **7. Implementasi `TaskReminderText.kt`**:
```kotlin
package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.TaskEntity

object TaskReminderText {
    const val NO_TASKS_MESSAGE = "🎉 Tidak ada tugas hari ini — selamat beraktivitas!"
    private const val PREFIX = "📚 "

    /**
     * null = slot ini silent (tidak ada tugas dan bukan slot 09:00 — spec §8.7).
     * Urutan deadline terdekat (dueAt ASC NULLS LAST) sudah dijamin query Room,
     * tapi tetap diurutkan di sini sebagai lapisan kedua.
     */
    fun build(tasks: List<TaskEntity>, slotHour: Int): String? {
        if (tasks.isEmpty()) return if (slotHour == 9) NO_TASKS_MESSAGE else null
        val sorted = tasks.sortedWith(compareBy<TaskEntity> { it.dueAt ?: Long.MAX_VALUE })
        return when (sorted.size) {
            1 -> "$PREFIX${sorted.size} tugas belum selesai: ${sorted[0].title}"
            2 -> "$PREFIX${sorted.size} tugas belum selesai: ${sorted[0].title}, ${sorted[1].title}"
            else -> "$PREFIX${sorted.size} tugas belum selesai: ${sorted[0].title}, ${sorted[1].title}, +${sorted.size - 2} lagi"
        }
    }
}
```
- [ ] **8. Implementasi `ClassAlarmText.kt`**:
```kotlin
package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduleEntity

object ClassAlarmText {
    fun title(schedule: ScheduleEntity): String = schedule.courseName

    fun body(schedule: ScheduleEntity): String {
        val jam = "${schedule.startTime}–${schedule.endTime}"
        val tempat = when {
            !schedule.room.isNullOrBlank() -> schedule.room
            !schedule.onlineUrl.isNullOrBlank() -> "Daring"
            else -> null
        }
        return if (tempat == null) jam else "$tempat · $jam"
    }
}
```
- [ ] **9. Implementasi `ReceiverGuard.kt`** (validator murni — receiver tidak menebak-nebak, spec §8.4):
```kotlin
package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import java.time.ZoneId

data class GuardInput(
    val isLoggedIn: Boolean,
    val schedule: ScheduleEntity?,
    val row: ScheduledAlarmEntity?,
    val extrasTriggerAtMillis: Long,
    val nowEpochMillis: Long,
    val hasNotificationPermission: Boolean,
)

sealed interface GuardResult {
    data class Show(
        val schedule: ScheduleEntity,
        val startDateTimeEpochMillis: Long,
        val row: ScheduledAlarmEntity,
    ) : GuardResult

    data object SkipSilent : GuardResult
    data object SkipCancel : GuardResult
}

/** Guard berlapis alarm kuliah — salah satu gagal → skip, tidak menampilkan apa pun (§8.4). */
object ReceiverGuard {
    fun evaluate(input: GuardInput, zone: ZoneId = ZoneId.of("Asia/Jakarta")): GuardResult {
        if (!input.isLoggedIn) return GuardResult.SkipCancel   // defense-in-depth pasca-logout
        val schedule = input.schedule ?: return GuardResult.SkipSilent
        if (!schedule.enabled) return GuardResult.SkipSilent
        val row = input.row ?: return GuardResult.SkipSilent   // identity tak ada → stale
        if (row.triggerAtMillis != input.extrasTriggerAtMillis) return GuardResult.SkipSilent
        val start = AlarmPlanner(zone).startDateTimeFor(row.occurrenceDate, schedule.startTime)
        if (input.nowEpochMillis >= start.toInstant().toEpochMilli()) return GuardResult.SkipSilent // alarm basi
        if (!input.hasNotificationPermission) return GuardResult.SkipSilent // silent, tidak crash (§10)
        return GuardResult.Show(schedule, start.toInstant().toEpochMilli(), row)
    }
}
```
- [ ] **10. Implementasi `ForfhNotifications.kt`** — channel "Alarm Kuliah" HIGH + sound + vibration + `CATEGORY_ALARM`; FSI bila tersedia, fallback heads-up; task channel DEFAULT:
```kotlin
package com.aryariap.forfh.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aryariap.forfh.R
import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity

/**
 * Semua tampilan notifikasi. App tidak pernah bergantung pada FSI:
 * dapatUseFullScreenIntent == false → notif biasa heads-up (HIGH + sound + vibration).
 * Sound/vibration tetap subject ke setelan user; app tidak pernah mem-bypass DND (§8.5).
 */
class ForfhNotifications(private val context: Context) {

    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_CLASS,
                    context.getString(R.string.channel_alarm_kuliah),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Alarm bangun kuliah"
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
                    enableVibration(true)
                    setCategory(NotificationCompat.CATEGORY_ALARM)
                },
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_TASK,
                    context.getString(R.string.channel_reminder_tugas),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Rekap tugas harian"
                },
            )
        }
    }

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 || NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun canUseFullScreenIntent(): Boolean =
        Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent()

    /** Alarm kuliah: FSI bila tersedia, kalau tidak heads-up. SnoozeAction sebagai aksi notif bila tersedia. */
    fun showClassAlarm(schedule: ScheduleEntity, row: ScheduledAlarmEntity, snoozeAvailable: Boolean) {
        ensureChannels()
        val requestCode = StableHash.of(row.id)
        val fsiIntent = Intent(context, FullScreenAlarmActivity::class.java)
            .putExtra("identity", row.id)
            .putExtra("scheduleId", row.scheduleId ?: "")
            .putExtra("offsetMinutes", row.offsetMinutes)
            .putExtra("occurrenceDate", row.occurrenceDate)
            .putExtra("triggerAtMillis", row.triggerAtMillis)
        val fsiPi = PendingIntent.getActivity(
            context, requestCode, fsiIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_CLASS)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(ClassAlarmText.title(schedule))
            .setContentText(ClassAlarmText.body(schedule))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setContentIntent(fsiPi)
            .setDeleteIntent(null)

        if (canUseFullScreenIntent()) {
            builder.setFullScreenIntent(fsiPi, true)
        }
        if (snoozeAvailable) {
            val snoozePi = PendingIntent.getBroadcast(
                context, requestCode + 1,
                Intent(context, AlarmReceiver::class.java)
                    .setAction(AlarmReceiver.ACTION_SNOOZE)
                    .putExtra("identity", row.id),
                PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, "Tidur lagi 3 menit", snoozePi)
        }

        NotificationManagerCompat.from(context).notify(StableHash.of(row.id), builder.build())
    }

    /** Reminder tugas: tap → halaman Tugas (MainActivity extra open_tasks). */
    fun showTaskReminder(text: String, slotHour: Int, date: String) {
        ensureChannels()
        val intent = Intent(context, com.aryariap.forfh.MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("open_tasks", true)
        val pi = PendingIntent.getActivity(
            context, StableHash.of("task|$slotHour|$date"), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_TASK)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle("Tugas")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(context).notify(StableHash.of("task|$slotHour|$date"), notif)
    }

    companion object {
        const val CHANNEL_CLASS = "alarm_kuliah"
        const val CHANNEL_TASK = "reminder_tugas"
    }
}
```
  > `ContextCompat` dan `Build` import yang tidak terpakai jangan disalin (tidak ada placeholder di file ini).
- [ ] **11. Implementasi `AlarmFlowHandler.kt`** — eksekusi receiver: validasi dulu, tampilkan setelah; reschedule one-shot tugas (bukan setRepeating); snooze update Room + reschedule:
```kotlin
package com.aryariap.forfh.alarm

import android.content.Context
import android.content.Intent
import com.aryariap.forfh.data.db.AppDatabase
import com.aryariap.forfh.data.prefs.Preferences
import com.aryariap.forfh.data.prefs.SessionManager
import com.aryariap.forfh.sync.AlarmRescheduler
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmFlowHandler(
    private val context: Context,
    private val database: AppDatabase,
    private val prefs: Preferences,
    private val sessionManager: SessionManager,
    private val rescheduler: AlarmRescheduler,
    private val notifications: ForfhNotifications,
    private val planner: AlarmPlanner,
    private val scope: CoroutineScope,
) {
    private val alarmsDao get() = database.scheduledAlarmsDao()
    private val schedulesDao get() = database.schedulesDao()
    private val tasksDao get() = database.tasksDao()
    private val zone = ZoneId.of("Asia/Jakarta")

    /** CLASS_ALARM: guard berlapis → tampil (FSI/heads-up). Row dibiarkan utk snooze; basi dihapus. */
    fun handleClassAlarm(intent: Intent) {
        val scheduleId = intent.getStringExtra("scheduleId") ?: return
        val offsetMinutes = intent.getIntExtra("offsetMinutes", -1)
        val occurrenceDate = intent.getStringExtra("occurrenceDate") ?: return
        val trigger = intent.getLongExtra("triggerAtMillis", -1L)
        if (offsetMinutes < 0 || trigger < 0) return
        val identity = AlarmPlanner.classIdentity(
            scheduleId, offsetMinutes, LocalDate.parse(occurrenceDate),
        )
        val result = ReceiverGuard.evaluate(
            GuardInput(
                isLoggedIn = runBlockingOrFalse(),
                schedule = schedulesDao.getByIdOnce(scheduleId),
                row = alarmsDao.getByIdOnce(identity),
                extrasTriggerAtMillis = trigger,
                nowEpochMillis = System.currentTimeMillis(),
                hasNotificationPermission = notifications.hasPermission(),
            ),
        )
        when (result) {
            is GuardResult.Show -> {
                notifications.showClassAlarm(
                    result.schedule, result.row,
                    snoozeAvailable = SnoozeCounter.canSnooze(result.row.snoozeCount),
                )
            }
            GuardResult.SkipSilent -> Unit // tidak menampilkan apa pun, tidak crash
            GuardResult.SkipCancel -> {
                rescheduler.cancelAlarm(identity)
            }
        }
    }

    private fun runBlockingOrFalse(): Boolean =
        kotlinx.coroutines.runBlocking { sessionManager.isLoggedIn() }

    /** TASK_REMINDER: one-shot → query Room → tampil → hapus row hari ini → schedule besok. */
    fun handleTaskReminder(intent: Intent) {
        val slotHour = intent.getIntExtra("slotHour", -1)
        val occurrenceDate = intent.getStringExtra("occurrenceDate") ?: return
        val trigger = intent.getLongExtra("triggerAtMillis", -1L)
        if (slotHour !in TASK_SLOTS || trigger < 0) return
        val identity = AlarmPlanner.taskIdentity(slotHour, LocalDate.parse(occurrenceDate))
        val row = alarmsDao.getByIdOnce(identity) ?: return
        if (row.triggerAtMillis != trigger) return
        if (!runBlockingOrFalse()) { // defense-in-depth pasca-logout
            rescheduler.cancelAlarm(identity)
            return
        }
        val tasks = tasksDao.getActiveByDeadline()
        val text = TaskReminderText.build(tasks, slotHour)
        if (text != null && notifications.hasPermission()) {
            notifications.showTaskReminder(text, slotHour, occurrenceDate)
        }
        // one-shot: selesai tampil → row besok (spec §8.7)
        rescheduler.replaceTaskSlotRow(slotHour, ZonedDateTime.now(zone))
    }

    /** Snooze dari FSI activity atau aksi notif: +3 menit, count++, update Room, reschedule (RTC_WAKEUP). */
    fun snooze(identity: String): Boolean {
        val row = alarmsDao.getByIdOnce(identity) ?: return false
        if (!SnoozeCounter.canSnooze(row.snoozeCount)) return false
        val updated = row.copy(
            triggerAtMillis = SnoozeCounter.nextTrigger(row.triggerAtMillis),
            snoozeCount = SnoozeCounter.nextCount(row.snoozeCount),
        )
        alarmsDao.upsert(updated)
        rescheduler.scheduleRow(updated)
        return true
    }

    companion object {
        val TASK_SLOTS = listOf(9, 15, 20)
    }
}
```
  > Catatan implementasi: `runBlockingOrFalse()` dipakai karena `BroadcastReceiver.onReceive` non-suspend; pada praktik eksekusi `AlarmFlowHandler` dibungkus `goAsync()` di receiver (lihat langkah 12). Nama `runBlockingOrFalse` merefleksikan perilaku: gagal/blocking singkat → false → skip.
- [ ] **12. Implementasi `AlarmReceiver.kt`** (ganti stub):
```kotlin
package com.aryariap.forfh.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aryariap.forfh.ForfhApp

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as ForfhApp
        val handler = app.container.alarmFlow
        app.container.applicationScope.launch {
            try {
                when (intent.action) {
                    ACTION_CLASS_ALARM -> handler.handleClassAlarm(intent)
                    ACTION_TASK_REMINDER -> handler.handleTaskReminder(intent)
                    ACTION_SNOOZE -> handler.snooze(intent.getStringExtra("identity") ?: "")
                    else -> Unit
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_CLASS_ALARM = "com.aryariap.forfh.action.CLASS_ALARM"
        const val ACTION_TASK_REMINDER = "com.aryariap.forfh.action.TASK_REMINDER"
        const val ACTION_SNOOZE = "com.aryariap.forfh.action.SNOOZE"
    }
}
```
  > Impor `launch` disediakan via `kotlinx.coroutines.launch` — tambahkan import bila compiler memintanya; tidak ada logika placeholder.
- [ ] **13. Update `AppContainer.kt`** (tambah planner, scheduler, rescheduler, notifications, alarmFlow; `AlarmRescheduler` disediakan T8 — tulis di T8):
```kotlin
// Potongan — AppContainer final lengkap ditulis di T10; di T7 cukup tambahkan:
val alarmFlow: com.aryariap.forfh.alarm.AlarmFlowHandler by lazy {
    com.aryariap.forfh.alarm.AlarmFlowHandler(
        context = app,
        database = database,
        prefs = prefs,
        sessionManager = sessionManager,
        rescheduler = rescheduler,
        notifications = notifications,
        planner = planner,
        scope = applicationScope,
    )
}
```
  > Karena `AlarmRescheduler` baru ada di T8, kompilasi T7 memakai versi minim dari `rescheduler` — tulis `AlarmRescheduler` lengkap di T8 (tidak ada kode kosong; urutan task sudah menjamin dependensi).
- [ ] **14. Jalankan test, buktikan lulus**: SnoozeCounter (3) + TaskReminderText (5) + ReceiverGuard (7) + ClassAlarmText (3) hijau; `assembleDebug` hijau.
- [ ] **15. Commit**: `git add -A && git commit -m "T7: receiver guard + snooze + task reminder + notifications"`.

---

## T8 — ReconcilePlanner + AlarmRescheduler (reconcile idempotent, rescheduleAll preserve snooze)

**Files:**
- Test `app/src/test/java/com/aryariap/forfh/sync/ReconcilePlannerTest.kt`
- Create `app/src/main/java/com/aryariap/forfh/sync/ReconcilePlanner.kt`
- Create `app/src/main/java/com/aryariap/forfh/sync/AlarmRescheduler.kt`

**Interfaces:**
- Consumes: `AlarmPlanner` (T5), `AlarmScheduler` (T6), DAO (T2), `Preferences` (T3)
- Produces:
  - `ReconcilePlanner(planner).computeOps(current, schedules, offsets: List<Int>, now, fullRebuild): List<AlarmOp>` — pure, utk test interleaving sync vs snooze.
  - `AlarmRescheduler`: `suspend fun rescheduleAll()`; `suspend fun reconcile()`; `suspend fun scheduleRow(row)`; `suspend fun cancelAlarm(identity)`; `suspend fun cancelAll()`; `suspend fun replaceTaskSlotRow(slotHour, now)` — dipakai T7/T9/T10/T11/T12.

**Langkah:**

- [ ] **1. Tulis test DULU (harus gagal)** — `ReconcilePlannerTest.kt` (3 skenario hardening spec §12 + kasus dasar):
```kotlin
package com.aryariap.forfh.sync

import com.aryariap.forfh.alarm.AlarmPlanner
import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ReconcilePlannerTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val planner = ReconcilePlanner(AlarmPlanner(zone))
    private val offsets = listOf(180, 120, 60)

    private fun wib(s: String): ZonedDateTime =
        LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")).atZone(zone)

    private fun sched(id: String = "s1", day: Int = 1, start: String = "08:00", enabled: Boolean = true) =
        ScheduleEntity(
            id = id, courseId = "c1", courseName = "Hukum", courseCode = null,
            courseColor = "#c9a84c", lecturer = null, credits = 2, dayOfWeek = day,
            startTime = start, endTime = "09:40", room = "A101", onlineUrl = null, enabled = enabled,
        )

    private fun classRow(id: String, trigger: Long, snoozeCount: Int = 0) = ScheduledAlarmEntity(
        id = id, kind = "CLASS_ALARM", scheduleId = "s1", offsetMinutes = 120,
        occurrenceDate = "2026-08-17", triggerAtMillis = trigger, snoozeCount = snoozeCount,
    )

    @Test
    fun `rescheduleAll membangun row untuk tiap offset aktif`() {
        val ops = planner.computeOps(emptyList(), listOf(sched()), offsets, wib("2026-08-15T10:00"), fullRebuild = true)
        val rows = ops.filterIsInstance<AlarmOp.Schedule>().map { it.row }
        assertEquals(3, rows.size)
        assertEquals(setOf(180, 120, 60), rows.map { it.offsetMinutes }.toSet())
        assertEquals(setOf("class|s1|180|2026-08-17", "class|s1|120|2026-08-17", "class|s1|60|2026-08-17"), rows.map { it.id }.toSet())
    }

    @Test
    fun `slot tugas selalu ada - 3 slot one-shot`() {
        val ops = planner.computeOps(emptyList(), emptyList(), emptyList(), wib("2026-08-15T10:00"), fullRebuild = true)
        val rows = ops.filterIsInstance<AlarmOp.Schedule>().map { it.row }
        assertEquals(3, rows.size)
        assertTrue(rows.all { it.kind == "TASK_REMINDER" })
        assertEquals(setOf("task|09|2026-08-16", "task|15|2026-08-15", "task|20|2026-08-15"), rows.map { it.id }.toSet()) // now 10:00 → slot 09 lewat → besok
    }

    @Test
    fun `HARDENING - sync selesai setelah snooze - tidak double schedule, trigger snooze dipertahankan`() {
        val now = wib("2026-08-17T00:00") // Senin
        val s = sched()
        // rescheduleAll pertama (sync selesai)
        val ops1 = planner.computeOps(emptyList(), listOf(s), listOf(120), now, fullRebuild = true)
        val base = (ops1.single { it is AlarmOp.Schedule && it.row.offsetMinutes == 120 } as AlarmOp.Schedule).row
        // user snooze: +3 menit, count 1 — di sela sync
        val snoozed = base.copy(triggerAtMillis = base.triggerAtMillis + 180_000L, snoozeCount = 1)
        // sync selesai → rescheduleAll lagi terhadap state ber-snooze
        val ops2 = planner.computeOps(listOf(snoozed), listOf(s), listOf(120), now, fullRebuild = true)
        // invariant: snoozeCount tetap 1, trigger tetap nilai snooze, tidak ada double-schedule utk identity ini
        assertTrue(ops2.filterIsInstance<AlarmOp.Schedule>().none { it.row.id == base.id })
        assertTrue(ops2.filterIsInstance<AlarmOp.Cancel>().none { it.row.id == base.id })
        assertTrue(ops2.any { it is AlarmOp.Keep })
    }

    @Test
    fun `HARDENING - jadwal diubah setelah alarm terpasang - alarm lama di-cancel, alarm baru terpasang`() {
        val now = wib("2026-08-15T00:00")
        val oldRow = classRow("class|s1|120|2026-08-17", trigger = 1_750_000_000_000L)
        // sync membawa jadwal pindah: Senin 08:00 → Rabu 10:00
        val moved = sched(day = 3, start = "10:00")
        val ops = planner.computeOps(listOf(oldRow), listOf(moved), listOf(120), now, fullRebuild = true)
        assertTrue(ops.any { it is AlarmOp.Cancel && it.row.id == oldRow.id })
        val newRows = ops.filterIsInstance<AlarmOp.Schedule>().map { it.row }
        assertTrue(newRows.any { it.scheduleId == "s1" && it.occurrenceDate == "2026-08-19" })
        // guard receiver utk alarm lama: row identity tak ada → SkipSilent (diuji di ReceiverGuardTest)
    }

    @Test
    fun `jadwal dinonaktifkan - row lama di-cancel tanpa schedule baru`() {
        val now = wib("2026-08-15T00:00")
        val oldRow = classRow("class|s1|120|2026-08-17", trigger = 1_750_000_000_000L)
        val ops = planner.computeOps(listOf(oldRow), listOf(sched(enabled = false)), listOf(120), now, fullRebuild = true)
        assertTrue(ops.any { it is AlarmOp.Cancel && it.row.id == oldRow.id })
        assertTrue(ops.filterIsInstance<AlarmOp.Schedule>().all { it.row.kind == "TASK_REMINDER" })
    }

    @Test
    fun `reconcile idempotent - state benar tidak disentuh`() {
        val now = wib("2026-08-17T00:00")
        val s = sched()
        val ops1 = planner.computeOps(emptyList(), listOf(s), listOf(120), now, fullRebuild = true)
        val installed = (ops1.single { it is AlarmOp.Schedule } as AlarmOp.Schedule).row
        // reconcile kedua: hanya 1 dari 6 row benar yang terpasang (ops1 = 3 class + 3 task slot)
        // → 1 Keep (row cocok), 5 Schedule (yang hilang), 0 Cancel
        val ops2 = planner.computeOps(listOf(installed), listOf(s), listOf(120), now, fullRebuild = false)
        assertTrue(ops2.none { it is AlarmOp.Cancel })
        assertTrue(ops2.any { it is AlarmOp.Keep })
        assertEquals(5, ops2.filterIsInstance<AlarmOp.Schedule>().size)
        // reconcile menambahkan yang hilang
        val ops3 = planner.computeOps(emptyList(), listOf(s), listOf(120), now, fullRebuild = false)
        assertTrue(ops3.any { it is AlarmOp.Schedule })
    }

    @Test
    fun `HARDENING - exact restore - rescheduleAll kembali exact preserve sesi snooze`() {
        val now = wib("2026-08-17T00:00")
        val s = sched()
        val snoozed = ScheduledAlarmEntity(
            id = "class|s1|120|2026-08-17", kind = "CLASS_ALARM", scheduleId = "s1",
            offsetMinutes = 120, occurrenceDate = "2026-08-17",
            triggerAtMillis = 1_787_007_780_000L, // 18 Agu 06:03 WIB (snooze +3 mnt dari trigger 06:00; anchor .NET) — future vs now 17 Agu 00:00 WIB
            snoozeCount = 2,
        )
        val ops = planner.computeOps(listOf(snoozed), listOf(s), listOf(120), now, fullRebuild = true)
        assertTrue(ops.any { it is AlarmOp.Keep })
        assertTrue(ops.filterIsInstance<AlarmOp.Schedule>().none { it.row.id == snoozed.id })
        // bila Keep dijalankan → scheduleRow(snoozed) memakai triggerAtMillis tersimpan (snooze), bukan base
        // (assert trigger bernilai snooze di AlarmSchedulerTest.restore exact)
    }
}
```
- [ ] **2. Jalankan, buktikan gagal**: `.\gradlew.bat :app:testDebugUnitTest --tests "*ReconcilePlannerTest*"` → unresolved reference.
- [ ] **3. Implementasi `ReconcilePlanner.kt`** (murni — keputusan "apa yang harus terpasang"):
```kotlin
package com.aryariap.forfh.sync

import com.aryariap.forfh.alarm.AlarmPlanner
import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import java.time.ZonedDateTime

sealed interface AlarmOp {
    /** Pasang + simpan row (menimpa bila identity sama). */
    data class Schedule(val row: ScheduledAlarmEntity) : AlarmOp

    /** Cancel PendingIntent + hapus row. */
    data class Cancel(val row: ScheduledAlarmEntity) : AlarmOp

    /** Tidak disentuh (sudah benar / sesi snooze aktif dipertahankan). */
    data object Keep : AlarmOp
}

/**
 * Murni: menghitung operasi alarm dari Room — tidak menyentuh AlarmManager.
 * Invariant spec: row snooze aktif (snoozeCount > 0 && trigger future) TIDAK di-cancel/reset;
 * snoozeCount tidak pernah turun oleh proses lain.
 */
class ReconcilePlanner(private val planner: AlarmPlanner) {

    fun computeOps(
        current: List<ScheduledAlarmEntity>,
        schedules: List<ScheduleEntity>,
        offsets: List<Int>,
        now: ZonedDateTime,
        fullRebuild: Boolean,
    ): List<AlarmOp> {
        val desired = desiredRows(schedules, offsets, now)
        val currentById = current.associateBy { it.id }
        val nowMs = now.toInstant().toEpochMilli()
        val ops = mutableListOf<AlarmOp>()

        for ((identity, row) in desired) {
            val existing = currentById[identity]
            ops += when {
                existing == null -> AlarmOp.Schedule(row)
                existing.snoozeCount > 0 && existing.triggerAtMillis > nowMs -> AlarmOp.Keep
                existing.snoozeCount == 0 && existing.triggerAtMillis == row.triggerAtMillis -> AlarmOp.Keep
                else -> AlarmOp.Schedule(row) // trigger lama beda (jadwal berubah) → timpa
            }
        }

        if (fullRebuild) {
            for (row in current) {
                val snoozed = row.snoozeCount > 0 && row.triggerAtMillis > nowMs
                if (row.id !in desired && !snoozed) ops += AlarmOp.Cancel(row)
            }
        }
        return ops
    }

    private fun desiredRows(
        schedules: List<ScheduleEntity>,
        offsets: List<Int>,
        now: ZonedDateTime,
    ): Map<String, ScheduledAlarmEntity> {
        val result = mutableMapOf<String, ScheduledAlarmEntity>()
        for (s in schedules) { // s.enabled == true (query getEnabledOnce)
            for (offset in offsets) {
                val occ = planner.nextClassOccurrence(s.id, s.dayOfWeek, s.startTime, offset, now)
                result[occ.identity] = ScheduledAlarmEntity(
                    id = occ.identity,
                    kind = "CLASS_ALARM",
                    scheduleId = s.id,
                    offsetMinutes = offset,
                    occurrenceDate = occ.occurrenceDate.toString(),
                    triggerAtMillis = occ.triggerAtMillis,
                    snoozeCount = 0,
                )
            }
        }
        for (slot in TASK_SLOTS) {
            val (date, trigger) = planner.nextTaskSlot(slot, now)
            val identity = AlarmPlanner.taskIdentity(slot, date)
            result[identity] = ScheduledAlarmEntity(
                id = identity,
                kind = "TASK_REMINDER",
                scheduleId = null,
                offsetMinutes = 0,
                occurrenceDate = date.toString(),
                triggerAtMillis = trigger,
                snoozeCount = 0,
            )
        }
        return result
    }

    companion object {
        val TASK_SLOTS = listOf(9, 15, 20)
    }
}
```
- [ ] **4. Implementasi `AlarmRescheduler.kt`** (eksekutor ops — satu-satunya pintu ubah `scheduled_alarms`):
```kotlin
package com.aryariap.forfh.sync

import com.aryariap.forfh.alarm.AlarmPlanner
import com.aryariap.forfh.alarm.AlarmScheduler
import com.aryariap.forfh.data.db.ScheduledAlarmsDao
import com.aryariap.forfh.data.db.SchedulesDao
import com.aryariap.forfh.data.prefs.Preferences
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmRescheduler(
    private val planner: AlarmPlanner,
    private val scheduler: AlarmScheduler,
    private val alarmsDao: ScheduledAlarmsDao,
    private val schedulesDao: SchedulesDao,
    private val prefs: Preferences,
) {
    private val zone = ZoneId.of("Asia/Jakarta")

    /** Cancel semua yang obsolete lalu bangun ulang; sesi snooze aktif dipertahankan apa adanya (§8.1). */
    suspend fun rescheduleAll() = execute(compute(fullRebuild = true))

    /** Idempotent: perbaiki yang hilang tanpa menyentuh yang sudah benar. */
    suspend fun reconcile() = execute(compute(fullRebuild = false))

    /** Pasang satu row (snooze, reschedule setelah restore exact) — tidak menyentuh row lain. */
    suspend fun scheduleRow(row: com.aryariap.forfh.data.db.ScheduledAlarmEntity) {
        alarmsDao.upsert(row)
        scheduler.schedule(row)
    }

    /** Cancel + hapus row identity (dipakai guard SkipCancel & slot tugas selesai). */
    suspend fun cancelAlarm(identity: String) {
        alarmsDao.getByIdOnce(identity)?.let { scheduler.cancel(it) }
        alarmsDao.deleteById(identity)
    }

    /** Logout §8.10: cancel seluruh alarm tanpa menghapus row (row dihapus terpisah). */
    suspend fun cancelAll() {
        alarmsDao.getAllOnce().forEach { scheduler.cancel(it) }
    }

    /** Pola one-shot tugas: hapus row hari ini, pasang row besok (§8.7). */
    suspend fun replaceTaskSlotRow(slotHour: Int, now: ZonedDateTime) {
        val (date, trigger) = planner.nextTaskSlot(slotHour, now)
        val identity = AlarmPlanner.taskIdentity(slotHour, date)
        val row = com.aryariap.forfh.data.db.ScheduledAlarmEntity(
            id = identity,
            kind = "TASK_REMINDER",
            scheduleId = null,
            offsetMinutes = 0,
            occurrenceDate = date.toString(),
            triggerAtMillis = trigger,
            snoozeCount = 0,
        )
        alarmsDao.upsert(row)
        scheduler.schedule(row)
    }

    private suspend fun compute(fullRebuild: Boolean): List<AlarmOp> {
        val now = ZonedDateTime.now(zone)
        val offsets = prefs.offsets.first().activeOffsets()
        return ReconcilePlanner(planner).computeOps(
            current = alarmsDao.getAllOnce(),
            schedules = schedulesDao.getEnabledOnce(),
            offsets = offsets,
            now = now,
            fullRebuild = fullRebuild,
        )
    }

    private suspend fun execute(ops: List<AlarmOp>) {
        for (op in ops) {
            when (op) {
                is AlarmOp.Schedule -> {
                    alarmsDao.upsert(op.row)
                    scheduler.schedule(op.row)
                }
                is AlarmOp.Cancel -> {
                    scheduler.cancel(op.row)
                    alarmsDao.deleteById(op.row.id)
                }
                AlarmOp.Keep -> Unit
            }
        }
    }
}
```
- [ ] **5. Update `AppContainer.kt`** (tambah planner/scheduler/rescheduler/notifications; lihat T10 untuk versi final):
```kotlin
val planner: com.aryariap.forfh.alarm.AlarmPlanner by lazy { com.aryariap.forfh.alarm.AlarmPlanner() }
val scheduler: com.aryariap.forfh.alarm.AlarmScheduler by lazy {
    com.aryariap.forfh.alarm.AlarmScheduler(com.aryariap.forfh.alarm.AndroidAlarmApi(app))
}
val rescheduler: com.aryariap.forfh.sync.AlarmRescheduler by lazy {
    com.aryariap.forfh.sync.AlarmRescheduler(planner, scheduler, database.scheduledAlarmsDao(), database.schedulesDao(), prefs)
}
val notifications: com.aryariap.forfh.alarm.ForfhNotifications by lazy { com.aryariap.forfh.alarm.ForfhNotifications(app) }
```
- [ ] **6. Jalankan test, buktikan lulus** (7 test ReconcilePlanner hijau); `assembleDebug` hijau.
- [ ] **7. Commit**: `git add -A && git commit -m "T8: ReconcilePlanner + AlarmRescheduler preserve snooze"`.

---
## T9 — FullScreenAlarmActivity (showWhenLocked/turnScreenOn, snooze, Tutup)

**Files:**
- Create `app/src/main/java/com/aryariap/forfh/alarm/FullScreenAlarmActivity.kt`
- Create `app/src/main/java/com/aryariap/forfh/alarm/FullScreenAlarmViewModel.kt`
- Modify `app/src/main/AndroidManifest.xml` (activity sudah dideklarasi di T1 — tidak ada perubahan)

**Interfaces:**
- Consumes: `AlarmFlowHandler.snooze(identity)` (T7), `ClassAlarmText` (T7), DAO (T2), `AlarmPlanner` (T5)
- Produces: activity yang membuka layer alarm di atas lock screen (API 27+ `setShowWhenLocked`/`setTurnScreenOn`, API 26 fallback window flags), tombol "Tidur lagi 3 menit" (hilang setelah snooze ke-5) + "Tutup"; finish otomatis bila row tak ada / trigger tak cocok.

**Langkah:**

- [ ] **1. Tulis `FullScreenAlarmViewModel.kt`** (state UI + aksi; logika snooze memakai AlarmFlowHandler sehingga konsisten dengan aksi notif):
```kotlin
package com.aryariap.forfh.alarm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aryariap.forfh.ForfhApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AlarmUiState(
    val valid: Boolean = false,
    val title: String = "",
    val body: String = "",
    val snoozeAvailable: Boolean = false,
    val snoozeCount: Int = 0,
)

class FullScreenAlarmViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as ForfhApp).container
    private val alarmsDao get() = container.database.scheduledAlarmsDao()
    private val schedulesDao get() = container.database.schedulesDao()

    private var identity: String? = null
    private var snoozedThisSession = false

    private val _state = MutableStateFlow(AlarmUiState())
    val state: StateFlow<AlarmUiState> = _state

    fun bind(identity: String, extrasTriggerAtMillis: Long) {
        this.identity = identity
        viewModelScope.launch {
            val row = alarmsDao.getByIdOnce(identity)
            val schedule = row?.scheduleId?.let { schedulesDao.getByIdOnce(it) }
            val valid = row != null && schedule != null && row.triggerAtMillis == extrasTriggerAtMillis
            _state.value = if (valid) {
                AlarmUiState(
                    valid = true,
                    title = ClassAlarmText.title(schedule!!),
                    body = ClassAlarmText.body(schedule),
                    snoozeAvailable = SnoozeCounter.canSnooze(row!!.snoozeCount),
                    snoozeCount = row.snoozeCount,
                )
            } else {
                AlarmUiState(valid = false)
            }
        }
    }

    fun snooze() {
        val id = identity ?: return
        viewModelScope.launch {
            if (container.alarmFlow.snooze(id)) {
                snoozedThisSession = true
                val row = alarmsDao.getByIdOnce(id)
                _state.value = _state.value.copy(
                    snoozeCount = row?.snoozeCount ?: 0,
                    snoozeAvailable = row != null && SnoozeCounter.canSnooze(row.snoozeCount),
                )
            }
        }
    }

    /** Tutup = occurrence selesai. Row dihapus bila tidak ada snooze tersisa yang ditunda. */
    fun close() {
        val id = identity ?: return
        viewModelScope.launch {
            val row = alarmsDao.getByIdOnce(id) ?: return@launch
            if (!snoozedThisSession && row.snoozeCount == 0) {
                alarmsDao.deleteById(id)
            }
        }
    }
}
```
  > Semantik "Tutup": bila user belum pernah snooze di sesi ini (row masih trigger asli, count 0) → hapus row (occurrence selesai, §8.4). Bila sudah snooze → row tetap ada utk trigger berikutnya; dibersihkan rescheduleAll berikutnya bila tak disentuh.
- [ ] **2. Tulis `FullScreenAlarmActivity.kt`** (edge-to-edge, kunci layar, tombol aksi):
```kotlin
package com.aryariap.forfh.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aryariap.forfh.ui.theme.ForfhTheme

/** Layer alarm di atas lock screen — API 27+ setShowWhenLocked/setTurnScreenOn, API 26 fallback flags. */
class FullScreenAlarmActivity : ComponentActivity() {

    private val viewModel: FullScreenAlarmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        enableEdgeToEdge()

        val identity = intent.getStringExtra("identity") ?: run { finish(); return }
        val trigger = intent.getLongExtra("triggerAtMillis", -1L)
        if (trigger < 0) { finish(); return }
        viewModel.bind(identity, trigger)

        setContent {
            ForfhTheme {
                AlarmUi(
                    viewModel = viewModel,
                    onSnooze = { viewModel.snooze() },
                    onClose = { viewModel.close(); finish() },
                )
            }
        }
    }
}

@Composable
private fun AlarmUi(
    viewModel: FullScreenAlarmViewModel,
    onSnooze: () -> Unit,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LifecycleResumeEffect(Unit) {
        onResume = { if (!state.valid) { /* finish dipicu dari state tidak valid */ } }
        onPause = {}
    }

    if (!state.valid) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Bangun!",
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            ),
        )
        Spacer(Modifier.height(8.dp))
        Text(text = state.title, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(8.dp))
        Text(text = state.body, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(48.dp))
        if (state.snoozeAvailable) {
            Button(onClick = onSnooze) {
                Text("Tidur lagi 3 menit")
            }
            Spacer(Modifier.height(12.dp))
        }
        OutlinedButton(onClick = onClose) {
            Text("Tutup")
        }
    }
}
```
  > Catatan: validasi tambahan saat activity sudah tampil (row hilang di tengah jalan) dilakukan `AlarmFlowHandler.snooze` yang return false; `Tutup` memakai `close()`. Tidak ada alarm basi: `bind` gagal → `valid=false` → layar kosong, user keluar via back.
- [ ] **3. Verifikasi**: `.\gradlew.bat :app:assembleDebug` hijau. (Perilaku lock screen diuji manual di Redmi — §12; tidak ada unit test JVM utk activity.)
- [ ] **4. Commit**: `git add -A && git commit -m "T9: FullScreenAlarmActivity + snooze + tutup"`.

---

## T10 — SyncRepository + SyncWorker + login flow + AppContainer final + logout

**Files:**
- Test `app/src/test/java/com/aryariap/forfh/sync/SyncRepositoryTest.kt`
- Create `app/src/main/java/com/aryariap/forfh/sync/SyncRepository.kt`
- Create `app/src/main/java/com/aryariap/forfh/sync/SyncWorker.kt`
- Create (tulis ulang final) `app/src/main/java/com/aryariap/forfh/AppContainer.kt`
- Modify `app/src/main/java/com/aryariap/forfh/ForfhApp.kt` (enkripsi periodic + goAsync launch import)

**Interfaces:**
- Consumes: `ForfhApiService` (T4), DAO (T2), `SyncStateStore` (T3/T4), `AlarmRescheduler` (T8), `SecureCookieStore`/`SessionManager` (T3)
- Produces:
  - `SyncRepository(api, schedulesDao, tasksDao, syncState): suspend fun sync(): SyncOutcome` — Success(schedules, tasks) / Failure(OFFLINE|SERVER); wipe-and-replace HANYA saat sukses.
  - `SyncWorker`: `doWork()` mode "sync" | "reconcile"; companion `enqueueOneShot(context)`, `enqueuePeriodic(context)`, `enqueueReconcile(context)`.
  - `AppContainer.logout(message)` — cancel alarm + wipe Room + DataStore + cookie (spec §8.10).

**Langkah:**

- [ ] **1. Tulis test DULU (harus gagal)** — `SyncRepositoryTest.kt` (fake DAO in-memory + fake API + fake SyncStateStore):
```kotlin
package com.aryariap.forfh.sync

import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.SchedulesDao
import com.aryariap.forfh.data.db.TaskEntity
import com.aryariap.forfh.data.db.TasksDao
import com.aryariap.forfh.network.ForfhApiService
import com.aryariap.forfh.network.LoginRequest
import com.aryariap.forfh.network.LoginResponse
import com.aryariap.forfh.network.MarkDoneRequest
import com.aryariap.forfh.network.ScheduleDto
import com.aryariap.forfh.network.SchedulesResponse
import com.aryariap.forfh.network.SuccessResponse
import com.aryariap.forfh.network.TaskDto
import com.aryariap.forfh.network.TasksResponse
import com.aryariap.forfh.network.UserDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class SyncRepositoryTest {

    private class FakeSchedulesDao : SchedulesDao {
        var items = emptyList<ScheduleEntity>()
        override fun getAll(): Flow<List<ScheduleEntity>> = MutableStateFlow(items)
        override fun getAllOnce(): List<ScheduleEntity> = items
        override fun getEnabledOnce(): List<ScheduleEntity> = items.filter { it.enabled }
        override fun getByIdOnce(id: String): ScheduleEntity? = items.firstOrNull { it.id == id }
        override fun clearAll() { items = emptyList() }
        override fun insertAll(items: List<ScheduleEntity>) { this.items = items }
        override suspend fun replaceAll(items: List<ScheduleEntity>) { this.items = items }
    }

    private class FakeTasksDao : TasksDao {
        var items = emptyList<TaskEntity>()
        override fun getAll(): Flow<List<TaskEntity>> = MutableStateFlow(items)
        override fun getById(id: String): Flow<TaskEntity?> = MutableStateFlow(items.firstOrNull { it.id == id })
        override fun getAllOnce(): List<TaskEntity> = items
        override fun getActiveByDeadline(): List<TaskEntity> = items.filter { it.status != "DONE" }
            .sortedWith(compareBy<TaskEntity> { it.dueAt ?: Long.MAX_VALUE })
        override fun updateStatus(id: String, status: String, computedStatus: String?) {
            items = items.map { if (it.id == id) it.copy(status = status, computedStatus = computedStatus) else it }
        }
        override fun clearAll() { items = emptyList() }
        override fun insertAll(items: List<TaskEntity>) { this.items = items }
        override suspend fun replaceAll(items: List<TaskEntity>) { this.items = items }
    }

    private class FakeApi(
        var scheduleResponse: Response<SchedulesResponse> = Response.success(200, SchedulesResponse(emptyList())),
        var tasksResponse: Response<TasksResponse> = Response.success(200, TasksResponse(emptyList())),
    ) : ForfhApiService {
        var schedulesCalls = 0
        override suspend fun login(body: LoginRequest): Response<LoginResponse> =
            Response.success(200, LoginResponse(true, UserDto("u1", "a", "A", "1")))
        override suspend fun schedules(): Response<SchedulesResponse> { schedulesCalls++; return scheduleResponse }
        override suspend fun tasks(): Response<TasksResponse> = tasksResponse
        override suspend fun markDone(id: String, body: MarkDoneRequest): Response<SuccessResponse> =
            Response.success(200, SuccessResponse(true, id))
    }

    private class FakeState : SyncStateStore {
        var lastSync = 0L
        var status = ""
        override suspend fun setLastSync(epochMillis: Long, status: String) { lastSync = epochMillis; this.status = status }
        override suspend fun lastSyncAt(): Long = lastSync
        override suspend fun lastSyncStatus(): String = status
    }

    @Test
    fun `sync sukses - wipe and replace kedua tabel dan state ok`() = runTest {
        val schedDao = FakeSchedulesDao()
        val taskDao = FakeTasksDao()
        val state = FakeState()
        schedDao.items = listOf(oldSchedule())
        taskDao.items = listOf(oldTask())
        val api = FakeApi(
            scheduleResponse = Response.success(200, SchedulesResponse(listOf(newSchedule()))),
            tasksResponse = Response.success(200, TasksResponse(listOf(newTask()))),
        )
        val repo = SyncRepository(api, schedDao, taskDao, state)
        val out = repo.sync()
        assertTrue(out is SyncOutcome.Success)
        assertEquals(1, schedDao.items.size)
        assertEquals("s-new", schedDao.items.single().id)      // yang lama tergantikan
        assertEquals("t-new", taskDao.items.single().id)
        assertEquals("ok", state.status)
        assertTrue(state.lastSync > 0)
    }

    @Test
    fun `sync gagal offline - Room TIDAK disentuh dan status error`() = runTest {
        val schedDao = FakeSchedulesDao()
        val taskDao = FakeTasksDao()
        val state = FakeState()
        schedDao.items = listOf(oldSchedule())
        taskDao.items = listOf(oldTask())
        val api = FakeApi(scheduleResponse = Response.error(500, okhttp3.ResponseBody.Companion.toResponseBody("{}", null)))
        val repo = SyncRepository(api, schedDao, taskDao, state)
        val out = repo.sync()
        assertTrue(out is SyncOutcome.Failure)
        assertEquals(SyncFailure.SERVER, (out as SyncOutcome.Failure).reason)
        assertEquals("s-old", schedDao.items.single().id)   // Room lama tetap
        assertEquals("t-old", taskDao.items.single().id)
        assertEquals("error", state.status)
    }

    private fun oldSchedule() = ScheduleEntity("s-old", "c1", "Lama", null, "#3b82f6", null, 2, 1, "08:00", "09:40", null, null, true)
    private fun newSchedule() = ScheduleDto("s-new", "c1", "Baru", null, "#3b82f6", null, 2, 3, "10:00", "11:40", null, null, 1)
    private fun oldTask() = TaskEntity("t-old", null, null, null, "Lama", null, 1L, "NOT_STARTED", null, "medium", null)
    private fun newTask() = TaskDto(
        "t-new", "u1", "c1", "Baru", null, "assignment", "2026-08-20T03:00:00.000Z",
        null, "medium", 30, "NOT_STARTED", 0, "manual", null, null, 1, null,
        "2026-08-01T03:00:00.000Z", "2026-08-01T03:00:00.000Z", null, null, emptyList(),
    )
}
```
- [ ] **2. Jalankan, buktikan gagal**: `.\gradlew.bat :app:testDebugUnitTest --tests "*SyncRepositoryTest*"` → unresolved reference.
- [ ] **3. Implementasi `SyncRepository.kt`**:
```kotlin
package com.aryariap.forfh.sync

import com.aryariap.forfh.data.db.SchedulesDao
import com.aryariap.forfh.data.db.TasksDao
import com.aryariap.forfh.network.ForfhApiService
import com.aryariap.forfh.network.toEntity
import java.io.IOException
import java.time.Clock

sealed interface SyncOutcome {
    data class Success(val schedules: Int, val tasks: Int) : SyncOutcome
    data class Failure(val reason: SyncFailure) : SyncOutcome
}

enum class SyncFailure { OFFLINE, SERVER }

/**
 * Wipe-and-replace HANYA saat response sukses & valid, dan HANYA tabel mirror
 * schedules & tasks — scheduled_alarms tidak pernah disentuh di sini (invariant spec §7, §9).
 * Sync Worker / UI tidak pernah menyentuh alarm langsung — AlarmRescheduler yang urus.
 */
class SyncRepository(
    private val api: ForfhApiService,
    private val schedulesDao: SchedulesDao,
    private val tasksDao: TasksDao,
    private val syncState: SyncStateStore,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun sync(): SyncOutcome {
        val schedResp = try {
            api.schedules()
        } catch (e: IOException) {
            return markFailure(SyncFailure.OFFLINE)
        } catch (e: Exception) {
            return markFailure(SyncFailure.SERVER)
        }

        val tasksResp = try {
            api.tasks()
        } catch (e: IOException) {
            return markFailure(SyncFailure.OFFLINE)
        } catch (e: Exception) {
            return markFailure(SyncFailure.SERVER)
        }

        if (!schedResp.isSuccessful || !tasksResp.isSuccessful) {
            return markFailure(SyncFailure.SERVER) // 401 ditangani SessionExpiryInterceptor (auto-logout)
        }

        val schedBody = schedResp.body()
        val tasksBody = tasksResp.body()
        if (schedBody == null || tasksBody == null) {
            return markFailure(SyncFailure.SERVER) // body null (mis. 204 tanpa konten) — bukan crash
        }
        val schedules = schedBody.schedules.map { it.toEntity() }
        val tasks = tasksBody.tasks.map { it.toEntity(nowMs = clock.millis()) }

        schedulesDao.replaceAll(schedules)
        tasksDao.replaceAll(tasks)
        syncState.setLastSync(clock.millis(), "ok")
        return SyncOutcome.Success(schedules.size, tasks.size)
    }

    private suspend fun markFailure(reason: SyncFailure): SyncOutcome {
        syncState.setLastSync(clock.millis(), "error") // Room tidak disentuh — alarm tetap jalan dari data lokal
        return SyncOutcome.Failure(reason)
    }
}
```
- [ ] **4. Implementasi `SyncWorker.kt`** — periodic ±6 jam (network-constrained) = safety net reconciliation, bukan timing guarantee:
```kotlin
package com.aryariap.forfh.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.aryariap.forfh.ForfhApp
import java.util.concurrent.TimeUnit

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ForfhApp
        return when (inputData.getString(MODE)) {
            MODE_RECONCILE -> {
                app.container.rescheduler.reconcile()
                Result.success()
            }
            else -> when (val out = app.container.syncRepository.sync()) {
                is SyncOutcome.Success -> {
                    app.container.rescheduler.rescheduleAll() // via AlarmRescheduler — tidak pernah langsung
                    Result.success()
                }
                SyncOutcome.Failure(SyncFailure.OFFLINE) -> Result.retry()
                SyncOutcome.Failure(SyncFailure.SERVER) -> Result.success()
            }
        }
    }

    companion object {
        private const val MODE = "mode"
        private const val MODE_RECONCILE = "reconcile"

        /** Login sukses / tombol "Coba lagi" / pull-to-refresh. */
        fun enqueueOneShot(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workDataOf(MODE to "sync"))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("sync_once", ExistingWorkPolicy.REPLACE, request)
        }

        /** Safety net: ±6 jam, network-constrained — bukan timing guarantee (§8.7, §9). */
        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workDataOf(MODE to "sync"))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("sync_periodic", ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** BOOT_COMPLETED / MY_PACKAGE_REPLACED — reconcile dari Room, tanpa perlu network. */
        fun enqueueReconcile(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(workDataOf(MODE to MODE_RECONCILE))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("reconcile", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
```
- [ ] **5. Tulis `AppContainer.kt` FINAL** (semua dependensi; logout = cancel alarm → wipe Room → hapus DataStore+cookie; receiver pasca-logout → skip/cancel):
```kotlin
package com.aryariap.forfh

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.aryariap.forfh.alarm.AlarmFlowHandler
import com.aryariap.forfh.alarm.AlarmPlanner
import com.aryariap.forfh.alarm.AlarmScheduler
import com.aryariap.forfh.alarm.AndroidAlarmApi
import com.aryariap.forfh.alarm.ForfhNotifications
import com.aryariap.forfh.data.db.AppDatabase
import com.aryariap.forfh.data.prefs.Preferences
import com.aryariap.forfh.data.prefs.SecureCookieStore
import com.aryariap.forfh.data.prefs.SessionEvent
import com.aryariap.forfh.data.prefs.SessionManager
import com.aryariap.forfh.network.ApiClient
import com.aryariap.forfh.network.ForfhApiService
import com.aryariap.forfh.network.PersistentCookieJar
import com.aryariap.forfh.sync.AlarmRescheduler
import com.aryariap.forfh.sync.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(private val app: ForfhApp) {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.build(app) }

    private val dataStore: DataStore<Preferences> by lazy {
        androidx.datastore.preferences.preferenceDataStoreFactory.create(
            scope = applicationScope,
            produceFile = { app.preferencesDataStoreFile("forfh_prefs") },
        )
    }

    val prefs: Preferences by lazy { Preferences(dataStore) }
    val secureCookieStore: SecureCookieStore by lazy { SecureCookieStore(dataStore, applicationScope) }
    val sessionManager: SessionManager by lazy { SessionManager(secureCookieStore) }

    private val cookieJar: PersistentCookieJar by lazy {
        PersistentCookieJar(secureCookieStore, applicationScope)
    }

    val apiService: ForfhApiService by lazy {
        ApiClient.retrofit(ApiClient.build(cookieJar, sessionManager))
    }

    val planner: AlarmPlanner by lazy { AlarmPlanner() }
    val scheduler: AlarmScheduler by lazy { AlarmScheduler(AndroidAlarmApi(app)) }
    val rescheduler: AlarmRescheduler by lazy {
        AlarmRescheduler(planner, scheduler, database.scheduledAlarmsDao(), database.schedulesDao(), prefs)
    }
    val notifications: ForfhNotifications by lazy { ForfhNotifications(app) }

    val syncRepository: SyncRepository by lazy {
        SyncRepository(apiService, database.schedulesDao(), database.tasksDao(), prefs)
    }

    val alarmFlow: AlarmFlowHandler by lazy {
        AlarmFlowHandler(
            context = app,
            database = database,
            prefs = prefs,
            sessionManager = sessionManager,
            rescheduler = rescheduler,
            notifications = notifications,
            planner = planner,
            scope = applicationScope,
        )
    }

    /** Logout §8.10: cancel alarm → hapus scheduled_alarms → wipe Room + DataStore + cookie. */
    fun logout(message: String) {
        applicationScope.launch {
            rescheduler.cancelAll()
            database.scheduledAlarmsDao().clearAll()
            database.schedulesDao().clearAll()
            database.tasksDao().clearAll()
            secureCookieStore.clear()
            prefs.setLastSync(0L, "")
            sessionManager.tryEmitLoggedOut(message)
        }
    }

    /** Dipanggil MainActivity: daftarkan emitter kejadian sesi. */
    fun collectSessionEvents(onEvent: (SessionEvent) -> Unit) {
        applicationScope.launch { sessionManager.events.collect(onEvent) }
    }
}
```
- [ ] **6. Update `SessionManager.kt`** — tambah `tryEmitLoggedOut` (dipakai `AppContainer.logout`):
```kotlin
    /** Emisi LoggedOut dari alur logout eksplisit (bukan 401). */
    fun tryEmitLoggedOut(message: String) { _events.tryEmit(SessionEvent.LoggedOut(message)) }
```
- [ ] **7. Update `ForfhApp.kt`**:
```kotlin
package com.aryariap.forfh

import android.app.Application

class ForfhApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SyncWorker.enqueuePeriodic(this) // safety net ±6 jam; KEEP bila sudah ada
    }
}
```
  (Tambahkan import `com.aryariap.forfh.sync.SyncWorker`.)
- [ ] **8. Jalankan test, buktikan lulus** (2 test SyncRepository hijau); `assembleDebug` hijau.
- [ ] **9. Commit**: `git add -A && git commit -m "T10: SyncRepository + SyncWorker + AppContainer final + logout"`.

---
## T11 — Boot / package-replaced / exact-restored receivers + deferred path

**Files:**
- Test `app/src/test/java/com/aryariap/forfh/sync/RecoveryPlanTest.kt`
- Create `app/src/main/java/com/aryariap/forfh/sync/RecoveryPlan.kt`
- Create `app/src/main/java/com/aryariap/forfh/sync/BootReceiver.kt`
- Create `app/src/main/java/com/aryariap/forfh/sync/ExactAlarmPermissionReceiver.kt`

**Interfaces:**
- Consumes: `SyncWorker.enqueueReconcile` (T10), `AlarmRescheduler.rescheduleAll` (T8)
- Produces: recovery otomatis — BOOT_COMPLETED (pasca-unlock, deferred path) & MY_PACKAGE_REPLACED → reconcile via worker; exact access kembali → `rescheduleAll()` langsung.

**Langkah:**

- [ ] **1. Tulis test DULU (harus gagal)** — `RecoveryPlanTest.kt`:
```kotlin
package com.aryariap.forfh.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecoveryPlanTest {

    @Test
    fun `BOOT_COMPLETED dan MY_PACKAGE_REPLACED memicu reconcile`() {
        assertEquals(RecoveryPlan.Mode.RECONCILE, RecoveryPlan.modeFor("android.intent.action.BOOT_COMPLETED"))
        assertEquals(RecoveryPlan.Mode.RECONCILE, RecoveryPlan.modeFor("android.intent.action.MY_PACKAGE_REPLACED"))
    }

    @Test
    fun `action lain tidak memicu apa pun`() {
        assertNull(RecoveryPlan.modeFor("com.aryariap.forfh.action.CLASS_ALARM"))
        assertNull(RecoveryPlan.modeFor(null))
    }
}
```
- [ ] **2. Jalankan, buktikan gagal**: `.\gradlew.bat :app:testDebugUnitTest --tests "*RecoveryPlanTest*"` → unresolved reference.
- [ ] **3. Implementasi `RecoveryPlan.kt`**:
```kotlin
package com.aryariap.forfh.sync

object RecoveryPlan {
    enum class Mode { RECONCILE }

    /**
     * BOOT_COMPLETED dikirim hanya setelah unlock pertama (receiver non-directBootAware) —
     * deferred path: sebelum unlock, storage credential-encrypted tak bisa dibaca, tidak ada
     * alarm yang di-rebuild dan tidak ada yang tampil; begitu unlock, reconcile otomatis (spec §8.9).
     */
    fun modeFor(action: String?): Mode? = when (action) {
        "android.intent.action.BOOT_COMPLETED",
        "android.intent.action.MY_PACKAGE_REPLACED",
        -> Mode.RECONCILE
        else -> null
    }
}
```
- [ ] **4. Implementasi `BootReceiver.kt`**:
```kotlin
package com.aryariap.forfh.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Rebuild seluruh alarm dari Room setelah reboot / update APK (§8.9). Non-directBootAware. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (RecoveryPlan.modeFor(intent.action)) {
            RecoveryPlan.Mode.RECONCILE -> SyncWorker.enqueueReconcile(context)
            null -> Unit
        }
    }
}
```
- [ ] **5. Implementasi `ExactAlarmPermissionReceiver.kt`**:
```kotlin
package com.aryariap.forfh.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aryariap.forfh.ForfhApp
import kotlinx.coroutines.launch

/**
 * ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED (foreground broadcast) —
 * begitu access dikembalikan, rescheduleAll(): semua alarm kembali exact,
 * sesi snooze aktif dipertahankan (spec §8.3).
 */
class ExactAlarmPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as ForfhApp
        app.container.applicationScope.launch { app.container.rescheduler.rescheduleAll() }
    }
}
```
- [ ] **6. Jalankan test, buktikan lulus** (2 test hijau); `assembleDebug` hijau.
- [ ] **7. Commit**: `git add -A && git commit -m "T11: boot + package-replaced + exact-restored recovery"`.

---

## T12 — UI Compose: Login, Jadwal, Tugas, Pengaturan + navigasi + tema DNA web

**Files:**
- Test `app/src/test/java/com/aryariap/forfh/ui/UiFormatTest.kt`
- Create `app/src/main/java/com/aryariap/forfh/ui/theme/Color.kt`
- Create `app/src/main/java/com/aryariap/forfh/ui/theme/Type.kt`
- Create `app/src/main/java/com/aryariap/forfh/ui/theme/Theme.kt`
- Create `app/src/main/java/com/aryariap/forfh/ui/UiFormat.kt`
- Create `app/src/main/java/com/aryariap/forfh/ui/ForfhAppRoot.kt`
- Create `app/src/main/java/com/aryariap/forfh/ui/login/LoginScreen.kt`
- Create `app/src/main/java/com/aryariap/forfh/ui/login/LoginViewModel.kt`
- Create `app/src/main/java/com/aryariap/forfh/ui/jadwal/JadwalScreen.kt`
- Create `app/src/main/java/com/aryariap/forfh/ui/jadwal/JadwalViewModel.kt`
- Create `app/src/main/java/com/aryariap/forfh/ui/tugas/TugasListScreen.kt`
- Create `app/src/main/java/com/aryariap/forfh/ui/tugas/TugasDetailScreen.kt`
- Create `app/src/main/java/com/aryariap/forfh/ui/tugas/TugasViewModel.kt`
- Create `app/src/main/java/com/aryariap/forfh/ui/pengaturan/PengaturanScreen.kt`
- Create `app/src/main/java/com/aryariap/forfh/ui/pengaturan/PengaturanViewModel.kt`
- Create (ganti) `app/src/main/java/com/aryariap/forfh/MainActivity.kt`

**Interfaces:**
- Consumes: `AppContainer` (T10), DAO (T2), `Preferences` (T3), `SessionManager` (T3), `LoginErrorMapper` (T4), `SyncWorker` (T10), `SyncRepository` (T10), `AlarmRescheduler` (T8)
- Produces: seluruh layar V1 + navigasi + tap notif → halaman Tugas (REQ-19).

**Langkah:**

- [ ] **1. Tulis test DULU (harus gagal)** — `UiFormatTest.kt`:
```kotlin
package com.aryariap.forfh.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class UiFormatTest {
    private val zone = ZoneId.of("Asia/Jakarta")

    @Test
    fun `deadline epoch millis diformat WIB`() {
        // 2026-08-20T03:00:00.000Z = 2026-08-20 10:00 WIB
        assertEquals("21 Agu 2026 · 10:00", UiFormat.deadline(1_787_281_200_000L, zone)) // 1_787_281_200_000 = 21 Agu 03:00Z = 10:00 WIB
    }

    @Test
    fun `null deadline jadi label tak berdeadline`() {
        assertEquals("Tanpa deadline", UiFormat.deadline(null, zone))
    }

    @Test
    fun `range jam dari start dan end`() {
        assertEquals("08:00–09:40", UiFormat.range("08:00", "09:40"))
    }

    @Test
    fun `label status tugas`() {
        assertEquals("Belum", UiFormat.statusLabel("NOT_STARTED"))
        assertEquals("Selesai", UiFormat.statusLabel("DONE"))
        assertEquals("Terlambat", UiFormat.statusLabel("OVERDUE"))
        assertEquals("Proses", UiFormat.statusLabel("IN_PROGRESS"))
        assertEquals("Revisi", UiFormat.statusLabel("REVISION"))
    }

    @Test
    fun `nama hari dan indeks WIB`() {
        assertEquals("Minggu", UiFormat.dayName(0))
        assertEquals("Senin", UiFormat.dayName(1))
        assertEquals("Sabtu", UiFormat.dayName(6))
    }
}
```
- [ ] **2. Jalankan, buktikan gagal**: `.\gradlew.bat :app:testDebugUnitTest --tests "*UiFormatTest*"` → unresolved reference.
- [ ] **3. Implementasi `UiFormat.kt`** (format WIB eksplisit — spec §8.8):
```kotlin
package com.aryariap.forfh.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object UiFormat {
    private val deadlineFmt = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale("id", "ID"))
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    /** dueAt epoch ms → tanggal WIB. Null → "Tanpa deadline". */
    fun deadline(dueAtEpochMillis: Long?, zone: ZoneId): String {
        if (dueAtEpochMillis == null) return "Tanpa deadline"
        return Instant.ofEpochMilli(dueAtEpochMillis).atZone(zone).format(deadlineFmt)
    }

    fun range(start: String, end: String): String = "$start–$end"

    fun statusLabel(status: String): String = when (status) {
        "DONE" -> "Selesai"
        "OVERDUE" -> "Terlambat"
        "IN_PROGRESS" -> "Proses"
        "REVISION" -> "Revisi"
        else -> "Belum"
    }

    fun dayName(dayOfWeek: Int): String = when (dayOfWeek) { // 0=Sunday..6=Saturday
        0 -> "Minggu"; 1 -> "Senin"; 2 -> "Selasa"; 3 -> "Rabu"
        4 -> "Kamis"; 5 -> "Jumat"; else -> "Sabtu"
    }

    fun timeText(t: String): String = t.take(5)
}
```
- [ ] **4. Implementasi tema DNA web ForFH** (warna disalin dari `globals.css` web ForFH — light & dark; `Color.kt`):
```kotlin
package com.aryariap.forfh.ui.theme

import androidx.compose.ui.graphics.Color

// Token warna DNA web ForFH (src/app/globals.css)
object ForfhColors {
    val Accent = Color(0xFF3D5A80)          // --accent light
    val AccentHover = Color(0xFF2C4A6E)     // --accent-hover light
    val AccentSubtle = Color(0xFFEEF2F7)    // --accent-subtle light
    val AccentDark = Color(0xFF6B9AC4)      // --accent dark
    val AccentHoverDark = Color(0xFF8BB4D9) // --accent-hover dark
    val CanvasLight = Color(0xFFFAF9F7)     // --bg-canvas light
    val Surface1Light = Color(0xFFFFFFFF)   // --bg-surface-1 light
    val TextPrimaryLight = Color(0xFF1A1A1A)
    val TextSecondaryLight = Color(0xFF64635E)
    val BorderStrongLight = Color(0xFFD5D1CA)
    val CanvasDark = Color(0xFF141413)      // --bg-canvas dark
    val Surface1Dark = Color(0xFF1E1D1B)    // --bg-surface-1 dark
    val Surface2Dark = Color(0xFF252422)    // --bg-surface-2 dark
    val TextPrimaryDark = Color(0xFFEEECE8)
    val TextSecondaryDark = Color(0xFFA5A39E)
    val BorderStrongDark = Color(0xFF3D3B37)
    val Danger = Color(0xFFC53030)
    val Success = Color(0xFF276749)
    val Warning = Color(0xFFB7791F)
}
```
- [ ] **5. `Type.kt`** (serif italic utk logo, mono uppercase utk label — DNA editorial/mono):
```kotlin
package com.aryariap.forfh.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ForfhTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Medium,
        fontSize = 44.sp,
    ),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 22.sp),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        fontSize = 13.sp,
    ),
    bodyMedium = TextStyle(fontSize = 14.sp),
)
```
- [ ] **6. `Theme.kt`** (dark/light ikut sistem; material3):
```kotlin
package com.aryariap.forfh.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = ForfhColors.Accent,
    onPrimary = Color.White,
    primaryContainer = ForfhColors.AccentSubtle,
    onPrimaryContainer = ForfhColors.AccentHover,
    background = ForfhColors.CanvasLight,
    onBackground = ForfhColors.TextPrimaryLight,
    surface = ForfhColors.Surface1Light,
    onSurface = ForfhColors.TextPrimaryLight,
    surfaceVariant = Color(0xFFF5F3F0),
    onSurfaceVariant = ForfhColors.TextSecondaryLight,
    outline = ForfhColors.BorderStrongLight,
    error = ForfhColors.Danger,
    onError = Color.White,
    secondary = ForfhColors.Warning,
    tertiary = ForfhColors.Success,
)

private val DarkScheme = darkColorScheme(
    primary = ForfhColors.AccentDark,
    onPrimary = ForfhColors.CanvasDark,
    primaryContainer = ForfhColors.AccentHover,
    onPrimaryContainer = ForfhColors.AccentSubtle,
    background = ForfhColors.CanvasDark,
    onBackground = ForfhColors.TextPrimaryDark,
    surface = ForfhColors.Surface1Dark,
    onSurface = ForfhColors.TextPrimaryDark,
    surfaceVariant = ForfhColors.Surface2Dark,
    onSurfaceVariant = ForfhColors.TextSecondaryDark,
    outline = ForfhColors.BorderStrongDark,
    error = ForfhColors.Danger,
    onError = Color.White,
)

@Composable
fun ForfhTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = ForfhTypography,
        content = content,
    )
}
```
- [ ] **7. Implementasi `LoginViewModel.kt`** (session cookie dipegang CookieJar; sukses → `onLoggedIn` + sync langsung):
```kotlin
package com.aryariap.forfh.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryariap.forfh.AppContainer
import com.aryariap.forfh.network.ErrorBody
import com.aryariap.forfh.network.LoginErrorMapper
import com.aryariap.forfh.network.LoginRequest
import com.aryariap.forfh.sync.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import kotlinx.serialization.json.Json

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

class LoginViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun onEmailChange(v: String) { _state.value = _state.value.copy(email = v, error = null) }
    fun onPasswordChange(v: String) { _state.value = _state.value.copy(password = v, error = null) }

    fun login() {
        val s = _state.value
        if (s.loading) return
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(error = "Isi email dan password.")
            return
        }
        _state.value = s.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val resp = container.apiService.login(LoginRequest(s.email.trim(), s.password))
                if (resp.isSuccessful && resp.body()?.success == true) {
                    // cookie sesi sudah tersimpan terenkripsi oleh CookieJar → sync pertama
                    container.sessionManager.onLoggedIn()
                    SyncWorker.enqueueOneShot(container.context)
                    _state.value = _state.value.copy(loading = false, success = true)
                } else {
                    val code = resp.code()
                    val serverMsg = resp.errorBody()?.let { body ->
                        runCatching {
                            Json { ignoreUnknownKeys = true }
                                .decodeFromString<ErrorBody>(body.string()).error
                        }.getOrNull()
                    }
                    _state.value = _state.value.copy(
                        loading = false,
                        error = LoginErrorMapper.map(code, serverMsg),
                    )
                }
            } catch (e: IOException) {
                _state.value = _state.value.copy(loading = false, error = LoginErrorMapper.mapNetwork())
            } catch (e: Exception) {
                // kegagalan selain network (mis. parse/timeout) → pesan umum login gagal
                _state.value = _state.value.copy(loading = false, error = LoginErrorMapper.map(502, null))
            }
        }
    }
}
```
  > Tambahkan 1 baris di `AppContainer.kt` (T10) agar ViewModel bisa enqueue worker: `val context: android.content.Context get() = app`.
- [ ] **8. Implementasi `LoginScreen.kt`** — logo serif italic + tagline mono uppercase (DNA web ForFH):
```kotlin
package com.aryariap.forfh.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.success) { if (state.success) Unit /* root pindah halaman via session event */ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "ForFH",
            style = MaterialTheme.typography.displayLarge, // serif italic (Type.kt)
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "JADWAL KULIAH · TUGAS · PENGINGAT",
            style = MaterialTheme.typography.labelLarge, // mono uppercase (Type.kt)
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(40.dp))
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email kampus") },
            singleLine = true,
            enabled = !state.loading,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            enabled = !state.loading,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = viewModel::login,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !state.loading,
        ) {
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Masuk")
            }
        }
        state.error?.let { msg ->
            Spacer(Modifier.height(16.dp))
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Akses terbatas untuk mahasiswa FH UNAIR.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```
- [ ] **9. Implementasi `JadwalViewModel.kt`** — sumber kebenaran Room (mirror hasil sync):
```kotlin
package com.aryariap.forfh.ui.jadwal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryariap.forfh.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime

data class JadwalItem(
    val id: String,
    val courseName: String,
    val courseCode: String?,
    val startTime: String,
    val endTime: String,
    val room: String?,
    val onlineUrl: String?,
    val color: String,
    val enabled: Boolean,
    val dayIndex: Int, // 0=Sunday .. 6=Saturday (konvensi API ForFH)
)

data class JadwalHari(
    val dayIndex: Int,
    val label: String,
    val items: List<JadwalItem>,
)

data class JadwalUiState(
    val today: List<JadwalItem> = emptyList(),
    val week: List<JadwalHari> = emptyList(),
    val lastSyncStatus: String = "",
    val lastSyncAt: Long = 0L,
)

class JadwalViewModel(private val container: AppContainer) : ViewModel() {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val _state = MutableStateFlow(JadwalUiState())
    val state: StateFlow<JadwalUiState> = _state

    init {
        viewModelScope.launch {
            container.database.schedulesDao().getAll().collect { entities ->
                val todayIdx = ZonedDateTime.now(zone).dayOfWeek.value % 7 // Senin=1..Minggu=7 → 0=Sunday
                val items = entities.map {
                    JadwalItem(
                        id = it.id,
                        courseName = it.courseName,
                        courseCode = it.courseCode,
                        startTime = it.startTime,
                        endTime = it.endTime,
                        room = it.room,
                        onlineUrl = it.onlineUrl,
                        color = it.courseColor,
                        enabled = it.enabled,
                        dayIndex = it.dayOfWeek,
                    )
                }
                _state.value = _state.value.copy(
                    today = items.filter { it.dayIndex == todayIdx },
                    week = (0..6).map { d ->
                        JadwalHari(d, UiFormat.dayName(d), items.filter { it.dayIndex == d })
                    },
                )
            }
        }
        viewModelScope.launch {
            container.prefs.lastSyncStatus.collect { s -> _state.value = _state.value.copy(lastSyncStatus = s) }
        }
        viewModelScope.launch {
            container.prefs.lastSyncAt.collect { t -> _state.value = _state.value.copy(lastSyncAt = t) }
        }
    }
}
```
- [ ] **10. Implementasi `JadwalScreen.kt`** — tab "Hari ini" / "Seminggu", kartu kuliah warna course:
```kotlin
package com.aryariap.forfh.ui.jadwal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aryariap.forfh.ui.UiFormat
import com.aryariap.forfh.ui.theme.ForfhColors

@Composable
fun JadwalScreen(viewModel: JadwalViewModel) {
    val state by viewModel.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                Text(
                    text = "Jadwal",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Hari ini") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Seminggu") })
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (tab == 0) {
                items(state.today) { item -> KuliahCard(item) }
            } else {
                items(state.week) { hari ->
                    if (hari.items.isNotEmpty()) {
                        Text(
                            text = hari.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        hari.items.forEach { KuliahCard(it) }
                    }
                }
            }
            item {
                Text(
                    text = "Terakhir sinkron: ${UiFormat.syncInfo(state.lastSyncStatus, state.lastSyncAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun KuliahCard(item: JadwalItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .background(runCatching { Color(android.graphics.Color.parseColor(item.color)) }
                        .getOrDefault(ForfhColors.Accent)),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = item.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(UiFormat.range(item.startTime, item.endTime))
                        when {
                            item.onlineUrl != null -> append(" · Daring")
                            !item.room.isNullOrBlank() -> append(" · ${item.room}")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.courseCode?.let { code ->
                    Text(
                        text = code,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
```
  > Tambahkan 1 fungsi di `UiFormat.kt`: `fun syncInfo(status: String, lastSyncAt: Long): String = if (status == "ok") "berhasil ${Instant.ofEpochMilli(lastSyncAt).atZone(ZoneId.of("Asia/Jakarta")).format(timeFmt)}" else if (status == "error") "gagal — coba lagi" else "belum pernah"` (import `java.time.Instant`; `timeFmt` privat sudah ada).
- [ ] **11. Implementasi `TugasViewModel.kt`** — daftar dari Room, markDone via PUT server, update lokal HANYA setelah sukses:
```kotlin
package com.aryariap.forfh.ui.tugas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryariap.forfh.AppContainer
import com.aryariap.forfh.network.MarkDoneRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId

data class TugasItem(
    val id: String,
    val title: String,
    val courseName: String?,
    val courseCode: String?,
    val courseColor: String?,
    val dueAt: Long?,
    val status: String,
    val computedStatus: String?,
    val priority: String,
    val description: String?,
)

data class TugasUiState(
    val items: List<TugasItem> = emptyList(),
    val detail: TugasItem? = null,
    val message: String? = null, // hasil aksi terakhir (mis. error markDone)
)

class TugasViewModel(private val container: AppContainer) : ViewModel() {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val _state = MutableStateFlow(TugasUiState())
    val state: StateFlow<TugasUiState> = _state

    init {
        viewModelScope.launch {
            container.database.tasksDao().getAll().collect { entities ->
                val detailId = _state.value.detail?.id
                _state.value = _state.value.copy(
                    items = entities.map { it.toItem() },
                    detail = entities.firstOrNull { it.id == detailId }?.toItem()
                        ?: _state.value.detail,
                )
            }
        }
    }

    fun openDetail(id: String) {
        viewModelScope.launch {
            container.database.tasksDao().getById(id).collect { entity ->
                _state.value = _state.value.copy(detail = entity?.toItem())
            }
        }
    }

    fun closeDetail() {
        _state.value = _state.value.copy(detail = null, message = null)
    }

    /**
     * Server sumber kebenaran (invariant §7, REQ-13): PUT sukses → baru update Room.
     * Gagal → tugas tetap utuh, user diberi tahu.
     */
    fun markDone(taskId: String) {
        viewModelScope.launch {
            val resp = container.apiService.markDone(taskId, MarkDoneRequest("DONE"))
            if (resp.isSuccessful) {
                container.database.tasksDao().updateStatus(taskId, "DONE", null)
                _state.value = _state.value.copy(message = "Tugas ditandai selesai.")
            } else {
                _state.value = _state.value.copy(message = "Gagal menandai selesai. Cek koneksi, coba lagi.")
            }
        }
    }

    private fun com.aryariap.forfh.data.db.TaskEntity.toItem() = TugasItem(
        id = id,
        title = title,
        courseName = courseName,
        courseCode = courseCode,
        courseColor = courseColor,
        dueAt = dueAt,
        status = status,
        computedStatus = computedStatus,
        priority = priority,
        description = description,
    )
}
```
- [ ] **12. Implementasi `TugasListScreen.kt`** — status chip dinamis (OVERDUE merah), deadline WIB, NULLS LAST:
```kotlin
package com.aryariap.forfh.ui.tugas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aryariap.forfh.ui.UiFormat
import com.aryariap.forfh.ui.theme.ForfhColors
import java.time.ZoneId

@Composable
fun TugasListScreen(viewModel: TugasViewModel) {
    val state by viewModel.state.collectAsState()
    val zone = ZoneId.of("Asia/Jakarta")

    LaunchedEffect(state.message) {
        if (state.message != null && state.detail == null) {
            // snackbar singkat — pesan aksi; di-V1 pakai Text banner sederhana
        }
    }

    if (state.detail != null) {
        TugasDetailScreen(viewModel = viewModel, taskId = state.detail!!.id)
        return
    }

    Scaffold(
        topBar = {
            Text(
                text = "Tugas",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.items.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada tugas. Tarik untuk sinkronkan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.items) { item ->
                Card(
                    onClick = { viewModel.openDetail(item.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        val dot = runCatching { Color(android.graphics.Color.parseColor(item.courseColor ?: "#3b82f6")) }
                            .getOrDefault(ForfhColors.Accent)
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(8.dp)
                                .background(dot, RoundedCornerShape(4.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textDecoration = if (item.status == "DONE") TextDecoration.LineThrough else null,
                            )
                            Text(
                                text = buildString {
                                    item.courseName?.let { append(it) }
                                    item.dueAt?.let { due ->
                                        if (isNotEmpty()) append(" · ")
                                        append("Deadline ${UiFormat.deadline(due, zone)}")
                                    }
                                }.ifEmpty { "Tanpa deadline" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        StatusChip(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(item: TugasItem) {
    val (label, bg, fg) = when {
        item.status == "DONE" -> Triple("Selesai", ForfhColors.Success, Color.White)
        item.computedStatus == "OVERDUE" || item.status == "OVERDUE" ->
            Triple("Terlambat", ForfhColors.Danger, Color.White)
        item.status == "IN_PROGRESS" -> Triple("Proses", ForfhColors.Accent, Color.White)
        item.status == "REVISION" -> Triple("Revisi", ForfhColors.Warning, Color.White)
        else -> Triple("Belum", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = fg,
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
```
- [ ] **13. Implementasi `TugasDetailScreen.kt`** — detail tugas + tombol "Tandai selesai" (hilang bila DONE):
```kotlin
package com.aryariap.forfh.ui.tugas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextDecoration
import com.aryariap.forfh.network.SubtaskDto
import com.aryariap.forfh.ui.UiFormat
import java.time.ZoneId
import kotlinx.serialization.json.Json

@Composable
fun TugasDetailScreen(viewModel: TugasViewModel, taskId: String) {
    val state by viewModel.state.collectAsState()
    val zone = ZoneId.of("Asia/Jakarta")
    val item = state.detail

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { viewModel.closeDetail() }) { Text("← Kembali") }
        }
        if (item == null) {
            Text("Tugas tidak ditemukan.", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        Text(item.title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        item.courseName?.let {
            Text("$it ${item.courseCode ?: ""}", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = buildString {
                append("Status: ${UiFormat.statusLabel(if (item.status == "DONE") "DONE" else item.computedStatus ?: item.status)}")
                append(" · Prioritas: ${item.priority.replaceFirstChar { it.uppercase() }}")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Deadline: ${UiFormat.deadline(item.dueAt, zone)}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.computedStatus == "OVERDUE" && item.status != "DONE") MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = item.description ?: "Tidak ada deskripsi.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Text("Subtugas", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        val subtasks = item.subtasksJson?.let {
            runCatching { Json.decodeFromString<List<SubtaskDto>>(it) }.getOrNull()
        } ?: emptyList()
        if (subtasks.isEmpty()) {
            Text(
                "Tidak ada subtugas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            subtasks.forEach { st ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (st.completed == 1) "✓ " else "• ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (st.completed == 1) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        st.title,
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (st.completed == 1) TextDecoration.LineThrough else null,
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        if (item.status != "DONE") {
            Button(
                onClick = { viewModel.markDone(item.id) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Tandai selesai") }
        }
        state.message?.let { msg ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = if (msg.startsWith("Gagal")) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}
```
- [ ] **14. Implementasi `PengaturanViewModel.kt`** — toggle offset → rescheduleAll (spec §8.1); sync manual; logout:
```kotlin
package com.aryariap.forfh.ui.pengaturan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryariap.forfh.AppContainer
import com.aryariap.forfh.data.prefs.AlarmOffsets
import com.aryariap.forfh.sync.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PengaturanUiState(
    val offsets: AlarmOffsets = AlarmOffsets(true, true, true),
    val lastSyncStatus: String = "",
    val lastSyncAt: Long = 0L,
)

class PengaturanViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(PengaturanUiState())
    val state: StateFlow<PengaturanUiState> = _state

    init {
        viewModelScope.launch { container.prefs.offsets.collect { o -> _state.value = _state.value.copy(offsets = o) } }
        viewModelScope.launch { container.prefs.lastSyncStatus.collect { s -> _state.value = _state.value.copy(lastSyncStatus = s) } }
        viewModelScope.launch { container.prefs.lastSyncAt.collect { t -> _state.value = _state.value.copy(lastSyncAt = t) } }
    }

    /** Ubah offset → rescheduleAll: alarm lama di-cancel, alarm baru dipasang; snooze aktif tetap (ReconcilePlanner). */
    fun setOffset(offsetMinutes: Int, enabled: Boolean) {
        viewModelScope.launch {
            val cur = _state.value.offsets
            val next = AlarmOffsets(
                offset3h = if (offsetMinutes == 180) enabled else cur.offset3h,
                offset2h = if (offsetMinutes == 120) enabled else cur.offset2h,
                offset1h = if (offsetMinutes == 60) enabled else cur.offset1h,
            )
            container.prefs.setOffsets(next)
            container.rescheduler.rescheduleAll()
        }
    }

    fun syncNow() { SyncWorker.enqueueOneShot(container.context) }

    fun logout() { container.logout("Kamu sudah keluar.") }
}
```
- [ ] **15. Implementasi `PengaturanScreen.kt`** — toggle offset, sinkron, izin sistem (notifikasi / alarm presisi / FSI), petunjuk HyperOS, versi, keluar:
```kotlin
package com.aryariap.forfh.ui.pengaturan

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aryariap.forfh.BuildConfig
import com.aryariap.forfh.ui.UiFormat

@Composable
fun PengaturanScreen(viewModel: PengaturanViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Pengaturan", style = MaterialTheme.typography.titleLarge)

        Text("Pengingat kuliah", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        ToggleRow("3 jam sebelum", state.offsets.offset3h) { viewModel.setOffset(180, it) }
        ToggleRow("2 jam sebelum", state.offsets.offset2h) { viewModel.setOffset(120, it) }
        ToggleRow("1 jam sebelum", state.offsets.offset1h) { viewModel.setOffset(60, it) }

        HorizontalDivider()

        Button(onClick = { viewModel.syncNow() }, modifier = Modifier.fillMaxWidth()) {
            Text("Sinkronkan sekarang")
        }
        Text(
            text = "Terakhir sinkron: ${UiFormat.syncInfo(state.lastSyncStatus, state.lastSyncAt)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()

        Text("Izin perangkat", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        if (Build.VERSION.SDK_INT >= 33) {
            OutlinedButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }, modifier = Modifier.fillMaxWidth()) { Text("Izin notifikasi") }
        }
        if (Build.VERSION.SDK_INT >= 31) {
            OutlinedButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.parse("package:${context.packageName}")))
            }, modifier = Modifier.fillMaxWidth()) { Text("Alarm presisi (buka setelan)") }
        }
        if (Build.VERSION.SDK_INT >= 34) {
            OutlinedButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                    .setData(Uri.parse("package:${context.packageName}")))
            }, modifier = Modifier.fillMaxWidth()) { Text("Alarm layar penuh (buka setelan)") }
        }
        Text(
            text = "Petunjuk MIUI/HyperOS: jika alarm tidak berbunyi, buka Setelan > Aplikasi > ForFH, " +
                "aktifkan izin \"Alarm & pengingat\" dan \"Buka di layar kunci\", lalu nonaktifkan penghemat baterai.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()

        OutlinedButton(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Keluar", color = MaterialTheme.colorScheme.error) }

        Text(
            text = "ForFH ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
```
- [ ] **16. Implementasi `ForfhAppRoot.kt`** — navigasi login/main via session events, tab bawah, detail tugas di dalam tab Tugas:
```kotlin
package com.aryariap.forfh.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aryariap.forfh.AppContainer
import com.aryariap.forfh.ForfhApp
import com.aryariap.forfh.data.prefs.SessionEvent
import com.aryariap.forfh.ui.jadwal.JadwalScreen
import com.aryariap.forfh.ui.jadwal.JadwalViewModel
import com.aryariap.forfh.ui.login.LoginScreen
import com.aryariap.forfh.ui.login.LoginViewModel
import com.aryariap.forfh.ui.pengaturan.PengaturanScreen
import com.aryariap.forfh.ui.pengaturan.PengaturanViewModel
import com.aryariap.forfh.ui.tugas.TugasListScreen
import com.aryariap.forfh.ui.tugas.TugasViewModel

private object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val MAIN = "main"
}

/** Factory sederhana: ViewModel dibangun dari AppContainer (bukan AndroidViewModel). */
fun <T : ViewModel> simpleFactory(create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
    }

@Composable
fun ForfhAppRoot(container: AppContainer, openTasks: Boolean) {
    val navController = rememberNavController()

    // Jalur awal: cek cookie sesi (terenkripsi di Keystore) — splash sesaat lalu login/main
    LaunchedEffect(Unit) {
        val loggedIn = container.sessionManager.isLoggedIn()
        navController.navigate(if (loggedIn) Routes.MAIN else Routes.LOGIN) { popUpTo(0) }
    }

    // Auto-logout (401) & login sukses → pindah halaman (spec §10)
    LaunchedEffect(Unit) {
        container.sessionManager.events.collect { ev ->
            when (ev) {
                SessionEvent.LoggedIn ->
                    navController.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
                is SessionEvent.LoggedOut ->
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
            }
        }
    }

    NavHost(navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { /* sesaat, lalu navigate oleh efek di atas */ }
        composable(Routes.LOGIN) {
            val vm: LoginViewModel = viewModel(factory = simpleFactory { LoginViewModel(container) })
            LoginScreen(vm)
        }
        composable(Routes.MAIN) {
            MainScaffold(container = container, openTasks = openTasks)
        }
    }
}

@Composable
private fun MainScaffold(container: AppContainer, openTasks: Boolean) {
    var tab by rememberSaveable { mutableIntStateOf(if (openTasks) 1 else 0) }
    val context = LocalContext.current
    val containerApp = (context.applicationContext as ForfhApp).container

    val jadwalVm: JadwalViewModel = viewModel(factory = simpleFactory { JadwalViewModel(containerApp) })
    val tugasVm: TugasViewModel = viewModel(factory = simpleFactory { TugasViewModel(containerApp) })
    val pengaturanVm: PengaturanViewModel = viewModel(factory = simpleFactory { PengaturanViewModel(containerApp) })

    val tabs = listOf(
        Triple("Jadwal", Icons.Filled.DateRange as ImageVector) { JadwalScreen(jadwalVm) },
        Triple("Tugas", Icons.AutoMirrored.Filled.List) { TugasListScreen(tugasVm) },
        Triple("Atur", Icons.Filled.Settings) { PengaturanScreen(pengaturanVm) },
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, (label, icon, _) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                0 -> JadwalScreen(jadwalVm)
                1 -> TugasListScreen(tugasVm)
                2 -> PengaturanScreen(pengaturanVm)
            }
        }
    }
}
```
  > Penyederhanaan: tab 1 (`Tugas`) menampilkan `TugasListScreen` yang internalnya switch ke `TugasDetailScreen` saat `detail != null` (lihat langkah 12–13) — detail bukan route NavHost terpisah, cukup utk V1. Padding bottom bar dipakai langsung lewat `Box(Modifier.padding(padding))` — tidak ada `LocalForfhTabs` (CompositionLocal itu tidak didefinisikan; Scaffold padding diserahkan ke Box).
- [ ] **17. Ganti `MainActivity.kt`** (versi T1 minimal → root lengkap + intent notif tugas):
```kotlin
package com.aryariap.forfh

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aryariap.forfh.ui.ForfhAppRoot
import com.aryariap.forfh.ui.theme.ForfhTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // edge-to-edge wajib (targetSdk 36)
        val app = application as ForfhApp
        val openTasks = intent.getBooleanExtra("open_tasks", false) // notif tugas (T7)
        setContent {
            ForfhTheme {
                ForfhAppRoot(container = app.container, openTasks = openTasks)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // tap notif kedua saat app sudah hidup (launchMode CLEAR_TOP) → extra open_tasks fresh
        recreate()
    }
}
```
- [ ] **18. Update `UiFormat.kt`** — tambah `syncInfo` (dipakai JadwalScreen & PengaturanScreen):
```kotlin
    /** "berhasil HH:mm" / "gagal — coba lagi" / "belum pernah" utk label sinkronisasi. */
    fun syncInfo(status: String, lastSyncAt: Long): String = when (status) {
        "ok" -> "berhasil ${Instant.ofEpochMilli(lastSyncAt).atZone(ZoneId.of("Asia/Jakarta")).format(timeFmt)}"
        "error" -> "gagal — coba lagi"
        else -> "belum pernah"
    }
```
  (Tambahkan import `java.time.Instant` di `UiFormat.kt`.)
- [ ] **19. Update `AppContainer.kt`** — 1 baris akses konteks utk worker dari ViewModel:
```kotlin
    val context: android.content.Context get() = app
```
- [ ] **20. Jalankan test, buktikan lulus**: `.\gradlew.bat :app:testDebugUnitTest --tests "*UiFormatTest*"` → 5 test hijau; `.\gradlew.bat :app:assembleDebug` hijau.
- [ ] **21. Commit**: `git add -A && git commit -m "T12: UI Compose lengkap + navigasi + tema DNA web"`.

---

## T13 — Release build: keystore lokal, keystore.properties, README, assembleRelease

**Files:**
- Create `keystore.properties` (JANGAN di-commit — sudah di .gitignore T1)
- Create `README.md` (root repo)
- Modify `app/build.gradle.kts` (release signing dari keystore.properties — skeleton sudah ada di T1, lengkapi kini)
- None: keystore `.jks` dibuat di folder pribadi user, TIDAK pernah masuk repo (§2: `keystore.properties` dan `.jks` di .gitignore)

**Interfaces:**
- Consumes: `keystore.properties` (T1 build script sudah membaca bila file ada), release signingConfig
- Produces: `app/build/outputs/apk/release/app-release.apk` — diinstall langsung (bukan Play Store; spec §11 hanya menyebutkan Redmi install manual).

**Langkah:**

- [ ] **1. Buat keystore lokal** (sekali saja, simpan di luar repo — mis. `%USERPROFILE%\.android\forfh-release.jks`):
```powershell
keytool -genkeypair -v -keystore "$env:USERPROFILE\.android\forfh-release.jks" `
  -alias forfh -keyalg RSA -keysize 2048 -validity 10000 `
  -storepass RAHASIA_ANDA -keypass RAHASIA_ANDA `
  -dname "CN=ForFH Android, OU=ForFH, O=Arya Rizky, L=Surabaya, ST=Jawa Timur, C=ID"
```
  > Ganti `RAHASIA_ANDA` dengan passphrase kuat. Catat di password manager. Bila keystore hilang, update app tidak bisa ditandatangani ulang — backup `forfh-release.jks` + passphrase.
- [ ] **2. Tulis `keystore.properties`** (di root repo, diabaikan git):
```properties
storeFile=C:\\Users\\Arya Rizky\\.android\\forfh-release.jks
storePassword=RAHASIA_ANDA
keyAlias=forfh
keyPassword=RAHASIA_ANDA
```
- [ ] **3. Lengkapi `app/build.gradle.kts`** — release signing (pastikan blok ini ada; `keystore.properties` dibaca dari root project):
```kotlin
import java.util.Properties

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    // ... konfigurasi yang sudah ada dari T1 ...

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false // V1: biarkan debug-size; R8 dapat diaktifkan di versi berikutnya
            isShrinkResources = false
            signingConfig = if (keystoreProps.isNotEmpty()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug") // dev tanpa keystore.properties tetap bisa build
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```
- [ ] **4. Tulis `README.md`**:
````markdown
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
````
- [ ] **5. Verifikasi**: `.\gradlew.bat :app:assembleRelease` → `BUILD SUCCESSFUL`, `app/build/outputs/apk/release/app-release.apk` ada dan ditandatangani release.
- [ ] **6. Commit**: `git add -A && git commit -m "T13: release signing + README"` (pastikan `git status` tidak memuat `keystore.properties` / `.jks` — .gitignore T1 sudah menutup).
- [ ] **7. Verifikasi akhir (acceptance)** — manual di Redmi, sesuai spec §12:
  - [ ] Login email+password kampus → jadwal & tugas tampil dari server.
  - [ ] Alarm kuliah: pasang jadwal Senin 08:00, offset 1 jam → Minggu 07:00 berbunyi (exact, layar menyala saat terkunci).
  - [ ] Snooze: "Tidur lagi 3 menit" → bunyi ulang +3 menit; setelah 5× tombol hilang.
  - [ ] Tugas: notif 09:00 "tidak ada tugas" / daftar + deadline; 15:00 & 20:00 rekap.
  - [ ] Matikan izin alarm presisi → fallback setWindow (jendela mulai dari trigger); aktifkan lagi → langsung exact.
  - [ ] Reboot ponsel → setelah unlock, alarm terpasang ulang (tidak double).
  - [ ] Update APK (install ulang) → alarm tetap, tidak dobel.
  - [ ] Logout → semua alarm batal, data lokal & cookie terhapus; login baru bersih.
  - [ ] Sesi 30 hari habis / 401 authed → auto-logout "Sesi berakhir, masuk lagi."

---

## Verifikasi global (dijalankan setiap akhir task)

```powershell
.\gradlew.bat :app:testDebugUnitTest   # seluruh unit test hijau
.\gradlew.bat :app:assembleDebug       # build hijau
```

## Referensi silang kebutuhan

| Task | REQ yang dipenuhi |
|---|---|
| T1 | REQ-01..04 (env, gradle, manifest izin) |
| T2 | REQ-14 (mirror Room), REQ-22 (kolom tugas) |
| T3 | REQ-15 (DataStore), REQ-17 (cookie terenkripsi), REQ-38 (sinkron 30 hari) |
| T4 | REQ-02 (login), REQ-13 (endpoint terbatas), REQ-16 (koneksi) |
| T5 | REQ-23..24 (offset & next occurrence), REQ-32 (WIB) |
| T6 | REQ-25 (exact + fallback setWindow), REQ-33 (stableHash, FLAG_IMMUTABLE) |
| T7 | REQ-26..28 (notifikasi & FSI), REQ-29..31 (guard & basi), REQ-36 (snooze ≤5×) |
| T8 | REQ-34 (reconcile idempotent), REQ-35 (scheduled_alarms tidak di-wipe), REQ-37 (preserve snooze) |
| T9 | REQ-28 (FSI di atas lock screen), REQ-36 (snooze) |
| T10 | REQ-39 (WorkManager ±6 jam), REQ-40 (wipe-and-replace hanya sukses), REQ-41 (401 authed) |
| T11 | REQ-42..44 (boot/replaced/exact-restored) |
| T12 | REQ-05..12 (UI login/jadwal/tugas/pengaturan), REQ-18..21 (detail, status, prioritas) |
| T13 | REQ-45 (release build), REQ-46 (README) |

## Risiko & keputusan yang didokumentasikan

- Semua waktu perhitungan alarm dalam WIB eksplisit (`Asia/Jakarta`); format tampilan WIB (spec §8.8).
- Bila AGP 9.3.0 built-in Kotlin bermasalah di mesin ini, fallback: `android.builtInKotlin=false` di `gradle.properties` + plugin `org.jetbrains.kotlin.android` 2.4.0 (versi diverifikasi via riset).
- Semua receiver `exported=false` — broadcast protected sistem (BOOT_COMPLETED, MY_PACKAGE_REPLACED) tetap terkirim; bila perangkat menolak, ubah `exported=true` untuk BootReceiver.
