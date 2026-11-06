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
            html, body { margin: 0; padding: 0; width: 100%; background: transparent; }
            .book-page {
                width: 100%;
                max-width: 100%;
                margin: 0 auto 16px auto;
                padding: 0;
                background: #ffffff;
                color: #000000;
                position: relative;
                page-break-after: always;
                break-after: page;
                overflow: hidden;
            }
            .book-page:last-child {
                margin-bottom: 0;
                page-break-after: auto;
                break-after: auto;
            }
            .book-page img {
                max-width: 100%;
                width: 100%;
                height: auto;
                display: block;
            }
            @media print {
                html, body { background: #ffffff; }
                .book-page {
                    width: ${wMm}mm;
                    min-height: ${hMm}mm;
                    margin: 0 auto;
                    padding: ${marginMm}mm;
                    box-shadow: none !important;
                }
            }
        """.trimIndent()
    }

    actual fun syncIframeDocument(
        iframeElement: WebDomElement,
        documentTitle: String,
        pageSize: PrintPageSize,
        orientation: PrintPageOrientation,
        marginMm: Float
    ) {
        val css = buildIframeStylesheet(pageSize, orientation, marginMm)
        iframeElement.setAttribute("data-iframe-css", css)
        if (iframeElement.getAttribute("title").isNullOrBlank()) {
            iframeElement.setAttribute("title", documentTitle)
        }
        if (!WebBrowserDomBridge.isBrowserEnvironment) return
        syncMountedBookIframe(iframeElement, documentTitle)
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

    private fun syncMountedBookIframe(
        iframeElement: WebDomElement,
        explicitTitle: String? = null
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
                    syncHostStylesIntoIframeHead(iframeDoc, head)
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

    private fun syncHostStylesIntoIframeHead(iframeDoc: dynamic, iframeHead: dynamic) {
        if (iframeHead.__genericUiHostStylesCopied == true) return
        val hostDoc: dynamic = js("document")
        val hostHead: dynamic = hostDoc.head ?: return
        val styleNodes: dynamic = hostHead.querySelectorAll("link[rel='stylesheet'], style")
        val len = (styleNodes?.length as? Number)?.toInt() ?: 0
        for (i in 0 until len) {
            val node: dynamic = styleNodes[i] ?: continue
            val clone: dynamic = node.cloneNode(true)
            clone.setAttribute("data-genericui-cloned", "true")
            iframeHead.insertBefore(clone, iframeDoc.getElementById("genericui-book-print-style"))
        }
        iframeHead.__genericUiHostStylesCopied = true
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
