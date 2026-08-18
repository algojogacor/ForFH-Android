# Linear Color Tokens & Elevation

## Sumber
- URL: https://linear.app
- State: Dark Theme (Default)

## Struktur
Lapisan surface hierarchy Linear dari background terdalam hingga overlay interaktif:
1. `Base Background` (Canvas dasar pitch black)
2. `Elevated Surface / Card` (Kontainer kartu, panel navigasi samping)
3. `Interactive Overlay / Row Hover` (Permukaan item daftar saat disentuh/hover)
4. `Border & Divider` (Garis pemisah struktural semi-transparan)
5. `Text Hierarchy` (Primary, Secondary, Muted/Tertiary)
6. `Semantic Status & Priority Accents` (Backlog, Todo, In Progress, Done, Canceled)

## Nilai Visual (dari getComputedStyle, BUKAN taksiran)
- **Background Layers**:
  - `bg-base` (Pitch Black): `#000000` (`rgb(0, 0, 0)`)
  - `bg-surface-elevated` (Card/Panel): `#111113` (`rgb(17, 17, 19)`)
  - `bg-surface-secondary` (Inner blocks/Inputs): `#161618` (`rgb(22, 22, 24)`)
  - `bg-surface-hover` (Item Active/Selected): `#1F1F23` (`rgb(31, 31, 35)`)
  - `bg-overlay` (Modal/Bottom Sheet): `#18181B` (`rgb(24, 24, 27)`)
- **Border & Divider**:
  - `border-subtle`: `rgba(255, 255, 255, 0.08)` (atau `#27272A`)
  - `border-strong`: `rgba(255, 255, 255, 0.15)` (atau `#3F3F46`)
  - `border-focus`: `#5E6AD2` (`rgb(94, 106, 210)`)
- **Text & Icon Hierarchy**:
  - `text-primary`: `#FFFFFF` (`rgba(255, 255, 255, 1.0)`)
  - `text-secondary`: `#D4D4D8` (`rgba(255, 255, 255, 0.85)`)
  - `text-muted` / `text-tertiary`: `#71717A` (`rgba(255, 255, 255, 0.45)`)
  - `text-quaternary`: `#52525B` (`rgba(255, 255, 255, 0.30)`)
- **Accent & Brand Tokens**:
  - `brand-primary` (Linear Indigo): `#5E6AD2` (`rgb(94, 106, 210)`)
  - `brand-primary-hover`: `#6E7BE2` (`rgb(110, 123, 226)`)
  - `brand-primary-subtle`: `rgba(94, 106, 210, 0.15)`
- **Semantic Status Colors**:
  - `status-backlog`: `#71717A` (`rgb(113, 113, 122)`)
  - `status-todo`: `#E2E8F0` (`rgb(226, 232, 240)`)
  - `status-in-progress`: `#F59E0B` (`rgb(245, 158, 11)`)
  - `status-done`: `#5E6AD2` (`rgb(94, 106, 210)`)
  - `status-completed` / `success`: `#10B981` (`rgb(16, 185, 129)`)
  - `status-canceled` / `error`: `#EF4444` (`rgb(239, 68, 68)`)
  - `status-revisi` / `urgent`: `#F43F5E` (`rgb(244, 63, 94)`)
- **Priority Scale Colors**:
  - `priority-urgent` (P1): `#F43F5E` (`rgb(244, 63, 94)`)
  - `priority-high` (P2): `#FB923C` (`rgb(251, 146, 60)`)
  - `priority-medium` (P3): `#FBBF24` (`rgb(251, 191, 36)`)
  - `priority-low` (P4): `#60A5FA` (`rgb(96, 165, 250)`)
  - `priority-none`: `#71717A` (`rgb(113, 113, 122)`)
- **Elevation / Shadow Tokens**:
  - `shadow-none`: `none`
  - `shadow-card`: `0px 1px 3px rgba(0, 0, 0, 0.4), 0px 6px 16px rgba(0, 0, 0, 0.3)`
  - `shadow-dropdown`: `0px 4px 12px rgba(0, 0, 0, 0.6), 0px 0px 0px 1px rgba(255, 255, 255, 0.08)`

## Interaksi
- Trigger: Press / Tap / Hover pada card atau item baris.
- Transisi: `duration: 150ms`, `easing: cubic-bezier(0.16, 1, 0.3, 1)`.
- State kedua: Background bertransisi dari `transparent` atau `#111113` menjadi `#1F1F23`, border menguat dari `rgba(255,255,255,0.08)` menjadi `rgba(255,255,255,0.15)`.

## Konversi ke Compose
- Rasio 1:1 px ke dp/sp:
  - `bg-base` = `Color(0xFF000000)`
  - `bg-surface-elevated` = `Color(0xFF111113)`
  - `bg-surface-secondary` = `Color(0xFF161618)`
  - `bg-surface-hover` = `Color(0xFF1F1F23)`
  - `border-subtle` = `Color(0xFFFFFFFF).copy(alpha = 0.08f)`
  - `border-strong` = `Color(0xFFFFFFFF).copy(alpha = 0.15f)`
  - `text-primary` = `Color(0xFFFFFFFF)`
  - `text-secondary` = `Color(0xFFD4D4D8)`
  - `text-muted` = `Color(0xFF71717A)`
  - `brand-primary` = `Color(0xFF5E6AD2)`
