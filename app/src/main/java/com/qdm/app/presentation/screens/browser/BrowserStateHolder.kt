package com.parveenbhadoo.qdm.presentation.screens.browser

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Survives NavController pop/push — keeps the last URL the browser was on
 * so we can restore it when the user navigates back to BrowserScreen.
 */
@Singleton
class BrowserStateHolder @Inject constructor() {
    var lastUrl: String = "https://www.google.com"
}
