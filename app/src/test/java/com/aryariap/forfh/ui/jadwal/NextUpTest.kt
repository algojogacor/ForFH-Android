package com.aryariap.forfh.ui.jadwal

import com.aryariap.forfh.data.db.ScheduleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Helper murni nextUp (V1.1 Task 4): kelas berikutnya = earliest upcoming class START
 * di semua schedule enabled (ruling R5) — satu sumber kebenaran untuk kartu "Berikutnya"
 * (NextUpViewModel) dan widget jadwal (ForfhWidget). Semua kasus WIB seperti AlarmPlannerTest.
 */
class NextUpTest {

    private val zone = ZoneId.of("Asia/Jakarta")

    private fun wib(s: String): ZonedDateTime =
        LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")).atZone(zone)

    private fun sched(id: String, day: Int, start: String, enabled: Boolean = true) = ScheduleEntity(
        id = id, courseId = "c$id", courseName = "Kuliah $id", courseCode = null,
        courseColor = "#c9a84c", lecturer = null, credits = 2, dayOfWeek = day,
        startTime = start, endTime = "09:40", room = "A101", onlineUrl = null, enabled = enabled,
    )

    @Test
    fun `kuliah pertama hari ini yang belum lewat dipilih - yang sudah lewat digeser ke minggu depan`() {
        val now = wib("2026-08-17T07:30") // Senin 07:30
        // Kontrak pemanggil: helper hanya menerima schedule enabled — filter enabled dilakukan
        // pemanggil (getEnabledOnce di NextUpViewModel/ForfhWidget), helper sendiri TIDAK membaca
        // flag enabled (lihat kasus "tanpa schedule enabled - null"). Di sini hanya soal waktu.
        val next = nextUp(
            listOf(
                sched("s1", day = 1, start = "08:00"), // Senin ini 08:00, belum lewat → dipilih
                sched("s2", day = 1, start = "07:00"), // sudah lewat (07:00 < 07:30) → Senin depan
                sched("s3", day = 2, start = "10:00"), // Selasa 10:00, lebih lambat dari s1
            ),
            now,
        )
        assertEquals("s1", next!!.first.id)
        assertEquals(wib("2026-08-17T08:00"), next.second)
    }

    @Test
    fun `semua kuliah hari ini sudah lewat - bukan null, ambil yang paling awal minggu depan`() {
        val now = wib("2026-08-17T08:30") // Senin 08:30, s1 08:00 dan s2 07:00 hari ini sudah lewat
        val next = nextUp(
            listOf(
                sched("s1", day = 1, start = "08:00"),
                sched("s2", day = 1, start = "07:00"),
            ),
            now,
        )
        assertEquals("s2", next!!.first.id)
        assertEquals(wib("2026-08-24T07:00"), next.second) // Senin depan, yang paling awal
    }

    @Test
    fun `tanpa schedule enabled - null`() {
        val now = wib("2026-08-17T07:30")
        // Kontrak helper: pemanggil menyerahkan schedule enabled (getEnabledOnce), jadi
        // "tidak ada enabled" tiba di helper sebagai list kosong (ruling R5: null hanya bila
        // tidak ada enabled schedule; filter enabled adalah pekerjaan pemanggil).
        assertNull(nextUp(emptyList(), now))
    }

    @Test
    fun `Senin dini hari 0030 dengan kelas Senin 0800 - hari ini, bukan minggu depan`() {
        val now = wib("2026-08-17T00:30") // Senin 00:30
        val next = nextUp(listOf(sched("s1", day = 1, start = "08:00")), now)
        assertEquals("s1", next!!.first.id)
        assertEquals(wib("2026-08-17T08:00"), next.second)
    }

    @Test
    fun `kelas yang mulainya sudah lewat tepat tengah malam - minggu depan hari yang sama`() {
        val now = wib("2026-08-17T00:30") // Senin 00:30, kelas Senin 00:00 sudah lewat
        val next = nextUp(listOf(sched("s1", day = 1, start = "00:00")), now)
        assertEquals("s1", next!!.first.id)
        assertEquals(wib("2026-08-24T00:00"), next.second) // Senin depan 00:00
    }
}
