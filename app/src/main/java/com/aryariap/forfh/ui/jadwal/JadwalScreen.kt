package com.aryariap.forfh.ui.jadwal

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            Column {
                ForfhTopBar(
                    title = "Jadwal",
                    eyebrow = todayFormatted,
                    trailing = {
                        IconButton(onClick = viewModel::syncNow) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Sinkronkan",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
                // Segmented Tab Switcher (Hari ini / Seminggu)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                ) {
                    listOf("Hari ini", "Seminggu").forEachIndexed { index, label ->
                        val selected = tab == index
                        val bg by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            label = "tabBg",
                        )
                        val textColor by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "tabText",
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .clickable { tab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
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
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "Nikmati waktu istirahat atau pelajari materi berikutnya.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.today) { item ->
                            ScheduleRowCard(item)
                        }
                    }
                } else {
                    items(state.week) { hari ->
                        if (hari.items.isNotEmpty()) {
                            Text(
                                text = hari.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                            )
                            hari.items.forEach {
                                ScheduleRowCard(it)
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Terakhir sinkron: ${UiFormat.syncInfo(state.lastSyncStatus, state.lastSyncAt)}",
                        style = ForfhTypeExtras.MonoMeta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        shape = RoundedCornerShape(20.dp),
        color = ForfhColors.NavyDark,
        border = BorderStroke(1.dp, ForfhColors.Brass.copy(alpha = 0.35f)),
    ) {
        Box(
            modifier = Modifier.background(
                Brush.horizontalGradient(
                    listOf(ForfhColors.NavyDark, ForfhColors.NavyHeroEnd)
                )
            )
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Dynamic 5dp left accent strip
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(140.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                        .background(accent)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
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
                            color = ForfhColors.Brass,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing,
                        )

                        state.nextAlarm?.let {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = "Alarm",
                                    tint = ForfhColors.Brass,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = UiFormat.timeOf(it.triggerAtMillis, wib),
                                    style = ForfhTypeExtras.MonoMeta,
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    state.nextClass?.let { (kls, start) ->
                        Text(
                            text = kls.courseName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
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
                                    color = Color.White.copy(alpha = 0.7f),
                                )
                                Text(
                                    text = UiFormat.countdownTo(now, start),
                                    style = ForfhTypeExtras.MonoCountdown,
                                    color = ForfhColors.Brass,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            Text(
                                text = "${UiFormat.timeOf(start)} WIB",
                                style = ForfhTypeExtras.MonoMeta,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }

                    if (state.mutedToday) {
                        Text(
                            text = "Aktifkan lagi alarm hari ini",
                            style = MaterialTheme.typography.labelSmall,
                            color = ForfhColors.Brass,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onUnmute() }
                                .padding(vertical = 4.dp),
                        )
                    } else if (state.nextAlarm != null) {
                        Text(
                            text = "Senyapkan alarm hari ini",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onMute() }
                                .padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun courseColor(hex: String?): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(ForfhColors.Navy)

@Composable
private fun ScheduleRowCard(item: JadwalItem) {
    ForfhSurface(accent = courseColor(item.color)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Time Column
            Column(
                modifier = Modifier.width(56.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = UiFormat.timeText(item.startTime),
                    style = ForfhTypeExtras.MonoMeta,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = UiFormat.timeText(item.endTime),
                    style = ForfhTypeExtras.MonoMeta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Details Column
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.courseName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        when {
                            item.onlineUrl != null -> append("Daring")
                            !item.room.isNullOrBlank() -> append(item.room)
                            else -> append("FH UNAIR")
                        }
                        item.courseCode?.let { append(" · $it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
