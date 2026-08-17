package com.aryariap.forfh.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.aryariap.forfh.ForfhApp
import com.aryariap.forfh.MainActivity
import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import com.aryariap.forfh.debug.AppLog
import com.aryariap.forfh.ui.UiFormat
import com.aryariap.forfh.ui.jadwal.nextUp
import com.aryariap.forfh.ui.theme.DarkScheme
import com.aryariap.forfh.ui.theme.ForfhColors
import com.aryariap.forfh.ui.theme.LightScheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Ukuran compact (~2x2 sel): konten minimal, cocok dengan minWidth/minHeight di XML. */
private val compactSize = DpSize(150.dp, 110.dp)

/** Ukuran standard (~4x2 sel): header + bar warna course + baris meta. */
private val standardSize = DpSize(260.dp, 110.dp)

/**
 * Widget jadwal ForFH: kelas berikutnya + alarm berikutnya, dibaca dari Room tiap render
 * (trigger: refreshAll dari 4 titik update + updatePeriodMillis 30 mnt + render sistem saat
 * widget ditambah/diubah ukuran). Tap seluruh widget → MainActivity.
 *
 * Responsive (SizeMode.Responsive, R-03): compact 150x110 = esensial saja (nama + jam + alarm,
 * tanpa header/bar warna: lebar 126dp tidak muat keduanya tanpa clip, akar bug lama "FHK25
 * terpotong"); standard 260x110 = header label + tanggal WIB, nama lebih besar, baris
 * "HH:mm · Alarm: HH:mm", bar warna course (motif identitas KuliahCard). Sistem memilih
 * ukuran terdekat lalu menskalakan render ke ukuran launcher aktual.
 */
class ForfhWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(compactSize, standardSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as ForfhApp
        // Room blocking → Dispatchers.Default (global constraint). Baca gagal (jarang) → fallback
        // placeholder jujur (R-27 error state): widget tidak pernah menampilkan data yang tidak
        // berasal dari Room. CancellationException (coroutine dibatalkan) di-rethrow — menelan
        // pembatalan di sini membuat status pembatalan hilang dari struktur supervisi.
        val data = withContext(Dispatchers.Default) {
            try {
                val now = ZonedDateTime.now(WIB)
                WidgetData(
                    now = now,
                    nextClass = nextUp(app.container.schedulesDao.getEnabledOnce(), now),
                    nextAlarm = app.container.alarmsDao.nextClassAlarmOnce(now.toInstant().toEpochMilli()),
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                AppLog.error(TAG, "widget render gagal, fallback kosong: ${t.message}")
                WidgetData(now = ZonedDateTime.now(WIB), nextClass = null, nextAlarm = null)
            }
        }
        provideContent {
            ForfhWidgetTheme {
                ForfhWidgetContent(now = data.now, nextClass = data.nextClass, nextAlarm = data.nextAlarm)
            }
        }
    }
}

/** Data render satu kali provideGlance (immutable, dibaca dari Room). */
private data class WidgetData(
    val now: ZonedDateTime,
    val nextClass: Pair<ScheduleEntity, ZonedDateTime>?,
    val nextAlarm: ScheduledAlarmEntity?,
)

/**
 * Tema widget = skema warna app (ForFH DNA, ui/theme/Theme.kt) yang diterjemahkan
 * ke token Glance; ikut mode gelap sistem otomatis (ColorProviders light/dark).
 */
@Composable
private fun ForfhWidgetTheme(content: @Composable () -> Unit) {
    GlanceTheme(colors = ColorProviders(light = LightScheme, dark = DarkScheme), content = content)
}

/**
 * Isi widget, responsif terhadap LocalSize.current (salah satu dari set sizeMode, ukuran
 * render Glance, bukan ukuran launcher). Konten dirancang fillMaxSize di dalam ukuran tsb
 * agar penskalaan sistem ke ukuran launcher seragam. Standard ≥ 260dp, sisanya compact.
 *
 * Empty state jujur (R-38/R-27): tanpa kelas berikutnya → "Belum ada data" (bukan angka/data
 * palsu); tanpa alarm → baris alarm disembunyikan (kartu app juga hanya menampilkan baris
 * alarm saat ada data). Angka dan nama hanya dari Room (R-17). Label tanpa em dash (R-02):
 * hanya ":" dan "·".
 */
@Composable
fun ForfhWidgetContent(
    now: ZonedDateTime,
    nextClass: Pair<ScheduleEntity, ZonedDateTime>?,
    nextAlarm: ScheduledAlarmEntity?,
) {
    if (LocalSize.current.width >= standardSize.width) {
        StandardContent(now = now, nextClass = nextClass, nextAlarm = nextAlarm)
    } else {
        CompactContent(nextClass = nextClass, nextAlarm = nextAlarm)
    }
}

/**
 * Compact (150x110, padding 12 → 126x86): esensial saja: nama course (kode, fallback nama
 * lengkap) + jam mulai inline satu teks, baris "Alarm: HH:mm" saat ada alarm. Nama MEMBUNGKUS
 * penuh (tanpa maxLines: Glance 1.1.1 tidak punya ellipsis, maxLines hanya meng-clip) dan font
 * mengecil bertahap agar perkiraan tinggi tidak pernah melebihi budget (lihat
 * compactNameFontSize), inilah perbaikan bug lama yang memotong "FHK25" di ukuran kecil.
 * Konten di-center vertikal: blok info pendek di widget tinggi tetap seimbang (bukan
 * dead-space di bawah). Teks esensial onSurface, baris sekunder onSurfaceVariant: hierarki
 * sama dengan kartu app.
 */
@Composable
private fun CompactContent(
    nextClass: Pair<ScheduleEntity, ZonedDateTime>?,
    nextAlarm: ScheduledAlarmEntity?,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            // clickable SEBELUM padding: seluruh footprint widget (termasuk tepi) dapat di-tap,
            // bukan hanya area dalam padding: kontras teks baru sah karena ada permukaan nyata
            // (widgetBackground = background skema app, light #FAF9F7 / dark gelap, R-25).
            .clickable(actionStartActivity<MainActivity>(parameters = actionParametersOf(ActionParameters.Key<Int>("open_tab").to(0))))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start,
    ) {
        if (nextClass != null) {
            val fullText =
                "${nextClass.first.courseCode ?: nextClass.first.courseName} (${UiFormat.timeOf(nextClass.second)})"
            Text(
                text = fullText,
                style = TextStyle(
                    fontSize = compactNameFontSize(fullText.length),
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurface,
                ),
            )
        } else {
            Text(
                text = "Belum ada data",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
            )
        }
        if (nextAlarm != null) {
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = "Alarm: ${UiFormat.timeOf(nextAlarm.triggerAtMillis, WIB)}",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
                maxLines = 1, // "Alarm: HH:mm" format tetap, aman satu baris
            )
        }
    }
}

/**
 * Standard (260x110, padding 14 → 232x82): komposisi sama dengan KuliahCard (JadwalScreen):
 * bar warna course 4dp di kiri (identitas visual course) + blok teks. Header "Berikutnya" +
 * tanggal WIB ("Sen, 17 Agu", widgetDate) memberi konteks hari (kelas bisa esok lusa), nama
 * course lebih besar, baris meta "HH:mm · Alarm: HH:mm". Baris alarm mengikuti pola app:
 * hanya tampil saat ada alarm; tanpa alarm baris meta tetap menampilkan jam mulai. Nama
 * membungkus + font mengecil sesuai budget (standardNameFontSize) → tidak pernah ter-clip.
 * Satu aksen disengaja = bar warna course (antislop: satu focal accent, bukan di mana-mana).
 */
@Composable
private fun StandardContent(
    now: ZonedDateTime,
    nextClass: Pair<ScheduleEntity, ZonedDateTime>?,
    nextAlarm: ScheduledAlarmEntity?,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            // clickable SEBELUM padding: seluruh footprint widget (termasuk tepi) dapat di-tap,
            // bukan hanya area dalam padding: kontras teks baru sah karena ada permukaan nyata
            // (widgetBackground = background skema app, light #FAF9F7 / dark gelap, R-25).
            .clickable(actionStartActivity<MainActivity>(parameters = actionParametersOf(ActionParameters.Key<Int>("open_tab").to(0))))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (nextClass != null) {
            Box(
                modifier = GlanceModifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(widgetCourseColor(nextClass.first.courseColor)),
            ) {}
            Spacer(GlanceModifier.width(12.dp))
        }
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Berikutnya",
                    style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = widgetDate(now),
                    style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.height(4.dp))
            if (nextClass != null) {
                val name = nextClass.first.courseCode ?: nextClass.first.courseName
                Text(
                    text = name,
                    style = TextStyle(
                        fontSize = standardNameFontSize(name.length),
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurface,
                    ),
                )
            } else {
                Text(
                    text = "Belum ada data",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
                )
            }
            val meta = buildString {
                nextClass?.let { append(UiFormat.timeOf(it.second)) }
                if (nextAlarm != null) {
                    if (nextClass != null) append(" · ")
                    append("Alarm: ")
                    append(UiFormat.timeOf(nextAlarm.triggerAtMillis, WIB))
                }
            }
            if (meta.isNotEmpty()) {
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = meta,
                    style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    maxLines = 1, // "HH:mm" / "HH:mm · Alarm: HH:mm" format tetap
                )
            }
        }
    }
}

/**
 * Ukuran font nama di compact. Glance 1.1.1 tanpa text measurement dan ellipsis, jadi
 * perkiraan konservatif: lebar rata-rata karakter ≈ 0.55em, line height ≈ 1.25x fontSize.
 * Budget compact: tinggi 86dp (110 - padding 12x2) dikurangi baris alarm (13sp ≈ 16.25dp)
 * + gap 4dp → nama ≤ 65.75dp. Tiap cabang memastikan baris_terestimasi x 1.25 x fontSize
 * ≤ 65.75 (karakter per baris di lebar 126dp).
 */
internal fun compactNameFontSize(fullTextLength: Int): TextUnit = when {
    fullTextLength <= 45 -> 15.sp // 3 baris x 18.75dp = 56.25 ✓ (15 char/baris @15sp)
    fullTextLength <= 68 -> 13.sp // 4 baris x 16.25dp = 65 ✓ (17 char/baris @13sp)
    else -> 12.sp                 // 4 baris x 15dp = 60 ✓ (19 char/baris @12sp); nama >76 char
    // (5 baris) ter-clip di ujung ekstrem: batas API Glance, nama course >76 char praktis tak ada
}

/**
 * Ukuran font nama di standard. Budget: tinggi 82dp (110 - padding 14x2) dikurangi header
 * (12sp ≈ 15dp) + gap 4+2dp + meta (13sp ≈ 16.25dp) → nama ≤ 44.75dp (kasus terpadat dengan
 * alarm; tanpa alarm budget lebih longgar, aturan tetap aman). Karakter per baris di lebar
 * 232dp. Ukuran base 17sp sengaja di atas compact (15sp): nama adalah focal point standard.
 */
internal fun standardNameFontSize(nameLength: Int): TextUnit = when {
    nameLength <= 48 -> 17.sp // 2 baris x 21.25dp = 42.5 ✓ (24 char/baris @17sp)
    nameLength <= 56 -> 15.sp // 2 baris x 18.75dp = 37.5 ✓ (28 char/baris)
    nameLength <= 64 -> 13.sp // 2 baris x 16.25dp = 32.5 ✓ (32 char/baris)
    nameLength <= 70 -> 12.sp // 2 baris x 15dp = 30 ✓ (35 char/baris)
    else -> 11.sp             // 3 baris x 13.75dp = 41.25 ✓ (38 char/baris @11sp); nama >114
    // char ter-clip di ujung ekstrem: batas API Glance, nama course >114 char praktis tak ada
}

/**
 * Tanggal header standard, format "Sen, 17 Agu" (WIB, lokale id, konsisten dengan label
 * app). UiFormat tidak punya formatter tanggal pendek, jadi helper kecil lokal.
 */
private val widgetDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale("id", "ID"))

private fun widgetDate(now: ZonedDateTime): String = now.format(widgetDateFmt)

/**
 * Warna course hex "RRGGBB" → ColorProvider; fallback accent DNA ForFH. Duplikat kecil dari
 * courseColor() di ui/jadwal/JadwalScreen.kt (private di sana; reuse berarti menyentuh file
 * lain, dihindari per batasan tugas). Perilaku sama persis: parse gagal → ForfhColors.Accent
 * di kedua mode, persis seperti kartu app.
 */
private fun widgetCourseColor(hex: String): ColorProvider =
    runCatching { ColorProvider(Color(android.graphics.Color.parseColor(hex))) }
        .getOrDefault(ColorProvider(ForfhColors.Accent))

private val WIB: ZoneId = ZoneId.of("Asia/Jakarta")

private const val TAG = "ForfhWidget"
