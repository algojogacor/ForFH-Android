package com.aryariap.forfh.ui.jadwal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aryariap.forfh.ui.UiFormat
import com.aryariap.forfh.ui.theme.ForfhColors
import com.aryariap.forfh.ui.theme.ForfhPriorityPill
import com.aryariap.forfh.ui.theme.ForfhSurface
import com.aryariap.forfh.ui.theme.ForfhTypeExtras
import java.time.LocalDate

@Composable
fun CalendarDayAgenda(
    date: LocalDate,
    events: DayEvents,
    modifier: Modifier = Modifier,
    onTaskClick: ((String) -> Unit)? = null,
) {
    ForfhSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Agenda Header with Full Indonesian Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "AGENDA TANGGAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForfhColors.TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = UiFormat.fullDateIndonesian(date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ForfhColors.TextPrimary
                    )
                }

                val totalCount = events.classes.size + events.tasks.size + events.academic.size
                if (totalCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ForfhColors.SurfaceSecondary,
                        border = BorderStroke(1.dp, ForfhColors.BorderSubtle)
                    ) {
                        Text(
                            text = "$totalCount Acara",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = ForfhTypeExtras.MonoMeta.copy(fontSize = 11.sp),
                            color = ForfhColors.LinearIndigo,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (events.isEmpty) {
                EmptyAgendaState()
            } else {
                // 1. UTAMAKAN KELAS / JADWAL KULIAH
                if (events.classes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "JADWAL KULIAH (${events.classes.size})",
                            style = ForfhTypeExtras.MonoMeta.copy(fontSize = 11.sp),
                            color = ForfhColors.LinearIndigo,
                            fontWeight = FontWeight.Bold
                        )
                        events.classes.forEach { item ->
                            ClassAgendaCard(item)
                        }
                    }
                }

                // 2. UTAMAKAN TUGAS / DEADLINE
                if (events.tasks.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "DEADLINE TUGAS (${events.tasks.size})",
                            style = ForfhTypeExtras.MonoMeta.copy(fontSize = 11.sp),
                            color = ForfhColors.PriorityP2,
                            fontWeight = FontWeight.Bold
                        )
                        events.tasks.forEach { item ->
                            TaskAgendaCard(item, onClick = { onTaskClick?.invoke(item.id) })
                        }
                    }
                }

                // 3. KALENDER AKADEMIK (DI PALING BAWAH, DENGAN TONE MUTED ELEGANT)
                if (events.academic.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "KALENDER AKADEMIK",
                            style = ForfhTypeExtras.MonoMeta.copy(fontSize = 11.sp),
                            color = ForfhColors.TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                        events.academic.forEach { item ->
                            AcademicAgendaCard(item, date = date)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAgendaState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Tidak Ada Agenda",
            style = MaterialTheme.typography.titleSmall,
            color = ForfhColors.TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Tidak ada jadwal kuliah, deadline tugas, atau kegiatan akademik pada tanggal ini.",
            style = MaterialTheme.typography.bodySmall,
            color = ForfhColors.TextMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun AcademicAgendaCard(item: AcademicEventItem, date: LocalDate) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = ForfhColors.SurfaceSecondary,
        border = BorderStroke(1.dp, ForfhColors.BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ForfhColors.SurfaceHover),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = ForfhColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val badge = item.statusBadge(date)
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
                        color = ForfhColors.TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassAgendaCard(item: JadwalItem) {
    val accent = runCatching { Color(android.graphics.Color.parseColor(item.color)) }.getOrDefault(ForfhColors.LinearIndigo)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .background(accent)
            )

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.width(48.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = UiFormat.timeText(item.startTime),
                    style = ForfhTypeExtras.MonoMeta.copy(fontSize = 11.sp),
                    color = ForfhColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = UiFormat.timeText(item.endTime),
                    style = ForfhTypeExtras.MonoMeta.copy(fontSize = 10.sp),
                    color = ForfhColors.TextMuted
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.courseName,
                    style = MaterialTheme.typography.titleSmall,
                    color = ForfhColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ForfhColors.SurfaceSecondary,
                        border = BorderStroke(1.dp, ForfhColors.BorderSubtle)
                    ) {
                        Text(
                            text = when {
                                item.onlineUrl != null -> "Daring"
                                !item.room.isNullOrBlank() -> item.room
                                else -> "FH UNAIR"
                            },
                            style = ForfhTypeExtras.MonoMeta.copy(fontSize = 10.sp),
                            color = ForfhColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    item.courseCode?.let {
                        Text(
                            text = it,
                            style = ForfhTypeExtras.MonoMeta.copy(fontSize = 10.sp),
                            color = ForfhColors.TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskAgendaCard(item: TaskCalendarItem, onClick: () -> Unit) {
    val isDone = item.status == "DONE"
    val isOverdue = item.computedStatus == "OVERDUE" && !isDone

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = ForfhColors.SurfaceSecondary,
        border = BorderStroke(1.dp, ForfhColors.BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = if (isDone) ForfhColors.StatusSuccess else if (isOverdue) ForfhColors.PriorityP1 else ForfhColors.PriorityP2,
                modifier = Modifier.size(16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isDone) ForfhColors.TextMuted else ForfhColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.courseName?.let {
                        Text(
                            text = it,
                            style = ForfhTypeExtras.MonoMeta.copy(fontSize = 10.sp),
                            color = ForfhColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (item.dueTimeText.isNotBlank()) {
                        Text(
                            text = "· ${item.dueTimeText}",
                            style = ForfhTypeExtras.MonoMeta.copy(fontSize = 10.sp),
                            color = if (isOverdue) ForfhColors.PriorityP1 else ForfhColors.TextMuted
                        )
                    }
                }
            }

            ForfhPriorityPill(priority = item.priority)
        }
    }
}
