package com.nexustools.genericui.web

import com.nexustools.genericui.image.*
import com.nexustools.genericui.image.codec.LibJpegCodec
import com.nexustools.genericui.image.codec.LibPngCodec
import com.nexustools.genericui.image.codec.LibWebpCodec
import com.nexustools.genericui.nativeimpl.image.ImageCodecNativeImpl
import com.nexustools.genericui.res.DataUrlCodec
import com.nexustools.genericui.util.WeakReference

/**
 * Web browser implementation of [ImageCodecNativeImpl] backed by an invisible `HTMLImageElement`
 * (`<img style="display: none" aria-hidden="true">`).
 *
 * - Sets `src` on the invisible `HTMLImageElement` and watches for `onload` / `onerror` while also
 *   checking `img.complete` so `:web` properly simulates and executes asynchronous image loading.
 * - Sets [usesBrowserManagedImageLoading] to `true` and performs **no** custom download caching of its own,
 *   relying 100% on the web browser's native HTTP/image cache and storing only [WeakReference]s to active objects.
 */
class WebBrowserImageCodecImpl : ImageCodecNativeImpl {
    override val codecBackendName: String = "web-browser-html-image-element"

    override val usesBrowserManagedImageLoading: Boolean = true

    override var isDisposed: Boolean = false
        private set

    private var lastInvisibleImageElementRef: WeakReference<WebDomElement>? = null

    /**
     * Returns the most recently created invisible `HTMLImageElement` ([WebDomElement]) if still referenced.
     */
    val lastInvisibleImageElement: WebDomElement?
        get() = lastInvisibleImageElementRef?.get()

    override fun probeMetadata(source: ImageSource): ImageMetadata? {
        source.memoryBuffer?.let { buf ->
            return ImageMetadata(
                width = buf.width,
                height = buf.height,
                format = ImageFormat.RAW_RGBA,
                pixelFormat = buf.pixelFormat,
                colorSpace = buf.colorSpace
            )
        }
        val effectiveUri = source.uriOrPath ?: source.resId?.toResourceUri()
        val bytes = resolveImmediateBytesWithoutCaching(source)
        if (bytes != null && bytes.isNotEmpty()) {
            val detected = ImageFormat.detectFromHeader(bytes)
            when (detected) {
                ImageFormat.JPEG -> return LibJpegCodec.probeJpegMetadata(bytes)
                ImageFormat.PNG -> return LibPngCodec.probePngMetadata(bytes)
                ImageFormat.WEBP -> return LibWebpCodec.probeWebpMetadata(bytes)
                else -> {}
            }
            if (WebBrowserDomBridge.isBrowserEnvironment) {
                WebBrowserDomBridge.decodePreloadedBrowserImage(
                    uriOrKey = effectiveUri,
                    encodedBytes = bytes,
                    targetWidth = null,
                    targetHeight = null,
                    preserveAspectRatio = true
                )?.let { preloaded ->
                    return ImageMetadata(
                        width = preloaded.width,
                        height = preloaded.height,
                        format = detected,
                        pixelFormat = preloaded.pixelFormat,
                        colorSpace = preloaded.colorSpace
                    )
                }
            }
        } else if (WebBrowserDomBridge.isBrowserEnvironment) {
            WebBrowserDomBridge.decodePreloadedBrowserImage(
                uriOrKey = effectiveUri,
                encodedBytes = source.encodedBytes,
                targetWidth = null,
                targetHeight = null,
                preserveAspectRatio = true
            )?.let { preloaded ->
                return ImageMetadata(
                    width = preloaded.width,
                    height = preloaded.height,
                    format = source.formatHint,
                    pixelFormat = preloaded.pixelFormat,
                    colorSpace = preloaded.colorSpace
                )
            }
        }
        return null
    }

    override fun decode(source: ImageSource, options: ImageDecodeOptions): ImageBuffer? {
        source.memoryBuffer?.let { return it.copy() }
        val effectiveUri = source.uriOrPath ?: source.resId?.toResourceUri()
        if (WebBrowserDomBridge.isBrowserEnvironment) {
            WebBrowserDomBridge.decodePreloadedBrowserImage(
                uriOrKey = effectiveUri,
                encodedBytes = source.encodedBytes,
                targetWidth = options.targetWidth,
                targetHeight = options.targetHeight,
                preserveAspectRatio = options.preserveAspectRatio
            )?.let { return it }
            val relPath = source.resId?.relativePath?.takeIf { it.isNotBlank() }
            if (relPath != null) {
                WebBrowserDomBridge.decodePreloadedBrowserImage(
                    uriOrKey = relPath,
                    encodedBytes = source.encodedBytes,
                    targetWidth = options.targetWidth,
                    targetHeight = options.targetHeight,
                    preserveAspectRatio = options.preserveAspectRatio
                )?.let { return it }
            }
        }
        val bytes = resolveImmediateBytesWithoutCaching(source) ?: return null
        if (bytes.isEmpty()) return null
        if (WebBrowserDomBridge.isBrowserEnvironment && source.encodedBytes == null) {
            WebBrowserDomBridge.decodePreloadedBrowserImage(
                uriOrKey = effectiveUri,
                encodedBytes = bytes,
                targetWidth = options.targetWidth,
                targetHeight = options.targetHeight,
                preserveAspectRatio = options.preserveAspectRatio
            )?.let { return it }
        }
        return when (ImageFormat.detectFromHeader(bytes)) {
            ImageFormat.JPEG -> LibJpegCodec.decode(bytes)
            ImageFormat.PNG -> LibPngCodec.decode(bytes)
            ImageFormat.WEBP -> LibWebpCodec.decode(bytes)
            else -> null
        }
    }

    override fun decodeAsync(
        source: ImageSource,
        options: ImageDecodeOptions,
        onLoaded: (ImageBuffer) -> Unit,
        onError: (Throwable) -> Unit
    ): Boolean {
        source.memoryBuffer?.let { existing ->
            onLoaded(existing.copy())
            return true
        }

        // Create an invisible HTMLImageElement, set src, and watch for onLoad / check if already loaded
        val invisibleImg = WebDomElement("img")
        invisibleImg.style["display"] = "none"
        invisibleImg.style["position"] = "fixed"
        invisibleImg.style["width"] = "0px"
        invisibleImg.style["height"] = "0px"
        invisibleImg.style["pointer-events"] = "none"
        invisibleImg.setAttribute("aria-hidden", "true")
        invisibleImg.setAttribute("decoding", "async")
        lastInvisibleImageElementRef = WeakReference(invisibleImg)

        val resolvedSrc = resolveSourceUrlForHtmlImage(source)
        if (resolvedSrc.isNullOrBlank()) {
            invisibleImg.setAttribute("data-error", "true")
            onError(IllegalArgumentException("Empty image source URL"))
            return true
        }

        // 1. Live browser environment: bind to real DOM HTMLImageElement onload / complete
        val handledInBrowser = WebBrowserDomBridge.bindInvisibleHtmlImageLoader(
            imgElement = invisibleImg,
            src = resolvedSrc,
            onLoaded = { natW, natH ->
                val (fallbackW, fallbackH) = ImageLoader.resolveTargetDimensions(
                    srcWidth = natW.coerceAtLeast(1),
                    srcHeight = natH.coerceAtLeast(1),
                    options = options
                )
                val decoded = decode(source, options) ?: ImageBuffer(
                    width = fallbackW,
                    height = fallbackH,
                    pixelFormat = options.preferredPixelFormat
                )
                onLoaded(decoded)
            },
            onError = { message ->
                onError(IllegalStateException(message))
            }
        )
        if (handledInBrowser) {
            return true
        }

        // 2. Simulated / headless web environment: watch load/error on invisibleImg and check if loaded
        var settled = false
        val completeLoad = {
            if (!settled) {
                settled = true
                invisibleImg.setAttribute("data-complete", "true")
                val decoded = decode(source, options) ?: ImageBuffer(
                    width = options.targetWidth ?: 64,
                    height = options.targetHeight ?: 64,
                    pixelFormat = options.preferredPixelFormat
                )
                invisibleImg.setAttribute("data-natural-width", decoded.width.toString())
                invisibleImg.setAttribute("data-natural-height", decoded.height.toString())
                onLoaded(decoded)
            }
        }
        val failLoad = {
            if (!settled) {
                settled = true
                invisibleImg.setAttribute("data-error", "true")
                onError(IllegalStateException("Failed to load image: $resolvedSrc"))
            }
        }

        invisibleImg.addEventListener("load") { completeLoad() }
        invisibleImg.addEventListener("error") { failLoad() }
        invisibleImg.setAttribute("src", resolvedSrc)

        // Check if already marked complete or if immediate simulated completion should run
        if (invisibleImg.getAttribute("data-complete") == "true") {
            completeLoad()
        } else if (isInvalidSimulatedUrl(resolvedSrc)) {
            if (WebBrowserDomBridge.deferSimulatedImageLoads) {
                pendingInvisibleImageRefs.add(WeakReference(invisibleImg))
            } else {
                invisibleImg.dispatchFromBrowser("error", null)
            }
        } else if (WebBrowserDomBridge.deferSimulatedImageLoads) {
            pendingInvisibleImageRefs.add(WeakReference(invisibleImg))
        } else {
            invisibleImg.dispatchFromBrowser("load", null)
        }
        return true
    }

    override fun decodeAnimated(source: ImageSource, options: ImageDecodeOptions): AnimatedImage? {
        val frame = decode(source, options) ?: return null
        return AnimatedImage(
            width = frame.width,
            height = frame.height,
            frames = listOf(ImageFrame(0, frame, durationMs = 100))
        )
    }

    override fun encode(buffer: ImageBuffer, options: ImageEncodeOptions): ByteArray? {
        return when (options.format) {
            ImageFormat.JPEG -> LibJpegCodec.encode(buffer, options.quality)
            ImageFormat.PNG -> LibPngCodec.encode(buffer, options.compressionLevel)
            ImageFormat.RAW_RGBA -> buffer.toRgba8888().bytes
            else -> null
        }
    }

    override fun dispose() {
        isDisposed = true
        lastInvisibleImageElementRef?.clear()
        lastInvisibleImageElementRef = null
    }

    private fun resolveSourceUrlForHtmlImage(source: ImageSource): String? {
        source.encodedBytes?.let { bytes ->
            val fmt = ImageFormat.detectFromHeader(bytes)
            val mime = when (fmt) {
                ImageFormat.JPEG -> "image/jpeg"
                ImageFormat.WEBP -> "image/webp"
                ImageFormat.GIF -> "image/gif"
                ImageFormat.SVG -> "image/svg+xml"
                else -> "image/png"
            }
            return DataUrlCodec.encodeBytes(bytes, mime)
        }
        val uri = (source.uriOrPath ?: source.resId?.toResourceUri())?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        WebBrowserDomBridge.getPreloadedResourceDataUrl(uri)?.let { return it }
        source.resId?.relativePath?.takeIf { it.isNotBlank() }?.let { rel ->
            WebBrowserDomBridge.getPreloadedResourceDataUrl(rel)?.let { return it }
        }
        source.resId?.let { id ->
            com.nexustools.genericui.resources.ResourceResolver.getBytes(id)?.takeIf { it.isNotEmpty() }?.let { bytes ->
                val fmt = ImageFormat.detectFromHeader(bytes)
                val mime = when (fmt) {
                    ImageFormat.JPEG -> "image/jpeg"
                    ImageFormat.WEBP -> "image/webp"
                    ImageFormat.GIF -> "image/gif"
                    ImageFormat.SVG -> "image/svg+xml"
                    else -> "image/png"
                }
                return DataUrlCodec.encodeBytes(bytes, mime)
            }
        }
        return uri
    }

    private fun resolveImmediateBytesWithoutCaching(source: ImageSource): ByteArray? {
        source.encodedBytes?.let { return it }
        val uri = (source.uriOrPath ?: source.resId?.toResourceUri())?.trim()?.takeIf { it.isNotEmpty() }
        if (uri != null) {
            if (DataUrlCodec.isDataUrl(uri)) {
                return DataUrlCodec.decodeBytes(uri)
            }
            WebBrowserDomBridge.getPreloadedResourceBytes(uri)?.let { return it }
        }
        source.resId?.relativePath?.takeIf { it.isNotBlank() }?.let { rel ->
            WebBrowserDomBridge.getPreloadedResourceBytes(rel)?.let { return it }
        }
        source.resId?.let { id ->
            com.nexustools.genericui.resources.ResourceResolver.getBytes(id)?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        if (uri != null) {
            com.nexustools.genericui.res.Resources.activeTable.findResourceId(
                com.nexustools.genericui.resources.ResourceType.DRAWABLE,
                uri
            )?.let { id ->
                com.nexustools.genericui.resources.ResourceResolver.getBytes(id)?.takeIf { it.isNotEmpty() }?.let { return it }
            }
        }
        return null
    }

    private fun isInvalidSimulatedUrl(url: String): Boolean {
        val lower = url.trim().lowercase()
        return lower.startsWith("error://") ||
            lower.startsWith("invalid://") ||
            lower.endsWith(".invalid")
    }
}
