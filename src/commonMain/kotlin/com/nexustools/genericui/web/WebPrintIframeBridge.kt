package com.nexustools.genericui.web

import com.nexustools.genericui.enums.document.PrintPageOrientation
import com.nexustools.genericui.enums.document.PrintPageSize

/**
 * Multiplatform bridge managing the onscreen `<iframe>` element's `contentDocument` and `contentWindow`
 * for [WebBookContainerImpl].
 *
 * Mounts child [com.nexustools.genericui.component.document.BookPage] DOM elements directly inside the
 * onscreen `<iframe>`'s `contentDocument.body`, injects `@page` size/orientation and sheet preview CSS
 * into `contentDocument.head`, and invokes `contentWindow.print()` when [printIframe] is called.
 */
expect object WebPrintIframeBridge {
    fun buildIframeStylesheet(
        pageSize: PrintPageSize,
        orientation: PrintPageOrientation,
        marginMm: Float
    ): String

    fun syncIframeDocument(
        iframeElement: WebDomElement,
        documentTitle: String,
        pageSize: PrintPageSize,
        orientation: PrintPageOrientation,
        marginMm: Float
    )

    fun removeChildFromIframe(
        iframeElement: WebDomElement,
        childElement: WebDomElement
    )

    fun clearIframeBody(iframeElement: WebDomElement)

    fun printIframe(
        iframeElement: WebDomElement,
        documentTitle: String,
        pageSize: PrintPageSize,
        orientation: PrintPageOrientation,
        marginMm: Float
    ): Boolean
}
