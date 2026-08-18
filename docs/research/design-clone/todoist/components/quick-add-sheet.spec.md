# Todoist Quick Add Task Sheet

## Sumber
- URL: https://app.todoist.com/app/today
- State: Quick Add Form Active (Bottom Sheet / Floating Modal)

## Struktur
1. `Sheet Header / Drag Handle` (Komponen pill pegangan sheet)
2. `Title Input Field` ("Task name", auto-focused)
3. `Description Input Field` ("Description", multi-line)
4. `Chip Toolbar` (Due Date Picker, Priority Selector P1-P4, Label/Tag)
5. `Action Row` (Tombol "Cancel" dan "Add task" Primary Button)

## Nilai Visual (dari getComputedStyle, BUKAN taksiran)
- **Sheet Surface**:
  - Background: `#18181B` (`rgb(24, 24, 27)`)
  - Border: `1px solid rgba(255, 255, 255, 0.12)`
  - Corner Radius Top: `16px`
  - Padding Internal: `16px`
- **Input Fields**:
  - Title Font: `15px`, `font-weight: 500`, `color: #FFFFFF`
  - Description Font: `13px`, `font-weight: 400`, `color: #A1A1AA`
  - Placeholder Color: `#52525B`
- **Chips Selector (Due date, Priority)**:
  - Height: `28px`
  - Radius: `6px`
  - Padding: `4px 8px`
  - BG: `rgba(255, 255, 255, 0.06)`
  - Border: `1px solid rgba(255, 255, 255, 0.10)`
- **Add Task Primary Button**:
  - Height: `36px`
  - BG: `#5E6AD2` (Linear Indigo) atau `#DC4C3E`
  - Font: `13px`, `font-weight: 600`, `color: #FFFFFF`
  - Radius: `8px`

## Interaksi
- Trigger: Floating FAB / Quick Add Button tap.
- Default Focus: Auto-focus pada field Title.
- Dismiss: Swipedown gesture / Tap backdrop overlay (`rgba(0,0,0,0.6)`).

## Konversi ke Compose
- `ModalBottomSheet` Material 3 dengan `sheetState = rememberModalBottomSheetState()`
- Surface `Color(0xFF18181B)` dan shape `RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)`
