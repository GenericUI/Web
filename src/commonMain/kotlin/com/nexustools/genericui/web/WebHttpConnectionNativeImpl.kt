package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.net.DefaultHttpConnectionNativeImpl
import com.nexustools.genericui.net.http.HttpRequest
import com.nexustools.genericui.net.http.HttpResponse

/**
 * Web (`:web`, `js` + `wasmJs`) implementation of [com.nexustools.genericui.nativeimpl.net.HttpConnectionNativeImpl]
 * representing a duplex HTTP exchange over browser `window.fetch()`.
 */
class WebHttpConnectionNativeImpl(
    request: HttpRequest,
    response: HttpResponse
) : DefaultHttpConnectionNativeImpl(request, response) {
    val nativeApi: String = "Browser_window_fetch"
}
