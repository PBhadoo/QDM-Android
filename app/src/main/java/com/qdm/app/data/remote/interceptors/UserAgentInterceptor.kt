package com.qdm.app.data.remote.interceptors

import com.qdm.app.domain.model.AppSettings
import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor(
    private val getUserAgent: () -> String = { AppSettings.DEFAULT_USER_AGENT }
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", getUserAgent())
            .build()
        return chain.proceed(request)
    }
}
