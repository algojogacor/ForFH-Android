package com.aryariap.forfh.network

import org.junit.Assert.assertEquals
import org.junit.Test

class LoginErrorMapperTest {

    @Test
    fun `429 menampilkan pesan rate limit dari server`() {
        assertEquals(
            "Terlalu banyak percobaan. Coba lagi dalam 214 detik.",
            LoginErrorMapper.map(429, "Terlalu banyak percobaan. Coba lagi dalam 214 detik."),
        )
    }

    @Test
    fun `401 login = email atau password salah`() {
        assertEquals("Email atau password salah.", LoginErrorMapper.map(401, null))
    }

    @Test
    fun `502 dari verifikasi kampus = email atau password salah`() {
        assertEquals("Email atau password salah.", LoginErrorMapper.map(502, "Gagal verifikasi UNAIR"))
    }

    @Test
    fun `network error = gangguan koneksi`() {
        assertEquals("Gangguan koneksi, coba lagi.", LoginErrorMapper.mapNetwork())
    }
}
