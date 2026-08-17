package com.aryariap.forfh.sync

import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Task 3 — TomorrowSummaryText pure-function test suite.
 * Covers all cases from the task brief:
 * (a) 2 kuliah + 1 tugas → string verbatim
 * (b) kuliah + 2 tugas → "· 2 tugas deadline"
 * (c) kuliah onlineUrl
 * (d) tanpa tugas → tanpa akhiran "·"
 * (e) urutan kuliah tidak sesuai input (acak) → terurut startTime
 * Plus: tanpa kuliah tapi ada tugas, null return, null dueAt, DONE tasks
 */
class TomorrowSummaryTextTest {

    private val zone = ZoneId.of("Asia/Jakarta")

    // 2026-08-17 = Senin. besok = 2026-08-18 = Selasa (java dow = 2)
    private val tomorrow = LocalDate.of(2026, 8, 18)

    private fun schedule(
        id: String = "s1",
        courseName: String = "Ilmu Negara",
        courseCode: String? = null,
        startTime: String = "15:00",
        room: String? = null,
        onlineUrl: String? = null,
        dayOfWeek: Int = 2, // besok Selasa
    ) = ScheduleEntity(
        id = id, courseId = "c1", courseName = courseName, courseCode = courseCode,
        courseColor = "#3b82f6", lecturer = null, credits = 2, dayOfWeek = dayOfWeek,
        startTime = startTime, endTime = "16:40", room = room, onlineUrl = onlineUrl, enabled = true,
    )

    /** dueAt dalam epoch ms — helper mengonversi tanggal WIB ke epoch ms tengah malam. */
    private fun dueAtWib(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun task(
        id: String = "t1",
        title: String = "Tugas Makalah",
        dueAt: Long? = dueAtWib(tomorrow),
        status: String = "NOT_STARTED",
    ) = TaskEntity(
        id = id, courseId = "c1", courseName = "Ilmu Negara", courseCode = null,
        title = title, description = null, dueAt = dueAt,
        status = status, computedStatus = null, priority = "medium",
        courseColor = null, subtasksJson = null,
    )

    // -------------------------------------------------------------------------
    // (a) 2 kuliah + 1 tugas → "Besok Selasa: PIH 13:00 (A101), Ilmu Negara 15:00 · 1 tugas deadline"
    // -------------------------------------------------------------------------
    @Test
    fun `2 kuliah + 1 tugas deadline`() {
        val sched1 = schedule(id = "s1", courseName = "Ilmu Negara", courseCode = null, startTime = "15:00", room = null)
        val sched2 = schedule(id = "s2", courseName = "PIH", courseCode = "PIH", startTime = "13:00", room = "A101")
        val t1 = task(id = "t1", title = "Makalah", dueAt = dueAtWib(tomorrow), status = "NOT_STARTED")

        val result = TomorrowSummaryText.build(listOf(sched1, sched2), listOf(t1), tomorrow, zone)

        assertEquals(
            "Besok Selasa: PIH 13:00 (A101), Ilmu Negara 15:00 · 1 tugas deadline",
            result,
        )
    }

    // -------------------------------------------------------------------------
    // (b) kuliah + 2 tugas → "· 2 tugas deadline"
    // -------------------------------------------------------------------------
    @Test
    fun `1 kuliah + 2 tugas deadline`() {
        val sched = schedule(courseName = "Hukum", courseCode = null, startTime = "08:00", room = "B201")
        val t1 = task(id = "t1", title = "Kuis", dueAt = dueAtWib(tomorrow), status = "NOT_STARTED")
        val t2 = task(id = "t2", title = "Makalah", dueAt = dueAtWib(tomorrow), status = "IN_PROGRESS")

        val result = TomorrowSummaryText.build(listOf(sched), listOf(t1, t2), tomorrow, zone)

        assertEquals(
            "Besok Selasa: Hukum 08:00 (B201) · 2 tugas deadline",
            result,
        )
    }

    // -------------------------------------------------------------------------
    // (c) kuliah dengan onlineUrl → "(Daring)"
    // -------------------------------------------------------------------------
    @Test
    fun `kuliah online - label Daring`() {
        val sched = schedule(
            id = "s1", courseName = "Konstitusi", courseCode = "KONST",
            startTime = "10:00", room = null, onlineUrl = "https://zoom.us/j/123",
        )
        // tidak ada tugas
        val result = TomorrowSummaryText.build(listOf(sched), emptyList(), tomorrow, zone)

        assertEquals(
            "Besok Selasa: KONST 10:00 (Daring)",
            result,
        )
    }

    // -------------------------------------------------------------------------
    // (d) tanpa tugas → tanpa akhiran "·"
    // -------------------------------------------------------------------------
    @Test
    fun `tanpa tugas - tidak ada akhiran titik`() {
        val sched = schedule(courseName = "PIH", courseCode = "PIH", startTime = "13:00", room = "A101")

        val result = TomorrowSummaryText.build(listOf(sched), emptyList(), tomorrow, zone)

        assertEquals(
            "Besok Selasa: PIH 13:00 (A101)",
            result,
        )
    }

    // -------------------------------------------------------------------------
    // (e) urutan kuliah tidak sesuai input (acak) → terurut startTime asc
    // -------------------------------------------------------------------------
    @Test
    fun `kuliah diacak - terurut startTime`() {
        // input: 15:00 dulu, baru 08:00 dan 13:00
        val sched1 = schedule(id = "s1", courseName = "Ilmu Negara", startTime = "15:00", room = null)
        val sched2 = schedule(id = "s2", courseName = "Hukum", startTime = "08:00", room = "A101")
        val sched3 = schedule(id = "s3", courseName = "PIH", courseCode = "PIH", startTime = "13:00", room = "A101")

        val result = TomorrowSummaryText.build(listOf(sched1, sched2, sched3), emptyList(), tomorrow, zone)

        assertEquals(
            "Besok Selasa: Hukum 08:00 (A101), PIH 13:00 (A101), Ilmu Negara 15:00",
            result,
        )
    }

    // -------------------------------------------------------------------------
    // Khusus: tanpa kuliah tapi ada tugas deadline → "tanpa kuliah · N tugas deadline"
    // -------------------------------------------------------------------------
    @Test
    fun `tanpa kuliah tapi ada tugas deadline`() {
        val t1 = task(id = "t1", title = "Makalah", dueAt = dueAtWib(tomorrow), status = "NOT_STARTED")

        val result = TomorrowSummaryText.build(emptyList(), listOf(t1), tomorrow, zone)

        assertEquals(
            "Besok Selasa: tanpa kuliah · 1 tugas deadline",
            result,
        )
    }

    // -------------------------------------------------------------------------
    // DONE tasks tidak dihitung
    // -------------------------------------------------------------------------
    @Test
    fun `tugas DONE tidak dihitung`() {
        val sched = schedule(courseName = "Hukum", startTime = "08:00", room = "B201")
        val t1 = task(id = "t1", title = "Kuis", dueAt = dueAtWib(tomorrow), status = "DONE")
        val t2 = task(id = "t2", title = "Makalah", dueAt = dueAtWib(tomorrow), status = "NOT_STARTED")

        val result = TomorrowSummaryText.build(listOf(sched), listOf(t1, t2), tomorrow, zone)

        assertEquals(
            "Besok Selasa: Hukum 08:00 (B201) · 1 tugas deadline",
            result,
        )
    }

    // -------------------------------------------------------------------------
    // dueAt null tidak crash, tidak dihitung
    // -------------------------------------------------------------------------
    @Test
    fun `tugas tanpa dueAt tidak dihitung`() {
        val sched = schedule(courseName = "Hukum", startTime = "08:00", room = "B201")
        val t1 = task(id = "t1", title = "Kuis", dueAt = null, status = "NOT_STARTED")

        val result = TomorrowSummaryText.build(listOf(sched), listOf(t1), tomorrow, zone)

        assertEquals(
            "Besok Selasa: Hukum 08:00 (B201)",
            result,
        )
    }

    // -------------------------------------------------------------------------
    // tugas jatuh besok lusa tidak dihitung
    // -------------------------------------------------------------------------
    @Test
    fun `tugas deadline lusa tidak dihitung`() {
        val sched = schedule(courseName = "Hukum", startTime = "08:00", room = "B201")
        val t1 = task(id = "t1", title = "Makalah", dueAt = dueAtWib(tomorrow.plusDays(1)), status = "NOT_STARTED")

        val result = TomorrowSummaryText.build(listOf(sched), listOf(t1), tomorrow, zone)

        assertEquals(
            "Besok Selasa: Hukum 08:00 (B201)",
            result,
        )
    }

    // -------------------------------------------------------------------------
    // null return: tidak ada kuliah DAN tidak ada tugas
    // -------------------------------------------------------------------------
    @Test
    fun `null bila tidak ada kuliah dan tidak ada tugas`() {
        val result = TomorrowSummaryText.build(emptyList(), emptyList(), tomorrow, zone)
        assertNull(result)
    }

    // -------------------------------------------------------------------------
    // Hari Indonesia — deterministik, bukan locale OS
    // -------------------------------------------------------------------------
    @Test
    fun `nama hari Indonesia - tiap hari`() {
        // Senin (dow=1, java) → besok = LocalDate + 1 day
        val cases = listOf(
            // (now dow, schedule dow) → besok nama hari
            Pair(1, 2) to "Selasa",
            Pair(2, 3) to "Rabu",
            Pair(3, 4) to "Kamis",
            Pair(4, 5) to "Jumat",
            Pair(5, 6) to "Sabtu",
            Pair(6, 0) to "Minggu",  // Sabtu (dow=6) → besok Minggu (schedule dow=0)
            Pair(0, 1) to "Senin",    // Minggu (dow=0) → besok Senin (schedule dow=1)
        )
        for ((pair, expectedDay) in cases) {
            val (nowDow, schedDow) = pair
            // Hitung tanggal besok relatif terhadap nowDow
            val today = LocalDate.of(2026, 8, 17) // Senin
            val nowDate = today.plusDays((nowDow - 1).toLong())
            val tomorrowDate = nowDate.plusDays(1)
            val sched = schedule(dayOfWeek = schedDow, startTime = "08:00")
            val result = TomorrowSummaryText.build(listOf(sched), emptyList(), tomorrowDate, zone)
            assertEquals(
                "Hari $expectedDay dari nowDow=$nowDow, schedDow=$schedDow",
                "Besok $expectedDay: Ilmu Negara 08:00",
                result,
            )
        }
    }
}
