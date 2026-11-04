package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.net.DefaultWebSocketNativeImpl
import com.nexustools.genericui.nativeimpl.net.WebSocketNativeImpl

/**
 * Web (`:web`, `js` + `wasmJs`) implementation of [WebSocketNativeImpl] wrapping browser
 * `window.WebSocket` via [WebBrowserDomBridge] and falling back to [DefaultWebSocketNativeImpl]
 * in headless unit test environments.
 */
class WebWebSocketNativeImpl(
    val fallbackWebSocket: DefaultWebSocketNativeImpl = DefaultWebSocketNativeImpl()
) : WebSocketNativeImpl {

    private var activeBrowserDelegate: WebSocketNativeImpl? = null

    private var onOpenListener: (() -> Unit)? = null
    private var onTextMessageListener: ((String) -> Unit)? = null
    private var onBinaryMessageListener: ((ByteArray) -> Unit)? = null
    private var onErrorListener: ((Throwable) -> Unit)? = null
    private var onCloseListener: ((Int, String) -> Unit)? = null

    override val isConnected: Boolean
        get() = (activeBrowserDelegate ?: fallbackWebSocket).isConnected

    override val url: String?
        get() = (activeBrowserDelegate ?: fallbackWebSocket).url

    override var isDisposed: Boolean = false
        private set

    init {
        syncCallbacksTo(fallbackWebSocket)
    }

    override fun setOnOpen(listener: (() -> Unit)?) {
        onOpenListener = listener
        (activeBrowserDelegate ?: fallbackWebSocket).setOnOpen(listener)
    }

    override fun setOnTextMessage(listener: ((String) -> Unit)?) {
        onTextMessageListener = listener
        (activeBrowserDelegate ?: fallbackWebSocket).setOnTextMessage(listener)
    }

    override fun setOnBinaryMessage(listener: ((ByteArray) -> Unit)?) {
        onBinaryMessageListener = listener
        (activeBrowserDelegate ?: fallbackWebSocket).setOnBinaryMessage(listener)
    }

    override fun setOnError(listener: ((Throwable) -> Unit)?) {
        onErrorListener = listener
        (activeBrowserDelegate ?: fallbackWebSocket).setOnError(listener)
    }

    override fun setOnClose(listener: ((code: Int, reason: String) -> Unit)?) {
        onCloseListener = listener
        (activeBrowserDelegate ?: fallbackWebSocket).setOnClose(listener)
    }

    override suspend fun connect(url: String, headers: Map<String, String>): Boolean {
        if (isDisposed || url.isBlank()) return false
        val customHookSocket = WebBrowserDomBridge.browserWebSocketFactoryHook?.invoke()
        val target = customHookSocket ?: fallbackWebSocket
        activeBrowserDelegate = customHookSocket
        syncCallbacksTo(target)
        return target.connect(url, headers)
    }

    override suspend fun sendText(text: String): Boolean {
        if (isDisposed) return false
        return (activeBrowserDelegate ?: fallbackWebSocket).sendText(text)
    }

    override suspend fun sendBinary(bytes: ByteArray): Boolean {
        if (isDisposed) return false
        return (activeBrowserDelegate ?: fallbackWebSocket).sendBinary(bytes)
    }

    override suspend fun close(code: Int, reason: String) {
        (activeBrowserDelegate ?: fallbackWebSocket).close(code, reason)
    }

    override fun dispose() {
        isDisposed = true
        activeBrowserDelegate?.dispose()
        activeBrowserDelegate = null
        fallbackWebSocket.dispose()
    }

    fun simulateServerText(text: String) {
        fallbackWebSocket.simulateServerText(text)
    }

    fun simulateServerBinary(bytes: ByteArray) {
        fallbackWebSocket.simulateServerBinary(bytes)
    }

    fun simulateServerError(error: Throwable) {
        fallbackWebSocket.simulateServerError(error)
    }

    fun simulateServerClose(code: Int = 1000, reason: String = "Server Closure") {
        fallbackWebSocket.simulateServerClose(code, reason)
    }

    fun sentTextHistory(): List<String> = fallbackWebSocket.sentTextHistory()

    fun sentBinaryHistory(): List<ByteArray> = fallbackWebSocket.sentBinaryHistory()

    private fun syncCallbacksTo(target: WebSocketNativeImpl) {
        target.setOnOpen(onOpenListener)
        target.setOnTextMessage(onTextMessageListener)
        target.setOnBinaryMessage(onBinaryMessageListener)
        target.setOnError(onErrorListener)
        target.setOnClose(onCloseListener)
    }
}
