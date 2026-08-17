package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.TaskEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Teks notifikasi deadline tugas H-1 per tugas — murni, tanpa Android, bisa unit-test
 * (pola TaskReminderText). Hint relatif terhadap hari notifikasi ditembakkan:
 * "deadline besok" (H-1) / "deadline hari ini"; di luar itu tanggal ditampilkan (jalur defensif).
 */
object TaskDeadlineText {
    private const val PREFIX = "📚 "
    private val dateFmt = DateTimeFormatter.ofPattern("d MMM", Locale("id", "ID"))

    fun build(task: TaskEntity, deadlineDay: LocalDate, today: LocalDate): String {
        val hint = when (deadlineDay) {
            today -> "deadline hari ini"
            today.plusDays(1) -> "deadline besok"
            else -> "deadline ${deadlineDay.format(dateFmt)}"
        }
        val course = task.courseName?.takeIf { it.isNotBlank() }
        val body = if (course != null) "$course: ${task.title}" else task.title
        return "$PREFIX$body — $hint"
    }
}
