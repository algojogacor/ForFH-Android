package com.aryariap.forfh.ui.tugas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aryariap.forfh.ui.UiFormat
import com.aryariap.forfh.ui.info.SyncActivity
import com.aryariap.forfh.ui.theme.ForfhColors
import com.aryariap.forfh.ui.theme.ForfhStatusPill
import com.aryariap.forfh.ui.theme.ForfhSurface
import com.aryariap.forfh.ui.theme.ForfhTopBar
import com.aryariap.forfh.ui.theme.ForfhTypeExtras
import com.aryariap.forfh.ui.theme.LinearFilterChip
import com.aryariap.forfh.ui.theme.PrimaryButton
import com.aryariap.forfh.ui.theme.PriorityPill
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class TaskFilter {
    ALL, TODAY, OVERDUE, P1_URGENT, COMPLETED
}

@Composable
fun TugasListScreen(viewModel: TugasViewModel) {
    val state by viewModel.state.collectAsState()
    val zone = ZoneId.of("Asia/Jakarta")
    var selectedFilter by remember { mutableStateOf(TaskFilter.ALL) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.message) {
        if (state.message != null && state.detail == null) {
            delay(5_000)
            viewModel.consumeMessage()
        }
    }

    if (state.detail != null) {
        TugasDetailScreen(viewModel = viewModel, taskId = state.detail!!.id)
        return
    }

    // Grouping tasks based on Todoist pattern (Overdue, Today, Upcoming, Done)
    val nowLocalDate = remember { LocalDate.now(zone) }
    val activeTasks = state.items.filter { it.status != "DONE" }
    val overdueTasks = activeTasks.filter {
        it.computedStatus == "OVERDUE" || (it.dueAt != null && Instant.ofEpochMilli(it.dueAt).atZone(zone).toLocalDate().isBefore(nowLocalDate))
    }
    val todayTasks = activeTasks.filter {
        it !in overdueTasks && it.dueAt != null && Instant.ofEpochMilli(it.dueAt).atZone(zone).toLocalDate().isEqual(nowLocalDate)
    }
    val upcomingTasks = activeTasks.filter {
        it !in overdueTasks && it !in todayTasks
    }
    val completedTasks = state.items.filter { it.status == "DONE" }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        containerColor = ForfhColors.PitchBlack,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ForfhColors.PitchBlack)
            ) {
                ForfhTopBar(
                    title = "Tugas",
                    eyebrow = "${activeTasks.size} TUGAS AKTIF",
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

                // Linear Filter Bar
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item {
                        LinearFilterChip(
                            selected = selectedFilter == TaskFilter.ALL,
                            onClick = { selectedFilter = TaskFilter.ALL },
                            label = "Semua",
                            count = activeTasks.size,
                        )
                    }
                    if (overdueTasks.isNotEmpty()) {
                        item {
                            LinearFilterChip(
                                selected = selectedFilter == TaskFilter.OVERDUE,
                                onClick = { selectedFilter = TaskFilter.OVERDUE },
                                label = "Terlambat",
                                count = overdueTasks.size,
                            )
                        }
                    }
                    item {
                        LinearFilterChip(
                            selected = selectedFilter == TaskFilter.TODAY,
                            onClick = { selectedFilter = TaskFilter.TODAY },
                            label = "Hari Ini",
                            count = todayTasks.size,
                        )
                    }
                    item {
                        LinearFilterChip(
                            selected = selectedFilter == TaskFilter.P1_URGENT,
                            onClick = { selectedFilter = TaskFilter.P1_URGENT },
                            label = "P1 Urgent",
                            count = activeTasks.count { it.priority.equals("urgent", ignoreCase = true) || it.priority.equals("high", ignoreCase = true) },
                        )
                    }
                    if (completedTasks.isNotEmpty()) {
                        item {
                            LinearFilterChip(
                                selected = selectedFilter == TaskFilter.COMPLETED,
                                onClick = { selectedFilter = TaskFilter.COMPLETED },
                                label = "Selesai",
                                count = completedTasks.size,
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
            Column(Modifier.fillMaxSize()) {
                state.message?.let { MessageBanner(it) }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.items.isEmpty()) {
                        item {
                            ForfhSurface {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(
                                        text = "Belum Ada Tugas",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "Semua tugas telah diselesaikan atau belum termuat.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    PrimaryButton(
                                        text = "Sinkronkan Sekarang",
                                        onClick = viewModel::syncNow,
                                        height = 46.dp,
                                    )
                                }
                            }
                        }
                    }

                    // Render sections based on selected filter
                    when (selectedFilter) {
                        TaskFilter.OVERDUE -> {
                            items(overdueTasks, key = { it.id }) { item ->
                                TodoistTaskItem(
                                    item = item,
                                    zone = zone,
                                    onClick = { viewModel.openDetail(item.id) },
                                    onCheck = { viewModel.markDone(item.id) },
                                )
                            }
                        }
                        TaskFilter.TODAY -> {
                            items(todayTasks, key = { it.id }) { item ->
                                TodoistTaskItem(
                                    item = item,
                                    zone = zone,
                                    onClick = { viewModel.openDetail(item.id) },
                                    onCheck = { viewModel.markDone(item.id) },
                                )
                            }
                        }
                        TaskFilter.P1_URGENT -> {
                            val p1List = activeTasks.filter { it.priority.equals("urgent", ignoreCase = true) || it.priority.equals("high", ignoreCase = true) }
                            items(p1List, key = { it.id }) { item ->
                                TodoistTaskItem(
                                    item = item,
                                    zone = zone,
                                    onClick = { viewModel.openDetail(item.id) },
                                    onCheck = { viewModel.markDone(item.id) },
                                )
                            }
                        }
                        TaskFilter.COMPLETED -> {
                            items(completedTasks, key = { it.id }) { item ->
                                TodoistTaskItem(
                                    item = item,
                                    zone = zone,
                                    onClick = { viewModel.openDetail(item.id) },
                                    onCheck = { },
                                )
                            }
                        }
                        TaskFilter.ALL -> {
                            // Section 1: Terlambat / Overdue
                            if (overdueTasks.isNotEmpty()) {
                                item {
                                    SectionHeader(title = "TERLAMBAT", count = overdueTasks.size, color = ForfhColors.PriorityP1)
                                }
                                items(overdueTasks, key = { it.id }) { item ->
                                    TodoistTaskItem(
                                        item = item,
                                        zone = zone,
                                        onClick = { viewModel.openDetail(item.id) },
                                        onCheck = { viewModel.markDone(item.id) },
                                    )
                                }
                            }

                            // Section 2: Hari Ini / Today
                            if (todayTasks.isNotEmpty()) {
                                item {
                                    SectionHeader(title = "HARI INI", count = todayTasks.size, color = MaterialTheme.colorScheme.primary)
                                }
                                items(todayTasks, key = { it.id }) { item ->
                                    TodoistTaskItem(
                                        item = item,
                                        zone = zone,
                                        onClick = { viewModel.openDetail(item.id) },
                                        onCheck = { viewModel.markDone(item.id) },
                                    )
                                }
                            }

                            // Section 3: Mendatang / Upcoming
                            if (upcomingTasks.isNotEmpty()) {
                                item {
                                    SectionHeader(title = "MENDATANG", count = upcomingTasks.size, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                items(upcomingTasks, key = { it.id }) { item ->
                                    TodoistTaskItem(
                                        item = item,
                                        zone = zone,
                                        onClick = { viewModel.openDetail(item.id) },
                                        onCheck = { viewModel.markDone(item.id) },
                                    )
                                }
                            }

                            // Section 4: Selesai / Completed (collapsible/subtle)
                            if (completedTasks.isNotEmpty()) {
                                item {
                                    SectionHeader(title = "SELESAI", count = completedTasks.size, color = ForfhColors.StatusSelesaiFg)
                                }
                                items(completedTasks.take(5), key = { it.id }) { item ->
                                    TodoistTaskItem(
                                        item = item,
                                        zone = zone,
                                        onClick = { viewModel.openDetail(item.id) },
                                        onCheck = { },
                                    )
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
}

@Composable
private fun SectionHeader(title: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = color.copy(alpha = 0.12f),
        ) {
            Text(
                text = "$count",
                style = ForfhTypeExtras.MonoMeta,
                color = color,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun TodoistTaskItem(
    item: TugasItem,
    zone: ZoneId,
    onClick: () -> Unit,
    onCheck: () -> Unit,
) {
    val isDone = item.status == "DONE"
    var isChecking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val priorityColor = when (item.priority.lowercase()) {
        "urgent", "tinggi", "p1" -> ForfhColors.PriorityP1
        "high", "sedang", "p2" -> ForfhColors.PriorityP2
        "medium", "normal", "p3" -> ForfhColors.PriorityP3
        else -> ForfhColors.PriorityP4
    }

    val courseAccent = runCatching { Color(android.graphics.Color.parseColor(item.courseColor ?: "#14325B")) }
        .getOrDefault(ForfhColors.Navy)

    ForfhSurface(
        accent = courseAccent,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Todoist Circular Checkbox with Priority Ring
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !isDone) {
                        isChecking = true
                        scope.launch {
                            delay(250)
                            onCheck()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (isDone || isChecking) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(priorityColor, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selesai",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.size(18.dp),
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(2.dp, priorityColor),
                    ) {}
                }
            }

            // Task Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item.courseName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isDone || isChecking) TextDecoration.LineThrough else null,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = item.dueAt?.let { UiFormat.deadline(it, zone) } ?: "Tanpa deadline",
                        style = ForfhTypeExtras.MonoMeta,
                        color = if (item.computedStatus == "OVERDUE" && !isDone) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (!item.priority.isNullOrBlank()) {
                        PriorityPill(priority = item.priority)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBanner(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
    }
}
