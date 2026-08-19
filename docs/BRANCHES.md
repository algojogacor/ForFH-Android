# Branch versi: JANGAN PERNAH DIHAPUS

Keputusan pemilik repo (2026-08-17): branch versi (`feat/v11`, `feat/v12`, dst.) dan tag rilis tidak pernah dihapus, baik lokal maupun di remote. Branch/Tag = sumber utama untuk downgrade/rollback versi app via build historis.

| Branch / Tag | Versi | versionCode | Tanggal | Keterangan |
|---|---|---|---|---|
| main (initial) | 1.0.0 | 1 | 16 Agu 2026 | Rilis perdana ForFH: Sinkronisasi jadwal & tugas, fullscreen alarm, offset per hari |
| feat/v11 | 1.1.0 | 2 | 17 Agu 2026 | Fitur V1.1 (widget jadwal, notif deadline H-1, Info Kampus & presensi, log in-app) |
| feat/v12 | 1.2.0 | 3 | 17 Agu 2026 | Notif Besok 20:00, deep-link open_tab, TasksWidget, guard sync logout |
| feat/v20 | 2.0.0 | 4 | 18 Agu 2026 | Evolusi Desain Konstitusi Bold awal |
| feat/v21 | 2.1.0 | 4 | 18 Agu 2026 | Redesain Total Linear OLED dark mode, Todoist stream, link HEBAT e-learning |
| feat/v22 | 2.2.0 | 4 | 18 Agu 2026 | Kalender lengkap 3 mode (Hari Ini, Seminggu, Bulan), multi-dot indicator, filter kategori |
| v2.3.0 (main) | 2.3.0 | 5 | 19 Agu 2026 | Ikon baru minimalis, widget course name & smart room, uncheck task, in-app changelog & update checker |

Cara downgrade (contoh, ke v1.1.0):

```powershell
git checkout feat/v11
.\gradlew.bat :app:assembleRelease
adb install -r app\build\outputs\apk\release\app-release.apk
```

Catatan: downgrade install di atas data versi lebih baru bisa memicu migrasi Room ke bawah yang tidak didukung; jika perlu bersih: `adb uninstall com.aryariap.forfh` dulu (data lokal hilang).

Aturan untuk agent/CI: jangan pernah menjalankan `git branch -D`, `git push origin --delete`, atau cleanup worktree yang menghapus branch versi. Setiap versi baru = branch baru (`feat/v...`, dst.) dan tambahkan baris di tabel ini.
