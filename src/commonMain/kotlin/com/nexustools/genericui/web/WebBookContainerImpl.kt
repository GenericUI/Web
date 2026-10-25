package com.nexustools.genericui.web

import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.enums.document.PrintPageOrientation
import com.nexustools.genericui.enums.document.PrintPageSize
import com.nexustools.genericui.nativeimpl.document.BookContainerNativeImpl

/**
 * Web (`js` + `wasmJs`) implementation of [BookContainerNativeImpl] backed by an onscreen HTML5 `<iframe>`
 * element (`WebDomElement("iframe")`).
 *
 * Renders child [com.nexustools.genericui.component.document.BookPage] DOM elements directly inside the
 * onscreen `<iframe>`'s `contentDocument.body` with `@page` size/orientation CSS rules so the user sees
 * an interactive onscreen Print Preview and [printBook] opens the browser's native print dialog for the
 * `<iframe>` directly via `iframe.contentWindow.print()`.
 */
class WebBookContainerImpl : WebBaseContainerNativeImpl("iframe", "book-container"), BookContainerNativeImpl<WebDomElement> {

    private var currentPageSize: PrintPageSize = PrintPageSize.A4
    private var currentOrientation: PrintPageOrientation = PrintPageOrientation.PORTRAIT
    private var currentMarginMm: Float = 12f
    private var currentTitle: String = "Document"
    private var printCount: Int = 0

    init {
        nativeWidget.style["width"] = "100%"
        nativeWidget.style["height"] = "100%"
        nativeWidget.style["border"] = "none"
        nativeWidget.style["background-color"] = "#ffffff"
        nativeWidget.style["display"] = "block"
        syncIframe()
    }

    override fun setPageSize(
        pageSize: PrintPageSize,
        orientation: PrintPageOrientation,
        marginMm: Float
    ) {
        currentPageSize = pageSize
        currentOrientation = orientation
        currentMarginMm = marginMm
        nativeWidget.setAttribute("data-page-size", pageSize.cssSize)
        nativeWidget.setAttribute("data-page-orientation", orientation.cssOrientation)
        nativeWidget.setAttribute("data-margin-mm", marginMm.toString())
        syncIframe()
    }

    override fun setDocumentTitle(title: String) {
        currentTitle = title
        nativeWidget.setAttribute("title", title)
        syncIframe()
    }

    override fun addChildNative(childImpl: NativeImpl<*>, index: Int) {
        super.addChildNative(childImpl, index)
        syncIframe()
    }

    override fun removeChildNative(childImpl: NativeImpl<*>) {
        val childDom = childImpl.nativeWidget as? WebDomElement
        if (childDom != null) {
            WebPrintIframeBridge.removeChildFromIframe(nativeWidget, childDom)
        }
        super.removeChildNative(childImpl)
    }

    override fun clearChildrenNative() {
        WebPrintIframeBridge.clearIframeBody(nativeWidget)
        super.clearChildrenNative()
    }

    override fun printBook(
        documentTitle: String,
        pageSize: PrintPageSize,
        orientation: PrintPageOrientation,
        marginMm: Float
    ): Boolean {
        currentTitle = documentTitle
        currentPageSize = pageSize
        currentOrientation = orientation
        currentMarginMm = marginMm
        printCount++
        nativeWidget.setAttribute("data-print-count", printCount.toString())
        return WebPrintIframeBridge.printIframe(
            iframeElement = nativeWidget,
            documentTitle = documentTitle,
            pageSize = pageSize,
            orientation = orientation,
            marginMm = marginMm
        )
    }

    private fun syncIframe() {
        WebPrintIframeBridge.syncIframeDocument(
            iframeElement = nativeWidget,
            documentTitle = currentTitle,
            pageSize = currentPageSize,
            orientation = currentOrientation,
            marginMm = currentMarginMm
        )
    }
}
