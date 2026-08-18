# Todoist Task Detail View

## Sumber
- URL: https://app.todoist.com/app/today
- State: Task Item Opened Full Detail View

## Struktur
1. `Top Navigation Bar` (Back button, Project Tag, Priority Pill, Delete/More Menu)
2. `Task Header` (Checkbox status, Large Title 18px)
3. `Description Section` (Editorial text body surface)
4. `Subtasks Section`
   - Section Header ("Sub-tasks" + progress bar)
   - Nested Task List
   - "+ Add sub-task" inline action
5. `Pinned Bottom Action Bar` (Complete Task Primary CTA button)

## Nilai Visual (dari getComputedStyle, BUKAN taksiran)
- **Detail Surface**:
  - Background: `#111113`
  - Internal Padding: `20px`
- **Title Typography**:
  - `font-size: 18px`
  - `font-weight: 700`
  - `line-height: 24px`
  - `color: #FFFFFF`
- **Description Body**:
  - `font-size: 14px`
  - `font-weight: 400`
  - `line-height: 22px`
  - `color: #D4D4D8`
  - Padding: `12px 14px`
  - Surface BG: `#161618`
  - Border Radius: `8px`
- **Subtasks List**:
  - Left Indent: `12px`
  - Item Height: `38px`
- **Pinned Bottom Primary CTA**:
  - Height: `50px`
  - Full Width: `fillMaxWidth()`
  - Radius: `12px`
  - Font: `15px`, `font-weight: 700`

## Konversi ke Compose
- `TugasDetailScreen`: `Scaffold` dengan `bottomBar = { PrimaryButton(...) }`
- Section Subtasks memakai `LazyColumn` atau `Column` terstruktur.
