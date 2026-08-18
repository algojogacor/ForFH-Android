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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aryariap.forfh.ui.UiFormat
import com.aryariap.forfh.ui.theme.ForfhColors
import com.aryariap.forfh.ui.theme.ForfhSurface
import com.aryariap.forfh.ui.theme.ForfhTypeExtras
import java.time.LocalDate
import java.time.YearMonth

private val DAY_NAMES_SHORT = listOf("SEN", "SEL", "RAB", "KAM", "JUM", "SAB", "MIN")

@Composable
fun CalendarMonthGrid(
    viewModel: JadwalViewModel,
    state: JadwalUiState,
    modifier: Modifier = Modifier,
) {
    val selectedMonth = state.selectedMonth
    val selectedDate = state.selectedDate
    val today = LocalDate.now()

    ForfhSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Month Navigation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = UiFormat.monthYear(selectedMonth),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ForfhColors.TextPrimary,
                    )

                    // Shortcut to Today
                    if (selectedMonth != YearMonth.from(today) || selectedDate != today) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewModel.jumpToToday() },
                            color = ForfhColors.LinearIndigo.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, ForfhColors.LinearIndigo.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "Hari Ini",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = ForfhTypeExtras.MonoMeta.copy(fontSize = 11.sp),
                                color = ForfhColors.LinearIndigo,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.prevMonth() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Bulan Sebelumnya",
                            tint = ForfhColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.nextMonth() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Bulan Selanjutnya",
                            tint = ForfhColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Days of Week Header (Mon - Sun)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                DAY_NAMES_SHORT.forEachIndexed { idx, name ->
                    val isWeekend = idx >= 5
                    Text(
                        text = name,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = ForfhTypeExtras.MonoMeta.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = if (isWeekend) ForfhColors.PriorityP1.copy(alpha = 0.70f) else ForfhColors.TextMuted
                    )
                }
            }

            // Month Grid Calculations (Monday = 1)
            val firstDayOfMonth = selectedMonth.atDay(1)
            val daysInMonth = selectedMonth.lengthOfMonth()
            val startDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1..7 (1 = Monday)
            val prevMonthDaysToShow = startDayOfWeek - 1

            val totalCells = ((prevMonthDaysToShow + daysInMonth + 6) / 7) * 7

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (weekIndex in 0 until (totalCells / 7)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (dayOffset in 0 until 7) {
                            val cellIndex = weekIndex * 7 + dayOffset
                            val dayNumber = cellIndex - prevMonthDaysToShow + 1

                            val date = when {
                                dayNumber < 1 -> {
                                    val prevMonth = selectedMonth.minusMonths(1)
                                    val prevMonthLength = prevMonth.lengthOfMonth()
                                    prevMonth.atDay(prevMonthLength + dayNumber)
                                }
                                dayNumber > daysInMonth -> {
                                    val nextMonth = selectedMonth.plusMonths(1)
                                    nextMonth.atDay(dayNumber - daysInMonth)
                                }
                                else -> selectedMonth.atDay(dayNumber)
                            }

                            val isCurrentMonth = dayNumber in 1..daysInMonth
                            val isToday = date == today
                            val isSelected = date == selectedDate
                            val dayEvents = state.getEventsForDate(date)

                            MonthDayCell(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                date = date,
                                isCurrentMonth = isCurrentMonth,
                                isToday = isToday,
                                isSelected = isSelected,
                                events = dayEvents,
                                onClick = { viewModel.selectDate(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    modifier: Modifier = Modifier,
    date: LocalDate,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    events: DayEvents,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> ForfhColors.LinearIndigo.copy(alpha = 0.22f)
            isToday -> ForfhColors.Surface2
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "cellBg"
    )

    val borderColor = when {
        isSelected -> ForfhColors.LinearIndigo
        isToday -> ForfhColors.LinearIndigo.copy(alpha = 0.60f)
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> ForfhColors.LinearIndigo
        isToday -> ForfhColors.TextPrimary
        isCurrentMonth -> ForfhColors.TextPrimary
        else -> ForfhColors.TextMuted.copy(alpha = 0.35f)
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(2.dp)
        ) {
            Text(
                text = "${date.dayOfMonth}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Multi-dot event indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Class Schedule Indicator (Sky Blue)
                if (events.classes.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(ForfhColors.LinearIndigo)
                    )
                }

                // 2. Task Deadlines Indicator (Priority Color)
                if (events.tasks.isNotEmpty()) {
                    val hasOverdue = events.tasks.any { it.computedStatus == "OVERDUE" && it.status != "DONE" }
                    val dotColor = if (hasOverdue) ForfhColors.PriorityP1 else ForfhColors.PriorityP2
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }

                // 3. Academic Calendar Indicator (Subtle Slate Pill)
                if (events.academic.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(width = 8.dp, height = 3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(ForfhColors.TextMuted)
                    )
                }
            }
        }
    }
}
