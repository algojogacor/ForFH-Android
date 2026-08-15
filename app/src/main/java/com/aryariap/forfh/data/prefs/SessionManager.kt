package com.aryariap.forfh.data.prefs

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface SessionEvent {
    data object LoggedIn : SessionEvent

    /**
     * cleanupDone = false → berasal dari 401 (wipe §8.10 belum jalan, collector wajib menjalankannya);
     * true → logout eksplisit yang sudah di-wipe oleh container.logout sendiri — TIDAK wipe dua kali.
     */
    data class LoggedOut(val message: String, val cleanupDone: Boolean = false) : SessionEvent
}

class SessionManager(private val cookieStore: SecureCookieStore) {
    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<SessionEvent> = _events

    // Pesan logout terakhir utk LoginScreen — StateFlow (bukan SharedFlow): collector LoginScreen lahir
    // SETELAH navigasi 401/eksplisit tetap menerima pesan (SharedFlow tanpa replay tidak cukup).
    private val _logoutMessage = MutableStateFlow<String?>(null)
    val logoutMessage: StateFlow<String?> = _logoutMessage

    /** isLoggedIn = cookie sesi tersimpan (terdekripsi). */
    suspend fun isLoggedIn(): Boolean = cookieStore.readAll().isNullOrEmpty().not()

    fun onLoggedIn() { _events.tryEmit(SessionEvent.LoggedIn) }

    /**
     * 401 di /api/ selain login → auto-logout (spec §10). Pesan mandated disimpan utk LoginScreen;
     * cleanupDone = false → collector ForfhAppRoot menjalankan container.logout(message): wipe penuh
     * §8.10 TEPAT SEKALI per kejadian 401 (alarm di-cancel, Room + DataStore + cookie dihapus).
     */
    fun onSessionExpired() {
        val message = "Sesi berakhir, masuk lagi."
        _logoutMessage.value = message
        _events.tryEmit(SessionEvent.LoggedOut(message))
    }

    /** Emisi LoggedOut dari alur logout eksplisit — wipe sudah dijalankan oleh container.logout (cleanupDone = true). */
    fun tryEmitLoggedOut(message: String) {
        _logoutMessage.value = message
        _events.tryEmit(SessionEvent.LoggedOut(message, cleanupDone = true))
    }

    /** LoginScreen menampilkan pesan lalu mengonsumsinya — tidak muncul lagi di kunjungan berikutnya. */
    fun consumeLogoutMessage() { _logoutMessage.value = null }
}
