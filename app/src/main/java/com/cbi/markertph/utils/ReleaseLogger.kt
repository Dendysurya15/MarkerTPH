package com.cbi.markertph.utils

import android.util.Log

object ReleaseLogger {
    private const val MAX_LOG_LENGTH = 4000
    private const val APP_TAG = "MarkerTPH"  // Change this to your app name
    private var isEnabled = true // You can control logging globally

    fun d(tag: String, message: String) {
        if (!isEnabled) return
        log(tag, message, Log.DEBUG)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (!isEnabled) return
        log(tag, if (throwable != null) "$message\n${Log.getStackTraceString(throwable)}" else message, Log.ERROR)
    }

    private fun log(tag: String, message: String, priority: Int) {
        // Split by line, then ensure each line can fit into Log's maximum length
        var i = 0
        val length = message.length
        while (i < length) {
            var newline = message.indexOf('\n', i)
            newline = if (newline != -1) newline else length
            do {
                val end = Math.min(newline, i + MAX_LOG_LENGTH)
                val part = message.substring(i, end)
                Log.println(priority, "$APP_TAG-$tag", part)
                i = end
            } while (i < newline)
            i++
        }
    }
}