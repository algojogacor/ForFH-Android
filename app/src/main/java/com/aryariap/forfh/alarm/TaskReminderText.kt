package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.TaskEntity

object TaskReminderText {
    const val NO_TASKS_MESSAGE = "🎉 Tidak ada tugas hari ini — selamat beraktivitas!"
    private const val PREFIX = "📚 "

    /**
     * null = slot ini silent (tidak ada tugas dan bukan slot 09:00 — spec §8.7).
     * Urutan deadline terdekat (dueAt ASC NULLS LAST) sudah dijamin query Room,
     * tapi tetap diurutkan di sini sebagai lapisan kedua.
     */
    fun build(tasks: List<TaskEntity>, slotHour: Int): String? {
        if (tasks.isEmpty()) return if (slotHour == 9) NO_TASKS_MESSAGE else null
        val sorted = tasks.sortedWith(compareBy<TaskEntity> { it.dueAt ?: Long.MAX_VALUE })
        return when (sorted.size) {
            1 -> "$PREFIX${sorted.size} tugas belum selesai: ${sorted[0].title}"
            2 -> "$PREFIX${sorted.size} tugas belum selesai: ${sorted[0].title}, ${sorted[1].title}"
            else -> "$PREFIX${sorted.size} tugas belum selesai: ${sorted[0].title}, ${sorted[1].title}, +${sorted.size - 2} lagi"
        }
    }
}
