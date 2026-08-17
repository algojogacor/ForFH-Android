package com.aryariap.forfh.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.aryariap.forfh.ForfhApp
import com.aryariap.forfh.MainActivity
import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import com.aryariap.forfh.ui.UiFormat
import com.aryariap.forfh.ui.jadwal.nextUp
import com.aryariap.forfh.ui.theme.DarkScheme
import com.aryariap.forfh.ui.theme.LightScheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Widget jadwal ForFH: kelas berikutnya + alarm berikutnya, dibaca dari Room tiap render
 * (trigger: refreshAll dari 4 titik update + updatePeriodMillis 30 mnt + render sistem saat
 * widget ditambah/diubah ukuran). Tap seluruh widget → MainActivity.
 */
class ForfhWidget : GlanceAppWidget() {

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
                    nextClass = nextUp(app.container.schedulesDao.getEnabledOnce(), now),
                    nextAlarm = app.container.alarmsDao.nextClassAlarmOnce(now.toInstant().toEpochMilli()),
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                WidgetData(nextClass = null, nextAlarm = null)
            }
        }
        provideContent {
            ForfhWidgetTheme {
                ForfhWidgetContent(nextClass = data.nextClass, nextAlarm = data.nextAlarm)
            }
        }
    }
}

/** Data render satu kali provideGlance (immutable, dibaca dari Room). */
private data class WidgetData(
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
 * Isi widget: baris utama = kelas berikutnya (nama + jam mulai), baris sekunder = alarm
 * berikutnya. Hierarki sama dengan kartu "Berikutnya" di JadwalScreen (judul onSurface,
 * baris sekunder onSurfaceVariant). Format teks mengikuti kartu app (ruling R13: ":", bukan
 * em dash): "Kelas berikutnya: NAMA (HH:mm)" + "Alarm: HH:mm".
 *
 * Empty state jujur (R-38/R-27): tanpa kelas berikutnya → "belum ada data" (bukan angka/data
 * palsu); tanpa alarm → baris alarm disembunyikan (kartu app juga hanya menampilkan baris
 * alarm saat ada data). Angka dan nama hanya dari Room (R-17).
 */
@Composable
fun ForfhWidgetContent(
    nextClass: Pair<ScheduleEntity, ZonedDateTime>?,
    nextAlarm: ScheduledAlarmEntity?,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        val name = nextClass?.first?.courseCode ?: nextClass?.first?.courseName
        Text(
            text = if (nextClass != null) {
                "Kelas berikutnya: $name (${UiFormat.timeOf(nextClass.second)})"
            } else {
                "Kelas berikutnya: belum ada data"
            },
            style = TextStyle(
                // Glance 1.1.1 tidak punya TextOverflow/ellipsis (maxLines=1 hanya meng-clip).
                // Nama panjang (>14 karakter, biasanya saat courseCode null dan fallback ke
                // courseName) dirender lebih kecil agar muat di baris 1; sisa yang lebih panjang
                // dari kapasitas 14sp tetap ter-clip (keterbatasan API, baris tetap terbaca
                // prefix-nya).
                fontSize = if (name != null && name.length > 14) 14.sp else 16.sp,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.onSurface,
            ),
            maxLines = 1,
        )
        if (nextAlarm != null) {
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = "Alarm: ${UiFormat.timeOf(nextAlarm.triggerAtMillis, WIB)}",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
                maxLines = 1,
            )
        }
    }
}

private val WIB: ZoneId = ZoneId.of("Asia/Jakarta")
