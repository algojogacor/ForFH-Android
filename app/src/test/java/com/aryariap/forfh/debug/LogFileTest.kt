package com.aryariap.forfh.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * LogFile murni (tanpa Android) — format WIB, rotasi, thread-safety, sanitasi baris.
 * Logging tidak pernah boleh crash: semua append di bawah kondisi ekstrem harus aman.
 */
class LogFileTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** 2026-08-17 07:00:00.123 UTC = 14:00:00.123 WIB — bukti konversi zona, bukan clock asal. */
    private val fixedWibClock: Clock =
        Clock.fixed(Instant.parse("2026-08-17T07:00:00.123Z"), ZoneId.of("UTC"))

    private fun newLog(maxBytes: Long = DEFAULT_CAP) =
        LogFile(File(tmp.root, "logs"), fixedWibClock, maxBytes)

    @Test
    fun `format garis - timestamp WIB plus level tag pesan`() {
        val log = newLog()
        assertTrue(log.append(LogLevel.INFO, "Test", "pesan"))
        val lines = log.readRecent(10)
        assertEquals(1, lines.size)
        assertEquals("2026-08-17 14:00:00.123 WIB | INFO | Test | pesan", lines[0])
    }

    @Test
    fun `format garis - warn dan error tetap satu garis`() {
        val log = newLog()
        assertTrue(log.append(LogLevel.WARN, "T1", "hati-hati"))
        assertTrue(log.append(LogLevel.ERROR, "T2", "rusak"))
        assertEquals(
            listOf(
                "2026-08-17 14:00:00.123 WIB | WARN | T1 | hati-hati",
                "2026-08-17 14:00:00.123 WIB | ERROR | T2 | rusak",
            ),
            log.readRecent(10),
        )
    }

    @Test
    fun `sanitasi - newline di pesan tidak boleh memecah garis`() {
        val log = newLog()
        assertTrue(log.append(LogLevel.INFO, "Test", "baris satu\nbaris dua\r\nbaris tiga"))
        val lines = log.readRecent(10)
        assertEquals(1, lines.size)
        assertEquals("2026-08-17 14:00:00.123 WIB | INFO | Test | baris satu baris dua baris tiga", lines[0])
    }

    @Test
    fun `sanitasi - pesan panjang dipangkas supaya file tetap terkendali`() {
        val log = newLog()
        val long = "x".repeat(2000)
        assertTrue(log.append(LogLevel.INFO, "Test", long))
        val line = log.readRecent(10).single()
        assertTrue("panjang garis harus dipangkas", line.length < 1000)
        assertTrue(line.endsWith("| " + "x".repeat(LogFile.MAX_MESSAGE_CHARS)))
    }

    @Test
    fun `rotasi - arsip dibuat saat aktif melebihi batas, hanya data terbaru dipertahankan`() {
        val log = newLog(maxBytes = 100)
        repeat(5) { i -> assertTrue(log.append(LogLevel.INFO, "Test", "pesan-$i")) }
        assertTrue("arsip harus ada setelah rotasi", log.archiveFile.exists())
        assertTrue("file aktif tidak boleh melebihi batas", log.activeFile.length() <= 100)
        // Kontrak rotasi: arsip + aktif = data TERBARU yang muat di 2x batas;
        // arsip lama ditimpa saat rotasi berikutnya (bukan menumpuk).
        val lines = log.readRecent(100)
        assertEquals(2, lines.size)
        assertTrue(lines[0].endsWith("pesan-3"))
        assertTrue(lines[1].endsWith("pesan-4"))
    }

    @Test
    fun `rotasi - arsip ditimpa, tidak pernah menumpuk`() {
        val log = newLog(maxBytes = 100)
        repeat(20) { i -> log.append(LogLevel.INFO, "Test", "pesan-$i") }
        assertTrue(log.archiveFile.exists())
        // maksimal satu arsip
        val archives = File(tmp.root, "logs").listFiles()!!.count { it.name == "forfh.log.1" }
        assertEquals(1, archives)
        val lines = log.readRecent(100)
        assertEquals(2, lines.size)
        assertTrue(lines.last().endsWith("pesan-19"))
    }

    @Test
    fun `readRecent - membatasi jumlah garis yang dikembalikan`() {
        val log = newLog()
        repeat(50) { i -> log.append(LogLevel.INFO, "Test", "pesan-$i") }
        val last = log.readRecent(20)
        assertEquals(20, last.size)
        assertTrue(last.first().endsWith("pesan-30"))
        assertTrue(last.last().endsWith("pesan-49"))
    }

    @Test
    fun `readRecent - batas tidak positif berarti kosong`() {
        val log = newLog()
        log.append(LogLevel.INFO, "Test", "pesan")
        assertTrue(log.readRecent(0).isEmpty())
        assertTrue(log.readRecent(-5).isEmpty())
    }

    @Test
    fun `thread safety - append konkuren tidak merusak garis`() {
        val log = newLog()
        val threads = 8
        val perThread = 40
        val pool = Executors.newFixedThreadPool(threads)
        try {
            val futures = (0 until threads).map { t ->
                pool.submit {
                    repeat(perThread) { i ->
                        assertTrue(log.append(LogLevel.INFO, "T$t", "m$i"))
                    }
                }
            }
            futures.forEach { it.get(30, TimeUnit.SECONDS) }
        } finally {
            pool.shutdown()
        }
        val lines = log.readRecent(10_000)
        assertEquals(threads * perThread, lines.size)
        assertEquals(threads * perThread, lines.distinct().size)
        lines.forEach { line ->
            assertTrue(line.startsWith("2026-08-17 14:00:00.123 WIB | INFO | "))
        }
    }

    @Test
    fun `clear - menghapus aktif dan arsip`() {
        val log = newLog(maxBytes = 100)
        repeat(5) { i -> log.append(LogLevel.INFO, "Test", "pesan-$i") }
        assertTrue(log.archiveFile.exists())
        log.clear()
        assertFalse(log.activeFile.exists())
        assertFalse(log.archiveFile.exists())
        assertTrue(log.readRecent(100).isEmpty())
    }

    @Test
    fun `append ke direktori yang belum ada - dibuat otomatis`() {
        val log = LogFile(File(tmp.root, "belum/ada/dir"), fixedWibClock)
        assertTrue(log.append(LogLevel.INFO, "Test", "pesan"))
        assertTrue(File(tmp.root, "belum/ada/dir/forfh.log").exists())
    }

    private companion object {
        const val DEFAULT_CAP = 512L * 1024
    }
}
