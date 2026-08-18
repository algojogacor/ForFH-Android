# Notion Calendar Daily Timeline Layout

## Sumber
- URL: https://calendar.notion.so/
- State: Day / Week Timeline View

## Struktur
1. `Time Column (Left Axis)` (Label jam 00:00 s/d 23:00)
2. `Timeline Grid Canvas` (Garis horizontal pemisah jam)
3. `Event Blocks Layer` (Kartu jadwal bertumpuk sesuai jam & durasi)
4. `Current Time Indicator` (Garis penanda waktu saat ini)

## Nilai Visual (dari getComputedStyle, BUKAN taksiran)
- **Time Axis Column**:
  - Width: `48px`
  - Text Align: `right`
  - Padding Right: `12px`
  - Label Font: `11px`, JetBrains Mono / Monospace, `font-weight: 500`
  - Label Color: `#707070` (`rgba(255, 255, 255, 0.45)`)
- **Hour Grid Row**:
  - Row Height per Hour: `48px` (proporsional 1 menit = 0.8dp)
  - Horizontal Divider Line: `1px solid rgba(255, 255, 255, 0.07)`
  - Sub-hour Divider (30m): `1px dashed rgba(255, 255, 255, 0.04)`
- **Overlap Handling**:
  - Ketika 2 jadwal bertabrakan jamnya, lebar kolom dibagi rata secara horizontal (`width = 50% - 2px`) dengan offset kiri `50%`.

## Konversi ke Compose
- `DailyTimelineView`: `Box` dengan `verticalScroll`
- Tinggi 1 jam = `48.dp`
- Posisi vertikal event = `(startMinutesFromMidnight / 60f) * 48.dp`
- Tinggi event block = `(durationMinutes / 60f) * 48.dp`
