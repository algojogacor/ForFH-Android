package com.aryariap.forfh.network

object LoginErrorMapper {
    const val WRONG_CREDENTIALS = "Email atau password salah."
    const val NETWORK = "Gangguan koneksi, coba lagi."

    /** HTTP error login: 429 → pesan rate limit server; lainnya (400/401/502 kampus) → kredensial salah. */
    fun map(code: Int?, serverMessage: String?): String = when (code) {
        429 -> serverMessage?.takeIf { it.isNotBlank() } ?: "Terlalu banyak percobaan. Coba lagi nanti."
        else -> WRONG_CREDENTIALS
    }

    fun mapNetwork(): String = NETWORK
}
