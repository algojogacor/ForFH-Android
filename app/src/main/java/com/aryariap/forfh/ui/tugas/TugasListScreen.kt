package com.aryariap.forfh.ui.tugas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aryariap.forfh.ui.UiFormat
import com.aryariap.forfh.ui.theme.ForfhColors
import java.time.ZoneId

@Composable
fun TugasListScreen(viewModel: TugasViewModel) {
    val state by viewModel.state.collectAsState()
    val zone = ZoneId.of("Asia/Jakarta")

    LaunchedEffect(state.message) {
        if (state.message != null && state.detail == null) {
            // snackbar singkat — pesan aksi; di-V1 pakai Text banner sederhana
        }
    }

    if (state.detail != null) {
        TugasDetailScreen(viewModel = viewModel, taskId = state.detail!!.id)
        return
    }

    Scaffold(
        topBar = {
            Text(
                text = "Tugas",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.items.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada tugas. Tarik untuk sinkronkan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.items) { item ->
                Card(
                    onClick = { viewModel.openDetail(item.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        val dot = runCatching { Color(android.graphics.Color.parseColor(item.courseColor ?: "#3b82f6")) }
                            .getOrDefault(ForfhColors.Accent)
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(8.dp)
                                .background(dot, RoundedCornerShape(4.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textDecoration = if (item.status == "DONE") TextDecoration.LineThrough else null,
                            )
                            Text(
                                text = buildString {
                                    item.courseName?.let { append(it) }
                                    item.dueAt?.let { due ->
                                        if (isNotEmpty()) append(" · ")
                                        append("Deadline ${UiFormat.deadline(due, zone)}")
                                    }
                                }.ifEmpty { "Tanpa deadline" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        StatusChip(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(item: TugasItem) {
    val (label, bg, fg) = when {
        item.status == "DONE" -> Triple("Selesai", ForfhColors.Success, Color.White)
        item.computedStatus == "OVERDUE" || item.status == "OVERDUE" ->
            Triple("Terlambat", ForfhColors.Danger, Color.White)
        item.status == "IN_PROGRESS" -> Triple("Proses", ForfhColors.Accent, Color.White)
        item.status == "REVISION" -> Triple("Revisi", ForfhColors.Warning, Color.White)
        else -> Triple("Belum", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = fg,
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
