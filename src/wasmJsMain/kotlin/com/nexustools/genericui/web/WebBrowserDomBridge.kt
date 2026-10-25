package com.nexustools.genericui.web

internal actual object WebBrowserDomBridge {
    actual val isBrowserEnvironment: Boolean = false

    actual fun createPeer(element: WebDomElement) {}
    actual fun bindApplicationRoot(element: WebDomElement) {}

    actual fun syncId(element: WebDomElement, id: String) {}
    actual fun syncClassName(element: WebDomElement, className: String) {}
    actual fun syncStyle(element: WebDomElement, property: String, value: String?) {}
    actual fun clearStyles(element: WebDomElement) {}
    actual fun syncAttribute(element: WebDomElement, name: String, value: String?) {}
    actual fun syncTextContent(element: WebDomElement, text: String) {}
    actual fun syncInnerHTML(element: WebDomElement, html: String) {}
    actual fun syncValue(element: WebDomElement, value: String) {}
    actual fun syncChecked(element: WebDomElement, checked: Boolean) {}
    actual fun syncDisabled(element: WebDomElement, disabled: Boolean) {}
    actual fun syncReadOnly(element: WebDomElement, readOnly: Boolean) {}
    actual fun syncMultiple(element: WebDomElement, multiple: Boolean) {}
    actual fun syncDirectory(element: WebDomElement, directory: Boolean) {}
    actual fun syncAccept(element: WebDomElement, accept: String) {}

    actual fun appendChild(parent: WebDomElement, child: WebDomElement) {}
    actual fun insertChildAt(parent: WebDomElement, index: Int, child: WebDomElement) {}
    actual fun removeChild(parent: WebDomElement, child: WebDomElement) {}
    actual fun clearChildren(parent: WebDomElement) {}

    actual fun getClientWidth(element: WebDomElement, fallback: Int): Int = fallback
    actual fun getClientHeight(element: WebDomElement, fallback: Int): Int = fallback
    actual fun getViewportWidth(fallback: Int): Int = fallback
    actual fun getViewportHeight(fallback: Int): Int = fallback

    actual fun setDocumentTitle(title: String) {}
    actual fun setDocumentTheme(theme: String) {}
    actual fun triggerClick(element: WebDomElement): Boolean = false
    actual fun registerWindowResize(onResize: (Int, Int) -> Unit) {}
    actual fun schedulePostLayoutPass(block: () -> Unit) {}

    actual fun showAlert(message: String) {}
    actual fun showConfirm(message: String): Boolean = true
    actual fun showPrompt(message: String, defaultValue: String): String? = defaultValue
}
