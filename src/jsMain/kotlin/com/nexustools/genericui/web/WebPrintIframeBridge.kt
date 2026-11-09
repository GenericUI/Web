package com.nexustools.genericui.web

import com.nexustools.genericui.enums.document.PrintPageOrientation
import com.nexustools.genericui.enums.document.PrintPageSize

actual object WebPrintIframeBridge {

    actual fun buildIframeStylesheet(
        pageSize: PrintPageSize,
        orientation: PrintPageOrientation,
        marginMm: Float
    ): String {
        val wMm = pageSize.widthMm(orientation)
        val hMm = pageSize.heightMm(orientation)
        return """
            @page { size: ${wMm}mm ${hMm}mm; margin: ${marginMm}mm; }
            * { box-sizing: border-box; -webkit-print-color-adjust: exact !important; print-color-adjust: exact !important; }
            html {
                min-width: 0 !important;
                width: 100% !important;
                height: 100% !important;
                margin: 0 !important;
                padding: 0 !important;
                overflow-x: hidden !important;
                overflow-y: auto !important;
                background: transparent;
            }
            body {
                min-width: 0 !important;
                width: 100% !important;
                height: auto !important;
                min-height: 100% !important;
                margin: 0 !important;
                padding: 0 !important;
                overflow-x: hidden !important;
                overflow-y: visible !important;
                background: transparent !important;
                background-image: none !important;
            }
            .book-page {
                width: ${wMm}mm;
                max-width: 100%;
                min-height: ${hMm}mm;
                margin: 0 auto 16px auto;
                padding: ${marginMm}mm;
                background: #ffffff;
                color: #000000;
                box-shadow: 0 2px 8px rgba(0,0,0,0.18);
                position: relative;
                page-break-after: always;
                break-after: page;
                page-break-inside: avoid;
                break-inside: avoid;
                overflow: visible;
            }
            .book-page:last-child {
                margin-bottom: 0;
                page-break-after: auto;
                break-after: auto;
            }
            .book-page img {
                max-width: 100% !important;
                width: 100% !important;
                height: auto !important;
                display: block !important;
            }
            @media print {
                html, body {
                    min-width: 0 !important;
                    width: 100% !important;
                    height: auto !important;
                    min-height: 0 !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    overflow: visible !important;
                    background: #ffffff !important;
                    background-image: none !important;
                }
                .book-page {
                    width: 100% !important;
                    max-width: 100% !important;
                    height: auto !important;
                    min-height: 0 !important;
                    margin: 0 auto !important;
                    padding: ${marginMm}mm !important;
                    box-shadow: none !important;
                    overflow: visible !important;
                    page-break-after: always !important;
                    break-after: page !important;
                    page-break-inside: avoid !important;
                    break-inside: avoid !important;
                }
                .book-page:last-child {
                    margin-bottom: 0 !important;
                    page-break-after: auto !important;
                    break-after: auto !important;
                }
                .book-page img {
                    max-height: 100vh !important;
                    object-fit: contain !important;
                }
            }
        """.trimIndent()
    }

    actual fun syncIframeDocument(
        iframeElement: WebDomElement,
        documentTitle: String,
        pageSize: PrintPageSize,
        orientation: PrintPageOrientation,
        marginMm: Float,
        styleSheetHrefs: List<String>?
    ) {
        val css = buildIframeStylesheet(pageSize, orientation, marginMm)
        iframeElement.setAttribute("data-iframe-css", css)
        val effectiveHrefs = styleSheetHrefs ?: parseBookStyleSheetHrefs(iframeElement)
        iframeElement.setAttribute("data-book-stylesheets", effectiveHrefs.joinToString("\n"))
        if (iframeElement.getAttribute("title").isNullOrBlank()) {
            iframeElement.setAttribute("title", documentTitle)
        }
        if (!WebBrowserDomBridge.isBrowserEnvironment) return
        syncMountedBookIframe(iframeElement, documentTitle, effectiveHrefs)
    }

    actual fun syncMountedSubtreeIframes(rootElement: WebDomElement) {
        if (!WebBrowserDomBridge.isBrowserEnvironment) return
        if (rootElement.tagName.equals("iframe", ignoreCase = true) &&
            (rootElement.hasClass("book-container") || rootElement.getAttribute("data-iframe-css") != null)
        ) {
            syncMountedBookIframe(rootElement)
        }
        for (child in rootElement.children) {
            syncMountedSubtreeIframes(child)
        }
    }

    private fun parseBookStyleSheetHrefs(iframeElement: WebDomElement): List<String> =
        iframeElement.getAttribute("data-book-stylesheets")
            ?.lines()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

    private fun syncMountedBookIframe(
        iframeElement: WebDomElement,
        explicitTitle: String? = null,
        styleSheetHrefs: List<String>? = null
    ) {
        if (!WebBrowserDomBridge.isBrowserEnvironment) return
        val iframePeer: dynamic = iframeElement.browserPeer ?: return
        val applyToIframe = {
            for (pageElement in iframeElement.children) {
                WebBrowserDomBridge.syncElementTree(pageElement)
            }
            val css = iframeElement.getAttribute("data-iframe-css") ?: ""
            val resolvedTitle = explicitTitle
                ?: iframeElement.getAttribute("title")
                ?: "Document"
            val effectiveHrefs = styleSheetHrefs ?: parseBookStyleSheetHrefs(iframeElement)
            val iframeDoc: dynamic = iframePeer.contentDocument ?: iframePeer.contentWindow?.document
            if (iframeDoc != null && iframeDoc.body != null) {
                iframeDoc.title = resolvedTitle
                val head: dynamic = iframeDoc.head
                if (head != null) {
                    var styleEl: dynamic = iframeDoc.getElementById("genericui-book-print-style")
                    if (styleEl == null) {
                        styleEl = iframeDoc.createElement("style")
                        styleEl.id = "genericui-book-print-style"
                        styleEl.setAttribute("type", "text/css")
                        head.appendChild(styleEl)
                    }
                    styleEl.textContent = css
                    syncBookStylesIntoIframeHead(iframeDoc, head, effectiveHrefs)
                }
                val body: dynamic = iframeDoc.body
                for (pageElement in iframeElement.children) {
                    val childPeer: dynamic = pageElement.browserPeer ?: continue
                    if (childPeer.parentNode !== body) {
                        body.appendChild(childPeer)
                    }
                }
                true
            } else {
                false
            }
        }

        if (!applyToIframe()) {
            WebBrowserDomBridge.schedulePostLayoutPass {
                applyToIframe()
            }
        }
        if (iframePeer.__genericUiLoadBound != true) {
            iframePeer.__genericUiLoadBound = true
            iframePeer.addEventListener("load", { _: dynamic ->
                syncMountedBookIframe(iframeElement)
            })
        }
    }

    private fun syncBookStylesIntoIframeHead(
        iframeDoc: dynamic,
        iframeHead: dynamic,
        styleSheetHrefs: List<String>
    ) {
        val desiredList = styleSheetHrefs.map { it.trim() }.filter { it.isNotEmpty() }
        val existingNodes: dynamic = iframeHead.querySelectorAll("link[data-genericui-book-style='true']")
        val existingLen = (existingNodes?.length as? Number)?.toInt() ?: 0
        val existingByHref = linkedMapOf<String, dynamic>()
        for (i in 0 until existingLen) {
            val linkNode: dynamic = existingNodes[i] ?: continue
            val href = (linkNode.getAttribute("href") as? String)?.trim().orEmpty()
            if (href.isEmpty() || href !in desiredList || href in existingByHref) {
                iframeHead.removeChild(linkNode)
            } else {
                existingByHref[href] = linkNode
            }
        }
        val printStyleEl: dynamic = iframeDoc.getElementById("genericui-book-print-style")
        for (href in desiredList) {
            if (href in existingByHref) continue
            val linkEl: dynamic = iframeDoc.createElement("link")
            linkEl.setAttribute("rel", "stylesheet")
            linkEl.setAttribute("data-genericui-book-style", "true")
            linkEl.setAttribute("href", href)
            if (printStyleEl != null && printStyleEl.parentNode === iframeHead) {
                iframeHead.insertBefore(linkEl, printStyleEl)
            } else {
                iframeHead.appendChild(linkEl)
            }
        }
    }

    actual fun removeChildFromIframe(
        iframeElement: WebDomElement,
        childElement: WebDomElement
    ) {
        if (!WebBrowserDomBridge.isBrowserEnvironment) return
        val iframePeer: dynamic = iframeElement.browserPeer ?: return
        val childPeer: dynamic = childElement.browserPeer ?: return
        val iframeDoc: dynamic = iframePeer.contentDocument ?: iframePeer.contentWindow?.document ?: return
        val body: dynamic = iframeDoc.body ?: return
        if (childPeer.parentNode === body) {
            body.removeChild(childPeer)
        }
    }

    actual fun clearIframeBody(iframeElement: WebDomElement) {
        if (!WebBrowserDomBridge.isBrowserEnvironment) return
        val iframePeer: dynamic = iframeElement.browserPeer ?: return
        val iframeDoc: dynamic = iframePeer.contentDocument ?: iframePeer.contentWindow?.document ?: return
        val body: dynamic = iframeDoc.body ?: return
        while (body.firstChild != null) {
            body.removeChild(body.firstChild)
        }
    }

    actual fun printIframe(
        iframeElement: WebDomElement,
        documentTitle: String,
        pageSize: PrintPageSize,
        orientation: PrintPageOrientation,
        marginMm: Float
    ): Boolean {
        syncIframeDocument(iframeElement, documentTitle, pageSize, orientation, marginMm)
        iframeElement.setAttribute("data-printed", "true")
        iframeElement.setAttribute("data-last-printed-title", documentTitle)
        if (!WebBrowserDomBridge.isBrowserEnvironment) return true

        val iframePeer: dynamic = iframeElement.browserPeer ?: return false
        val contentWin: dynamic = iframePeer.contentWindow ?: return false
        if (js("typeof contentWin.focus === 'function'") as Boolean) {
            contentWin.focus()
        }
        if (js("typeof contentWin.print === 'function'") as Boolean) {
            contentWin.print()
            return true
        }
        return false
    }
}
