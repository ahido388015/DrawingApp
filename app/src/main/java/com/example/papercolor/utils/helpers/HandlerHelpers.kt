package com.example.papercolor.utils.helpers
import android.os.Handler
import android.os.Looper

object HandlerHelper {
    fun postDelayed(delayMillis: Long, action: () -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed(action, delayMillis)
    }
}