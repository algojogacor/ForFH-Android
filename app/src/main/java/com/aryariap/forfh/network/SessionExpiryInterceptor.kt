package com.aryariap.forfh.network

import com.aryariap.forfh.data.prefs.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class SessionExpiryInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code == 401 && !request.url.encodedPath.startsWith("/api/auth/login")) {
            sessionManager.onSessionExpired()
        }
        return response
    }
}
