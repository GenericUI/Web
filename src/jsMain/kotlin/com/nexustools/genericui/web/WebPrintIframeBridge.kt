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
        return "html,body{margin:0;padding:0;box-sizing:border-box;background:#ffffff;color:#000000;" +
            "-webkit-print-color-adjust:exact;print-color-adjust:exact}" +
            "*{box-sizing:border-box}" +
            ".book-page{width:${wMm}mm;min-height:${hMm}mm;padding:${marginMm}mm;margin:0 auto;" +
            "background:#ffffff;color:#000000;position:relative;page-break-after:always;break-after:page}" +
            ".book-page:last-child{page-break-after:auto;break-after:auto}" +
            "canvas{width:100%;height:200%}" +
            "@media print{@page{size:${pageSize.cssSize} ${orientation.cssOrientation};margin:${marginMm}mm}" +
            "html,body{margin:0;padding:0;background:#ffffff}" +
            ".book-page{margin:0;box-shadow:none;width:${wMm}mm;height:${hMm}mm;page-break-after:always;break-after:page}" +
            ".book-page:last-child{page-break-after:auto;break-after:auto}" +
            "canvas{width:${wMm}mm;height:${hMm}mm}}"
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
        if (!WebBrowserDomBridge.isBrowserEnvironment) return

        val iframePeer: dynamic = iframeElement.browserPeer ?: return
        val applyToIframe = {
            val iframeDoc: dynamic = iframePeer.contentDocument ?: iframePeer.contentWindow?.document
            if (iframeDoc != null && iframeDoc.body != null) {
                iframeDoc.title = documentTitle
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
                for (child in iframeElement.children) {
                    val childPeer: dynamic = child.browserPeer ?: continue
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
                applyToIframe()
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
