package com.deepsleep.app.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings

object HibernationManager {
    private val queue = mutableListOf<String>()
    private var currentIndex = 0
    var isRunning = false
        private set

    var onProgressCallback: ((current: Int, total: Int, packageName: String) -> Unit)? = null
    var onCompleteCallback: ((hibernatedCount: Int) -> Unit)? = null

    fun startHibernation(context: Context, packages: List<String>) {
        if (packages.isEmpty()) return
        queue.clear()
        queue.addAll(packages)
        currentIndex = 0
        isRunning = true
        processNext(context)
    }

    fun processNext(context: Context) {
        if (!isRunning) return
        if (currentIndex >= queue.size) {
            val count = queue.size
            stopHibernation()
            onCompleteCallback?.invoke(count)
            return
        }

        val pkg = queue[currentIndex]
        onProgressCallback?.invoke(currentIndex + 1, queue.size, pkg)

        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$pkg")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            currentIndex++
            processNext(context)
        }
    }

    fun notifyAppProcessed(context: Context) {
        if (!isRunning) return
        currentIndex++
        Handler(Looper.getMainLooper()).postDelayed({
            processNext(context)
        }, 700)
    }

    fun stopHibernation() {
        isRunning = false
        queue.clear()
        currentIndex = 0
    }
}
