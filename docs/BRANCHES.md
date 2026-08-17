# Branch versi: JANGAN PERNAH DIHAPUS

Keputusan pemilik repo (2026-08-17): branch versi (`feat/v11`, `feat/v12`, dst.) tidak pernah dihapus, baik lokal maupun di remote. Branch = sumber utama untuk downgrade/rollback versi app via build historis.

| Branch | Versi | versionCode | Keterangan |
|---|---|---|---|
| feat/v11 | 1.1.0 | 2 | Fitur V1.1 (widget jadwal, notif deadline, Info Kampus, dll.) |
| feat/v12 | 1.2.0 | 3 | Notif Besok 20:00, deep-link open_tab, TasksWidget, guard sync logout |

Cara downgrade (contoh, ke v1.1.0):

```powershell
git checkout feat/v11
.\gradlew.bat :app:assembleRelease
adb install -r app\build\outputs\apk\release\app-release.apk
```

Catatan: downgrade install di atas data versi lebih baru bisa memicu migrasi Room ke bawah yang tidak didukung; jika perlu bersih: `adb uninstall com.aryariap.forfh` dulu (data lokal hilang).

Aturan untuk agent/CI: jangan pernah menjalankan `git branch -D`, `git push origin --delete`, atau cleanup worktree yang menghapus branch versi. Setiap versi baru = branch baru (`feat/v13`, dst.) dan tambahkan baris di tabel ini.
