package com.aryariap.forfh.ui.jadwal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aryariap.forfh.ui.UiFormat
import com.aryariap.forfh.ui.info.SyncActivity
import com.aryariap.forfh.ui.theme.ForfhColors
import com.aryariap.forfh.ui.theme.ForfhPriorityPill
import com.aryariap.forfh.ui.theme.ForfhSurface
import com.aryariap.forfh.ui.theme.ForfhTopBar
import com.aryariap.forfh.ui.theme.ForfhTypeExtras
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun JadwalScreen(viewModel: JadwalViewModel, nextUpViewModel: NextUpViewModel) {
    val state by viewModel.state.collectAsState()
    val nextUp by nextUpViewModel.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(tab) {
        if (tab != 0) return@LaunchedEffect
        while (isActive) {
            nowMs = System.currentTimeMillis()
            nextUpViewModel.refresh()
            delay(30_000)
        }
    }

    val todayFormatted = remember {
        LocalDate.now(ZoneId.of("Asia/Jakarta")).format(
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))
        )
    }

    Scaffold(
        modifier = Modifier
            .background(ForfhColors.PitchBlack)
            .statusBarsPadding(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ForfhColors.PitchBlack)
            ) {
                ForfhTopBar(
                    title = "Kalender",
                    eyebrow = todayFormatted,
                    trailing = {
                        IconButton(onClick = viewModel::syncNow) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Sinkronkan",
                                tint = ForfhColors.TextSecondary,
                            )
                        }
                    },
                )

                // Linear Segmented Tab Switcher (Hari Ini / Seminggu / Bulan)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ForfhColors.SurfaceSecondary)
                        .padding(3.dp),
                ) {
                    listOf("Hari ini", "Seminggu", "Bulan").forEachIndexed { index, label ->
                        val selected = tab == index
                        val bg by animateColorAsState(
                            if (selected) ForfhColors.SurfaceHover else Color.Transparent,
                            animationSpec = tween(150),
                            label = "tabBg",
                        )
                        val textColor by animateColorAsState(
                            if (selected) ForfhColors.TextPrimary else ForfhColors.TextMuted,
                            animationSpec = tween(150),
                            label = "tabText",
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(bg)
                                .clickable { tab = index }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }

                // Linear Filter Bar (Kuliah, Tugas, Akademik)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CalendarFilterChip(
                        label = "Kuliah",
                        color = ForfhColors.LinearIndigo,
                        selected = state.activeFilters.contains(CalendarFilter.KULIAH),
                        onClick = { viewModel.toggleFilter(CalendarFilter.KULIAH) }
                    )

                    CalendarFilterChip(
                        label = "Tugas",
                        color = ForfhColors.PriorityP2,
                        selected = state.activeFilters.contains(CalendarFilter.TUGAS),
                        onClick = { viewModel.toggleFilter(CalendarFilter.TUGAS) }
                    )

                    CalendarFilterChip(
                        label = "Akademik",
                        color = ForfhColors.TextMuted,
                        selected = state.activeFilters.contains(CalendarFilter.AKADEMIK),
                        onClick = { viewModel.toggleFilter(CalendarFilter.AKADEMIK) }
                    )
                }
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.syncActivity == SyncActivity.RUNNING,
            onRefresh = viewModel::syncNow,
            modifier = Modifier
                .fillMaxSize()
                .background(ForfhColors.PitchBlack)
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (tab) {
                    // TAB 0: HARI INI
                    0 -> {
                        // 1. NextUp Hero Card (Kuliah Berikutnya)
                        if (state.activeFilters.contains(CalendarFilter.KULIAH) &&
                            (nextUp.nextClass != null || nextUp.nextAlarm != null)
                        ) {
                            item {
                                NextUpHeroCard(
                                    state = nextUp,
                                    nowMs = nowMs,
                                    onMute = nextUpViewModel::muteToday,
                                    onUnmute = nextUpViewModel::unmuteToday,
                                )
                            }
                        }

                        val showClasses = state.activeFilters.contains(CalendarFilter.KULIAH) && state.todayClasses.isNotEmpty()
                        val showTasks = state.activeFilters.contains(CalendarFilter.TUGAS) && state.todayTasks.isNotEmpty()
                        val showAcademic = state.activeFilters.contains(CalendarFilter.AKADEMIK) && state.todayAcademic.isNotEmpty()

                        if (!showClasses && !showTasks && !showAcademic) {
                            item {
                                ForfhSurface {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = "Tidak Ada Jadwal atau Deadline Hari Ini",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = ForfhColors.TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Nikmati waktu istirahat atau pelajari materi berikutnya.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = ForfhColors.TextMuted,
                                        )
                                    }
                                }
                            }
                        } else {
                            // 2. Notion Real-Time Timeline Indicator
                            if (showClasses || showTasks) {
                                item {
                                    NotionCurrentTimeLine(nowMs = nowMs)
                                }
                            }

                            // 3. Class items for today (UTAMAKAN JADWAL KULIAH)
                            if (showClasses) {
                                items(state.todayClasses) { item ->
                                    NotionEventCard(item)
                                }
                            }

                            // 4. Tasks due today (UTAMAKAN TUGAS)
                            if (showTasks) {
                                item {
                                    Text(
                                        text = "DEADLINE TUGAS HARI INI",
                                        style = ForfhTypeExtras.MonoMeta.copy(fontSize = 11.sp),
                                        color = ForfhColors.PriorityP2,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                    )
                                }
                                items(state.todayTasks) { task ->
                                    TaskDayCard(task)
                                }
                            }

                            // 5. Academic Events (TARUH PALING BAWAH)
                            if (showAcademic) {
                                item {
                                    Text(
                                        text = "KALENDER AKADEMIK HARI INI",
                                        style = ForfhTypeExtras.MonoMeta.copy(fontSize = 11.sp),
                                        color = ForfhColors.TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                    )
                                }
                                items(state.todayAcademic) { item ->
                                    AcademicAllDayBanner(item)
                                }
                            }
                        }
                    }

                    // TAB 1: SEMINGGU
                    1 -> {
                        items(state.week) { hari ->
                            val hasClasses = state.activeFilters.contains(CalendarFilter.KULIAH) && hari.items.isNotEmpty()
                            val hasTasks = state.activeFilters.contains(CalendarFilter.TUGAS) && hari.tasks.isNotEmpty()
                            val hasAcademic = state.activeFilters.contains(CalendarFilter.AKADEMIK) && hari.academic.isNotEmpty()

                            if (hasClasses || hasTasks || hasAcademic) {
                                Text(
                                    text = hari.label.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ForfhColors.LinearIndigo,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                                )

                                // 1. Classes first
                                if (hasClasses) {
                                    hari.items.forEach {
                                        NotionEventCard(it)
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }

                                // 2. Tasks second
                                if (hasTasks) {
                                    hari.tasks.forEach { task ->
                                        TaskDayCard(task)
                                        Spacer(Modifier.height(6.dp))
                                    }
                                }

                                // 3. Academic at the bottom
                                if (hasAcademic) {
                                    hari.academic.forEach { acad ->
                                        AcademicAllDayBanner(acad)
                                        Spacer(Modifier.height(6.dp))
                                    }
                                }
                            }
                        }
                    }

                    // TAB 2: BULAN (Notion Calendar Grid + Agenda)
                    2 -> {
                        item {
                            CalendarMonthGrid(
                                viewModel = viewModel,
                                state = state,
                            )
                        }

                        item {
                            CalendarDayAgenda(
                                date = state.selectedDate,
                                events = state.getEventsForDate(state.selectedDate),
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Terakhir sinkron: ${UiFormat.syncInfo(state.lastSyncStatus, state.lastSyncAt)}",
                        style = ForfhTypeExtras.MonoMeta,
                        color = ForfhColors.TextMuted,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarFilterChip(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) color.copy(alpha = 0.15f) else ForfhColors.Surface1
    val border = if (selected) color.copy(alpha = 0.45f) else ForfhColors.BorderSubtle
    val textC = if (selected) color else ForfhColors.TextMuted

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (selected) color else ForfhColors.TextMuted.copy(alpha = 0.4f))
            )
            Text(
                text = label,
                style = ForfhTypeExtras.MonoMeta.copy(fontSize = 11.sp),
                color = textC,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun AcademicAllDayBanner(item: AcademicEventItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = ForfhColors.SurfaceSecondary,
        border = BorderStroke(1.dp, ForfhColors.BorderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ForfhColors.SurfaceHover),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = ForfhColors.TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val today = LocalDate.now(ZoneId.of("Asia/Jakarta"))
                val badge = item.statusBadge(today)
                val isEnding = badge == "Berakhir Hari Ini" || badge == "Berakhir Besok"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = ForfhColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    if (badge != "Sedang Berlangsung") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isEnding) ForfhColors.PriorityP2.copy(alpha = 0.15f) else ForfhColors.SurfaceHover,
                            border = BorderStroke(1.dp, if (isEnding) ForfhColors.PriorityP2.copy(alpha = 0.3f) else ForfhColors.BorderSubtle)
                        ) {
                            Text(
                                text = badge,
                                style = ForfhTypeExtras.MonoMeta.copy(fontSize = 10.sp),
                                color = if (isEnding) ForfhColors.PriorityP2 else ForfhColors.TextMuted,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }

                if (item.dateRangeText.isNotBlank()) {
                    Text(
                        text = item.dateRangeText,
                        style = ForfhTypeExtras.MonoMeta.copy(fontSize = 11.sp),
                        color = ForfhColors.TextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskDayCard(task: TaskCalendarItem) {
    val isDone = task.status == "DONE"
    val isOverdue = task.computedStatus == "OVERDUE" && !isDone

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = ForfhColors.Surface1,
        border = BorderStroke(1.dp, ForfhColors.BorderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = if (isDone) ForfhColors.StatusSuccess else if (isOverdue) ForfhColors.PriorityP1 else ForfhColors.PriorityP2,
                modifier = Modifier.size(18.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isDone) ForfhColors.TextMuted else ForfhColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    task.courseName?.let {
                        Text(
                            text = it,
                            style = ForfhTypeExtras.MonoMeta.copy(fontSize = 11.sp),
                            color = ForfhColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (task.dueTimeText.isNotBlank()) {
                        Text(
                            text = "· ${task.dueTimeText}",
                            style = ForfhTypeExtras.MonoMeta.copy(fontSize = 11.sp),
                            color = if (isOverdue) ForfhColors.PriorityP1 else ForfhColors.TextMuted,
                        )
                    }
                }
            }

            ForfhPriorityPill(priority = task.priority)
        }
    }
}

@Composable
private fun NextUpHeroCard(
    state: NextUpUiState,
    nowMs: Long = System.currentTimeMillis(),
    onMute: () -> Unit,
    onUnmute: () -> Unit,
) {
    val wib = ZoneId.of("Asia/Jakarta")
    val now = Instant.ofEpochMilli(nowMs).atZone(wib)
    val accent = courseColor(state.nextClass?.first?.courseColor)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ForfhColors.SurfaceElevated,
        border = BorderStroke(1.dp, ForfhColors.BorderStrong),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left Accent Strip (3.5dp)
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(130.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(accent)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "KELAS BERIKUTNYA",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForfhColors.LinearIndigo,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )

                    state.nextAlarm?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ForfhColors.SurfaceSecondary)
                                .border(BorderStroke(1.dp, ForfhColors.BorderSubtle), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Alarm",
                                tint = ForfhColors.Warning,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = UiFormat.timeOf(it.triggerAtMillis, wib),
                                style = ForfhTypeExtras.MonoMeta,
                                color = ForfhColors.TextPrimary,
                            )
                        }
                    }
                }

                state.nextClass?.let { (kls, start) ->
                    Text(
                        text = kls.courseName,
                        style = MaterialTheme.typography.titleMedium,
                        color = ForfhColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column {
                            Text(
                                text = "Mulai dalam",
                                style = MaterialTheme.typography.bodySmall,
                                color = ForfhColors.TextMuted,
                            )
                            Text(
                                text = UiFormat.countdownTo(now, start),
                                style = ForfhTypeExtras.MonoCountdown,
                                color = ForfhColors.LinearIndigo,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ForfhColors.SurfaceSecondary,
                            border = BorderStroke(1.dp, ForfhColors.BorderSubtle),
                        ) {
                            Text(
                                text = "${UiFormat.timeOf(start)} WIB",
                                style = ForfhTypeExtras.MonoMeta,
                                color = ForfhColors.TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                if (state.mutedToday) {
                    Text(
                        text = "Aktifkan lagi alarm hari ini",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForfhColors.LinearIndigo,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onUnmute() }
                            .padding(vertical = 2.dp),
                    )
                } else if (state.nextAlarm != null) {
                    Text(
                        text = "Senyapkan alarm hari ini",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForfhColors.TextMuted,
                        modifier = Modifier
                            .clickable { onMute() }
                            .padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

private fun courseColor(hex: String?): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(ForfhColors.LinearIndigo)

/**
 * Notion Calendar Event Card:
 * Tinted background 10%, 3.5dp solid left accent strip, monospaced time, clean room badge.
 */
@Composable
private fun NotionEventCard(item: JadwalItem) {
    val accent = courseColor(item.color)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Notion Left Accent Strip
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                    .background(accent)
            )

            Spacer(Modifier.width(12.dp))

            // Time Range Column (Monospaced Notion Style)
            Column(
                modifier = Modifier.width(52.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = UiFormat.timeText(item.startTime),
                    style = ForfhTypeExtras.MonoMeta,
                    color = ForfhColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = UiFormat.timeText(item.endTime),
                    style = ForfhTypeExtras.MonoMeta,
                    color = ForfhColors.TextMuted,
                )
            }

            Spacer(Modifier.width(12.dp))

            // Course & Room Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.courseName,
                    style = MaterialTheme.typography.titleSmall,
                    color = ForfhColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ForfhColors.SurfaceSecondary.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, ForfhColors.BorderSubtle),
                    ) {
                        Text(
                            text = when {
                                item.onlineUrl != null -> "Daring"
                                !item.room.isNullOrBlank() -> item.room
                                else -> "FH UNAIR"
                            },
                            style = ForfhTypeExtras.MonoMeta,
                            color = ForfhColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    item.courseCode?.let { code ->
                        Text(
                            text = code,
                            style = ForfhTypeExtras.MonoMeta,
                            color = ForfhColors.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Penanda Waktu Real-Time mengikuti spec Notion Calendar (Red dot 8dp + Red line 2dp + Mono Time label).
 */
@Composable
private fun NotionCurrentTimeLine(nowMs: Long) {
    val zone = ZoneId.of("Asia/Jakarta")
    val now = Instant.ofEpochMilli(nowMs).atZone(zone)
    val timeStr = UiFormat.timeOf(now)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Red Marker Dot (8dp)
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(ForfhColors.NotionTimeIndicator)
        )

        // Red Indicator Line
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(ForfhColors.NotionTimeIndicator)
        )

        // Monospaced Time Tag
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = ForfhColors.NotionTimeIndicator.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, ForfhColors.NotionTimeIndicator.copy(alpha = 0.4f)),
        ) {
            Text(
                text = "$timeStr WIB",
                style = ForfhTypeExtras.MonoMeta,
                color = ForfhColors.NotionTimeIndicator,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}
