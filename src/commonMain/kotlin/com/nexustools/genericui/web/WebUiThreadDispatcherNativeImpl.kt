package com.nexustools.genericui.web

import com.nexustools.genericui.threading.DefaultUiThreadDispatcherNativeImpl

/**
 * Web (`js` + `wasmJs`) implementation of [com.nexustools.genericui.threading.UiThreadDispatcherNativeImpl].
 *
 * Because browser JavaScript and WebAssembly DOM access run on a single cooperative event loop,
 * [isSplitThreadingEnabled] is `false` so both UI and Logic tasks execute directly on the browser
 * event loop without Web Worker message serialization overhead.
 */
class WebUiThreadDispatcherNativeImpl : DefaultUiThreadDispatcherNativeImpl(
    isSplitThreadingEnabled = false
)
