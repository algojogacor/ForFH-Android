package com.aryariap.forfh.data.prefs

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

sealed interface SessionEvent {
    data object LoggedIn : SessionEvent
    data class LoggedOut(val message: String) : SessionEvent
}

class SessionManager(private val cookieStore: SecureCookieStore) {
    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<SessionEvent> = _events

    /** isLoggedIn = cookie sesi tersimpan (terdekripsi). */
    suspend fun isLoggedIn(): Boolean = cookieStore.readAll().isNullOrEmpty().not()

    fun onLoggedIn() { _events.tryEmit(SessionEvent.LoggedIn) }

    /** 401 di /api/ selain login → auto-logout (data dibersihkan oleh AppContainer.logout). */
    fun onSessionExpired() { _events.tryEmit(SessionEvent.LoggedOut("Sesi berakhir, masuk lagi.")) }

    /** Emisi LoggedOut dari alur logout eksplisit (bukan 401). */
    fun tryEmitLoggedOut(message: String) { _events.tryEmit(SessionEvent.LoggedOut(message)) }
}
