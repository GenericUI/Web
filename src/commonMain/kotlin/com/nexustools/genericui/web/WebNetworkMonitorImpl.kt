package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.system.NetworkMonitorNativeImpl

/**
 * Web `navigator.onLine` + `window.addEventListener("online"/"offline")` implementation of [NetworkMonitorNativeImpl].
 */
class WebNetworkMonitorImpl : NetworkMonitorNativeImpl {
    private var manualOnlineOverride: Boolean? = null
    private var listener: ((Boolean) -> Unit)? = null
    private var unsubscribeBrowserListeners: (() -> Unit)? = null

    init {
        activeMonitors.add(this)
        unsubscribeBrowserListeners = WebBrowserDomBridge.installBrowserConnectivityListeners { online ->
            if (manualOnlineOverride == null) {
                listener?.invoke(online)
            }
        }
    }

    override fun isOnline(): Boolean =
        manualOnlineOverride ?: simulatedOnlineOverride ?: WebBrowserDomBridge.isBrowserOnline()

    override fun setOnConnectivityChanged(listener: ((Boolean) -> Unit)?) {
        this.listener = listener
    }

    /**
     * Overrides the online state of this monitor instance and notifies [listener].
     */
    fun setOnlineState(online: Boolean) {
        manualOnlineOverride = online
        listener?.invoke(online)
    }

    /**
     * Clears any per-instance online state override so [isOnline] delegates to [WebBrowserDomBridge.isBrowserOnline].
     */
    fun clearOnlineStateOverride() {
        manualOnlineOverride = null
    }

    override fun dispose() {
        unsubscribeBrowserListeners?.invoke()
        unsubscribeBrowserListeners = null
        listener = null
        activeMonitors.remove(this)
    }

    companion object {
        private val activeMonitors = mutableSetOf<WebNetworkMonitorImpl>()
        private var simulatedOnlineOverride: Boolean? = null

        /**
         * Simulates a browser-wide online/offline transition (`window` `"online"` / `"offline"` event)
         * and notifies all active [WebNetworkMonitorImpl] instances.
         */
        fun simulateBrowserConnectivity(online: Boolean) {
            simulatedOnlineOverride = online
            WebBrowserDomBridge.dispatchBrowserConnectivityChanged(online)
        }

        /**
         * Clears any simulated browser connectivity override and restores default online state (`true`).
         */
        fun resetSimulatedConnectivity() {
            simulatedOnlineOverride = null
            WebBrowserDomBridge.dispatchBrowserConnectivityChanged(true)
        }
    }
}
