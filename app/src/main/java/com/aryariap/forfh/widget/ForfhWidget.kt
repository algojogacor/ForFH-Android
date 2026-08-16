package com.aryariap.forfh.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
import com.aryariap.forfh.MainActivity
import com.aryariap.forfh.ui.theme.DarkScheme
import com.aryariap.forfh.ui.theme.LightScheme

/**
 * Widget jadwal ForFH (V1.1 Task 3, skeleton): kelas berikutnya + alarm berikutnya.
 * Task 4 membacakan data Room di provideGlance dan mengisi konten; sampai saat itu
 * teks "belum ada data" adalah placeholder jujur (antislop R-38), bukan data palsu.
 * Tap seluruh widget → MainActivity (action android.intent.action.MAIN).
 */
class ForfhWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            ForfhWidgetTheme {
                ForfhWidgetContent()
            }
        }
    }
}

/**
 * Tema widget = skema warna app (ForFH DNA, ui/theme/Theme.kt) yang diterjemahkan
 * ke token Glance; ikut mode gelap sistem otomatis (ColorProviders light/dark).
 */
@Composable
private fun ForfhWidgetTheme(content: @Composable () -> Unit) {
    GlanceTheme(colors = ColorProviders(light = LightScheme, dark = DarkScheme), content = content)
}

/**
 * Isi widget: dua baris label + placeholder jujur. Hierarki sama dengan kartu
 * "Berikutnya" di JadwalScreen (judul onSurface, baris sekunder onSurfaceVariant).
 */
@Composable
fun ForfhWidgetContent() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(16)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "Kelas berikutnya: belum ada data",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.onSurface,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(4))
        Text(
            text = "Alarm: belum ada data",
            style = TextStyle(
                fontSize = 14.sp,
                color = GlanceTheme.colors.onSurfaceVariant,
            ),
            maxLines = 1,
        )
    }
}
