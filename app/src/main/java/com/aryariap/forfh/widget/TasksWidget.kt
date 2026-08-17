package com.aryariap.forfh.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.aryariap.forfh.ForfhApp
import com.aryariap.forfh.MainActivity
import com.aryariap.forfh.data.db.TaskEntity
import com.aryariap.forfh.debug.AppLog
import com.aryariap.forfh.ui.UiFormat
import com.aryariap.forfh.ui.theme.DarkScheme
import com.aryariap.forfh.ui.theme.ForfhColors
import com.aryariap.forfh.ui.theme.LightScheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId

/** Ukuran compact: 150x110, isian vertikal tanpa header tanggal. */
private val compactSize = DpSize(150.dp, 110.dp)

/** Ukuran standard: 260x110, header label + maks 3 baris tugas + sync. */
private val standardSize = DpSize(260.dp, 110.dp)

private val WIB: ZoneId = ZoneId.of("Asia/Jakarta")

/**
 * Widget tugas: 3 tugas terdekat + status sync, dibaca dari Room tiap render.
 * Trigger: refreshAll (WidgetUpdater) + updatePeriodMillis 30 mnt + render sistem.
 * Tap seluruh widget ke MainActivity tab 1 (TugasScreen).
 */
class TasksWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(compactSize, standardSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as ForfhApp
        val data = withContext(Dispatchers.Default) {
            try {
                TasksWidgetData(
                    tasks = nextTasks(app.container.tasksDao.getActiveByDeadline()),
                    lastSyncAt = app.container.syncState.lastSyncAt(),
                    lastSyncStatus = app.container.syncState.lastSyncStatus(),
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                AppLog.error(TAG, "tasks widget render gagal, fallback kosong: ${t.message}")
                TasksWidgetData(tasks = emptyList(), lastSyncAt = 0L, lastSyncStatus = "")
            }
        }
        provideContent {
            TasksWidgetTheme {
                TasksWidgetContent(data = data)
            }
        }
    }
}

private data class TasksWidgetData(
    val tasks: List<TaskEntity>,
    val lastSyncAt: Long,
    val lastSyncStatus: String,
)

@Composable
private fun TasksWidgetTheme(content: @Composable () -> Unit) {
    GlanceTheme(colors = ColorProviders(light = LightScheme, dark = DarkScheme), content = content)
}

@Composable
private fun TasksWidgetContent(data: TasksWidgetData) {
    if (LocalSize.current.width >= standardSize.width) {
        TasksWidgetStandardContent(data)
    } else {
        TasksWidgetCompactContent(data)
    }
}

@Composable
private fun TasksWidgetCompactContent(data: TasksWidgetData) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .clickable(actionStartActivity<MainActivity>(parameters = actionParametersOf(ActionParameters.Key<Int>("open_tab").to(1))))
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        if (data.tasks.isEmpty()) {
            Text(
                text = "Tidak ada tugas aktif",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
            )
        } else {
            data.tasks.take(3).forEach { task ->
                CompactTaskRow(task)
                Spacer(GlanceModifier.height(4.dp))
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        Text(
            text = syncStatusLine(data.lastSyncAt, data.lastSyncStatus, System.currentTimeMillis()),
            style = TextStyle(
                fontSize = 11.sp,
                color = GlanceTheme.colors.onSurfaceVariant,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun CompactTaskRow(task: TaskEntity) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dotColor = taskDotColor(task.courseColor)
        Box(
            modifier = GlanceModifier
                .size(6.dp)
                .background(dotColor),
        ) {}
        Spacer(GlanceModifier.width(5.dp))
        Text(
            text = task.title,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.onSurface,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.defaultWeight())
        task.dueAt?.let { dueAt ->
            Text(
                text = UiFormat.timeOf(dueAt, WIB),
                style = TextStyle(
                    fontSize = 11.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TasksWidgetStandardContent(data: TasksWidgetData) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .clickable(actionStartActivity<MainActivity>(parameters = actionParametersOf(ActionParameters.Key<Int>("open_tab").to(1))))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "Tugas terdekat",
            style = TextStyle(
                fontSize = 12.sp,
                color = GlanceTheme.colors.onSurfaceVariant,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(6.dp))
        if (data.tasks.isEmpty()) {
            Text(
                text = "Tidak ada tugas aktif",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
            )
        } else {
            data.tasks.take(3).forEach { task ->
                StandardTaskRow(task)
                Spacer(GlanceModifier.height(5.dp))
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        Text(
            text = syncStatusLine(data.lastSyncAt, data.lastSyncStatus, System.currentTimeMillis()),
            style = TextStyle(
                fontSize = 11.sp,
                color = GlanceTheme.colors.onSurfaceVariant,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun StandardTaskRow(task: TaskEntity) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dotColor = taskDotColor(task.courseColor)
        Box(
            modifier = GlanceModifier
                .size(8.dp)
                .background(dotColor),
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = task.title,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.onSurface,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.defaultWeight())
        task.dueAt?.let { dueAt ->
            Text(
                text = UiFormat.timeOf(dueAt, WIB),
                style = TextStyle(
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
                maxLines = 1,
            )
        }
    }
}

/**
 * Warna dot course hex "RRGGBB" -> ColorProvider; fallback accent DNA ForFH.
 * Pola sama persis dengan widgetCourseColor() di ForfhWidget.kt.
 */
private fun taskDotColor(hex: String?): ColorProvider =
    runCatching {
        hex?.let { ColorProvider(Color(android.graphics.Color.parseColor(it))) }
    }.getOrNull()
        ?: ColorProvider(ForfhColors.Accent)

private const val TAG = "TasksWidget"
