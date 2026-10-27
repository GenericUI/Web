package com.nexustools.genericui.web

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
    fun schedulePostLayoutPass(block: () -> Unit)
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
}
