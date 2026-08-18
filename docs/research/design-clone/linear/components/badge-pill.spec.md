# Linear Badge & Status Pill

## Sumber
- URL: https://linear.app
- State: Default, Active, Hover, Small & Regular Sizes

## Struktur
Komponen badge pill Linear dirancang sangat presisi:
1. `Outer Container` (Capsule / Pill shape dengan 1px border tipis)
2. `Status Icon / Dot` (Lingkaran 6px/8px atau icon SVG status mono 12px)
3. `Label Text` (Teks padat, medium weight, tabular font spacing)

```
[ (●) In Progress ]
  ^      ^
 Dot   Label
```

## Nilai Visual (dari getComputedStyle, BUKAN taksiran)
- **Dimensi & Spacing**:
  - `Small Pill (Tag/Meta)`:
    - Height: `20px`
    - Padding horizontal: `6px`
    - Padding vertical: `2px`
    - Gap (icon to text): `4px`
    - Dot size: `5px`
    - Border radius: `9999px` (Full Capsule)
  - `Regular Pill (Status / Priority Chip)`:
    - Height: `24px`
    - Padding horizontal: `8px`
    - Padding vertical: `3px`
    - Gap (icon to text): `6px`
    - Dot size: `6px`
    - Border radius: `6px` atau `9999px`
- **Tipografi**:
  - Small: `font-size: 11px`, `font-weight: 500`, `line-height: 14px`, `letter-spacing: 0.02em`
  - Regular: `font-size: 12px`, `font-weight: 500`, `line-height: 16px`, `letter-spacing: 0.01em`
- **Warna & Permukaan**:
  - **In Progress**:
    - Background: `rgba(245, 158, 11, 0.12)` (`#F59E0B` 12% alpha)
    - Foreground / Dot: `#F59E0B` (`rgb(245, 158, 11)`)
    - Border: `rgba(245, 158, 11, 0.25)`
  - **Done / Selesai**:
    - Background: `rgba(94, 106, 210, 0.12)` (`#5E6AD2` 12% alpha)
    - Foreground / Dot: `#818CF8` (`rgb(129, 140, 248)`)
    - Border: `rgba(94, 106, 210, 0.25)`
  - **Completed (Success)**:
    - Background: `rgba(16, 185, 129, 0.12)` (`#10B981` 12% alpha)
    - Foreground / Dot: `#34D399` (`rgb(52, 211, 153)`)
    - Border: `rgba(16, 185, 129, 0.25)`
  - **Canceled / Terlambat**:
    - Background: `rgba(239, 68, 68, 0.12)` (`#EF4444` 12% alpha)
    - Foreground / Dot: `#F87171` (`rgb(248, 113, 113)`)
    - Border: `rgba(239, 68, 68, 0.25)`
  - **Backlog / Neutral (Belum Mulai)**:
    - Background: `rgba(255, 255, 255, 0.05)`
    - Foreground / Dot: `#A1A1AA` (`rgb(161, 161, 170)`)
    - Border: `rgba(255, 255, 255, 0.10)`
  - **Priority Urgent (P1)**:
    - Background: `rgba(244, 63, 94, 0.15)`
    - Foreground: `#FB7185`
    - Border: `rgba(244, 63, 94, 0.30)`

## Interaksi
- Trigger: Hover / Tap saat pill bersifat interaktif (misal Filter atau Dropdown Selector).
- Transisi: `150ms ease-out`.
- State Hover: Background opacity naik menjadi `0.20`, border naik menjadi `0.40`.

## Konversi ke Compose
- 1:1 Mapping:
  - Small: `height = 20.dp`, `contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)`, `textStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)`
  - Regular: `height = 24.dp`, `contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)`, `textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)`
  - Shape: `RoundedCornerShape(6.dp)` atau `CircleShape`
  - Border: `BorderStroke(1.dp, borderColor)`
