package com.parveenbhadoo.qdm.data.remote.interceptors

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap

class CookieInterceptor : Interceptor {
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    val cookieJar: CookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore.getOrPut(url.host) { mutableListOf() }.addAll(cookies)
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            cookieStore[url.host] ?: emptyList()
    }

    fun addCookiesForHost(host: String, cookieString: String) {
        val list = cookieStore.getOrPut(host) { mutableListOf() }
        cookieString.split(";").forEach { pair ->
            val parts = pair.trim().split("=", limit = 2)
            if (parts.size == 2) {
                val cookie = Cookie.Builder()
                    .domain(host)
                    .name(parts[0].trim())
                    .value(parts[1].trim())
                    .build()
                list.removeIf { it.name == cookie.name }
                list.add(cookie)
            }
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}
