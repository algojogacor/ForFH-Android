package com.aryariap.forfh.ui.tugas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextDecoration
import com.aryariap.forfh.network.SubtaskDto
import com.aryariap.forfh.ui.UiFormat
import java.time.ZoneId
import kotlinx.serialization.json.Json

@Composable
fun TugasDetailScreen(viewModel: TugasViewModel, taskId: String) {
    val state by viewModel.state.collectAsState()
    val zone = ZoneId.of("Asia/Jakarta")
    val item = state.detail

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { viewModel.closeDetail() }) { Text("← Kembali") }
        }
        if (item == null) {
            Text("Tugas tidak ditemukan.", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        Text(item.title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        item.courseName?.let {
            Text("$it ${item.courseCode ?: ""}", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = buildString {
                append("Status: ${UiFormat.statusLabel(if (item.status == "DONE") "DONE" else item.computedStatus ?: item.status)}")
                append(" · Prioritas: ${item.priority.replaceFirstChar { it.uppercase() }}")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Deadline: ${UiFormat.deadline(item.dueAt, zone)}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.computedStatus == "OVERDUE" && item.status != "DONE") MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = item.description ?: "Tidak ada deskripsi.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Text("Subtugas", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        val subtasks = item.subtasksJson?.let {
            runCatching { Json.decodeFromString<List<SubtaskDto>>(it) }.getOrNull()
        } ?: emptyList()
        if (subtasks.isEmpty()) {
            Text(
                "Tidak ada subtugas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            subtasks.forEach { st ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (st.completed == 1) "✓ " else "• ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (st.completed == 1) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        st.title,
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (st.completed == 1) TextDecoration.LineThrough else null,
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        if (item.status != "DONE") {
            Button(
                onClick = { viewModel.markDone(item.id) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Tandai selesai") }
        }
        state.message?.let { msg ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = if (msg.startsWith("Gagal")) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}
