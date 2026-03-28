package com.parveenbhadoo.qdm.data.remote.interceptors

import com.parveenbhadoo.qdm.domain.model.AppSettings
import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor(
    private val getUserAgent: () -> String = { AppSettings.DEFAULT_USER_AGENT }
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = if (chain.request().header("User-Agent") == null) {
            chain.request().newBuilder().header("User-Agent", getUserAgent()).build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
