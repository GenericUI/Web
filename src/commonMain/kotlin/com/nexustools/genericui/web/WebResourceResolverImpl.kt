package com.nexustools.genericui.web

import com.nexustools.genericui.io.ByteBuffer
import com.nexustools.genericui.io.Readable
import com.nexustools.genericui.io.ReadableByteBuffer
import com.nexustools.genericui.net.http.HttpRequest
import com.nexustools.genericui.res.DataUrlCodec
import com.nexustools.genericui.res.DrawableResource
import com.nexustools.genericui.res.Resources
import com.nexustools.genericui.resources.ResourceType
import com.nexustools.genericui.resources.DefaultResourceResolverNativeImpl
import com.nexustools.genericui.resources.ResourceID
import com.nexustools.genericui.style.css.StyleSheetRegistry

/**
 * Web (`:web`) implementation of [com.nexustools.genericui.resources.ResourceResolverNativeImpl]
 * that resolves preloaded [ResourceID] handles (`STYLE`, `DRAWABLE`, `RAW`, `FONT`, `XML`, `STRING`)
 * from [WebBrowserDomBridge] (`window.__GENERICUI_PRELOADED_RESOURCES__`) and [Resources.activeTable]
 * to `data:<mimeType>;base64,...` Data-URLs, falling back to relative Web asset paths when not preloaded.
 */
class WebResourceResolverImpl : DefaultResourceResolverNativeImpl() {

    override fun resolveUrl(resId: ResourceID): String =
        Companion.resolveResourceUrl(resId)

    override fun resolveBytes(resId: ResourceID): ByteArray? =
        getRawBytes(resId)

    override fun resolveString(resId: ResourceID): String? =
        getText(resId) ?: super.resolveString(resId)

    override suspend fun openResourceStream(resId: ResourceID): Readable? {
        resolveBytes(resId)?.let { return ReadableByteBuffer(ByteBuffer.wrap(it)) }
        resolveString(resId)?.let { return ReadableByteBuffer(ByteBuffer.wrap(it.encodeToByteArray())) }
        if (WebBrowserDomBridge.isBrowserEnvironment || WebBrowserDomBridge.browserFetchHook != null) {
            val url = resolveUrl(resId)
            if (url.isNotBlank() && !DataUrlCodec.isDataUrl(url)) {
                val response = WebHttpClientNativeImpl().execute(HttpRequest(url = url))
                if (response.isSuccessful) {
                    return response
                }
            }
        }
        return super.openResourceStream(resId)
    }

    /**
     * Asynchronously opens a [Readable] stream for a numeric resource ID [resId]
     * from [WebBrowserDomBridge] preloaded resources or [Resources.activeTable].
     */
    suspend fun openResourceStream(resId: Int): Readable? {
        val registered = Resources.activeTable.findResourceId(resId)
        if (registered != null) {
            return openResourceStream(registered)
        }
        val synthetic = ResourceID(
            id = resId,
            type = ResourceType.RAW,
            name = resId.toString(),
            relativePath = ""
        )
        return openResourceStream(synthetic)
    }

    /**
     * Resolves a [DrawableResource] for [id] from preloaded memory ([WebBrowserDomBridge]) or [Resources.activeTable].
     */
    fun getDrawable(id: ResourceID): DrawableResource? =
        Companion.resolveDrawable(id)

    /**
     * Resolves raw binary bytes for [id] from preloaded memory ([WebBrowserDomBridge]) or [Resources.activeTable].
     */
    fun getRawBytes(id: ResourceID): ByteArray? =
        Companion.resolveRawBytes(id)

    /**
     * Resolves UTF-8 text content for [id] from preloaded memory ([WebBrowserDomBridge]) or [Resources.activeTable].
     */
    fun getText(id: ResourceID): String? =
        Companion.resolveText(id)

    /**
     * Registers preloaded binary bytes for [id] into [WebBrowserDomBridge] so [resolveUrl],
     * [getRawBytes], [getDrawable], and [getText] resolve it immediately as a `data:` URL.
     */
    fun registerPreloadedResource(
        id: ResourceID,
        bytes: ByteArray,
        mimeType: String? = null,
        text: String? = null
    ) {
        Companion.registerPreloadedResource(id, bytes, mimeType, text)
    }

    /**
     * Registers preloaded UTF-8 text (such as CSS or SVG) for [id] into [WebBrowserDomBridge].
     */
    fun registerPreloadedResource(
        id: ResourceID,
        text: String,
        mimeType: String? = null
    ) {
        Companion.registerPreloadedResource(id, text.encodeToByteArray(), mimeType, text)
    }

    companion object {

        fun registerPreloadedResource(
            id: ResourceID,
            bytes: ByteArray,
            mimeType: String? = null,
            text: String? = null
        ) {
            val resolvedMime = mimeType?.trim()?.takeIf { it.isNotEmpty() } ?: inferMimeType(id, bytes)
            for (key in candidateKeysFor(id)) {
                WebBrowserDomBridge.registerPreloadedResource(key, resolvedMime, bytes, text)
            }
        }

        fun registerPreloadedResource(
            id: ResourceID,
            text: String,
            mimeType: String? = null
        ) {
            registerPreloadedResource(id, text.encodeToByteArray(), mimeType, text)
        }

        fun resolveResourceUrl(resId: ResourceID): String {
            val registered = findRegisteredResourceId(resId)
            val candidates = candidateKeysFor(registered, resId)

            for (key in candidates) {
                val preloadedDataUrl = WebBrowserDomBridge.getPreloadedResourceDataUrl(key)
                if (!preloadedDataUrl.isNullOrBlank() && DataUrlCodec.isDataUrl(preloadedDataUrl)) {
                    if (registered.type == ResourceType.STYLE && !preloadedDataUrl.startsWith("data:text/css", ignoreCase = true)) {
                        val decoded = DataUrlCodec.decodeBytes(preloadedDataUrl)
                        if (decoded != null) {
                            return DataUrlCodec.encodeBytes(decoded, "text/css")
                        }
                    }
                    return preloadedDataUrl
                }
                val preloadedBytes = WebBrowserDomBridge.getPreloadedResourceBytes(key)
                if (preloadedBytes != null) {
                    return DataUrlCodec.encodeBytes(preloadedBytes, inferMimeType(registered, preloadedBytes))
                }
                val preloadedText = WebBrowserDomBridge.getPreloadedResourceText(key)
                if (preloadedText != null) {
                    return DataUrlCodec.encodeUtf8Text(preloadedText, inferMimeType(registered))
                }
            }

            if (registered.type == ResourceType.STYLE) {
                val sheet = StyleSheetRegistry.resolveByResourceId(registered)
                    ?: Resources.getStyleSheet(registered)
                if (sheet != null) {
                    val cssSource = sheet.rawCss.ifBlank { sheet.styleSheet.sourceCssText }
                    if (cssSource.isNotBlank()) {
                        return DataUrlCodec.encodeUtf8Text(cssSource, "text/css")
                    }
                    for (pathKey in listOf(sheet.cssResourcePath, sheet.sourcePath)) {
                        if (pathKey.isNotBlank()) {
                            WebBrowserDomBridge.getPreloadedResourceDataUrl(pathKey)?.let { return it }
                            WebBrowserDomBridge.getPreloadedResourceText(pathKey)?.let {
                                return DataUrlCodec.encodeUtf8Text(it, "text/css")
                            }
                        }
                    }
                }
                Resources.activeTable.getRawBytes(registered)?.let { bytes ->
                    return DataUrlCodec.encodeBytes(bytes, "text/css")
                }
                Resources.activeTable.getRawText(registered)?.let { text ->
                    if (DataUrlCodec.isDataUrl(text)) return text
                    return DataUrlCodec.encodeUtf8Text(text, "text/css")
                }
            }

            if (registered.type == ResourceType.DRAWABLE) {
                Resources.activeTable.getDrawable(registered)?.let { drawable ->
                    val rawB64 = drawable.base64Data?.trim()
                    if (!rawB64.isNullOrEmpty()) {
                        if (DataUrlCodec.isDataUrl(rawB64)) return rawB64
                        val mime = DataUrlCodec.guessMimeType(
                            drawable.format.ifBlank { registered.relativePath.ifBlank { registered.name } },
                            "drawable"
                        )
                        return "data:$mime;base64,$rawB64"
                    }
                    val svgSrc = drawable.svgSource
                    if (!svgSrc.isNullOrBlank()) {
                        return DataUrlCodec.encodeUtf8Text(svgSrc, "image/svg+xml")
                    }
                }
            }

            Resources.activeTable.getRawBytes(registered)?.let { bytes ->
                return DataUrlCodec.encodeBytes(bytes, inferMimeType(registered, bytes))
            }

            Resources.activeTable.getRawText(registered)?.let { text ->
                if (DataUrlCodec.isDataUrl(text)) return text
                return DataUrlCodec.encodeUtf8Text(text, inferMimeType(registered))
            }

            return fallbackRelativeAssetPath(registered, resId)
        }

        fun resolveRawBytes(id: ResourceID): ByteArray? {
            val registered = findRegisteredResourceId(id)
            for (key in candidateKeysFor(registered, id)) {
                WebBrowserDomBridge.getPreloadedResourceBytes(key)?.let { return it }
                WebBrowserDomBridge.getPreloadedResourceDataUrl(key)?.let { url ->
                    DataUrlCodec.decodeBytes(url)?.let { return it }
                }
                WebBrowserDomBridge.getPreloadedResourceText(key)?.let { text ->
                    return text.encodeToByteArray()
                }
            }
            Resources.activeTable.getRawBytes(registered)?.let { return it }
            if (registered.type == ResourceType.STYLE) {
                val sheet = StyleSheetRegistry.resolveByResourceId(registered)
                    ?: Resources.getStyleSheet(registered)
                val css = sheet?.rawCss?.ifBlank { sheet.styleSheet.sourceCssText }.orEmpty()
                if (css.isNotEmpty()) {
                    return css.encodeToByteArray()
                }
            }
            return null
        }

        fun resolveText(id: ResourceID): String? {
            val registered = findRegisteredResourceId(id)
            for (key in candidateKeysFor(registered, id)) {
                WebBrowserDomBridge.getPreloadedResourceText(key)?.let { return it }
                WebBrowserDomBridge.getPreloadedResourceBytes(key)?.let { bytes ->
                    return runCatching { bytes.decodeToString() }.getOrNull()
                }
                WebBrowserDomBridge.getPreloadedResourceDataUrl(key)?.let { url ->
                    DataUrlCodec.decodeUtf8Text(url)?.let { return it }
                }
            }
            Resources.activeTable.getRawText(registered)?.let { return it }
            Resources.activeTable.getStringOrNull(registered)?.let { return it }
            if (registered.type == ResourceType.STYLE) {
                val sheet = StyleSheetRegistry.resolveByResourceId(registered)
                    ?: Resources.getStyleSheet(registered)
                val css = sheet?.rawCss?.ifBlank { sheet.styleSheet.sourceCssText }.orEmpty()
                if (css.isNotEmpty()) return css
            }
            if (registered.type == ResourceType.DRAWABLE) {
                Resources.activeTable.getDrawable(registered)?.svgSource?.let { return it }
            }
            Resources.activeTable.getRawBytes(registered)?.let { bytes ->
                return runCatching { bytes.decodeToString() }.getOrNull()
            }
            return null
        }

        fun resolveDrawable(id: ResourceID): DrawableResource? {
            val registered = findRegisteredResourceId(id)
            val existing = Resources.activeTable.getDrawable(registered)
            if (existing != null && (!existing.svgSource.isNullOrBlank() || !existing.base64Data.isNullOrBlank())) {
                return existing
            }
            for (key in candidateKeysFor(registered, id)) {
                val dataUrl = WebBrowserDomBridge.getPreloadedResourceDataUrl(key)
                val bytes = WebBrowserDomBridge.getPreloadedResourceBytes(key)
                val text = WebBrowserDomBridge.getPreloadedResourceText(key)
                if (dataUrl != null || bytes != null || text != null) {
                    val mime = (dataUrl?.let { DataUrlCodec.extractMimeType(it) })
                        ?: inferMimeType(registered, bytes)
                    val format = when {
                        mime.contains("svg", ignoreCase = true) -> "svg"
                        mime.contains("png", ignoreCase = true) -> "png"
                        mime.contains("jpeg", ignoreCase = true) || mime.contains("jpg", ignoreCase = true) -> "jpg"
                        mime.contains("webp", ignoreCase = true) -> "webp"
                        mime.contains("gif", ignoreCase = true) -> "gif"
                        mime.contains("icon", ignoreCase = true) -> "ico"
                        else -> existing?.format?.ifBlank { "png" } ?: "png"
                    }
                    val resolvedDataUrl = dataUrl
                        ?: (bytes?.let { DataUrlCodec.encodeBytes(it, mime) })
                        ?: (text?.let { DataUrlCodec.encodeUtf8Text(it, mime) })
                    val b64Payload = resolvedDataUrl?.substringAfter(";base64,", "")?.takeIf { it.isNotEmpty() }
                    val svgText = if (format == "svg") {
                        text ?: bytes?.decodeToString() ?: resolvedDataUrl?.let { DataUrlCodec.decodeUtf8Text(it) }
                    } else {
                        existing?.svgSource
                    }
                    return DrawableResource(
                        resId = registered,
                        format = format,
                        svgSource = svgText,
                        svgPathDataList = existing?.svgPathDataList ?: emptyList(),
                        viewBoxWidth = existing?.viewBoxWidth ?: 24f,
                        viewBoxHeight = existing?.viewBoxHeight ?: 24f,
                        base64Data = b64Payload ?: existing?.base64Data
                    )
                }
            }
            return existing
        }

        internal fun findRegisteredResourceId(resId: ResourceID): ResourceID =
            Resources.activeTable.findResourceId(resId.id)
                ?: Resources.activeTable.findResourceId(resId.type, resId.name)
                ?: resId

        internal fun candidateKeysFor(primary: ResourceID, secondary: ResourceID = primary): List<String> {
            val keys = linkedSetOf<String>()
            for (r in listOf(primary, secondary)) {
                keys.add(r.id.toString())
                keys.add("res://${r.id}")
                keys.add(r.toResourceUri())
                if (r.relativePath.isNotBlank()) {
                    keys.add(r.relativePath)
                    val cleanRel = r.relativePath.trim().replace('\\', '/').removePrefix("/")
                    keys.add(cleanRel)
                    keys.add("res/$cleanRel")
                    keys.add("gui/$cleanRel")
                    if (cleanRel.startsWith("res/") || cleanRel.startsWith("gui/")) {
                        keys.add(cleanRel.substring(4))
                    }
                    if (r.type == ResourceType.STYLE) {
                        val normalizedCss = WebStyleSheetManager.normalizeCssHref(cleanRel)
                        keys.add(normalizedCss)
                        keys.add("res/$normalizedCss")
                        keys.add("gui/$normalizedCss")
                    }
                }
                if (r.name.isNotBlank()) {
                    keys.add(r.name)
                    keys.add("${r.type.directoryName}/${r.name}")
                    keys.add("res/${r.type.directoryName}/${r.name}")
                    keys.add("gui/${r.type.directoryName}/${r.name}")
                    keys.add("@${r.type.directoryName}/${r.name}")
                    if (r.type == ResourceType.STYLE) {
                        keys.add("styles/${r.name}.css")
                        keys.add("res/styles/${r.name}.css")
                        keys.add("styles/${r.name}.scss")
                    }
                }
            }
            keys.add(fallbackRelativeAssetPath(primary, secondary))
            return keys.filter { it.isNotBlank() }
        }

        internal fun inferMimeType(resId: ResourceID, bytes: ByteArray? = null): String {
            if (resId.type == ResourceType.STYLE) {
                return "text/css"
            }
            if (bytes != null) {
                sniffImageOrTextMimeType(bytes)?.let { return it }
            }
            if (resId.type == ResourceType.DRAWABLE) {
                val drawable = Resources.activeTable.getDrawable(resId)
                if (drawable != null) {
                    if (!drawable.svgSource.isNullOrBlank() && drawable.base64Data.isNullOrBlank()) {
                        return "image/svg+xml"
                    }
                    if (drawable.format.isNotBlank()) {
                        return DataUrlCodec.guessMimeType(drawable.format, "drawable")
                    }
                }
            }
            val pathOrName = resId.relativePath.ifBlank { resId.name }
            return DataUrlCodec.guessMimeType(pathOrName, resId.type.directoryName)
        }

        private fun sniffImageOrTextMimeType(bytes: ByteArray): String? {
            if (bytes.size >= 8 &&
                bytes[0] == 0x89.toByte() &&
                bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() &&
                bytes[3] == 0x47.toByte()
            ) {
                return "image/png"
            }
            if (bytes.size >= 3 &&
                bytes[0] == 0xFF.toByte() &&
                bytes[1] == 0xD8.toByte() &&
                bytes[2] == 0xFF.toByte()
            ) {
                return "image/jpeg"
            }
            if (bytes.size >= 6 &&
                bytes[0] == 'G'.code.toByte() &&
                bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte()
            ) {
                return "image/gif"
            }
            if (bytes.size >= 12 &&
                bytes[0] == 'R'.code.toByte() &&
                bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() &&
                bytes[3] == 'F'.code.toByte() &&
                bytes[8] == 'W'.code.toByte() &&
                bytes[9] == 'E'.code.toByte() &&
                bytes[10] == 'B'.code.toByte() &&
                bytes[11] == 'P'.code.toByte()
            ) {
                return "image/webp"
            }
            val head = runCatching {
                bytes.copyOfRange(0, minOf(bytes.size, 128)).decodeToString().trimStart()
            }.getOrNull()
            if (head != null && (head.startsWith("<svg", ignoreCase = true) || head.contains("<svg", ignoreCase = true))) {
                return "image/svg+xml"
            }
            return null
        }

        internal fun fallbackRelativeAssetPath(registered: ResourceID, original: ResourceID = registered): String {
            val relPath = registered.relativePath.ifEmpty { original.relativePath }
                .trim()
                .replace('\\', '/')
                .removePrefix("/")
            if (registered.type == ResourceType.STYLE) {
                if (relPath.isNotBlank()) {
                    val normalized = WebStyleSheetManager.normalizeCssHref(relPath)
                    return if (normalized.startsWith("gui/") || normalized.startsWith("res/") || normalized.startsWith("http")) {
                        normalized
                    } else {
                        "res/$normalized"
                    }
                }
                return "res/styles/${registered.name.ifBlank { "style_${registered.id}" }}.css"
            }
            if (relPath.startsWith("gui/") || relPath.startsWith("res/") || relPath == WebNamespaceSpec.DEFAULT_NAMESPACE_XML_FILENAME) {
                return relPath
            }
            if (relPath.isNotBlank()) {
                return "res/$relPath"
            }
            val drawable = Resources.activeTable.getDrawable(registered)
            val ext = drawable?.format?.ifEmpty { "svg" } ?: when (registered.type) {
                ResourceType.DRAWABLE -> "svg"
                ResourceType.FONT -> "ttf"
                ResourceType.XML -> "xml"
                else -> "bin"
            }
            val fileName = "${registered.name}.$ext"
            val isBuiltInGui = registered.id == Resources.LOADING_IMAGE.id ||
                registered.id == Resources.GENERICUI.id ||
                registered.id == Resources.BOOTSTRAP.id ||
                registered.id == Resources.BOOTSTRAP_JS.id ||
                registered.id == Resources.BOOTSTRAP_ICONS.id
            val prefix = if (isBuiltInGui) "gui" else "res"
            return "$prefix/${registered.type.directoryName}/$fileName"
        }
    }
}
