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
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
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
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Ukuran compact (~2x2 sel): konten minimal, cocok dengan minWidth/minHeight di XML. */
private val compactSize = DpSize(150.dp, 110.dp)

/** Ukuran standard (~4x2 sel): header + bar warna course + baris meta. */
private val standardSize = DpSize(260.dp, 110.dp)

/**
 * Widget jadwal ForFH: kelas berikutnya + alarm berikutnya, dibaca dari Room tiap render.
 * Tap seluruh widget -> MainActivity (Tab Kalender).
 */
class ForfhWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(compactSize, standardSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as ForfhApp
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

@Composable
private fun ForfhWidgetTheme(content: @Composable () -> Unit) {
    GlanceTheme(colors = ColorProviders(light = LightScheme, dark = DarkScheme), content = content)
}

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

@Composable
private fun CompactContent(
    nextClass: Pair<ScheduleEntity, ZonedDateTime>?,
    nextAlarm: ScheduledAlarmEntity?,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.surface)
            .clickable(actionStartActivity<MainActivity>(parameters = actionParametersOf(ActionParameters.Key<Int>("open_tab").to(0))))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (nextClass != null) {
            Box(
                modifier = GlanceModifier
                    .width(3.5.dp)
                    .fillMaxHeight()
                    .cornerRadius(2.dp)
                    .background(widgetCourseColor(nextClass.first.courseColor)),
            ) {}
            Spacer(GlanceModifier.width(9.dp))
        }

        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start,
        ) {
            if (nextClass != null) {
                val course = nextClass.first
                Text(
                    text = course.courseName,
                    style = TextStyle(
                        fontSize = compactNameFontSize(course.courseName.length),
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                    ),
                    maxLines = 2,
                )
                Spacer(GlanceModifier.height(2.dp))
                val timeStr = "${course.startTime} - ${course.endTime}"
                val roomStr = formatWidgetRoom(course.room)
                val secondLine = if (roomStr != null) "$timeStr · $roomStr" else timeStr
                Text(
                    text = secondLine,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
                    maxLines = 2,
                )
                if (nextAlarm != null) {
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = "Alarm: ${UiFormat.timeOf(nextAlarm.triggerAtMillis, WIB)}",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = ColorProvider(ForfhColors.LinearIndigo),
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                }
            } else {
                Text(
                    text = "Tidak ada kuliah",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                    ),
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = "Semua jadwal selesai",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

@Composable
private fun StandardContent(
    now: ZonedDateTime,
    nextClass: Pair<ScheduleEntity, ZonedDateTime>?,
    nextAlarm: ScheduledAlarmEntity?,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.surface)
            .clickable(actionStartActivity<MainActivity>(parameters = actionParametersOf(ActionParameters.Key<Int>("open_tab").to(0))))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (nextClass != null) {
            Box(
                modifier = GlanceModifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .cornerRadius(2.dp)
                    .background(widgetCourseColor(nextClass.first.courseColor)),
            ) {}
            Spacer(GlanceModifier.width(12.dp))
        }
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start,
        ) {
            // Header / Eyebrow row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "KULIAH BERIKUTNYA",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(ForfhColors.LinearIndigo),
                    ),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.defaultWeight())
                val rightHeader = if (nextClass != null) {
                    UiFormat.countdownTo(now, nextClass.second)
                } else {
                    widgetDate(now)
                }
                Text(
                    text = rightHeader,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(ForfhColors.TextMuted),
                    ),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.height(4.dp))

            // Main Title (Course Name)
            if (nextClass != null) {
                val course = nextClass.first
                Text(
                    text = course.courseName,
                    style = TextStyle(
                        fontSize = standardNameFontSize(course.courseName.length),
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                    ),
                    maxLines = 2,
                )

                Spacer(GlanceModifier.height(3.dp))

                // Secondary Info Row (Time, Room, Course Code, Alarm)
                val roomStr = formatWidgetRoom(course.room)
                val metaText = buildString {
                    append("${course.startTime} - ${course.endTime} WIB")
                    if (roomStr != null) append(" · $roomStr")
                    course.courseCode?.let { append(" · $it") }
                    if (nextAlarm != null) {
                        append(" · 🔔 ${UiFormat.timeOf(nextAlarm.triggerAtMillis, WIB)}")
                    }
                }
                Text(
                    text = metaText,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
                    maxLines = 2,
                )
            } else {
                Text(
                    text = "Tidak ada kuliah lagi",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                    ),
                )
                Spacer(GlanceModifier.height(3.dp))
                Text(
                    text = "Semua jadwal kuliah minggu ini telah selesai",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Format nama ruangan agar ringkas dan muat di widget home screen.
 * Mengambil kode ruangan spesifik (contoh: "LG02 B", "304", "3.06") tanpa redundansi nama gedung panjang.
 */
internal fun formatWidgetRoom(rawRoom: String?): String? {
    if (rawRoom.isNullOrBlank()) return null
    val trimmed = rawRoom.trim()
    if (trimmed.contains("daring", ignoreCase = true) || 
        trimmed.contains("online", ignoreCase = true) || 
        trimmed.contains("zoom", ignoreCase = true)) {
        return "Daring"
    }

    val parts = trimmed.split(" - ").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.isEmpty()) return null

    // Cari bagian yang mengandung kode ruangan spesifik (misal "LG02 B", "304", "3.06")
    val codePart = parts.firstOrNull { part ->
        part.matches(Regex("^[A-Za-z]{0,4}\\d+.*")) && !part.startsWith("Ruang Kelas", ignoreCase = true)
    } ?: parts.firstOrNull { it.matches(Regex(".*\\d+.*")) && !it.startsWith("Ruang Kelas", ignoreCase = true) }
      ?: parts.first()

    val cleaned = codePart
        .replace(Regex("^(Ruang Kelas|Ruang)\\s*", RegexOption.IGNORE_CASE), "")
        .trim()

    return if (cleaned.startsWith("R.", ignoreCase = true) || cleaned.startsWith("Ruang", ignoreCase = true)) {
        cleaned
    } else {
        "R. $cleaned"
    }
}

/**
 * Ukuran font nama di compact.
 */
internal fun compactNameFontSize(fullTextLength: Int): TextUnit = when {
    fullTextLength <= 45 -> 15.sp
    fullTextLength <= 68 -> 13.sp
    else -> 12.sp
}

/**
 * Ukuran font nama di standard.
 */
internal fun standardNameFontSize(nameLength: Int): TextUnit = when {
    nameLength <= 48 -> 17.sp
    nameLength <= 56 -> 15.sp
    nameLength <= 64 -> 13.sp
    nameLength <= 70 -> 12.sp
    else -> 11.sp
}

private val widgetDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.forLanguageTag("id-ID"))

private fun widgetDate(now: ZonedDateTime): String = now.format(widgetDateFmt)

private fun widgetCourseColor(hex: String): ColorProvider =
    runCatching { ColorProvider(Color(android.graphics.Color.parseColor(hex))) }
        .getOrDefault(ColorProvider(ForfhColors.LinearIndigo))

private val WIB: ZoneId = ZoneId.of("Asia/Jakarta")

private const val TAG = "ForfhWidget"
