package com.nexustools.genericui.web

import com.nexustools.genericui.enums.ui.ObjectFit
import com.nexustools.genericui.nativeimpl.content.ImageNativeImpl

/**
 * Web implementation of [ImageNativeImpl] rendered as `<img>`.
 */
class WebImageImpl : WebBaseNativeImpl("img"), ImageNativeImpl<WebDomElement> {
    private var sourceUrl: String? = null
    private var objectFit: ObjectFit = ObjectFit.CONTAIN
    private var onLoadCallback: (() -> Unit)? = null
    private var onErrorCallback: (() -> Unit)? = null

    init {
        nativeWidget.addEventListener("load") { onLoadCallback?.invoke() }
        nativeWidget.addEventListener("error") { onErrorCallback?.invoke() }
    }

    override fun setSourceUrl(url: String?) {
        sourceUrl = url
        if (url != null) {
            nativeWidget.setAttribute("src", url)
        } else {
            nativeWidget.removeAttribute("src")
        }
    }

    override fun getSourceUrl(): String? = sourceUrl

    override fun setFallbackText(fallback: String?) {
        if (fallback != null) {
            nativeWidget.setAttribute("alt", fallback)
        } else {
            nativeWidget.removeAttribute("alt")
        }
    }

    override fun setObjectFit(fit: ObjectFit) {
        objectFit = fit
        nativeWidget.style["object-fit"] = fit.name.lowercase()
    }

    override fun getObjectFit(): ObjectFit = objectFit

    override fun setOnLoad(listener: (() -> Unit)?) {
        onLoadCallback = listener
    }

    override fun setOnError(listener: (() -> Unit)?) {
        onErrorCallback = listener
    }
}
