package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.system.DefaultUiTimerNativeImpl
import com.nexustools.genericui.nativeimpl.system.UiTimerNativeImpl

/**
 * Web (`:web`) implementation of [UiTimerNativeImpl] backed by `window.setTimeout` / `window.setInterval` /
 * `window.clearTimeout` / `window.clearInterval` via [WebBrowserDomBridge], with automatic fallback to
 * [DefaultUiTimerNativeImpl] for deterministic headless unit tests.
 */
class WebUiTimerImpl : DefaultUiTimerNativeImpl() {

    private class BrowserTimerHandle(
        val browserTimerId: Int,
        val isInterval: Boolean
    )

    private val browserHandles = linkedMapOf<Int, BrowserTimerHandle>()

    override fun setTimeout(delayMs: Int, callback: () -> Unit): Int {
        if (isDisposed) return -1
        var timerId = -1
        timerId = super.setTimeout(delayMs) {
            browserHandles.remove(timerId)?.let { handle ->
                WebBrowserDomBridge.clearTimeoutId(handle.browserTimerId)
            }
            callback()
        }
        if (timerId > 0 && WebBrowserDomBridge.isBrowserEnvironment) {
            val browserId = WebBrowserDomBridge.setTimeoutId(delayMs) {
                browserHandles.remove(timerId)
                if (isTimerActive(timerId)) {
                    super.clearTimer(timerId)
                    callback()
                }
            }
            if (browserId >= 0) {
                browserHandles[timerId] = BrowserTimerHandle(browserId, isInterval = false)
            }
        }
        return timerId
    }

    override fun setInterval(intervalMs: Int, callback: () -> Unit): Int {
        if (isDisposed) return -1
        val timerId = super.setInterval(intervalMs, callback)
        if (timerId > 0 && WebBrowserDomBridge.isBrowserEnvironment) {
            val browserId = WebBrowserDomBridge.setIntervalId(intervalMs) {
                if (isTimerActive(timerId)) {
                    callback()
                }
            }
            if (browserId >= 0) {
                browserHandles[timerId] = BrowserTimerHandle(browserId, isInterval = true)
            }
        }
        return timerId
    }

    override fun clearTimer(timerId: Int) {
        browserHandles.remove(timerId)?.let { handle ->
            if (handle.isInterval) {
                WebBrowserDomBridge.clearIntervalId(handle.browserTimerId)
            } else {
                WebBrowserDomBridge.clearTimeoutId(handle.browserTimerId)
            }
        }
        super.clearTimer(timerId)
    }

    override fun clearAllTimers() {
        val snapshot = browserHandles.values.toList()
        browserHandles.clear()
        for (handle in snapshot) {
            if (handle.isInterval) {
                WebBrowserDomBridge.clearIntervalId(handle.browserTimerId)
            } else {
                WebBrowserDomBridge.clearTimeoutId(handle.browserTimerId)
            }
        }
        super.clearAllTimers()
    }
}
