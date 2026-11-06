package com.nexustools.genericui.web

import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.image.ImageColorSpace
import com.nexustools.genericui.image.PixelFormat
import com.nexustools.genericui.nativeimpl.net.WebSocketNativeImpl
import com.nexustools.genericui.net.http.HttpRequest
import com.nexustools.genericui.net.http.HttpResponse
import com.nexustools.genericui.res.DataUrlCodec
import com.nexustools.genericui.style.color.RgbaColor
import com.nexustools.genericui.util.db.DatabaseRecord
import com.nexustools.genericui.util.db.DatabaseTableSchema
import com.nexustools.genericui.util.db.DatabaseValue
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Multiplatform bridge synchronizing [WebDomElement] nodes and events with the live browser DOM
 * (`window.document`) when running in a browser environment, while remaining a safe no-op in
 * headless Node/Wasm unit test environments.
 */
expect object WebBrowserDomBridge {
    val isBrowserEnvironment: Boolean

    fun createPeer(element: WebDomElement)
    fun bindApplicationRoot(element: WebDomElement)
    fun syncElementTree(element: WebDomElement)

    fun syncId(element: WebDomElement, id: String)
    fun syncClassName(element: WebDomElement, className: String = element.getEffectiveDomClassName())
    fun syncStyle(element: WebDomElement, property: String, value: String?, important: Boolean = false)
    fun clearStyles(element: WebDomElement)
    fun syncAttribute(element: WebDomElement, name: String, value: String?)
    fun syncTextContent(element: WebDomElement, text: String)
    fun syncInnerHTML(element: WebDomElement, html: String)
    fun syncValue(element: WebDomElement, value: String)
    fun syncChecked(element: WebDomElement, checked: Boolean)
    fun syncDisabled(element: WebDomElement, disabled: Boolean)
    fun syncReadOnly(element: WebDomElement, readOnly: Boolean)
    fun syncMultiple(element: WebDomElement, multiple: Boolean)
    fun syncDirectory(element: WebDomElement, directory: Boolean)
    fun syncAccept(element: WebDomElement, accept: String)
    fun syncScrollTop(element: WebDomElement, scrollTop: Double)
    fun syncScrollLeft(element: WebDomElement, scrollLeft: Double)

    fun appendChild(parent: WebDomElement, child: WebDomElement)
    fun insertChildAt(parent: WebDomElement, index: Int, child: WebDomElement)
    fun removeChild(parent: WebDomElement, child: WebDomElement)
    fun clearChildren(parent: WebDomElement)

    fun getClientWidth(element: WebDomElement, fallback: Int): Int
    fun getClientHeight(element: WebDomElement, fallback: Int): Int
    fun getOffsetLeft(element: WebDomElement, fallback: Int): Int
    fun getOffsetTop(element: WebDomElement, fallback: Int): Int
    fun getViewportWidth(fallback: Int): Int
    fun getViewportHeight(fallback: Int): Int

    fun setDocumentTitle(title: String)
    fun setDocumentTheme(theme: String)
    fun syncBootstrapTheme(themeCssValue: String)
    fun addStyleSheetLink(id: String, href: String)
    fun removeStyleSheetLink(id: String)
    fun isBrowserDarkMode(onSystemThemeChanged: ((Boolean) -> Unit)? = null): Boolean

    fun triggerClick(element: WebDomElement): Boolean
    fun registerWindowResize(onResize: (Int, Int) -> Unit)
    fun registerWindowKeyEvents(
        onKeyDown: (com.nexustools.genericui.event.KeyEvent) -> Unit,
        onKeyUp: (com.nexustools.genericui.event.KeyEvent) -> Unit
    )
    fun schedulePostLayoutPass(block: () -> Unit)
    fun requestAnimationFrame(callback: () -> Unit): Int
    fun cancelAnimationFrame(handle: Int)
    fun scheduleTimeout(delayMs: Int, block: () -> Unit): Boolean
    fun setTimeoutId(delayMs: Int, block: () -> Unit): Int
    fun setIntervalId(intervalMs: Int, block: () -> Unit): Int
    fun clearTimeoutId(timerId: Int): Boolean
    fun clearIntervalId(timerId: Int): Boolean

    fun showAlert(message: String)
    fun showConfirm(message: String): Boolean
    fun showPrompt(message: String, defaultValue: String): String?

    fun sendHttpRequest(
        url: String,
        method: String,
        contentType: String?,
        body: String?,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    )

    val hasLocalStorage: Boolean
    fun localStorageGetItem(key: String): String?
    fun localStorageSetItem(key: String, value: String): Boolean
    fun localStorageRemoveItem(key: String): Boolean
    fun localStorageClear(): Boolean
    fun localStorageKeys(): List<String>
    fun registerStorageListener(onStorageChanged: (key: String?, newValue: String?) -> Unit)

    fun getLocationHash(): String
    fun setLocationHash(hash: String)
    fun historyPushState(routeHash: String, historyIndex: Int, title: String, stateJson: String?)
    fun historyReplaceState(routeHash: String, historyIndex: Int, title: String, stateJson: String?)
    fun historyBack(): Boolean
    fun historyForward(): Boolean
    fun getCurrentHistoryState(): Pair<Int?, String?>
    fun registerPopStateListener(onPopState: (routeHash: String, historyIndex: Int?, stateJson: String?) -> Unit)

    fun getPreloadedResourceDataUrl(key: String): String?
    fun getPreloadedResourceBytes(key: String): ByteArray?
    fun getPreloadedResourceText(key: String): String?
    fun hasPreloadedResource(key: String): Boolean
    fun registerPreloadedResource(key: String, mimeType: String, bytes: ByteArray, text: String? = null)
    fun clearPreloadedResources()

    fun surfacePeerToDataUrl(peer: Any?, mimeType: String = "image/png"): String?
    fun decodePreloadedBrowserImage(
        uriOrKey: String?,
        encodedBytes: ByteArray?,
        targetWidth: Int?,
        targetHeight: Int?
    ): ImageBuffer?

    fun requestElementFullscreen(element: WebDomElement?): Boolean
    fun exitFullscreen(): Boolean

    fun bindInvisibleHtmlImageLoader(
        imgElement: WebDomElement,
        src: String,
        onLoaded: (naturalWidth: Int, naturalHeight: Int) -> Unit,
        onError: (String) -> Unit
    ): Boolean

    fun attachMediaElementListeners(element: WebDomElement)
    fun mediaCanPlayType(element: WebDomElement, mimeType: String): Boolean
    fun mediaLoad(element: WebDomElement, seekOffsetSeconds: Double = 0.0): Boolean
    fun mediaPlay(element: WebDomElement, onRejected: ((String) -> Unit)? = null): Boolean
    fun mediaPause(element: WebDomElement): Boolean
    fun mediaSeekTo(element: WebDomElement, seconds: Double): Boolean
    fun mediaSetVolume(element: WebDomElement, volume: Float): Boolean
    fun mediaSetMuted(element: WebDomElement, muted: Boolean): Boolean
    fun mediaSetPlaybackRate(element: WebDomElement, rate: Float): Boolean

    fun installModalFocusTrap(modalElement: WebDomElement)
    fun releaseModalFocusTrap(modalElement: WebDomElement)
    fun attachOverscrollStretch(scrollElement: WebDomElement, contentViewport: WebDomElement)
    fun attachLazyViewportObserver(
        scrollElement: WebDomElement,
        viewportElement: WebDomElement,
        isHorizontal: () -> Boolean,
        onViewportChanged: (
            scrollOffsetPx: Double,
            viewportExtentPx: Int,
            measuredViewportSpanPx: Int,
            renderedChildCount: Int
        ) -> Unit
    )
    fun attachSeekBarScrubbing(
        containerElement: WebDomElement,
        rangeInputElement: WebDomElement,
        onScrubStart: (fraction: Double) -> Unit,
        onScrubMove: (fraction: Double) -> Unit,
        onScrubEnd: (fraction: Double) -> Unit
    )

    fun setWebSplashStatus(statusText: String, type: BootstrapType = BootstrapType.NONE)
    fun setWebSplashProgress(progressPercent: Int, type: BootstrapType = BootstrapType.PRIMARY)
    fun setWebSplashError(message: String, stackTrace: String? = null, title: String = "Error")
    fun finishWebSplash(fadeDurationMs: Int = 350)
    fun triggerBrowserFileDownload(fileName: String, mimeType: String, bytes: ByteArray): Boolean
}

internal var lastWebSplashStatusState: String = WebSplashConfig.STATUS_INITIALIZING
internal var lastWebSplashStatusTypeState: BootstrapType = BootstrapType.NONE
internal var lastWebSplashProgressState: Int = 0
internal var lastWebSplashProgressTypeState: BootstrapType = BootstrapType.PRIMARY
internal var lastWebSplashErrorMessageState: String? = null
internal var lastWebSplashErrorStackState: String? = null
internal var lastWebSplashErrorTitleState: String? = null
internal var isWebSplashFinishedState: Boolean = false

var WebBrowserDomBridge.lastWebSplashStatus: String
    get() = lastWebSplashStatusState
    internal set(value) {
        lastWebSplashStatusState = value
    }

var WebBrowserDomBridge.lastWebSplashStatusType: BootstrapType
    get() = lastWebSplashStatusTypeState
    internal set(value) {
        lastWebSplashStatusTypeState = value
    }

var WebBrowserDomBridge.lastWebSplashProgress: Int
    get() = lastWebSplashProgressState
    internal set(value) {
        lastWebSplashProgressState = value
    }

var WebBrowserDomBridge.lastWebSplashProgressType: BootstrapType
    get() = lastWebSplashProgressTypeState
    internal set(value) {
        lastWebSplashProgressTypeState = value
    }

var WebBrowserDomBridge.lastWebSplashErrorMessage: String?
    get() = lastWebSplashErrorMessageState
    internal set(value) {
        lastWebSplashErrorMessageState = value
    }

var WebBrowserDomBridge.lastWebSplashErrorStack: String?
    get() = lastWebSplashErrorStackState
    internal set(value) {
        lastWebSplashErrorStackState = value
    }

var WebBrowserDomBridge.lastWebSplashErrorTitle: String?
    get() = lastWebSplashErrorTitleState
    internal set(value) {
        lastWebSplashErrorTitleState = value
    }

var WebBrowserDomBridge.isWebSplashFinished: Boolean
    get() = isWebSplashFinishedState
    internal set(value) {
        isWebSplashFinishedState = value
    }

fun WebBrowserDomBridge.resetHeadlessWebSplashState() {
    lastWebSplashStatusState = WebSplashConfig.STATUS_INITIALIZING
    lastWebSplashStatusTypeState = BootstrapType.NONE
    lastWebSplashProgressState = 0
    lastWebSplashProgressTypeState = BootstrapType.PRIMARY
    lastWebSplashErrorMessageState = null
    lastWebSplashErrorStackState = null
    lastWebSplashErrorTitleState = null
    isWebSplashFinishedState = false
}

private var deferSimulatedImageLoadsFlag: Boolean = false
internal val pendingInvisibleImageRefs = ArrayList<com.nexustools.genericui.util.WeakReference<WebDomElement>>()

/**
 * When `true` in headless `:web` unit tests, simulated invisible `HTMLImageElement` loads created by
 * [com.nexustools.genericui.image.ImageLoader] remain in the loading state (displaying any configured
 * placeholder image) until [completePendingImageLoads] or [failPendingImageLoads] is invoked.
 */
var WebBrowserDomBridge.deferSimulatedImageLoads: Boolean
    get() = deferSimulatedImageLoadsFlag
    set(value) {
        deferSimulatedImageLoadsFlag = value
    }

/**
 * Fires `"load"` on all pending simulated invisible `HTMLImageElement` instances.
 */
fun WebBrowserDomBridge.completePendingImageLoads(): Int {
    val snapshot = pendingInvisibleImageRefs.mapNotNull { it.get() }
    pendingInvisibleImageRefs.clear()
    for (img in snapshot) {
        img.dispatchFromBrowser("load", null)
    }
    return snapshot.size
}

/**
 * Fires `"error"` on all pending simulated invisible `HTMLImageElement` instances.
 */
fun WebBrowserDomBridge.failPendingImageLoads(): Int {
    val snapshot = pendingInvisibleImageRefs.mapNotNull { it.get() }
    pendingInvisibleImageRefs.clear()
    for (img in snapshot) {
        img.dispatchFromBrowser("error", null)
    }
    return snapshot.size
}

private var activeFullscreenElementState: WebDomElement? = null

var WebBrowserDomBridge.activeFullscreenElement: WebDomElement?
    get() = activeFullscreenElementState
    internal set(value) {
        activeFullscreenElementState = value
    }

private var browserOnlineState: Boolean = true
private val browserConnectivityListeners = mutableListOf<(Boolean) -> Unit>()

internal val headlessLocalStorage = LinkedHashMap<String, String>()
internal val headlessStorageListeners = mutableListOf<(key: String?, newValue: String?) -> Unit>()

/**
 * Dispatches a simulated browser `storage` event to all listeners registered via
 * [WebBrowserDomBridge.registerStorageListener].
 */
fun WebBrowserDomBridge.dispatchStorageEvent(key: String?, newValue: String?) {
    if (key == null) {
        localStorageClear()
    } else if (newValue == null) {
        localStorageRemoveItem(key)
    } else {
        localStorageSetItem(key, newValue)
    }
    for (listener in headlessStorageListeners.toList()) {
        listener(key, newValue)
    }
}

private val preloadedDataUrlsByKey = linkedMapOf<String, String>()
private val preloadedBytesByKey = linkedMapOf<String, ByteArray>()
private val preloadedTextsByKey = linkedMapOf<String, String>()
private val preloadedMimeTypesByKey = linkedMapOf<String, String>()

internal fun expandPreloadedResourceLookupKeys(rawKey: String): List<String> {
    val trimmed = rawKey.trim()
    if (trimmed.isEmpty()) return emptyList()
    val keys = linkedSetOf<String>()
    keys.add(trimmed)
    keys.add(trimmed.lowercase())

    val noQuery = trimmed.substringBefore('?').substringBefore('#')
    keys.add(noQuery)
    val clean = noQuery.replace('\\', '/').removePrefix("./").removePrefix("/")
    if (clean.isNotEmpty()) {
        keys.add(clean)
        keys.add(clean.lowercase())
        keys.add("res://$clean")
        keys.add("res/$clean")
        keys.add("gui/$clean")
    }

    if (clean.endsWith(".scss", ignoreCase = true) || clean.endsWith(".sass", ignoreCase = true)) {
        val cssVariant = clean.dropLast(5) + ".css"
        keys.add(cssVariant)
        keys.add(cssVariant.lowercase())
    }

    if (clean.startsWith("res://", ignoreCase = true)) {
        val afterScheme = clean.substring(6)
        if (afterScheme.isNotEmpty()) {
            keys.add(afterScheme)
            keys.add(afterScheme.lowercase())
            val typePart = afterScheme.substringBefore('/', "")
            val namePart = afterScheme.substringAfter('/', "")
            if (namePart.isNotEmpty()) {
                keys.add(namePart)
                keys.add(namePart.lowercase())
                keys.add("@$typePart/$namePart")
                if (typePart.equals("style", ignoreCase = true)) {
                    keys.add("styles/$namePart.css")
                    keys.add("styles/$namePart.scss")
                } else if (typePart.isNotEmpty()) {
                    keys.add("assets/$typePart/$namePart")
                }
            }
        }
    } else if (clean.startsWith("@") && clean.contains('/')) {
        val afterAt = clean.removePrefix("@")
        keys.add(afterAt)
        keys.add("res://$afterAt")
        val namePart = afterAt.substringAfter('/', "")
        if (namePart.isNotEmpty()) {
            keys.add(namePart)
            keys.add(namePart.lowercase())
        }
    }

    val fileName = clean.substringAfterLast('/')
    val stem = fileName.substringBeforeLast('.')
    if (fileName.isNotEmpty()) {
        keys.add(fileName)
        keys.add(fileName.lowercase())
    }
    if (stem.isNotEmpty() && !stem.startsWith("res:")) {
        keys.add(stem)
        keys.add(stem.lowercase())
        when {
            clean.startsWith("styles/", ignoreCase = true) ||
                fileName.endsWith(".css", ignoreCase = true) ||
                fileName.endsWith(".scss", ignoreCase = true) ||
                fileName.endsWith(".sass", ignoreCase = true) -> {
                keys.add("style/$stem")
                keys.add("res://style/$stem")
                keys.add("@style/$stem")
                keys.add("styles/$stem.css")
            }
            clean.startsWith("assets/drawable/", ignoreCase = true) ||
                clean.startsWith("drawable/", ignoreCase = true) -> {
                keys.add("drawable/$stem")
                keys.add("res://drawable/$stem")
                keys.add("@drawable/$stem")
            }
            clean.startsWith("assets/font/", ignoreCase = true) ||
                clean.startsWith("font/", ignoreCase = true) -> {
                keys.add("font/$stem")
                keys.add("res://font/$stem")
                keys.add("@font/$stem")
            }
            clean.startsWith("assets/xml/", ignoreCase = true) ||
                clean.startsWith("xml/", ignoreCase = true) -> {
                keys.add("xml/$stem")
                keys.add("res://xml/$stem")
                keys.add("@xml/$stem")
            }
            clean.startsWith("assets/raw/", ignoreCase = true) ||
                clean.startsWith("raw/", ignoreCase = true) -> {
                keys.add("raw/$stem")
                keys.add("res://raw/$stem")
                keys.add("@raw/$stem")
            }
        }
    }

    return keys.toList()
}

internal fun isTextLikeMimeType(mimeType: String): Boolean {
    val lower = mimeType.trim().lowercase()
    return lower.startsWith("text/") ||
        lower.contains("xml") ||
        lower.contains("json") ||
        lower.contains("svg") ||
        lower.contains("javascript")
}

internal fun registerPreloadedResourceInMemory(
    key: String,
    mimeType: String,
    bytes: ByteArray,
    text: String?
): Triple<String, String, String?> {
    val resolvedMime = mimeType.trim().ifEmpty { DataUrlCodec.guessMimeType(key) }
    val dataUrl = DataUrlCodec.encodeBytes(bytes, resolvedMime)
    val resolvedText = text ?: if (isTextLikeMimeType(resolvedMime)) {
        runCatching { bytes.decodeToString() }.getOrNull()
    } else {
        null
    }
    for (candidate in expandPreloadedResourceLookupKeys(key)) {
        preloadedDataUrlsByKey[candidate] = dataUrl
        preloadedBytesByKey[candidate] = bytes
        preloadedMimeTypesByKey[candidate] = resolvedMime
        if (resolvedText != null) {
            preloadedTextsByKey[candidate] = resolvedText
        }
    }
    return Triple(resolvedMime, dataUrl, resolvedText)
}

internal fun getPreloadedDataUrlFromMemory(key: String): String? {
    for (candidate in expandPreloadedResourceLookupKeys(key)) {
        preloadedDataUrlsByKey[candidate]?.let { return it }
    }
    return null
}

internal fun getPreloadedBytesFromMemory(key: String): ByteArray? {
    for (candidate in expandPreloadedResourceLookupKeys(key)) {
        preloadedBytesByKey[candidate]?.let { return it }
    }
    return null
}

internal fun getPreloadedTextFromMemory(key: String): String? {
    for (candidate in expandPreloadedResourceLookupKeys(key)) {
        preloadedTextsByKey[candidate]?.let { return it }
        preloadedBytesByKey[candidate]?.let { return runCatching { it.decodeToString() }.getOrNull() }
    }
    return null
}

internal fun hasPreloadedResourceInMemory(key: String): Boolean {
    for (candidate in expandPreloadedResourceLookupKeys(key)) {
        if (candidate in preloadedDataUrlsByKey || candidate in preloadedBytesByKey || candidate in preloadedTextsByKey) {
            return true
        }
    }
    return false
}

internal fun clearPreloadedResourcesInMemory() {
    preloadedDataUrlsByKey.clear()
    preloadedBytesByKey.clear()
    preloadedTextsByKey.clear()
    preloadedMimeTypesByKey.clear()
}

internal fun createLazyBrowserCanvasImageBuffer(
    width: Int,
    height: Int,
    canvasPeer: Any,
    toDataUrlFn: (String) -> String?,
    readRgbaPixelsFn: () -> ByteArray
): ImageBuffer {
    val outW = width.coerceAtLeast(1)
    val outH = height.coerceAtLeast(1)
    return object : ImageBuffer(
        width = outW,
        height = outH,
        pixelFormat = PixelFormat.RGBA_8888,
        colorSpace = ImageColorSpace.SRGB,
        bytes = ByteArray(0)
    ) {
        private var lazyBytesLoaded: Boolean = false
        private var pixelsMutated: Boolean = false

        private fun ensurePixelsLoaded() {
            if (!lazyBytesLoaded) {
                lazyBytesLoaded = true
                val expectedSize = outW * outH * 4
                val raw = runCatching { readRgbaPixelsFn() }.getOrNull()
                val loadedBytes = if (raw != null && raw.size == expectedSize) {
                    raw
                } else if (raw != null) {
                    ByteArray(expectedSize) { idx -> if (idx < raw.size) raw[idx] else 0 }
                } else {
                    ByteArray(expectedSize)
                }
                updateDimensionsAndBytes(outW, outH, loadedBytes)
            }
        }

        override val nativeSurfacePeer: Any?
            get() = if (pixelsMutated) null else canvasPeer

        override fun toDataUrl(mimeType: String): String? =
            if (pixelsMutated) null else toDataUrlFn(mimeType)

        override var bytes: ByteArray
            get() {
                ensurePixelsLoaded()
                return super.bytes
            }
            protected set(value) {
                lazyBytesLoaded = true
                super.bytes = value
            }

        override fun getPixel(x: Int, y: Int): RgbaColor {
            ensurePixelsLoaded()
            return super.getPixel(x, y)
        }

        override fun setPixel(x: Int, y: Int, color: RgbaColor) {
            ensurePixelsLoaded()
            pixelsMutated = true
            super.setPixel(x, y, color)
        }

        override fun fill(color: RgbaColor): ImageBuffer {
            ensurePixelsLoaded()
            pixelsMutated = true
            return super.fill(color)
        }

        override fun copy(): ImageBuffer {
            ensurePixelsLoaded()
            if (pixelsMutated) {
                return super.copy()
            }
            val copiedBytes = super.bytes.copyOf()
            return createLazyBrowserCanvasImageBuffer(
                width = width,
                height = height,
                canvasPeer = canvasPeer,
                toDataUrlFn = toDataUrlFn,
                readRgbaPixelsFn = { copiedBytes }
            )
        }

        override fun toRgba8888(): ImageBuffer {
            ensurePixelsLoaded()
            return if (pixelsMutated) super.toRgba8888() else copy()
        }
    }
}

/**
 * Returns `true` when the browser environment has network connectivity (`navigator.onLine`),
 * defaulting to `true` in headless test environments where `navigator` is undefined.
 */
fun WebBrowserDomBridge.isBrowserOnline(): Boolean = browserOnlineState

/**
 * Installs browser `"online"` and `"offline"` connectivity listeners and returns an unsubscribe lambda.
 */
fun WebBrowserDomBridge.installBrowserConnectivityListeners(
    onConnectivityChanged: (Boolean) -> Unit
): () -> Unit {
    browserConnectivityListeners.add(onConnectivityChanged)
    return {
        browserConnectivityListeners.remove(onConnectivityChanged)
    }
}

/**
 * Updates the tracked browser connectivity state and notifies all listeners registered via
 * [installBrowserConnectivityListeners].
 */
fun WebBrowserDomBridge.dispatchBrowserConnectivityChanged(online: Boolean) {
    browserOnlineState = online
    for (listener in browserConnectivityListeners.toList()) {
        listener(online)
    }
}

// ============================================================================
// 1. Direct UserDirectory Browser IndexedDB Namespace ("genericui_user_dirs:<appName>")
//    Object stores: "config" (ConfigDirectory) and "cache" (CacheDirectory)
//    Strictly isolated from Database ("genericui_db:<databaseName>:<sanitizedFilePath>")
// ============================================================================

private val userDirIndexedDbStores =
    LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, ByteArray>>>()

private var userDirIdbPutBrowserHook: ((dbName: String, storeName: String, key: String, bytes: ByteArray) -> Unit)? = null
private var userDirIdbDeleteBrowserHook: ((dbName: String, storeName: String, key: String) -> Unit)? = null
private var userDirIdbClearBrowserHook: ((dbName: String, storeName: String) -> Unit)? = null

/**
 * Resolves the dedicated UserDirectory IndexedDB database name (`"genericui_user_dirs:<appName>"`).
 * Never uses the `"genericui_db:"` namespace reserved for [com.nexustools.genericui.util.Database].
 */
fun WebBrowserDomBridge.resolveUserDirIndexedDbName(appNameOrDbName: String): String {
    val trimmed = appNameOrDbName.trim()
    if (trimmed.startsWith("genericui_user_dirs:")) {
        val after = trimmed.removePrefix("genericui_user_dirs:").trim().ifEmpty { "GenericUI" }
        return "genericui_user_dirs:$after"
    }
    val cleanApp = trimmed.removePrefix("genericui_db:").ifEmpty { "GenericUI" }
    return "genericui_user_dirs:$cleanApp"
}

/**
 * Resolves the dedicated UserDirectory IndexedDB object store name (`"config"` or `"cache"`).
 */
fun WebBrowserDomBridge.resolveUserDirObjectStoreName(kindOrStore: String): String =
    if (kindOrStore.trim().equals("cache", ignoreCase = true)) "cache" else "config"

/**
 * Reads raw file bytes directly from the UserDirectory IndexedDB (`"genericui_user_dirs:<appName>"`,
 * object store `"config"` or `"cache"`) without using [com.nexustools.genericui.util.Database].
 */
fun WebBrowserDomBridge.idbUserDirGet(
    dbNameOrApp: String,
    storeName: String,
    key: String
): ByteArray? {
    val dbName = resolveUserDirIndexedDbName(dbNameOrApp)
    val store = resolveUserDirObjectStoreName(storeName)
    return userDirIndexedDbStores[dbName]?.get(store)?.get(key)?.copyOf()
}

/**
 * Writes raw file bytes directly to the UserDirectory IndexedDB (`"genericui_user_dirs:<appName>"`,
 * object store `"config"` or `"cache"`) without using [com.nexustools.genericui.util.Database].
 */
fun WebBrowserDomBridge.idbUserDirPut(
    dbNameOrApp: String,
    storeName: String,
    key: String,
    bytes: ByteArray
): Boolean {
    val dbName = resolveUserDirIndexedDbName(dbNameOrApp)
    val store = resolveUserDirObjectStoreName(storeName)
    val dbMap = userDirIndexedDbStores.getOrPut(dbName) {
        LinkedHashMap<String, LinkedHashMap<String, ByteArray>>().apply {
            put("config", LinkedHashMap())
            put("cache", LinkedHashMap())
        }
    }
    val objectStore = dbMap.getOrPut(store) { LinkedHashMap() }
    val copy = bytes.copyOf()
    objectStore[key] = copy
    userDirIdbPutBrowserHook?.invoke(dbName, store, key, copy)
    return true
}

/**
 * Deletes a file entry directly from the UserDirectory IndexedDB (`"genericui_user_dirs:<appName>"`,
 * object store `"config"` or `"cache"`).
 */
fun WebBrowserDomBridge.idbUserDirDelete(
    dbNameOrApp: String,
    storeName: String,
    key: String
): Boolean {
    val dbName = resolveUserDirIndexedDbName(dbNameOrApp)
    val store = resolveUserDirObjectStoreName(storeName)
    val removed = userDirIndexedDbStores[dbName]?.get(store)?.remove(key) != null
    if (removed) {
        userDirIdbDeleteBrowserHook?.invoke(dbName, store, key)
    }
    return removed
}

/**
 * Lists all keys (optionally filtered by [prefix]) in the UserDirectory IndexedDB
 * (`"genericui_user_dirs:<appName>"`, object store `"config"` or `"cache"`).
 */
fun WebBrowserDomBridge.idbUserDirKeys(
    dbNameOrApp: String,
    storeName: String,
    prefix: String = ""
): List<String> {
    val dbName = resolveUserDirIndexedDbName(dbNameOrApp)
    val store = resolveUserDirObjectStoreName(storeName)
    val keys = userDirIndexedDbStores[dbName]?.get(store)?.keys ?: return emptyList()
    if (prefix.isEmpty()) return keys.toList()
    return keys.filter { it.startsWith(prefix) }
}

/**
 * Clears all keys (or keys matching [prefix]) in the UserDirectory IndexedDB
 * (`"genericui_user_dirs:<appName>"`, object store `"config"` or `"cache"`), returning the count removed.
 */
fun WebBrowserDomBridge.idbUserDirClear(
    dbNameOrApp: String,
    storeName: String,
    prefix: String = ""
): Int {
    val dbName = resolveUserDirIndexedDbName(dbNameOrApp)
    val store = resolveUserDirObjectStoreName(storeName)
    val objectStore = userDirIndexedDbStores[dbName]?.get(store) ?: return 0
    if (prefix.isEmpty()) {
        val count = objectStore.size
        objectStore.clear()
        userDirIdbClearBrowserHook?.invoke(dbName, store)
        return count
    }
    val matching = objectStore.keys.filter { it.startsWith(prefix) }
    for (k in matching) {
        objectStore.remove(k)
        userDirIdbDeleteBrowserHook?.invoke(dbName, store, k)
    }
    return matching.size
}

/**
 * Returns the active UserDirectory IndexedDB database names (`"genericui_user_dirs:<appName>"`).
 */
fun WebBrowserDomBridge.idbUserDirDatabaseNames(): Set<String> =
    userDirIndexedDbStores.keys.toSet()

/**
 * Returns the object store names (`"config"`, `"cache"`) inside a UserDirectory IndexedDB database.
 */
fun WebBrowserDomBridge.idbUserDirObjectStoreNames(dbNameOrApp: String): Set<String> {
    val dbName = resolveUserDirIndexedDbName(dbNameOrApp)
    return userDirIndexedDbStores[dbName]?.keys?.toSet() ?: emptySet()
}

/**
 * Clears all in-memory UserDirectory IndexedDB stores (for test isolation).
 */
fun WebBrowserDomBridge.idbUserDirResetAll() {
    userDirIndexedDbStores.clear()
}

internal fun WebBrowserDomBridge.installUserDirBrowserHooks(
    onPut: (dbName: String, storeName: String, key: String, bytes: ByteArray) -> Unit,
    onDelete: (dbName: String, storeName: String, key: String) -> Unit,
    onClear: (dbName: String, storeName: String) -> Unit
) {
    userDirIdbPutBrowserHook = onPut
    userDirIdbDeleteBrowserHook = onDelete
    userDirIdbClearBrowserHook = onClear
}

// ============================================================================
// 2. Strictly Isolated Database Browser IndexedDB Namespace
//    ("genericui_db:<databaseName>:<sanitizedFilePath>", object stores "tbl:<tableName>")
//    Can NEVER conflict with ConfigDirectory ("config") or CacheDirectory ("cache")!
// ============================================================================

private val isolatedDbVersions = LinkedHashMap<String, Int>()
private val isolatedDbSchemas = LinkedHashMap<String, LinkedHashMap<String, DatabaseTableSchema>>()
private val isolatedDbStores =
    LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, DatabaseRecord>>>()
private var isolatedDbTickCounter: Long = 1700000000000L

/**
 * Builds the strictly isolated IndexedDB database name for a [WebDatabaseNativeImpl] instance:
 * `"genericui_db:<databaseName>:<sanitizedFilePath>"`.
 * Always starts with `"genericui_db:"` and never `"genericui_user_dirs:"`.
 */
fun WebBrowserDomBridge.buildIsolatedIndexedDbName(
    databaseName: String,
    filePath: String = "$databaseName.db"
): String {
    val cleanDb = databaseName.trim()
        .removePrefix("genericui_db:")
        .removePrefix("genericui_user_dirs:")
        .ifEmpty { "GenericUI" }
        .replace(Regex("[^A-Za-z0-9._:-]"), "_")
    val rawPath = filePath.trim().ifEmpty { "$cleanDb.db" }
    val sanitizedPath = rawPath
        .replace('\\', '/')
        .replace("://", "_")
        .replace(Regex("[^A-Za-z0-9._/-]"), "_")
        .trim('/')
        .ifEmpty { "$cleanDb.db" }
    return "genericui_db:$cleanDb:$sanitizedPath"
}

/**
 * Resolves the prefixed IndexedDB object store name (`"tbl:<tableName>"`) for a [Database] table.
 * Because every table store is prefixed with `"tbl:"`, even tables named `"config"` or `"cache"`
 * resolve to `"tbl:config"` and `"tbl:cache"`, never bare `"config"` or `"cache"`.
 */
fun WebBrowserDomBridge.resolveDatabaseObjectStoreName(tableName: String): String {
    val clean = tableName.trim().removePrefix("tbl:").ifEmpty { "default" }
    return "tbl:$clean"
}

private fun ensureIsolatedDbNamespace(rawIndexedDbName: String): String {
    val trimmed = rawIndexedDbName.trim()
    return if (trimmed.startsWith("genericui_db:")) {
        trimmed
    } else {
        WebBrowserDomBridge.buildIsolatedIndexedDbName(trimmed, "$trimmed.db")
    }
}

/**
 * Opens or upgrades an isolated `Database` IndexedDB instance (`"genericui_db:..."`).
 */
fun WebBrowserDomBridge.idbDatabaseOpen(indexedDbName: String, version: Int = 1): Boolean {
    val dbKey = ensureIsolatedDbNamespace(indexedDbName)
    val cleanVersion = version.coerceAtLeast(1)
    val prev = isolatedDbVersions[dbKey] ?: 0
    if (cleanVersion > prev) {
        isolatedDbVersions[dbKey] = cleanVersion
    }
    isolatedDbSchemas.getOrPut(dbKey) { LinkedHashMap() }
    isolatedDbStores.getOrPut(dbKey) { LinkedHashMap() }
    return true
}

/**
 * Defines a table schema inside the isolated `Database` IndexedDB (`"genericui_db:..."`) under
 * object store `"tbl:<tableName>"`.
 */
fun WebBrowserDomBridge.idbDatabaseDefineTable(
    indexedDbName: String,
    schema: DatabaseTableSchema
): Boolean {
    val dbKey = ensureIsolatedDbNamespace(indexedDbName)
    idbDatabaseOpen(dbKey)
    val storeName = resolveDatabaseObjectStoreName(schema.tableName)
    isolatedDbSchemas.getOrPut(dbKey) { LinkedHashMap() }[storeName] = schema
    isolatedDbStores.getOrPut(dbKey) { LinkedHashMap() }.getOrPut(storeName) { LinkedHashMap() }
    return true
}

/**
 * Inserts or updates a [DatabaseRecord] in the isolated `Database` IndexedDB (`"genericui_db:..."`)
 * inside object store `"tbl:<tableName>"`.
 */
fun WebBrowserDomBridge.idbDatabasePutRecord(
    indexedDbName: String,
    tableName: String,
    record: DatabaseRecord
): Boolean {
    val dbKey = ensureIsolatedDbNamespace(indexedDbName)
    idbDatabaseOpen(dbKey)
    val storeName = resolveDatabaseObjectStoreName(tableName)
    val store = isolatedDbStores.getOrPut(dbKey) { LinkedHashMap() }
        .getOrPut(storeName) { LinkedHashMap() }

    val enrichedValues = LinkedHashMap(record.values)
    if (!enrichedValues.containsKey("key")) {
        enrichedValues["key"] = DatabaseValue.of(record.key)
    }
    if (!enrichedValues.containsKey("updatedAt")) {
        enrichedValues["updatedAt"] = DatabaseValue.of(++isolatedDbTickCounter)
    }
    store[record.key] = DatabaseRecord(key = record.key, values = enrichedValues)
    return true
}

/**
 * Retrieves a single [DatabaseRecord] by [key] from the isolated `Database` IndexedDB
 * (`"genericui_db:..."`, object store `"tbl:<tableName>"`).
 */
fun WebBrowserDomBridge.idbDatabaseGetRecord(
    indexedDbName: String,
    tableName: String,
    key: String
): DatabaseRecord? {
    val dbKey = ensureIsolatedDbNamespace(indexedDbName)
    val storeName = resolveDatabaseObjectStoreName(tableName)
    return isolatedDbStores[dbKey]?.get(storeName)?.get(key)
}

/**
 * Deletes a single record by [key] from the isolated `Database` IndexedDB
 * (`"genericui_db:..."`, object store `"tbl:<tableName>"`).
 */
fun WebBrowserDomBridge.idbDatabaseDeleteRecord(
    indexedDbName: String,
    tableName: String,
    key: String
): Boolean {
    val dbKey = ensureIsolatedDbNamespace(indexedDbName)
    val storeName = resolveDatabaseObjectStoreName(tableName)
    return isolatedDbStores[dbKey]?.get(storeName)?.remove(key) != null
}

/**
 * Returns all [DatabaseRecord]s stored in the isolated `Database` IndexedDB
 * (`"genericui_db:..."`, object store `"tbl:<tableName>"`).
 */
fun WebBrowserDomBridge.idbDatabaseGetAllRecords(
    indexedDbName: String,
    tableName: String
): List<DatabaseRecord> {
    val dbKey = ensureIsolatedDbNamespace(indexedDbName)
    val storeName = resolveDatabaseObjectStoreName(tableName)
    return isolatedDbStores[dbKey]?.get(storeName)?.values?.toList() ?: emptyList()
}

/**
 * Returns all record keys (optionally filtered by [prefix]) in the isolated `Database` IndexedDB
 * (`"genericui_db:..."`, object store `"tbl:<tableName>"`).
 */
fun WebBrowserDomBridge.idbDatabaseKeys(
    indexedDbName: String,
    tableName: String,
    prefix: String = ""
): List<String> {
    val dbKey = ensureIsolatedDbNamespace(indexedDbName)
    val storeName = resolveDatabaseObjectStoreName(tableName)
    val keys = isolatedDbStores[dbKey]?.get(storeName)?.keys ?: return emptyList()
    if (prefix.isEmpty()) return keys.toList()
    return keys.filter { it.startsWith(prefix) }
}

/**
 * Clears all records in `"tbl:<tableName>"` inside the isolated `Database` IndexedDB
 * (`"genericui_db:..."`), returning the number of deleted records.
 */
fun WebBrowserDomBridge.idbDatabaseClearTable(
    indexedDbName: String,
    tableName: String
): Int {
    val dbKey = ensureIsolatedDbNamespace(indexedDbName)
    val storeName = resolveDatabaseObjectStoreName(tableName)
    val store = isolatedDbStores[dbKey]?.get(storeName) ?: return 0
    val count = store.size
    store.clear()
    return count
}

/**
 * Drops the object store `"tbl:<tableName>"` and its schema from the isolated `Database` IndexedDB
 * (`"genericui_db:..."`).
 */
fun WebBrowserDomBridge.idbDatabaseDropTable(
    indexedDbName: String,
    tableName: String
): Boolean {
    val dbKey = ensureIsolatedDbNamespace(indexedDbName)
    val storeName = resolveDatabaseObjectStoreName(tableName)
    val removedSchema = isolatedDbSchemas[dbKey]?.remove(storeName) != null
    val removedStore = isolatedDbStores[dbKey]?.remove(storeName) != null
    return removedSchema || removedStore
}

/**
 * Returns the logical table names (with `"tbl:"` prefix stripped) defined or populated in the
 * isolated `Database` IndexedDB (`"genericui_db:..."`).
 */
fun WebBrowserDomBridge.idbDatabaseTables(indexedDbName: String): List<String> {
    val dbKey = ensureIsolatedDbNamespace(indexedDbName)
    val names = LinkedHashSet<String>()
    isolatedDbSchemas[dbKey]?.keys?.forEach { names.add(it.removePrefix("tbl:")) }
    isolatedDbStores[dbKey]?.keys?.forEach { names.add(it.removePrefix("tbl:")) }
    return names.toList()
}

/**
 * Returns the raw IndexedDB object store names (each prefixed with `"tbl:"`) for [indexedDbName].
 */
fun WebBrowserDomBridge.idbDatabaseObjectStoreNames(indexedDbName: String): Set<String> {
    val dbKey = ensureIsolatedDbNamespace(indexedDbName)
    val rawStores = LinkedHashSet<String>()
    isolatedDbSchemas[dbKey]?.keys?.let { rawStores.addAll(it) }
    isolatedDbStores[dbKey]?.keys?.let { rawStores.addAll(it) }
    return rawStores
}

/**
 * Returns all active isolated `Database` IndexedDB names (`"genericui_db:..."`).
 */
fun WebBrowserDomBridge.idbDatabaseNames(): Set<String> {
    val names = LinkedHashSet<String>()
    names.addAll(isolatedDbVersions.keys)
    names.addAll(isolatedDbSchemas.keys)
    names.addAll(isolatedDbStores.keys)
    return names
}

/**
 * Clears all in-memory isolated `Database` IndexedDB stores (for test isolation).
 */
fun WebBrowserDomBridge.idbDatabaseResetAll() {
    isolatedDbVersions.clear()
    isolatedDbSchemas.clear()
    isolatedDbStores.clear()
}

// ============================================================================
// 3. Browser File System Access API, Fetch API, and WebSocket Bridge Helpers
// ============================================================================

private var fileSystemAccessSupportedFlag: Boolean = true
private var browserFetchOverrideHook: (suspend (HttpRequest) -> HttpResponse?)? = null
private var browserWebSocketFactoryOverrideHook: (() -> WebSocketNativeImpl?)? = null

/**
 * Indicates whether the browser File System Access API (`window.showOpenFilePicker`,
 * `window.showSaveFilePicker`, `window.showDirectoryPicker`) is supported.
 */
var WebBrowserDomBridge.isFileSystemAccessSupported: Boolean
    get() = fileSystemAccessSupportedFlag
    set(value) {
        fileSystemAccessSupportedFlag = value
    }

/**
 * Optional override hook for browser `window.fetch()` execution in [WebHttpClientNativeImpl].
 */
var WebBrowserDomBridge.browserFetchHook: (suspend (HttpRequest) -> HttpResponse?)?
    get() = browserFetchOverrideHook
    set(value) {
        browserFetchOverrideHook = value
    }

/**
 * Optional override factory for browser `window.WebSocket` creation in [WebWebSocketNativeImpl].
 */
var WebBrowserDomBridge.browserWebSocketFactoryHook: (() -> WebSocketNativeImpl?)?
    get() = browserWebSocketFactoryOverrideHook
    set(value) {
        browserWebSocketFactoryOverrideHook = value
    }

/**
 * Executes an asynchronous HTTP request via [browserFetchHook] or browser `fetch()` / `XMLHttpRequest`
 * when running in a live browser DOM environment, returning `null` in headless tests so
 * [WebHttpClientNativeImpl] can delegate to [com.nexustools.genericui.nativeimpl.net.DefaultHttpClientNativeImpl].
 */
suspend fun WebBrowserDomBridge.fetchHttp(request: HttpRequest): HttpResponse? {
    browserFetchOverrideHook?.let { hook ->
        hook.invoke(request)?.let { return it }
    }
    if (!isBrowserEnvironment) {
        return null
    }
    val contentType = request.headers.entries
        .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
        ?.value
    return suspendCoroutine { cont ->
        sendHttpRequest(
            url = request.url,
            method = request.method.name,
            contentType = contentType,
            body = request.bodyText,
            onSuccess = { responseText ->
                val bytes = responseText.encodeToByteArray()
                cont.resume(
                    HttpResponse(
                        statusCode = 200,
                        statusText = "OK",
                        headers = mapOf(
                            "Content-Type" to (contentType ?: "application/json"),
                            "Content-Length" to bytes.size.toString()
                        ),
                        bodyBytes = bytes,
                        url = request.url
                    )
                )
            },
            onError = { errorMsg ->
                cont.resume(
                    HttpResponse(
                        statusCode = 502,
                        statusText = errorMsg,
                        bodyBytes = ByteArray(0),
                        url = request.url
                    )
                )
            }
        )
    }
}

// ============================================================================
// 4. Modal Focus Trap & Android-Style Overscroll Stretch Helpers
// ============================================================================

private val activeFocusTrapModals = ArrayList<WebDomElement>()
private var focusedModalElementState: WebDomElement? = null

val WebBrowserDomBridge.activeModalFocusTrapStack: List<WebDomElement>
    get() = activeFocusTrapModals.toList()

var WebBrowserDomBridge.focusedModalElement: WebDomElement?
    get() = focusedModalElementState
    internal set(value) {
        focusedModalElementState = value
    }

private fun isElementVisibleForFocus(element: WebDomElement): Boolean {
    var cur: WebDomElement? = element
    while (cur != null) {
        if (cur.getStyle("display") == "none" || cur.getStyle("visibility") == "hidden" || cur.enumClasses.contains("d-none") || cur.classList.contains("d-none")) {
            return false
        }
        cur = cur.parent
    }
    return true
}

private fun isFocusableDomElement(element: WebDomElement): Boolean {
    if (element.disabled || !isElementVisibleForFocus(element)) return false
    val tag = element.tagName.lowercase()
    val tabIdx = element.getAttribute("tabindex")?.toIntOrNull()
    if (tabIdx != null) {
        return tabIdx >= 0
    }
    return when (tag) {
        "button", "select", "textarea" -> true
        "input" -> element.getAttribute("type")?.lowercase() != "hidden"
        "a" -> element.hasAttribute("href")
        else -> false
    }
}

fun WebBrowserDomBridge.findFocusableDescendants(root: WebDomElement): List<WebDomElement> {
    val result = ArrayList<WebDomElement>()
    fun walk(node: WebDomElement) {
        for (child in node.children) {
            if (isFocusableDomElement(child)) {
                result.add(child)
            }
            walk(child)
        }
    }
    walk(root)
    return result
}

fun WebBrowserDomBridge.cycleModalTabFocus(
    modalElement: WebDomElement,
    shiftKey: Boolean,
    currentFocused: WebDomElement? = focusedModalElementState
): WebDomElement? {
    val focusables = findFocusableDescendants(modalElement)
    if (focusables.isEmpty()) {
        focusedModalElementState = modalElement
        modalElement.setAttribute("data-focused-descendant-id", modalElement.id)
        return modalElement
    }
    val currentIndex = if (currentFocused != null) focusables.indexOfFirst { it === currentFocused } else -1
    val nextIndex = if (shiftKey) {
        if (currentIndex <= 0) focusables.lastIndex else currentIndex - 1
    } else {
        if (currentIndex < 0 || currentIndex >= focusables.lastIndex) 0 else currentIndex + 1
    }
    val nextFocused = focusables[nextIndex]
    focusedModalElementState = nextFocused
    modalElement.setAttribute("data-focused-descendant-index", nextIndex.toString())
    return nextFocused
}

internal fun WebBrowserDomBridge.registerHeadlessModalFocusTrap(modalElement: WebDomElement) {
    activeFocusTrapModals.remove(modalElement)
    activeFocusTrapModals.add(modalElement)
    modalElement.setAttribute("data-focus-trap-active", "true")
    val firstFocusable = findFocusableDescendants(modalElement).firstOrNull() ?: modalElement
    focusedModalElementState = firstFocusable
    modalElement.setAttribute("data-focused-descendant-index", if (firstFocusable === modalElement) "-1" else "0")
    if (modalElement.getAttribute("data-focus-trap-listener") != "true") {
        modalElement.setAttribute("data-focus-trap-listener", "true")
        modalElement.addEventListener("keydown") { ev ->
            val ke = ev as? com.nexustools.genericui.event.KeyEvent
            if (ke != null && ke.key.equals("Tab", ignoreCase = true) && activeFocusTrapModals.lastOrNull() === modalElement) {
                cycleModalTabFocus(modalElement, ke.shiftKey)
            }
        }
    }
}

internal fun WebBrowserDomBridge.unregisterHeadlessModalFocusTrap(modalElement: WebDomElement) {
    activeFocusTrapModals.remove(modalElement)
    modalElement.removeAttribute("data-focus-trap-active")
    val nextTop = activeFocusTrapModals.lastOrNull()
    focusedModalElementState = if (nextTop != null) {
        findFocusableDescendants(nextTop).firstOrNull() ?: nextTop
    } else {
        null
    }
}

internal fun computeOverscrollStretchScale(overscrollPx: Double): Double {
    val absDelta = kotlin.math.abs(overscrollPx)
    if (absDelta <= 0.5) return 1.0
    val normalized = (absDelta / 220.0).coerceIn(0.0, 4.0)
    // Smooth saturating curve: x / (1 + x) * maxStretch
    val ratio = normalized / (1.0 + normalized)
    val stretch = (ratio * 0.14).coerceIn(0.0, 0.14)
    return kotlin.math.round((1.0 + stretch) * 10000.0) / 10000.0
}

internal fun WebBrowserDomBridge.attachHeadlessOverscrollStretch(
    scrollElement: WebDomElement,
    contentViewport: WebDomElement
) {
    if (scrollElement.getAttribute("data-overscroll-attached") == "true") return
    scrollElement.setAttribute("data-overscroll-attached", "true")
    scrollElement.setAttribute("data-overscroll-stretch", "1.0")
    if (isBrowserEnvironment) return
    scrollElement.style["overscroll-behavior"] = "contain"

    var dragging = false
    var dragStartX = 0f
    var dragStartY = 0f

    fun isHorizontal(): Boolean =
        scrollElement.getStyle("overflow-x") == "auto" && scrollElement.getStyle("overflow-y") != "auto"

    fun applyStretch(overscrollDelta: Double) {
        val horiz = isHorizontal()
        val scale = computeOverscrollStretchScale(overscrollDelta)
        if (scale <= 1.0001) {
            contentViewport.setStyle("transition", "transform 260ms cubic-bezier(0.22, 1, 0.36, 1)")
            contentViewport.setStyle("transform", if (horiz) "scaleX(1)" else "scaleY(1)")
            scrollElement.setAttribute("data-overscroll-stretch", "1.0")
            scrollElement.removeAttribute("data-overscroll-edge")
            return
        }
        val origin = if (horiz) {
            if (overscrollDelta > 0) "0% 50%" else "100% 50%"
        } else {
            if (overscrollDelta > 0) "50% 0%" else "50% 100%"
        }
        val edge = if (horiz) {
            if (overscrollDelta > 0) "start" else "end"
        } else {
            if (overscrollDelta > 0) "top" else "bottom"
        }
        contentViewport.setStyle("transform-origin", origin)
        contentViewport.setStyle("transition", "none")
        contentViewport.setStyle("transform", if (horiz) "scaleX($scale)" else "scaleY($scale)")
        scrollElement.setAttribute("data-overscroll-stretch", scale.toString())
        scrollElement.setAttribute("data-overscroll-edge", edge)
    }

    fun releaseStretch() {
        val horiz = isHorizontal()
        dragging = false
        contentViewport.setStyle("transition", "transform 260ms cubic-bezier(0.22, 1, 0.36, 1)")
        contentViewport.setStyle("transform", if (horiz) "scaleX(1)" else "scaleY(1)")
        scrollElement.setAttribute("data-overscroll-stretch", "1.0")
        scrollElement.removeAttribute("data-overscroll-edge")
    }

    scrollElement.addEventListener("pointerdown") { ev ->
        val pe = ev as? com.nexustools.genericui.event.PointerEvent ?: return@addEventListener
        dragging = true
        dragStartX = pe.x
        dragStartY = pe.y
    }

    scrollElement.addEventListener("pointermove") { ev ->
        if (!dragging) return@addEventListener
        val pe = ev as? com.nexustools.genericui.event.PointerEvent ?: return@addEventListener
        val horiz = isHorizontal()
        val delta = if (horiz) (pe.x - dragStartX).toDouble() else (pe.y - dragStartY).toDouble()
        val scrollPos = if (horiz) scrollElement.scrollLeft else scrollElement.scrollTop
        val clientExt = if (horiz) scrollElement.clientWidth else scrollElement.clientHeight
        val totalExt = if (horiz) scrollElement.scrollWidth else scrollElement.scrollHeight
        val atStart = scrollPos <= 1.0
        val atEnd = (scrollPos + clientExt) >= (totalExt - 2).coerceAtLeast(0)
        if (atStart && delta > 0.0) {
            applyStretch(delta)
        } else if (atEnd && delta < 0.0) {
            applyStretch(delta)
        } else if (scrollElement.getAttribute("data-overscroll-stretch") != "1.0") {
            applyStretch(0.0)
        }
    }

    scrollElement.addEventListener("pointerup") {
        if (dragging || scrollElement.getAttribute("data-overscroll-stretch") != "1.0") {
            releaseStretch()
        }
    }

    scrollElement.addEventListener("mouseleave") {
        if (dragging || scrollElement.getAttribute("data-overscroll-stretch") != "1.0") {
            releaseStretch()
        }
    }
}

private var activeHeadlessSeekBarMove: ((com.nexustools.genericui.event.PointerEvent) -> Unit)? = null
private var activeHeadlessSeekBarUp: ((com.nexustools.genericui.event.PointerEvent?) -> Unit)? = null

internal fun dispatchHeadlessSeekBarGlobalMove(event: com.nexustools.genericui.event.PointerEvent): Boolean {
    val move = activeHeadlessSeekBarMove ?: return false
    move(event)
    return true
}

internal fun dispatchHeadlessSeekBarGlobalUp(event: com.nexustools.genericui.event.PointerEvent?): Boolean {
    val up = activeHeadlessSeekBarUp ?: return false
    up(event)
    return true
}

internal fun WebBrowserDomBridge.attachHeadlessSeekBarScrubbing(
    containerElement: WebDomElement,
    onScrubStart: (fraction: Double) -> Unit,
    onScrubMove: (fraction: Double) -> Unit,
    onScrubEnd: (fraction: Double) -> Unit
) {
    if (containerElement.getAttribute("data-seekbar-scrubbing-attached") == "true") return
    containerElement.setAttribute("data-seekbar-scrubbing-attached", "true")
    var scrubbing = false
    var lastFraction = 0.0
    var lastMoveEvent: com.nexustools.genericui.event.PointerEvent? = null

    fun computeFraction(pe: com.nexustools.genericui.event.PointerEvent): Double {
        val w = containerElement.clientWidth.takeIf { it > 0 } ?: 100
        val relX = (pe.x - containerElement.offsetLeft.toFloat()).toDouble()
        return (relX / w.toDouble()).coerceIn(0.0, 1.0)
    }

    fun handleMove(pe: com.nexustools.genericui.event.PointerEvent) {
        if (scrubbing && lastMoveEvent !== pe) {
            lastMoveEvent = pe
            lastFraction = computeFraction(pe)
            onScrubMove(lastFraction)
        }
    }

    fun handleUp(pe: com.nexustools.genericui.event.PointerEvent?) {
        if (scrubbing) {
            scrubbing = false
            activeHeadlessSeekBarMove = null
            activeHeadlessSeekBarUp = null
            val frac = if (pe != null) computeFraction(pe).also { lastFraction = it } else lastFraction
            onScrubEnd(frac)
        }
    }

    containerElement.addEventListener("pointerdown") { ev ->
        val pe = ev as? com.nexustools.genericui.event.PointerEvent ?: return@addEventListener
        if (pe.button != 0) return@addEventListener
        activeHeadlessSeekBarUp?.invoke(pe)
        scrubbing = true
        lastMoveEvent = pe
        lastFraction = computeFraction(pe)
        activeHeadlessSeekBarMove = ::handleMove
        activeHeadlessSeekBarUp = ::handleUp
        onScrubStart(lastFraction)
    }

    containerElement.addEventListener("pointermove") { ev ->
        if (!scrubbing) return@addEventListener
        val pe = ev as? com.nexustools.genericui.event.PointerEvent ?: return@addEventListener
        handleMove(pe)
    }

    containerElement.addEventListener("pointerup") { ev ->
        if (!scrubbing) return@addEventListener
        handleUp(ev as? com.nexustools.genericui.event.PointerEvent)
    }

    containerElement.addEventListener("pointercancel") { ev ->
        if (!scrubbing) return@addEventListener
        handleUp(ev as? com.nexustools.genericui.event.PointerEvent)
    }
}
