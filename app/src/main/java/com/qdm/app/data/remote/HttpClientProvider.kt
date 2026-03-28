package com.qdm.app.data.remote

import com.qdm.app.data.remote.interceptors.CookieInterceptor
import com.qdm.app.data.remote.interceptors.HeaderInterceptor
import com.qdm.app.data.remote.interceptors.UserAgentInterceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpClientProvider @Inject constructor() {

    val cookieInterceptor = CookieInterceptor()

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .cookieJar(cookieInterceptor.cookieJar)
        .addInterceptor(HeaderInterceptor())
        .addInterceptor(cookieInterceptor)
        .addInterceptor(UserAgentInterceptor())
        .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
        .connectionPool(
            okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES)
        )
        .build()
}
