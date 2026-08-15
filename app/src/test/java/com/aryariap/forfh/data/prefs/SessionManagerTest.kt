package com.aryariap.forfh.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Fix round final review (§10): 401 → auto-logout HARUS lewat cleanup penuh + pesan mandated
 * di LoginScreen. Contract baru:
 *  - onSessionExpired (401) → LoggedOut(pesan mandated, cleanupDone = false) → collector menjalankan wipe §8.10.
 *  - tryEmitLoggedOut (logout eksplisit) → LoggedOut(pesan, cleanupDone = true) → wipe sudah dilakukan
 *    oleh container.logout sendiri — TIDAK dua kali.
 *  - logoutMessage (StateFlow) → LoginScreen yang lahir SETELAH navigasi tetap melihat pesan (SharedFlow
 *    tanpa replay tidak cukup), lalu consumeLogoutMessage().
 */
class SessionManagerTest {

    /** SecureCookieStore asli di atas DataStore JVM — metode yang diuji tidak menyentuh Android Keystore. */
    private fun sessionManager(): SessionManager {
        val dir = Files.createTempDirectory("forfh-session-test").toFile()
        val dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO)) {
            File(dir, "test.preferences_pb")
        }
        return SessionManager(SecureCookieStore(dataStore))
    }

    @Test
    fun `onSessionExpired - LoggedOut pesan mandated dgn cleanupDone false - 401 butuh cleanup`() = runTest {
        val sm = sessionManager()
        val received = mutableListOf<SessionEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { sm.events.collect { received += it } }

        sm.onSessionExpired()
        job.cancel()

        assertEquals(
            listOf<SessionEvent>(SessionEvent.LoggedOut("Sesi berakhir, masuk lagi.", cleanupDone = false)),
            received,
        )
        // Pesan siap dibaca LoginScreen meski navigasi baru terjadi setelahnya.
        assertEquals("Sesi berakhir, masuk lagi.", sm.logoutMessage.value)
    }

    @Test
    fun `tryEmitLoggedOut - cleanupDone true - logout eksplisit sudah wipe sendiri`() = runTest {
        val sm = sessionManager()
        val received = mutableListOf<SessionEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { sm.events.collect { received += it } }

        sm.tryEmitLoggedOut("Kamu sudah keluar.")
        job.cancel()

        assertEquals(
            listOf<SessionEvent>(SessionEvent.LoggedOut("Kamu sudah keluar.", cleanupDone = true)),
            received,
        )
        assertEquals("Kamu sudah keluar.", sm.logoutMessage.value)
    }

    @Test
    fun `consumeLogoutMessage - pesan dikonsumsi setelah ditampilkan`() {
        val sm = sessionManager()
        sm.tryEmitLoggedOut("Kamu sudah keluar.")
        assertEquals("Kamu sudah keluar.", sm.logoutMessage.value)
        sm.consumeLogoutMessage()
        assertNull(sm.logoutMessage.value)
    }
}
