package com.parveenbhadoo.qdm.presentation.screens.browser

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.DownloadListener
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
import com.parveenbhadoo.qdm.utils.QdmLog
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
    onDownloadRequested: (url: String, headers: Map<String, String>, cookies: String) -> Unit,
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
                    QdmLog.d("WebView", "Media URL intercepted: $urlStr")
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
                handler.cancel()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                onProgressChanged(newProgress)
                onNavigationState(view.canGoBack(), view.canGoForward())
            }
            override fun onReceivedTitle(view: WebView, title: String) {
                onPageTitleChanged(title)
            }
        }

        // DownloadListener catches ALL server-initiated downloads:
        // Content-Disposition: attachment, direct file links, etc.
        // This is the primary mechanism — shouldInterceptRequest misses these.
        webView.setDownloadListener(DownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, contentLength ->
            QdmLog.i("WebView", "DownloadListener fired: url=$downloadUrl mime=$mimeType size=$contentLength")
            val cookies = android.webkit.CookieManager.getInstance().getCookie(downloadUrl) ?: ""
            val headers = mutableMapOf<String, String>()
            userAgent?.let { headers["User-Agent"] = it }
            cookies.takeIf { it.isNotBlank() }?.let { headers["Cookie"] = it }
            onDownloadRequested(downloadUrl, headers, cookies)
        })

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
