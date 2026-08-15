# ForFH Android V1 — Design Spec

> Tanggal: 2026-08-15 · Repo: `algojogacor/ForFH-Android` (private) · Status: terkunci, menunggu review

## 1. Ringkasan & Tujuan

Aplikasi Android native pendamping ForFH (web, https://usual-olwen-algojogacorbgt-a2be655b.koyeb.app). V1 berfokus pada **jadwal, tugas, dan alarm bangun kuliah** yang bekerja tanpa perlu membuka app — alarm dijadwalkan dari data lokal sehingga tetap jalan saat offline atau server mati.

- Pengguna: user + teman dekat (sideload APK, **tanpa Play Store**).
- Server: **tetap 1 deploy Koyeb** (web ForFH). App Android hanyalah klien API — tidak ada server baru, tidak ada deploy baru.
- Target device utama: **Redmi Note 15 Pro — Android 16, HyperOS**.

## 2. Keputusan Arsitektur (terkunci)

| Keputusan | Pilihan | Alasan |
|---|---|---|
| Pendekatan | **Pendekatan 1**: Room lokal + WorkManager sync + AlarmManager exact | Alarm tetap hidup saat offline/Doze; recovery boot; tanpa foreground service permanen |
| Repo | Repo terpisah `ForFH-Android` | Kode Android tidak pernah di-deploy ke Koyeb; build APK murni lokal; tidak merancukan repo web |
| Stack | Kotlin + Jetpack Compose + Material 3 | Standar modern, satu bahasa |
| minSdk / targetSdk | **26** (Android 8.0) / **36** (Android 16) | Jangkau semua HP teman; izin alarm exact & full-screen intent makin ketat tapi tersedia; targetSdk 36 ⇒ edge-to-edge wajib (§3) |
| Server API | Pakai API ForFH yang ada, **nol perubahan server** | Auth via session cookie `__Host-forfh-session` (30 hari, token DB) yang di-persist OkHttp CookieJar — bukan JWT, jadi tanpa endpoint token baru |
| Desain visual | **DNA visual web ForFH** (warna + tipografi editorial/mono, dark/light ikut sistem) + **layout Material 3 native** | Konsisten brand, ergonomis platform |
| Sumber data | Remote/API = data terbaru · Room = source of truth runtime/offline · AlarmEngine = scheduler · Receiver = validator + executor | Lihat §11 |

## 3. Stack & Target

- Kotlin, Jetpack Compose, Material 3, single module `app/`.
- Room (persist), DataStore Preferences (settings non-Room), Retrofit + OkHttp (CookieJar persist), WorkManager (sync), AlarmManager (exact alarms), BroadcastReceiver (trigger + boot recovery).
- Android 16 (API 36) di device target; minSdk 26; **targetSdk 36**.
- **Edge-to-edge wajib** (targetSdk 35+ ditegakkan; 36 mengetatkan): semua layar memakai `Scaffold` + `WindowInsets` — konten tidak pernah tersembunyi di balik status/navigation bar; system bars transparan/dynamic.
- Izin exact alarm: **`USE_EXACT_ALARM`** (bukan `SCHEDULE_EXACT_ALARM`) — keputusan dan alasan di §8.3.
- Timezone: **selalu `Asia/Jakarta` (WIB) eksplisit** — tidak pernah mengandalkan timezone device implisit (§8.8).

## 4. Kontrak API (tanpa perubahan server)

| Endpoint | Method | Dipakai untuk |
|---|---|---|
| `/api/auth/login` | POST | Login `{email, password}` → 200 + Set-Cookie `__Host-forfh-session` (30 hari) |
| `/api/schedules` | GET | Daftar jadwal: `{ schedules: [{ id, courseId, courseName, courseCode, courseColor, lecturer, credits, dayOfWeek (0=Min..6=Sab), startTime "HH:MM", endTime, room, onlineUrl, enabled }] }` |
| `/api/tasks` | GET | Daftar tugas: `{ tasks: [{ id, courseId, title, dueAt (epoch ms | null), status, computedStatus (OVERDUE dinamis), priority, subtasks, course: {...} }] }` |
| `/api/tasks/{id}` | PUT | Tandai selesai: body `{ status: "DONE" }` → `{ success: true }` (juga menerima title/description/dueAt/priority — app hanya pakai status) |

- Auth: OkHttp CookieJar persist (cookie disimpan di DataStore, dikirim di tiap request). Semua request `/api/*` butuh cookie; `401` = sesi habis (bedakan dengan 401 login, §10).
- Mem-cache server (`mem-cache`, 30 dtk untuk tasks) tidak masalah — app sync tidak real-time.

## 5. Struktur Proyek & Komponen

```
app/src/main/java/com/aryariap/forfh/
├── ui/               → Compose + navigation
│   ├── login/        → LoginScreen
│   ├── jadwal/       → JadwalScreen (hari ini + seminggu)
│   ├── tugas/        → TugasListScreen, TugasDetailScreen
│   └── pengaturan/   → PengaturanScreen
├── data/
│   ├── db/           → Room: entity + DAO + database (schedules, tasks, scheduled_alarms)
│   └── prefs/        → DataStore: cookie, toggles, lastSyncAt, lastSyncStatus
├── network/          → Retrofit service, OkHttp CookieJar persist, ApiClient
├── sync/             → SyncWorker (WorkManager), AlarmRescheduler
└── alarm/
    ├── AlarmPlanner  → math next occurrence (WIB) — murni, bisa unit-test
    ├── AlarmScheduler→ pasang/cancel AlarmManager (exact/fallback)
    ├── AlarmReceiver → validator + executor (kuliah & tugas)
    └── FullScreenAlarmActivity → layer alarm di atas lock screen
```

Setiap unit punya satu tanggung jawab, interface jelas, bisa di-test sendiri.

## 6. Halaman & UX

| Halaman | Isi |
|---|---|
| **Login** | Logo "ForFH" (serif italic) + tagline mono uppercase. Form email + password, tombol "Masuk" (loading saat verifikasi), pesan error (§10). |
| **Jadwal** | Tab/segmented: **Hari ini** (kuliah tersisa, kartu warna course, ruang, jam) & **Seminggu** (grid 7 hari, hari ini disorot). |
| **Tugas** | Daftar tugas (status badge, deadline, warna course). Tap → **detail** (title, matkul, deadline, subtasks, deskripsi) + tombol **"Tandai selesai"** (mengubah `status` → `DONE` di server via API yang ada). Tap notif tugas → halaman Tugas. |
| **Pengaturan** | Toggle per offset alarm kuliah (3 jam / 2 jam + snooze / 1 jam); status izin (notifikasi, exact alarm, full-screen) + tombol buka system Settings; penjelasan halus saat exact tidak tersedia; info versi APK; tombol Logout. |

Tampilan "Tandai selesai" di V1: `PUT /api/tasks/{id}` body `{ status: "DONE" }` → setelah response sukses, baris tasks lokal di-update (status & computedStatus → `DONE`); kegagalan → toast error, tidak ada perubahan lokal. Server tetap sumber kebenaran.

## 7. Data Lokal

### Room — tabel mirror (wipe-and-replace saat sync sukses)

**`schedules`** (PK `id`, salin field `/api/schedules`): `id, courseId, courseName, courseCode, courseColor, lecturer, credits, dayOfWeek (0..6), startTime "HH:MM", endTime, room, onlineUrl, enabled (Boolean)`

**`tasks`** (PK `id`): `id, courseName, courseCode, title, dueAt (epoch ms | null), status, computedStatus (OVERDUE), priority`

### Room — state alarm eksplisit

**`scheduled_alarms`** — identity alarm yang sedang terpasang. Inilah yang membuat rantai `create → schedule → receiver → snooze → cancel → reboot → logout` konsisten **deterministic** (bukan hitung ulang dari memori):

| kolom | isi |
|---|---|
| `id` (PK) | identity string deterministic: `"class\|scheduleId\|offsetMinutes\|occurrenceDate"` / `"task\|slot\|date"` |
| `kind` | `CLASS_ALARM` \| `TASK_REMINDER` |
| `scheduleId` | id jadwal (null untuk task slot) |
| `offsetMinutes` | 0 untuk task slot |
| `occurrenceDate` | `"2026-08-17"` (LocalDate WIB) — occurrence yang diwakili |
| `triggerAtMillis` | epoch millis trigger saat ini (berubah saat snooze) |
| `snoozeCount` | counter snooze per occurrence (reset saat row baru) |

- RequestCode PendingIntent & notificationId: `stableHash(id)` — deterministic, aman untuk cancel/duplicate.
- PendingIntent extras membawa: `scheduleId, offsetMinutes, occurrenceDate, triggerAtMillis` — receiver memvalidasi dari extras + Room, tidak menebak-nebak.

**Aturan wipe (MUST FIX):** wipe-and-replace **hanya** menyentuh tabel mirror `schedules` & `tasks` (dan hanya saat response sukses, §9). `scheduled_alarms` **tidak pernah di-wipe** — perubahannya selalu lewat `AlarmRescheduler` yang preserve sesi snooze (§8.1).

### DataStore Preferences & keamanan kredensial

`alarm_offsets` (3j / 2j / 1j boolean) · `last_sync_at` · `last_sync_status` (ok/error)

- **Cookie sesi TIDAK pernah disimpan plaintext** (HARDENING): dibungkus `SecureCookieStore` — AES-256-GCM, kunci non-exportable di **Android Keystore** (alias `forfh_session_key`); ciphertext + IV disimpan di DataStore. Diberikan ke OkHttp CookieJar hanya dalam memori saat runtime.
- Password kampus **tidak pernah** disimpan di app — sesi cukup dengan cookie.
- Fail-safe: key Keystore hilang saat uninstall/reinstall → ciphertext tak terbaca → app auto-logout, alur 401 standar (§10) menangani.

## 8. Alarm Engine

### 8.1 Prinsip umum

- Room = satu-satunya sumber state alarm. Alarm tidak pernah dibangun dari memori saja.
- Semua schedule melewati `AlarmScheduler`: cek `canScheduleExactAlarms()` → exact bila tersedia, fallback `setWindow` bila tidak (§8.3).
- `AlarmRescheduler.reconcile()` (dipanggil setelah sync, boot, package-replaced, exact-restored): idempotent — pastikan semua alarm yang *harus* ada memang ada (row `scheduled_alarms` + PendingIntent terpasang). `rescheduleAll()` = cancel semua lalu build ulang; `reconcile()` = perbaiki yang hilang tanpa menyentuh yang sudah benar.
- **Keduanya wajib preserve sesi snooze aktif (MUST FIX):** row dengan `snoozeCount > 0` dan `triggerAtMillis` masih future **tidak di-cancel dan tidak di-reset** oleh sync/reconcile/boot — dipertahankan apa adanya. Jika rebuild penuh terpaksa menyentuhnya, schedule ulang langsung ke `triggerAtMillis` tersimpan (sisa waktu dipertahankan, `snoozeCount` tidak berubah). Invariant: `snoozeCount` hanya naik lewat aksi snooze user; tidak pernah turun oleh proses lain.

### 8.2 Alarm kuliah

Untuk tiap jadwal `enabled = true` dengan offset aktif di Pengaturan:

1. Hitung next occurrence: `LocalDate + DayOfWeek(dayOfWeek) + LocalTime(startTime) → ZonedDateTime WIB`. Cari tanggal terdekat; jika occurrence hari ini sudah lewat (trigger ≤ now) → lompat ke occurrence minggu berikutnya.
2. `triggerAtMillis = startDateTime − offsetMinutes`.
3. Schedule via `AlarmScheduler` dengan **`RTC_WAKEUP`**: exact → `setExactAndAllowWhileIdle(RTC_WAKEUP, triggerAtMillis, pi)`; fallback → `setWindow(RTC_WAKEUP, triggerAtMillis, windowLengthMs, pi)`. PendingIntent `FLAG_IMMUTABLE`, requestCode `stableHash(id)`. `RTC_WAKEUP` = membangunkan device dari sleep saat alarm menembak.
4. Simpan/timpa row `scheduled_alarms` dengan id `"class|scheduleId|offsetMinutes|occurrenceDate"`.

### 8.3 Exact alarm & fallback

**Keputusan izin (eksplisit): `USE_EXACT_ALARM`** — declared di manifest, granted otomatis untuk app kategori alarm/reminder (targetSdk 33+; di Android 16 tetap berlaku untuk alarm clock), tanpa dialog special access. **Bukan `SCHEDULE_EXACT_ALARM`** (perlu special access manual di Settings — friction tinggi, tidak dipakai). `USE_EXACT_ALARM` tetap bisa dicabut user → fallback di bawah menangani.

- Sebelum tiap schedule: `AlarmManager.canScheduleExactAlarms()`.
- Tersedia → `setExactAndAllowWhileIdle(RTC_WAKEUP, ...)`.
- Tidak tersedia → `setWindow(RTC_WAKEUP, triggerAtMillis, windowLengthMs = 10 menit)` — sistem menembak alarm kapan pun **di dalam jendela mulai dari** `triggerAtMillis` (bukan "±10 menit"; istilah itu tidak ditulis di mana pun). Di Pengaturan ditulis halus: *"waktu alarm dapat sedikit bergeser"*.
- Dengar `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` → begitu access tersedia kembali, `rescheduleAll()` (semua alarm kembali exact; sesi snooze aktif dipertahankan, §8.1).
- Access dicabut setelah pernah diberi → PendingIntent terpasang tetap jalan; reschedule berikutnya otomatis pakai fallback.

### 8.4 Receiver guard (alarm kuliah)

Saat `AlarmReceiver` menerima CLASS_ALARM:

1. Baca extras + row `scheduled_alarms` (identity cocok).
2. Load jadwal terbaru dari Room.
3. Guard berlapis — salah satu gagal → **skip, jangan tampilkan apa pun**:
   - masih login (`isLoggedIn == true`; kalau tidak → cancel alarm ini);
   - jadwal masih ada & `enabled`;
   - row identity masih ada dan `row.triggerAtMillis == extras.triggerAtMillis` (jadwal diubah → stale → skip);
   - `now < startDateTime` (hitung ulang dari jadwal Room + occurrenceDate, WIB) — alarm yang terlambat keluar dari Doze **tidak pernah tampil**.
4. Valid → tampilkan (§8.5). Setelah alarm utama selesai (tampil atau skip karena lewat) → hapus row identity (occurrence selesai).

### 8.5 Tampilan alarm kuliah

- `NotificationManager.canUseFullScreenIntent()` → tersedia: **FullScreenAlarmActivity** di atas lock screen — nama MK, ruang, jam mulai, tombol **"Tidur lagi 3 menit"** + **"Tutup"**. Activity memakai `setShowWhenLocked(true)` + `setTurnScreenOn(true)` (API 27+); untuk API 26 (minSdk) fallback window flags `FLAG_SHOW_WHEN_LOCKED | FLAG_TURN_SCREEN_ON`.
- Tidak tersedia/ditolak: fallback **notifikasi biasa heads-up** (importance HIGH, sound + vibration), tap → FullScreenAlarmActivity atau langsung dismiss. App tidak pernah bergantung pada FSI.
- Channel: `Alarm Kuliah` — importance `HIGH`, sound + vibration, `setCategory(CATEGORY_ALARM)` (prioritas tampil lebih baik di Doze/DND policy).
- **Sound/vibration tetap subject ke setelan user**: volume channel yang bisa dikecilkan user, mode DND, dan optimisasi per-device (HyperOS). App tidak pernah mem-bypass DND (tanpa izin `INTERRUPTION_FILTER`).

### 8.6 Snooze

- Key state: `(scheduleId, offsetMinutes, occurrenceDate)` — tersimpan di Room (`snoozeCount` pada row identity, bukan counter memori) sehingga reboot tidak merusak counter.
- Tiap snooze: +3 menit (`triggerAtMillis += 180_000`), `snoozeCount++`, update row, reschedule exact/fallback sesuai availability dengan **`RTC_WAKEUP`** (bukan `RTC` — snooze harus tetap membangunkan dari sleep).
- Maks **5× per occurrence**. Snooze 1–4 → tombol tetap ada. Setelah snooze ke-5 → **tidak schedule ulang lagi**; tombol "Tidur lagi 3 menit" hilang dari tampilan; alarm selesai setelah ditutup.

### 8.7 Reminder tugas (3 slot harian, one-shot)

Slot: **09:00 / 15:00 / 20:00 WIB**. **Bukan** `setRepeating` — pola per occurrence:

```
one-shot alarm → receiver → query Room → tampilkan notif → schedule occurrence berikutnya
```

- Row id: `"task|slot|date"`; receiver: guard login → query `tasks` `status != DONE`, urut **deadline terdekat dulu** (`dueAt ASC NULLS LAST`).
- Ada tugas: `"📚 N tugas belum selesai: Judul 1, Judul 2, +K lagi"` (≤ 2 judul; `K = N − 2`). Tap → halaman Tugas.
- Tidak ada tugas: **hanya slot 09:00** yang menampilkan `"🎉 Tidak ada tugas hari ini — selamat beraktivitas!"`; slot 15:00 & 20:00 **silent** (tidak spam).
- Setelah notif → hapus row hari ini, schedule row `"task|slot|besok"`.
- **Safety net — bukan timing guarantee**: `AlarmRescheduler.reconcile()` ikut WorkManager periodic ±6 jam hanya memastikan alarm (3 slot tugas & alarm kuliah) terpasang dengan benar. WorkManager tidak menjamin waktu eksekusi (flexible window, bisa molor) — waktu alarm ditentukan **AlarmManager semata**. Kalau reschedule di receiver gagal, alarm besok tetap terpasang oleh reconcile berikutnya (selambat-lambatnya beberapa jam kemudian, bukan persis 09:00).

### 8.8 Timezone

Semua perhitungan pakai `ZoneId.of("Asia/Jakarta")` eksplisit:

```
LocalDate + DayOfWeek + LocalTime + Asia/Jakarta → ZonedDateTime → epoch millis
```

Berlaku untuk: next occurrence kuliah, 3 slot tugas, snooze (+3 menit juga WIB), recovery, guard `now < startDateTime`.

### 8.9 Recovery (rebuild dari Room)

Trigger rebuild seluruh alarm:

- `BOOT_COMPLETED` (receiver non-directBootAware, izin `RECEIVE_BOOT_COMPLETED`) → `AlarmRescheduler.reconcile()`.
- **Deferred path (storage belum accessible)**: `BOOT_COMPLETED` baru diterima **setelah unlock pertama** — sebelum itu storage credential-encrypted (termasuk Room & `scheduled_alarms`) tidak bisa dibaca. Perilaku eksplisit: tanpa unlock pertama pasca-reboot, alarm tidak di-rebuild dan tidak ada yang tampil (ini batas Android, bukan bug); begitu unlock, reconcile berjalan otomatis. Data alarm ikut aman karena tersimpan di storage terenkripsi.
- `ACTION_MY_PACKAGE_REPLACED` (update APK) → sama.
- Exact access kembali tersedia (§8.3) → `rescheduleAll()`.
- Habis sync sukses / login → `rescheduleAll()`.

Room tetap source of truth: alarm lama tidak diandalkan sebagai satu-satunya state; seluruh alarm dapat direkonstruksi dari Room.

### 8.10 Logout

1. Cancel seluruh alarm (iterate `scheduled_alarms` → `AlarmManager.cancel`).
2. Hapus tabel `scheduled_alarms` (state snooze ikut hilang).
3. Hapus Room (schedules, tasks) + DataStore (cookie, status sync).
4. Kembali ke Login.
5. Defense-in-depth: receiver apa pun yang jalan setelah logout → `isLoggedIn == false` → skip/cancel.

### 8.11 HyperOS compatibility

- Mekanisme murni `AlarmManager`; **tanpa foreground service permanen** dan tanpa service penjaga hidup.
- Optimisasi background/power HyperOS diperlakukan sebagai **compatibility concern**, bukan fondasi scheduling.
- Saran di halaman Pengaturan (baris kecil, langkah manual sekali): *"aktifkan Autostart & nonaktifkan Battery Restriction untuk ForFH di Pengaturan → Aplikasi → ForFH"* (lazim di MIUI/HyperOS).
- Pengujian wajib mencakup: layar mati, terkunci, Doze/idle lama, app lama tak dibuka, reboot, package update, battery saver (§12).

## 9. Alur Sync

```
Login sukses (cookie tersimpan)
  → SyncWorker sekali jalan: GET /schedules + /tasks
  → sukses: wipe-and-replace Room → AlarmRescheduler.rescheduleAll()
  → gagal: Room lama tetap, status "error" di UI

WorkManager periodic ±6 jam (network-constrained) → sync → reconcile alarm
  (WorkManager = safety net reconciliation, bukan timing guarantee — §8.7)
Manual: pull-to-refresh / tombol "Coba lagi" → sync → reschedule
```

- Wipe-and-replace **hanya** saat response sukses & valid (§10) dan **hanya** menyentuh `schedules` & `tasks` — `scheduled_alarms` tidak pernah di-wipe (§7, §8.1).
- Sync Worker tidak pernah menyentuh alarm langsung — selalu lewat `AlarmRescheduler` agar state `scheduled_alarms` konsisten.

## 10. Error Handling

| Kasus | Perilaku |
|---|---|
| Sync gagal (offline / 5xx) | Room **tidak disentuh**; alarm tetap jalan dari data lokal. Banner: "Sync gagal · data terakhir 12:30" + tombol **Coba lagi**. |
| Login salah | 401 di `/api/auth/login` → "Email atau password salah." Timeout/network → "Gangguan koneksi, coba lagi." |
| Sesi kedaluwarsa | 401 di `/api/*` **selain login** → auto-logout (hapus cookie + Room + cancel alarm) → Login, pesan "Sesi berakhir, masuk lagi." |
| Izin notifikasi ditolak | App tetap dipakai; Pengaturan tampil status + tombol buka system Settings. Receiver tetap jalan tapi **silent** (guard `POST_NOTIFICATIONS` → tidak tampil apa pun, tidak crash); begitu izin diaktifkan kembali, alarm berikutnya tampil normal tanpa re-login (acceptance §12). |
| Full-screen ditolak | Fallback notif biasa heads-up + sound + vibration. |
| Exact alarm tak tersedia | Fallback `setWindow`; penjelasan halus di Pengaturan; reschedule exact saat access kembali. |
| Doze/sleep lama | Alarm kuliah `setExactAndAllowWhileIdle`; guard `now >= startDateTime` → skip (tidak ada notif basi). |

## 11. Prinsip Arsitektur

- **Remote/API = sumber data terbaru** — semua perubahan di web tersinkron turun.
- **Room = source of truth lokal untuk runtime/offline** — alarm, daftar, state.
- **Alarm Engine = scheduler** — satu-satunya pintu pasang/cancel.
- **Receiver = validator + executor** — validasi dulu, tampilkan setelah.
- Alarm tetap berguna walau sync gagal (data lama + alarm lama tetap jalan).
- Semua alarm dapat di-cancel, di-rebuild, di-recover secara deterministic.
- **Tidak pernah** menampilkan alarm stale atau alarm kuliah yang sudah lewat waktu mulai.

## 12. Pengujian

### Unit test (JVM, `test/`)

- **AlarmPlanner**: next occurrence WIB — Senin 08:00 offset 2j → Minggu 06:00; trigger hari ini sudah lewat → minggu depan; lintas minggu; pergantian tanggal.
- **SnoozeCounter**: batas 5×; reset saat occurrence baru (row baru).
- **TaskReminderText**: 0 / 1 / 2 / 3+ tugas; urutan deadline terdekat; format "+K lagi".
- **ReceiverGuard**: `enabled=false` → skip; `now >= startDateTime` → skip; `isLoggedIn=false` → skip; row hilang → skip; `POST_NOTIFICATIONS` ditolak → silent, tidak crash.
- **Concurrent sync vs snooze** (HARDENING): interleaving — sync/reconcile mulai → user snooze (+3 menit, count 1) → sync selesai cancel+rebuild → assert: `snoozeCount` tetap 1, `triggerAtMillis` tetap nilai snooze, tidak ada double-schedule untuk occurrence yang sama, alarm base tidak muncul lagi untuk occurrence yang sedang di-snooze.
- **Jadwal diubah setelah alarm terpasang** (HARDENING): alarm terpasang untuk jadwal Senin 08:00 → sync membawa jadwal pindah ke Rabu 10:00 (atau `enabled=false`) → alarm lama membakar receiver → guard: row identity tak ada / jadwal tak cocok → **skip & silent**; assert: nol notifikasi, nol crash; alarm baru (Rabu) terpasang dengan benar.
- **Exact revoke/restore**: `canScheduleExactAlarms()==false` → `setWindow` dipanggil; `true` → `setExactAndAllowWhileIdle`; setelah restore → `rescheduleAll()` kembali exact sambil preserve sesi snooze.

### Manual E2E — Redmi Note 15 Pro (Android 16 / HyperOS)

Checklist wajib: layar mati · terkunci · Doze/idle 2+ jam · app tak dibuka berhari-hari · reboot · update APK · battery saver · izin notif ditolak · exact alarm tak tersedia · full-screen intent ditolak. Ekspektasi: alarm tetap muncul tepat waktu (atau fallback sesuai izin) dan **tidak pernah ada alarm basi**.

Acceptance khusus izin & fallback:
- **POST_NOTIFICATIONS**: (a) diberi → alarm muncul normal; (b) ditolak saat runtime → app tetap dipakai, receiver silent tanpa crash, Pengaturan tampil status + tombol buka Settings; (c) diaktifkan kembali → alarm berikutnya tampil normal **tanpa re-login**.
- **Exact tak tersedia** → alarm tetap muncul (dalam window); setelah access diberikan kembali di Settings → alarm berikutnya kembali exact.
- **FSI ditolak** → fallback heads-up + sound + vibration tetap berfungsi.

## 13. Distribusi (sideload)

- APK release ditandatangani keystore milik user (dibuat sekali saat rilis pertama; **tidak pernah di-commit ke git**; salinan cadangan disimpan terpisah).
- Tanpa Play Store: install via "izinkan sumber tidak dikenal" biasa.
- Tanpa auto-update di V1: APK baru dikirim manual (chat/Drive); versi terpasang tampil di Pengaturan.
- Build: `./gradlew assembleRelease` di repo ForFH-Android (lokal, tidak ada CI/deploy).

## 14. Non-Goals V1

- Tidak ada edit/CRUD jadwal, tugas, atau matkul di app (semua edit tetap lewat web ForFH).
- Tidak ada integrasi KRS, presensi, nilai, HE-BAT, atau Kampus Kita langsung dari app.
- Tidak ada notifikasi dari server (push) — alarm & reminder murni on-device.
- Tidak ada Play Store, auto-update, analytics, atau akun multi-user.
- Tidak ada foreground service.

## 15. Keputusan yang Sudah Terkunci (checkpoint)

- Pendekatan 1 (Room + WorkManager + AlarmManager exact, recovery boot). ✅
- Repo terpisah `ForFH-Android`; server tetap 1 Koyeb; nol perubahan server API. ✅
- Alarm kuliah on-device: offset 3j / 2j (+snooze 3 menit × 5) / 1j, full-screen intent + fallback heads-up. ✅
- Notif tugas 3 slot 09:00/15:00/20:00 WIB, rekap dinamis, "tidak ada tugas" di slot pagi saja. ✅
- Halaman: Login, Jadwal (hari ini + seminggu), Tugas (list + detail + tandai selesai), Pengaturan. ✅
- DNA visual web + layout Material 3; target Redmi Note 15 Pro (Android 16, HyperOS). ✅
- Sideload APK (keystore pribadi, tanpa Play Store). ✅
