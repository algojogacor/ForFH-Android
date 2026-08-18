# Linear Filter Bar & Tab Component

## Sumber
- URL: https://linear.app
- State: Default, Active Selection, Hover

## Struktur
Filter bar Linear menggunakan segment capsule horizontal yang ramping dan modern:
1. `Scrollable Row Container` (Padding horizontal 16px, gap 6px antar filter)
2. `Filter Chip / Capsule`
   - `Leading Icon` (Optional: Icon filter/status 14px)
   - `Label` (Teks 12px/13px)
   - `Counter Badge` (Opsional: Angka count dalam pill kecil)

## Nilai Visual (dari getComputedStyle, BUKAN taksiran)
- **Dimensi & Spacing**:
  - Container Height: `32px` - `36px`
  - Chip Height: `28px` - `30px`
  - Gap antar chip: `6px`
  - Chip Padding Horizontal: `10px`
  - Chip Padding Vertical: `4px`
  - Chip Border Radius: `8px` (atau `16px` untuk rounded capsule style)
- **Tipografi**:
  - `font-size: 13px`
  - `font-weight: 500`
  - `line-height: 16px`
  - `letter-spacing: -0.01em`
- **Warna & Permukaan (Inactive vs Active)**:
  - **Inactive Chip**:
    - Background: `rgba(255, 255, 255, 0.04)` (atau `transparent`)
    - Foreground / Text: `#A1A1AA` (`rgba(255, 255, 255, 0.65)`)
    - Border: `1px solid rgba(255, 255, 255, 0.08)`
  - **Inactive Chip Hover**:
    - Background: `rgba(255, 255, 255, 0.08)`
    - Foreground / Text: `#FFFFFF`
    - Border: `1px solid rgba(255, 255, 255, 0.15)`
  - **Active / Selected Chip**:
    - Background: `rgba(94, 106, 210, 0.18)` (`brand-primary` tint) atau `#27272A`
    - Foreground / Text: `#FFFFFF` (`rgb(255, 255, 255)`)
    - Border: `1px solid #5E6AD2` (`rgba(94, 106, 210, 0.5)`)
  - **Counter Badge di dalam Chip**:
    - Background: `rgba(255, 255, 255, 0.10)`
    - Foreground: `#D4D4D8`
    - Padding: `2px 6px`
    - Font: `11px`, Monospace/JetBrains Mono
    - Radius: `4px`

## Interaksi
- Trigger: Tap / Click pada chip.
- Transisi: `duration: 120ms`, `easing: ease-out`.
- Animasi Seleksi: Active background dan border meluncur dengan transisi warna halus tanpa layout shift.

## Konversi ke Compose
- 1:1 Mapping:
  - `FilterChip`: `height = 30.dp`, `shape = RoundedCornerShape(8.dp)`
  - `Border`: `BorderStroke(1.dp, if (selected) Color(0xFF5E6AD2) else Color.White.copy(alpha = 0.08f))`
  - `ContainerColor`: `if (selected) Color(0xFF5E6AD2).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f)`
  - `LabelColor`: `if (selected) Color.White else Color(0xFFA1A1AA)`
