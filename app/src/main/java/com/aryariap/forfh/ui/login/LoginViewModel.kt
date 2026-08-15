package com.aryariap.forfh.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryariap.forfh.AppContainer
import com.aryariap.forfh.network.ErrorBody
import com.aryariap.forfh.network.LoginErrorMapper
import com.aryariap.forfh.network.LoginRequest
import com.aryariap.forfh.sync.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import kotlinx.serialization.json.Json

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

class LoginViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    init {
        // Pesan logout (eksplisit "Kamu sudah keluar." / 401 "Sesi berakhir, masuk lagi.") ditampilkan
        // sebagai baris error di LoginScreen (spec §10). StateFlow: VM lahir SETELAH navigasi tetap
        // menerima pesan; dikonsumsi setelah tampil agar tidak muncul lagi di kunjungan berikutnya.
        viewModelScope.launch {
            container.sessionManager.logoutMessage.collect { msg ->
                if (msg != null) {
                    _state.value = _state.value.copy(error = msg)
                    container.sessionManager.consumeLogoutMessage()
                }
            }
        }
    }

    fun onEmailChange(v: String) { _state.value = _state.value.copy(email = v, error = null) }
    fun onPasswordChange(v: String) { _state.value = _state.value.copy(password = v, error = null) }

    fun login() {
        val s = _state.value
        if (s.loading) return
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(error = "Isi email dan password.")
            return
        }
        _state.value = s.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val resp = container.apiService.login(LoginRequest(s.email.trim(), s.password))
                if (resp.isSuccessful && resp.body()?.success == true) {
                    // cookie sesi sudah tersimpan terenkripsi oleh CookieJar → sync pertama
                    container.sessionManager.onLoggedIn()
                    SyncWorker.enqueueOneShot(container.context)
                    _state.value = _state.value.copy(loading = false, success = true)
                } else {
                    val code = resp.code()
                    val serverMsg = resp.errorBody()?.let { body ->
                        runCatching {
                            Json { ignoreUnknownKeys = true }
                                .decodeFromString<ErrorBody>(body.string()).error
                        }.getOrNull()
                    }
                    _state.value = _state.value.copy(
                        loading = false,
                        error = LoginErrorMapper.map(code, serverMsg),
                    )
                }
            } catch (e: IOException) {
                _state.value = _state.value.copy(loading = false, error = LoginErrorMapper.mapNetwork())
            } catch (e: Exception) {
                // kegagalan selain network (mis. parse/timeout) → pesan umum login gagal
                _state.value = _state.value.copy(loading = false, error = LoginErrorMapper.map(502, null))
            }
        }
    }
}
