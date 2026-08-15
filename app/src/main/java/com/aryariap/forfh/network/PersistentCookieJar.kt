package com.aryariap.forfh.network

import com.aryariap.forfh.data.prefs.SecureCookieStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

class PersistentCookieJar(
    private val secureCookieStore: SecureCookieStore,
    private val scope: CoroutineScope,
) : CookieJar {

    // host -> string cookie (header value) — HANYA dalam memori saat runtime
    private val cookies = ConcurrentHashMap<String, String>()

    init {
        scope.launch { secureCookieStore.readAll()?.let { cookies.putAll(it) } }
    }

    override fun saveFromResponse(url: HttpUrl, cookieList: List<Cookie>) {
        if (cookieList.isEmpty()) return
        val value = cookieList.joinToString("; ") { it.toString() }
        cookies[url.host] = value
        scope.launch { secureCookieStore.writeAll(cookies.toMap()) }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val value = cookies[url.host] ?: return emptyList()
        // OkHttp 5.1: parseAll(url, Headers) — Header tak bisa dibangun dari string
        // "name=value" (butuh colon), jadi parse per-cookie (format persisted = toString()).
        return runCatching {
            value.split("; ").mapNotNull { Cookie.parse(url, it) }
        }.getOrDefault(emptyList())
    }

    /** Logout §8.10 — evict cookie in-memory (store dibersihkan terpisah oleh AppContainer). */
    fun clear() { cookies.clear() }
}
