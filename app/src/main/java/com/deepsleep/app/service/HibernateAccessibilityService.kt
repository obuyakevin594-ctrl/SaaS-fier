package com.deepsleep.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent
import android.os.Handler
import android.os.Looper

class HibernateAccessibilityService : AccessibilityService() {

    companion object {
        var instance: HibernateAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!HibernationManager.isRunning) return

        val rootNode = rootInActiveWindow ?: return
        val packageName = event.packageName?.toString() ?: return

        if (packageName.contains("settings", ignoreCase = true) ||
            packageName.contains("securitycenter", ignoreCase = true) ||
            packageName.contains("safecenter", ignoreCase = true)) {

            if (findAndClickForceStop(rootNode)) {
                Handler(Looper.getMainLooper()).postDelayed({
                    val popupRoot = rootInActiveWindow
                    if (popupRoot != null) {
                        findAndClickConfirmation(popupRoot)
                    }
                    HibernationManager.notifyAppProcessed(this)
                }, 500)
            }
        }
    }

    private fun findAndClickForceStop(node: AccessibilityNodeInfo): Boolean {
        if (isForceStopNode(node)) {
            if (node.isEnabled) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            } else {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickForceStop(child)) {
                child.recycle()
                return true
            }
            child.recycle()
        }
        return false
    }

    private fun isForceStopNode(node: AccessibilityNodeInfo): Boolean {
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""

        val isIdMatch = viewId.contains("force_stop") ||
                viewId.contains("right_button") ||
                viewId.contains("button1") ||
                viewId.contains("kill")

        val isTextMatch = text.contains("force stop") ||
                text.contains("force close") ||
                text.contains("forcer l'arrêt") ||
                text.contains("stoppen erzwingen") ||
                text.contains("detener") ||
                text.contains("forçar parada") ||
                text.contains("arresto forzato") ||
                text.contains("остановить")

        return isIdMatch || isTextMatch
    }

    private fun findAndClickConfirmation(node: AccessibilityNodeInfo): Boolean {
        if (isConfirmationNode(node)) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickConfirmation(child)) {
                child.recycle()
                return true
            }
            child.recycle()
        }
        return false
    }

    private fun isConfirmationNode(node: AccessibilityNodeInfo): Boolean {
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""

        val isIdMatch = viewId.contains("button1") || viewId.contains("ok")
        val isTextMatch = text.contains("ok") ||
                text.contains("force stop") ||
                text.contains("force close") ||
                text.contains("confirm") ||
                text.contains("yes")

        return isIdMatch || isTextMatch
    }

    override fun onInterrupt() {
        HibernationManager.stopHibernation()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        HibernationManager.stopHibernation()
    }
}
