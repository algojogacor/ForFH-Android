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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.aryariap.forfh.network.SubtaskDto
import com.aryariap.forfh.ui.UiFormat
import com.aryariap.forfh.ui.theme.ForfhColors
import com.aryariap.forfh.ui.theme.ForfhStatusPill
import com.aryariap.forfh.ui.theme.ForfhSurface
import com.aryariap.forfh.ui.theme.ForfhTypeExtras
import com.aryariap.forfh.ui.theme.PrimaryButton
import java.time.ZoneId
import kotlinx.serialization.json.Json

@Composable
fun TugasDetailScreen(viewModel: TugasViewModel, taskId: String) {
    val state by viewModel.state.collectAsState()
    val zone = ZoneId.of("Asia/Jakarta")
    val item = state.detail

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = { viewModel.closeDetail() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "Detail Tugas",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        bottomBar = {
            if (item != null && item.status != "DONE") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    PrimaryButton(
                        text = "Tandai Selesai",
                        onClick = { viewModel.markDone(item.id) },
                        height = 50.dp,
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (item == null) {
                Text(
                    text = "Tugas tidak ditemukan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            // Status & Priority Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val statusLabel = UiFormat.statusLabel(if (item.status == "DONE") "DONE" else item.computedStatus ?: item.status)
                val (statusBg, statusFg) = when (statusLabel) {
                    "Selesai" -> ForfhColors.StatusSelesaiBg to ForfhColors.StatusSelesaiFg
                    "Terlambat" -> ForfhColors.StatusTerlambatBg to ForfhColors.StatusTerlambatFg
                    "Proses" -> ForfhColors.StatusProsesBg to ForfhColors.StatusProsesFg
                    "Revisi" -> ForfhColors.StatusRevisiBg to ForfhColors.StatusRevisiFg
                    else -> ForfhColors.StatusBelumBg to ForfhColors.StatusBelumFg
                }
                ForfhStatusPill(text = statusLabel, foreground = statusFg, background = statusBg)

                ForfhStatusPill(
                    text = "Prioritas ${item.priority.replaceFirstChar { it.uppercase() }}",
                    foreground = MaterialTheme.colorScheme.secondary,
                    background = MaterialTheme.colorScheme.secondaryContainer,
                )
            }

            // Title & Course
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item.courseName?.let {
                    Text(
                        text = "$it ${item.courseCode ?: ""}".trim(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Metadata Card (Deadline & Jam)
            ForfhSurface {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "DEADLINE PENGUMPULAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = item.dueAt?.let { UiFormat.deadline(it, zone) } ?: "Tanpa deadline",
                            style = ForfhTypeExtras.MonoMeta,
                            color = if (item.computedStatus == "OVERDUE" && item.status != "DONE") MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Description Card
            ForfhSurface {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "DESKRIPSI TUGAS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.description ?: "Tidak ada deskripsi tambahan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Subtasks Section
            val subtasks = item.subtasksJson?.let {
                runCatching { Json.decodeFromString<List<SubtaskDto>>(it) }.getOrNull()
            } ?: emptyList()

            if (subtasks.isNotEmpty()) {
                ForfhSurface {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "SUBTUGAS (${subtasks.count { it.completed == 1 }}/${subtasks.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        subtasks.forEach { st ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (st.completed == 1) ForfhColors.StatusSelesaiFg
                                            else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (st.completed == 1) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selesai",
                                            tint = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = st.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (st.completed == 1) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (st.completed == 1) TextDecoration.LineThrough else null,
                                )
                            }
                        }
                    }
                }
            }

            state.message?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (msg.startsWith("Gagal")) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
