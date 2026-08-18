package com.aryariap.forfh.ui.jadwal

import androidx.compose.animation.animateColorAsState
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
                    title = "Jadwal",
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

                // Linear Segmented Tab Switcher (Hari ini / Seminggu)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ForfhColors.SurfaceSecondary)
                        .padding(3.dp),
                ) {
                    listOf("Hari ini", "Seminggu").forEachIndexed { index, label ->
                        val selected = tab == index
                        val bg by animateColorAsState(
                            if (selected) ForfhColors.SurfaceHover else Color.Transparent,
                            label = "tabBg",
                        )
                        val textColor by animateColorAsState(
                            if (selected) ForfhColors.TextPrimary else ForfhColors.TextMuted,
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
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (tab == 0) {
                    if (nextUp.nextClass != null || nextUp.nextAlarm != null) {
                        item {
                            NextUpHeroCard(
                                state = nextUp,
                                nowMs = nowMs,
                                onMute = nextUpViewModel::muteToday,
                                onUnmute = nextUpViewModel::unmuteToday,
                            )
                        }
                    }

                    if (state.today.isEmpty()) {
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
                                        text = "Tidak Ada Kuliah Hari Ini",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ForfhColors.TextPrimary,
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
                        // Notion Real-Time Timeline Indicator
                        item {
                            NotionCurrentTimeLine(nowMs = nowMs)
                        }
                        items(state.today) { item ->
                            NotionEventCard(item)
                        }
                    }
                } else {
                    items(state.week) { hari ->
                        if (hari.items.isNotEmpty()) {
                            Text(
                                text = hari.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = ForfhColors.LinearIndigo,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                            )
                            hari.items.forEach {
                                NotionEventCard(it)
                                Spacer(Modifier.height(8.dp))
                            }
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
 * Tinted background 12%, 3.5dp solid left accent strip, monospaced time, clean room badge.
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
