# Todoist Task Item Component

## Sumber
- URL: https://app.todoist.com/app/today
- State: Default, Checked (Animated Completion), Hover

## Struktur
```
[ (O) Checkbox ]  [ Title Text                         ]  [ Priority P1 ]
                  [ Description Text                   ]
                  [ Due Date Chip ]  [ Subtasks Count  ]
```

## Nilai Visual (dari getComputedStyle, BUKAN taksiran)
- **Container Row**:
  - Min Height: `44px`
  - Padding: `8px 12px`
  - Border Radius: `8px`
  - Background (Normal): `transparent` / `#111113`
  - Background (Hover/Press): `rgba(255, 255, 255, 0.05)` (`#1F1F23`)
  - Bottom Divider: `1px solid rgba(255, 255, 255, 0.06)`
- **Checkbox (Priority Circle Ring)**:
  - Outer Diameter: `18px`
  - Border Width: `2px`
  - Corner Radius: `50%` (Circle)
  - **Priority Color Codes**:
    - `P1 (Urgent/Red)`: Border `#DC4C3E`, Fill Hover `rgba(220, 76, 62, 0.15)`
    - `P2 (High/Orange)`: Border `#E15E00`, Fill Hover `rgba(225, 94, 0, 0.15)`
    - `P3 (Medium/Blue)`: Border `#246FE0`, Fill Hover `rgba(36, 111, 224, 0.15)`
    - `P4 (Normal/Grey)`: Border `#71717A`, Fill Hover `rgba(113, 113, 122, 0.15)`
- **Tipografi Task Item**:
  - **Title**: `font-size: 14px`, `font-weight: 500`, `line-height: 20px`, `color: #FFFFFF`
  - **Description**: `font-size: 12px`, `font-weight: 400`, `line-height: 16px`, `color: #71717A`
- **Due Date & Meta Chips**:
  - Font: `11px`, `font-weight: 500`, `line-height: 14px`
  - Padding: `2px 6px`
  - Radius: `4px`
  - Overdue Chip: Text `#DC4C3E`, BG `rgba(220, 76, 62, 0.12)`
  - Today Chip: Text `#10B981`, BG `rgba(16, 185, 129, 0.12)`

## Interaksi & Micro-Animation Centang
- **Trigger**: Tap pada Checkbox.
- **Urutan Animasi**:
  1. Checkbox langsung terisi warna solid priority + muncul check icon SVG centang (duration: `80ms`).
  2. Teks Title bertransisi strikethrough + opacity turun ke `0.4` (`150ms cubic-bezier(0.4, 0, 0.2, 1)`).
  3. Baris task bertahan `300ms` sebelum slide-out & collapse (memberikan umpan balik visual yang jelas bagi pengguna sebelum hilang dari list).

## Konversi ke Compose
- `TaskItemRow`: `Row(modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 8.dp))`
- Checkbox Ring: `IconButton` / Canvas circle `18.dp` dengan stroke `2.dp`
- Animasi Centang: `AnimatedVisibility` dengan `shrinkVertically()` & `fadeOut()` pasca-delay `300.ms`
