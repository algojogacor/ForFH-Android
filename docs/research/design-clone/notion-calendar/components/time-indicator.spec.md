# Notion Calendar Current Time Indicator

## Sumber
- URL: https://calendar.notion.so/
- State: Real-time Indicator Line Active

## Struktur
1. `Left Marker Dot` (Lingkaran merah di ujung sumbu waktu)
2. `Horizontal Line` (Garis merah melintasi lebar kolom jadwal)

```
(●)------------------------------------------------------
  ^                     ^
Red Dot               Red Line
```

## Nilai Visual (dari getComputedStyle, BUKAN taksiran)
- **Line Color**: `#E25553` (`rgb(226, 85, 83)`) / `#F43F5E`
- **Line Thickness**: `2px`
- **Marker Dot**:
  - Diameter: `8px`
  - Shape: `50%` (Circle)
  - Color: `#E25553`
  - Position: `-4px` vertikal terpusat di tengah garis.
- **Z-Index / Elevation**: `10` (Tampil di atas event cards dan grid lines)

## Interaksi & Real-time Update
- Line berpindah secara dinamis mengikuti waktu sistem saat ini.
- Di Compose: Diupdate menggunakan `LaunchedEffect(Unit)` dengan `delay(60.seconds)` agar tidak memicu rekomposisi berlebihan.

## Konversi ke Compose
- `CurrentTimeIndicator`:
  - `Box(modifier = Modifier.offset(y = currentTimeYOffset).fillMaxWidth().height(2.dp).background(Color(0xFFE25553)))`
  - `Canvas` / `Box` `8.dp` circle marker di kiri.
