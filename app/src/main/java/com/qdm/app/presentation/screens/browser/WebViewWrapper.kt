package com.qdm.app.presentation.screens.browser

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

@Composable
fun WebViewWrapper(
    url: String,
    adBlockHosts: Set<String>,
    onMediaDetected: (String, Map<String, String>) -> Unit,
    onPageTitleChanged: (String) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onNavigationState: (canBack: Boolean, canForward: Boolean) -> Unit,
    onAdBlocked: () -> Unit,
    webViewRef: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                @Suppress("DEPRECATION")
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                userAgentString = userAgentString // keep default
            }
        }
    }

    DisposableEffect(webView) {
        webViewRef(webView)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val host = request.url.host?.lowercase() ?: ""
                if (adBlockHosts.contains(host)) {
                    onAdBlocked()
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                }

                val urlStr = request.url.toString()
                if (isMediaUrl(urlStr)) {
                    val headers = request.requestHeaders ?: emptyMap()
                    onMediaDetected(urlStr, headers)
                }
                return null
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                onPageStarted(url)
                onNavigationState(view.canGoBack(), view.canGoForward())
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                onPageFinished(url)
                onNavigationState(view.canGoBack(), view.canGoForward())
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.cancel() // Strict SSL
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                onProgressChanged(newProgress)
            }
            override fun onReceivedTitle(view: WebView, title: String) {
                onPageTitleChanged(title)
            }
        }

        webView.loadUrl(url)

        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(factory = { webView }, modifier = modifier)
}

private fun isMediaUrl(url: String): Boolean {
    val lower = url.lowercase()
    return lower.contains(".mp4") || lower.contains(".mkv") || lower.contains(".avi") ||
           lower.contains(".mov") || lower.contains(".webm") ||
           lower.contains(".mp3") || lower.contains(".flac") || lower.contains(".aac") ||
           lower.contains(".m4a") || lower.contains(".wav") ||
           lower.contains(".m3u8") || lower.contains(".mpd") ||
           lower.contains("googlevideo.com") || lower.contains("videoplayback") ||
           lower.contains(".pdf")
}
