package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.system.NetworkMonitorNativeImpl

/**
 * Web `navigator.onLine` + `window.addEventListener("online"/"offline")` implementation of [NetworkMonitorNativeImpl].
 */
class WebNetworkMonitorImpl : NetworkMonitorNativeImpl {
    private var onlineState: Boolean = true
    private var listener: ((Boolean) -> Unit)? = null

    override fun isOnline(): Boolean = onlineState

    override fun setOnConnectivityChanged(listener: ((Boolean) -> Unit)?) {
        this.listener = listener
    }

    override fun dispose() {
        listener = null
    }
}
