package com.aryariap.forfh.ui.tugas

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aryariap.forfh.ui.UiFormat
import com.aryariap.forfh.ui.info.SyncActivity
import com.aryariap.forfh.ui.theme.ForfhAccentDot
import com.aryariap.forfh.ui.theme.ForfhColors
import com.aryariap.forfh.ui.theme.ForfhStatusPill
import com.aryariap.forfh.ui.theme.ForfhSurface
import com.aryariap.forfh.ui.theme.ForfhTopBar
import com.aryariap.forfh.ui.theme.ForfhTypeExtras
import com.aryariap.forfh.ui.theme.PrimaryButton
import java.time.ZoneId
import kotlinx.coroutines.delay

@Composable
fun TugasListScreen(viewModel: TugasViewModel) {
    val state by viewModel.state.collectAsState()
    val zone = ZoneId.of("Asia/Jakarta")

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

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            ForfhTopBar(
                title = "Tugas",
                eyebrow = "${state.items.size} TUGAS AKTIF",
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
                state.message?.let {
                    MessageBanner(it)
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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

                    items(state.items) { item ->
                        TaskRowCard(
                            item = item,
                            zone = zone,
                            onClick = { viewModel.openDetail(item.id) },
                            onRetry = { viewModel.markDone(item.id) },
                        )
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

@Composable
private fun TaskRowCard(
    item: TugasItem,
    zone: ZoneId,
    onClick: () -> Unit,
    onRetry: () -> Unit,
) {
    val dotColor = runCatching { Color(android.graphics.Color.parseColor(item.courseColor ?: "#14325B")) }
        .getOrDefault(ForfhColors.Navy)

    ForfhSurface(
        accent = dotColor,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                item.courseName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (item.status == "DONE") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (item.status == "DONE") TextDecoration.LineThrough else null,
                )

                Text(
                    text = item.dueAt?.let { "Deadline ${UiFormat.deadline(it, zone)}" } ?: "Tanpa deadline",
                    style = ForfhTypeExtras.MonoMeta,
                    color = if (item.computedStatus == "OVERDUE" && item.status != "DONE") MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TaskStatusPill(item = item, onRetry = onRetry)
        }
    }
}

@Composable
private fun TaskStatusPill(item: TugasItem, onRetry: () -> Unit) {
    val syncState = com.aryariap.forfh.data.db.TaskEntity.SyncState
    val (label, bg, fg) = when {
        item.status == "DONE" && item.syncState == syncState.PENDING ->
            Triple("Mengirim...", ForfhColors.StatusMengirimBg, ForfhColors.StatusMengirimFg)
        item.status == "DONE" && item.syncState == syncState.FAILED ->
            Triple("Gagal · Kirim ulang", ForfhColors.StatusGagalBg, ForfhColors.StatusGagalFg)
        item.status == "DONE" -> Triple("Selesai", ForfhColors.StatusSelesaiBg, ForfhColors.StatusSelesaiFg)
        item.computedStatus == "OVERDUE" || item.status == "OVERDUE" ->
            Triple("Terlambat", ForfhColors.StatusTerlambatBg, ForfhColors.StatusTerlambatFg)
        item.status == "IN_PROGRESS" -> Triple("Proses", ForfhColors.StatusProsesBg, ForfhColors.StatusProsesFg)
        item.status == "REVISION" -> Triple("Revisi", ForfhColors.StatusRevisiBg, ForfhColors.StatusRevisiFg)
        else -> Triple("Belum", ForfhColors.StatusBelumBg, ForfhColors.StatusBelumFg)
    }

    if (item.status == "DONE" && item.syncState == syncState.FAILED) {
        Box(modifier = Modifier.clickable { onRetry() }) {
            ForfhStatusPill(text = label, foreground = fg, background = bg)
        }
    } else {
        ForfhStatusPill(text = label, foreground = fg, background = bg)
    }
}
