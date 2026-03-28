package com.qdm.app.presentation.screens.browser

import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    initialUrl: String,
    onNavigateBack: () -> Unit,
    onDownloadRequested: (url: String, headers: Map<String, String>, cookies: String) -> Unit,
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adBlockHosts by viewModel.adBlockHosts.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var urlInput by remember { mutableStateOf(initialUrl) }

    // Show snackbar when media is detected
    val detectedUrl = uiState.detectedMediaUrl
    if (detectedUrl != null) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Media detected",
                actionLabel = "Download"
            )
            if (result == SnackbarResult.ActionPerformed) {
                val cookies = android.webkit.CookieManager.getInstance().getCookie(uiState.currentUrl) ?: ""
                onDownloadRequested(detectedUrl, uiState.detectedMediaHeaders, cookies)
            }
            viewModel.dismissMedia()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                        placeholder = { Text("Enter URL") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                val url = if (urlInput.startsWith("http")) urlInput
                                         else "https://$urlInput"
                                viewModel.setUrl(url)
                                webViewInstance?.loadUrl(url)
                            }
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { webViewInstance?.goBack() },
                        enabled = uiState.canGoBack
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Browser Back") }
                    IconButton(
                        onClick = { webViewInstance?.goForward() },
                        enabled = uiState.canGoForward
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Forward") }
                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { uiState.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                WebViewWrapper(
                    url = initialUrl,
                    adBlockHosts = adBlockHosts,
                    onMediaDetected = { url, headers -> viewModel.onMediaDetected(url, headers) },
                    onPageTitleChanged = { title -> viewModel.onPageFinished(uiState.currentUrl, title) },
                    onProgressChanged = viewModel::onProgressChanged,
                    onPageStarted = { url -> viewModel.onPageStarted(url); urlInput = url },
                    onPageFinished = { url -> viewModel.onPageFinished(url, uiState.pageTitle) },
                    onNavigationState = viewModel::onNavigationStateChanged,
                    onAdBlocked = viewModel::onAdBlocked,
                    webViewRef = { webViewInstance = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
