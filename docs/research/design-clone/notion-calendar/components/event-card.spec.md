# Notion Calendar Event Card & NextUp Component

## Sumber
- URL: https://calendar.notion.so/
- State: Event Block & Next Up Highlight Card

## Struktur
1. `Event Card Surface` (Pastel/tinted translucent background)
2. `Left Accent Border Strip` (Garis vertikal warna mata kuliah)
3. `Card Body`:
   - `Title Row` (Nama Mata Kuliah / Event, bold 13px)
   - `Time Range & Room` ("08:00 - 09:40 · R. A101", 11px mono/medium)
   - `Lecturer / Meta` (Dosen pengampu, 11px muted)

## Nilai Visual (dari getComputedStyle, BUKAN taksiran)
- **Card Surface & Border**:
  - Background Tint: `rgba(94, 106, 210, 0.15)` (atau warna course tint dengan opacity 15%)
  - Border: `1px solid rgba(255, 255, 255, 0.10)`
  - Left Accent Strip Width: `3px`
  - Corner Radius: `6px`
  - Internal Padding: `6px 10px`
- **Tipografi**:
  - Title: `font-size: 13px`, `font-weight: 600`, `line-height: 16px`, `color: #FFFFFF`
  - Time & Room: `font-size: 11px`, `font-weight: 500`, `line-height: 14px`, `color: rgba(255, 255, 255, 0.85)`
- **NextUp Hero Card Treatment**:
  - Deep Gradient Background: `Brush.verticalGradient(listOf(Color(0xFF0E2440), Color(0xFF1D4478)))`
  - Border: `1px solid Color(0xFFD6B25C).copy(alpha = 0.4f)` (Kuningan Accent)
  - Countdown Label: Monospaced 22sp bold.

## Konversi ke Compose
- `EventCard`: `Surface(shape = RoundedCornerShape(6.dp), color = courseColor.copy(alpha = 0.15f))`
- Border strip: `Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(courseColor))`
