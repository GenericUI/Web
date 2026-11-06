package com.nexustools.genericui.web

import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.enums.ui.ObjectFit
import com.nexustools.genericui.enums.ui.ThemeMode
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.image.ImageDecodeOptions
import com.nexustools.genericui.image.ImageLoader
import com.nexustools.genericui.image.ImageSource
import com.nexustools.genericui.image.codec.LibPngCodec
import com.nexustools.genericui.nativeimpl.content.ImageNativeImpl
import com.nexustools.genericui.res.DataUrlCodec
import com.nexustools.genericui.style.color.ImageTintMode
import com.nexustools.genericui.style.color.RgbaColor
import com.nexustools.genericui.style.css.CssFilter
import kotlinx.coroutines.Job

/**
 * Web implementation of [ImageNativeImpl] rendered as `<img>`.
 *
 * Uses [ImageLoader] (which creates an invisible `HTMLImageElement`, sets `src`, and watches `onload` /
 * checks `complete`) so placeholder loading images, `isLoading`, and `onLoad` / `onError` work
 * consistently in both live browsers and simulated `:web` test environments without custom download caching.
 */
class WebImageImpl : WebBaseNativeImpl("img"), ImageNativeImpl<WebDomElement> {
    private var sourceUrl: String? = null
    private var placeholderUrl: String? = ImageLoader.DEFAULT_PLACEHOLDER_URI
    private var objectFit: ObjectFit = ObjectFit.CONTAIN
    private var antialiasing: Boolean = true
    private var loadingState: Boolean = false
    private var activeLoadGeneration: Int = 0
    private var activeLoadJob: Job? = null
    private var retainedBuffer: ImageBuffer? = null
    private var tintColor: RgbaColor? = null
    private var tintMode: ImageTintMode = ImageTintMode.NONE
    private var cssFilter: CssFilter? = null
    private var onLoadCallback: (() -> Unit)? = null
    private var onErrorCallback: (() -> Unit)? = null

    init {
        nativeWidget.setAttribute("decoding", "async")
        syncAntialiasingStyle()
        nativeWidget.addEventListener("load") {
            if (loadingState) {
                loadingState = false
                nativeWidget.removeAttribute("data-genericui-loading")
                nativeWidget.removeAttribute("data-genericui-placeholder-active")
            }
            onLoadCallback?.invoke()
        }
        nativeWidget.addEventListener("error") {
            loadingState = false
            nativeWidget.removeAttribute("data-genericui-loading")
            onErrorCallback?.invoke()
        }
    }

    override fun setSourceUrl(url: String?) {
        val trimmed = url?.trim().orEmpty()
        if (trimmed.startsWith("memory://", ignoreCase = true)) {
            ImageLoader.resolveMemoryBuffer(trimmed)?.let {
                setImageBuffer(it)
                return
            }
        }

        activeLoadJob?.cancel()
        activeLoadJob = null
        retainedBuffer = null

        val resolved = resolveResourceUrl(url)
        sourceUrl = resolved
        val gen = ++activeLoadGeneration
        if (resolved.isNullOrBlank()) {
            loadingState = false
            nativeWidget.removeAttribute("data-genericui-loading")
            nativeWidget.removeAttribute("data-genericui-target-src")
            val resolvedPlaceholder = resolveResourceUrl(placeholderUrl)
            if (!resolvedPlaceholder.isNullOrBlank()) {
                nativeWidget.setAttribute("src", resolvedPlaceholder)
                nativeWidget.setAttribute("data-genericui-placeholder-active", "true")
            } else {
                nativeWidget.removeAttribute("src")
                nativeWidget.removeAttribute("data-genericui-placeholder-active")
            }
            return
        }

        loadingState = true
        nativeWidget.setAttribute("data-genericui-loading", "true")
        nativeWidget.setAttribute("data-genericui-target-src", resolved)

        val resolvedPlaceholder = resolveResourceUrl(placeholderUrl)
        if (!resolvedPlaceholder.isNullOrBlank()) {
            nativeWidget.setAttribute("src", resolvedPlaceholder)
            nativeWidget.setAttribute("data-genericui-placeholder-active", "true")
        } else {
            nativeWidget.setAttribute("src", resolved)
            nativeWidget.removeAttribute("data-genericui-placeholder-active")
        }

        activeLoadJob = ImageLoader.launchLoad(
            source = ImageSource.fromUrl(resolved),
            options = ImageDecodeOptions(antialiasing = antialiasing),
            onLoaded = {
                if (gen == activeLoadGeneration) {
                    val wasLoading = loadingState
                    loadingState = false
                    nativeWidget.removeAttribute("data-genericui-loading")
                    nativeWidget.removeAttribute("data-genericui-placeholder-active")
                    nativeWidget.setAttribute("src", resolved)
                    if (wasLoading && !WebBrowserDomBridge.isBrowserEnvironment) {
                        onLoadCallback?.invoke()
                    }
                }
            },
            onError = {
                if (gen == activeLoadGeneration) {
                    val wasLoading = loadingState
                    loadingState = false
                    nativeWidget.removeAttribute("data-genericui-loading")
                    if (wasLoading && !WebBrowserDomBridge.isBrowserEnvironment) {
                        onErrorCallback?.invoke()
                    }
                }
            }
        )
    }

    override fun setImageBuffer(buffer: ImageBuffer?) {
        activeLoadGeneration++
        activeLoadJob?.cancel()
        activeLoadJob = null
        retainedBuffer = buffer
        if (buffer == null) {
            loadingState = false
            sourceUrl = ""
            nativeWidget.removeAttribute("src")
            nativeWidget.removeAttribute("data-genericui-loading")
            nativeWidget.removeAttribute("data-genericui-placeholder-active")
            nativeWidget.removeAttribute("data-load-state")
            nativeWidget.removeAttribute("data-natural-width")
            nativeWidget.removeAttribute("data-natural-height")
            return
        }

        val memoryUri = "memory://imagebuffer/${buffer.width}x${buffer.height}@${buffer.hashCode()}"
        ImageLoader.registerMemoryBuffer(memoryUri, buffer)
        sourceUrl = memoryUri

        loadingState = false
        nativeWidget.removeAttribute("data-genericui-loading")
        nativeWidget.removeAttribute("data-genericui-placeholder-active")
        nativeWidget.setAttribute("data-load-state", "loaded")

        val dataUrl = buffer.toDataUrl("image/png")
            ?: WebBrowserDomBridge.surfacePeerToDataUrl(buffer.nativeSurfacePeer, "image/png")
            ?: DataUrlCodec.encodeBytes(LibPngCodec.encode(buffer), "image/png")

        nativeWidget.setAttribute("src", dataUrl)
        nativeWidget.setAttribute("data-natural-width", buffer.width.toString())
        nativeWidget.setAttribute("data-natural-height", buffer.height.toString())
        onLoadCallback?.invoke()
    }

    override fun getSourceUrl(): String? = sourceUrl

    override fun setPlaceholderUrl(url: String?) {
        placeholderUrl = resolveResourceUrl(url)
        if (!placeholderUrl.isNullOrBlank()) {
            nativeWidget.setAttribute("data-genericui-placeholder", placeholderUrl!!)
            if ((sourceUrl.isNullOrBlank() && retainedBuffer == null) || loadingState) {
                nativeWidget.setAttribute("src", placeholderUrl!!)
                nativeWidget.setAttribute("data-genericui-placeholder-active", "true")
            }
        } else {
            nativeWidget.removeAttribute("data-genericui-placeholder")
            nativeWidget.removeAttribute("data-genericui-placeholder-active")
            if (retainedBuffer == null && !sourceUrl.isNullOrBlank()) {
                nativeWidget.setAttribute("src", sourceUrl!!)
            }
        }
    }

    override fun getPlaceholderUrl(): String? = placeholderUrl

    override fun setAntialiasing(enabled: Boolean) {
        antialiasing = enabled
        syncAntialiasingStyle()
    }

    override fun isAntialiasing(): Boolean = antialiasing

    override fun isLoading(): Boolean = loadingState

    private fun syncAntialiasingStyle() {
        nativeWidget.style["image-rendering"] = if (antialiasing) "auto" else "pixelated"
    }

    private fun resolveResourceUrl(url: String?): String? {
        if (url == null) return null
        val trimmed = url.trim()
        if (trimmed.equals("res://drawable/loading", ignoreCase = true) ||
            trimmed.equals("drawable/loading.svg", ignoreCase = true) ||
            trimmed.equals("assets/drawable/loading.svg", ignoreCase = true) ||
            trimmed.equals("loading.svg", ignoreCase = true)
        ) {
            return WebBrowserDomBridge.getPreloadedResourceDataUrl(trimmed)
                ?: com.nexustools.genericui.res.Resources.DEFAULT_LOADING_SVG_DATA_URL
        }
        return if (trimmed.startsWith("res://", ignoreCase = true)) {
            WebBrowserDomBridge.getPreloadedResourceDataUrl(trimmed) ?: trimmed
        } else {
            trimmed
        }
    }

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

    override fun setTintColor(color: RgbaColor?) {
        tintColor = color
        syncVisualFilterAndTint()
    }

    override fun getTintColor(): RgbaColor? = tintColor

    override fun setTintMode(mode: ImageTintMode) {
        tintMode = mode
        syncVisualFilterAndTint()
    }

    override fun getTintMode(): ImageTintMode = tintMode

    override fun setCssFilter(filter: CssFilter?) {
        cssFilter = filter
        syncVisualFilterAndTint()
    }

    override fun getCssFilter(): CssFilter? = cssFilter

    private fun syncVisualFilterAndTint() {
        val explicitFilter = cssFilter?.takeIf { !it.isNone }
        when (tintMode) {
            ImageTintMode.MONOCOLOR_AUTO_THEME -> {
                val isDark = Application.current?.getResolvedTheme() == ThemeMode.DARK
                val autoFilterCss = if (isDark) "brightness(0) invert(1)" else "brightness(0)"
                nativeWidget.style["filter"] = explicitFilter?.toCssString() ?: autoFilterCss
                nativeWidget.setAttribute("data-genericui-tint-mode", tintMode.name)
                tintColor?.let { nativeWidget.setAttribute("data-genericui-tint-color", it.toHex()) }
                    ?: nativeWidget.removeAttribute("data-genericui-tint-color")
            }
            ImageTintMode.SRC_IN, ImageTintMode.MULTIPLY -> {
                nativeWidget.setAttribute("data-genericui-tint-mode", tintMode.name)
                tintColor?.let { nativeWidget.setAttribute("data-genericui-tint-color", it.toHex()) }
                    ?: nativeWidget.removeAttribute("data-genericui-tint-color")
                if (explicitFilter != null) {
                    nativeWidget.style["filter"] = explicitFilter.toCssString()
                }
            }
            ImageTintMode.NONE -> {
                nativeWidget.removeAttribute("data-genericui-tint-mode")
                nativeWidget.removeAttribute("data-genericui-tint-color")
                if (explicitFilter != null) {
                    nativeWidget.style["filter"] = explicitFilter.toCssString()
                } else {
                    nativeWidget.style.remove("filter")
                }
            }
        }
    }

    override fun setOnLoad(listener: (() -> Unit)?) {
        onLoadCallback = listener
    }

    override fun setOnError(listener: (() -> Unit)?) {
        onErrorCallback = listener
    }
}
