package com.aryariap.forfh.network

import com.aryariap.forfh.data.prefs.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // 1 deploy Koyeb yang ada — nol deploy baru, nol perubahan server (spec §2, §4)
    private const val BASE_URL = "https://usual-olwen-algojogacorbgt-a2be655b.koyeb.app/"

    // Fix pasca-rilis 2026-08-15: server produksi mengirim "credits":null (courses.credits NULL
    // di DB). kotlinx-serialization TIDAK memakai default value saat field ada tapi null —
    // null untuk Int non-nullable = SerializationException → sync selalu gagal. coerceInputValues
    // menetapkan: null → default value (dokumentasi resmi kotlinx-serialization). Menutup kelas
    // bug null-vs-default untuk SEMUA DTO tanpa mengubah shape response (nol perubahan server).
    internal val forfhJson = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun build(cookieJar: PersistentCookieJar, sessionManager: SessionManager): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .addInterceptor(SessionExpiryInterceptor(sessionManager))
            .addInterceptor(logging)
            .build()
    }

    fun retrofit(okHttpClient: OkHttpClient): ForfhApiService {
        val json = forfhJson
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ForfhApiService::class.java)
    }
}
