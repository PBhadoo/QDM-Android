package com.parveenbhadoo.qdm.utils

import android.util.Log

/**
 * Centralised logger — all output appears in Android Studio Logcat filtered by tag "QDM".
 * Usage: QdmLog.d("DownloadEngine", "Starting $id")
 */
object QdmLog {
    private const val TAG = "QDM"

    fun d(component: String, msg: String) = Log.d(TAG, "[$component] $msg")
    fun i(component: String, msg: String) = Log.i(TAG, "[$component] $msg")
    fun w(component: String, msg: String) = Log.w(TAG, "[$component] $msg")
    fun e(component: String, msg: String, t: Throwable? = null) =
        if (t != null) Log.e(TAG, "[$component] $msg", t)
        else Log.e(TAG, "[$component] $msg")
}
