package com.aryariap.forfh.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Preferences — pending mark selesai (Task 10, ruling R25): stringSet DataStore
 * di key "pending_mark_done" (id tugas yang PUT-nya belum dikonfirmasi server).
 *
 * Mengapa DataStore fake, bukan file asli (dokumentasi keputusan, lihat juga report Task 10):
 * datastore android 1.2.1 menulis via file tmp + java.io.File.renameTo bila
 * Build.VERSION.SDK_INT < 26 — dan di unit test JVM SDK_INT = 0. Di Windows, renameTo
 * TIDAK BISA menggantikan file yang sudah ada: tulis pertama sukses, tulis kedua dan
 * seterusnya selalu gagal "Unable to rename" (jalur yang diperbaiki Files.move(REPLACE_EXISTING)
 * hanya aktif di API 26+, dipakai produksi Android — di mesin Windows tidak terjangkau).
 * Ini bug library (google issue 203087070), bukan kode aplikasi; tanpa Robolectric
 * (constraint proyek: JUnit4 murni) multi-write ke file yang sama di Windows mustahil.
 *
 * DataStore adalah interface — fake di bawah menjalankan kontrak yang sama dengan yang asli
 * (updateData = transform atomik terserialisasi via Mutex) sehingga SEMUA logika Preferences
 * (key stringSet, default orEmpty, penghapusan key saat set kosong, add/remove atomik)
 * benar-benar diuji; lapisan file DataStore adalah kode Google yang teruji di device.
 */
class PreferencesTest {

    /** Fake DataStore<Preferences> — kontrak updateData identik dengan DataStore asli. */
    private class FakeDataStore : DataStore<Preferences> {
        private val mutex = Mutex()
        private val state = MutableStateFlow<Preferences>(preferencesOf())
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            mutex.withLock {
                val updated = transform(state.value)
                state.value = updated
                updated
            }
    }

    private fun prefs(dataStore: DataStore<Preferences> = FakeDataStore()) = Preferences(dataStore)

    @Test
    fun `pending mark done - round trip stringSet`() = runTest {
        val p = prefs()

        // Belum pernah di-set → kosong
        assertTrue(p.pendingMarkDone().isEmpty())

        p.setPendingMarkDone(setOf("t1", "t2"))
        assertEquals(setOf("t1", "t2"), p.pendingMarkDone())

        p.setPendingMarkDone(setOf("t2"))
        assertEquals(setOf("t2"), p.pendingMarkDone())

        // Set kosong → key dihapus (tidak menyisakan set kosong)
        p.setPendingMarkDone(emptySet())
        assertTrue(p.pendingMarkDone().isEmpty())
    }

    @Test
    fun `pending mark done - add dan remove atomik`() = runTest {
        val p = prefs()

        p.addPendingMarkDone("t1")
        p.addPendingMarkDone("t2")
        assertEquals(setOf("t1", "t2"), p.pendingMarkDone())

        p.removePendingMarkDone("t1")
        assertEquals(setOf("t2"), p.pendingMarkDone())

        // Set kosong setelah remove terakhir → key dihapus
        p.removePendingMarkDone("t2")
        assertTrue(p.pendingMarkDone().isEmpty())
    }

    @Test
    fun `pending mark done - state sync lain tidak ikut berubah`() = runTest {
        val p = prefs()

        p.setLastSync(1234L, "ok")
        p.addPendingMarkDone("t1")

        assertEquals(setOf("t1"), p.pendingMarkDone())
        assertEquals(1234L, p.lastSyncAt())
        assertEquals("ok", p.lastSyncStatus())
    }
}
