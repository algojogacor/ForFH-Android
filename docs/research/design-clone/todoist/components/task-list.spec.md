# Todoist Task List & Grouping

## Sumber
- URL: https://app.todoist.com/app/today
- State: Logged In / Today View & Overdue

## Struktur
1. `View Header` ("Today", tanggal Indonesia/WIB, progress indicator)
2. `Section Group` (Overdue, Today, Upcoming)
   - `Section Header` (Judul seksi, count badge)
   - `Task Items Stream` (Daftar item task vertikal)
3. `Add Task Row Trigger` (Tombol inline "+ Add task")

## Nilai Visual (dari getComputedStyle, BUKAN taksiran)
- **Header Seksi ("Overdue", "Today")**:
  - `font-size: 14px`
  - `font-weight: 700`
  - `line-height: 20px`
  - `letter-spacing: -0.01em`
  - Overdue text color: `#DC4C3E` (`rgb(220, 76, 62)`)
  - Today text color: `#FFFFFF` (`rgb(255, 255, 255)`)
  - Margin bottom: `8px`
  - Padding top: `16px`
- **Section Divider Line**:
  - `height: 1px`
  - `background-color: rgba(255, 255, 255, 0.08)`
  - Margin: `12px 0`
- **Add Task Inline Button**:
  - Height: `32px`
  - Font: `13px`, `font-weight: 500`
  - Text color: `#71717A` (Hover: `#5E6AD2`)
  - Icon "+": `16px`

## Interaksi
- Trigger: Scroll / Expand-Collapse section.
- Section Header Click: Toggle collapse list di bawahnya dengan transisi `150ms ease-out`.

## Konversi ke Compose
- `SectionHeader`: `Text(style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold))`
- Overdue Header tint: `Color(0xFFDC4C3E)`
- Spacing antar seksi: `Spacer(modifier = Modifier.height(16.dp))`
