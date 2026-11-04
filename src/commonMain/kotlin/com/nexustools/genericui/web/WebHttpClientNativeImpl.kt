package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.net.DefaultHttpClientNativeImpl
import com.nexustools.genericui.net.http.HttpRequest
import com.nexustools.genericui.net.http.HttpResponse

/**
 * Web (`:web`, `js` + `wasmJs`) implementation of [com.nexustools.genericui.nativeimpl.net.HttpClientNativeImpl]
 * wrapping browser `window.fetch()` Promises via [WebBrowserDomBridge] and falling back to
 * [DefaultHttpClientNativeImpl] route handlers in headless unit test environments.
 */
class WebHttpClientNativeImpl(
    private val defaultHandler: (suspend (HttpRequest) -> HttpResponse)? = null
) : DefaultHttpClientNativeImpl() {

    val nativeApi: String = "Browser_window_fetch"

    private val capturedRequests = ArrayList<HttpRequest>()

    var requestCount: Int = 0
        private set

    fun requestHistory(): List<HttpRequest> =
        capturedRequests.toList()

    override suspend fun execute(request: HttpRequest): WebHttpResponseNativeImpl {
        requestCount += 1
        capturedRequests.add(request)
        if (isDisposed) {
            return WebHttpResponseNativeImpl(
                statusCode = 499,
                statusText = "Client Closed Request",
                url = request.url,
                byteOrder = request.byteOrder
            )
        }
        val raw = WebBrowserDomBridge.browserFetchHook?.invoke(request)
            ?: findRouteHandler(request.url)?.invoke(request)
            ?: defaultHandler?.invoke(request)
            ?: WebBrowserDomBridge.fetchHttp(request)
            ?: super.execute(request)
        if (raw is WebHttpResponseNativeImpl) return raw
        return WebHttpResponseNativeImpl(
            statusCode = raw.statusCode,
            statusText = raw.statusText,
            headers = raw.headers,
            initialBodyBytes = raw.bodyBytes,
            url = raw.url.ifEmpty { request.url },
            byteOrder = raw.byteOrder
        )
    }

    override suspend fun openConnection(request: HttpRequest): WebHttpConnectionNativeImpl {
        val nativeRequest = request as? WebHttpRequestNativeImpl ?: WebHttpRequestNativeImpl(
            url = request.url,
            method = request.method,
            headers = request.headers,
            initialBodyBytes = request.bodyBytes,
            timeoutMs = request.timeoutMs,
            followRedirects = request.followRedirects,
            byteOrder = request.byteOrder
        )
        val response = execute(nativeRequest)
        return WebHttpConnectionNativeImpl(nativeRequest, response)
    }

    override fun dispose() {
        capturedRequests.clear()
        super.dispose()
    }
}
