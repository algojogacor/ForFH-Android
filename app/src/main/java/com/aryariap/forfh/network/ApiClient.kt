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

    fun build(cookieJar: PersistentCookieJar, sessionManager: SessionManager): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(SessionExpiryInterceptor(sessionManager))
            .addInterceptor(logging)
            .build()
    }

    fun retrofit(okHttpClient: OkHttpClient): ForfhApiService {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ForfhApiService::class.java)
    }
}
