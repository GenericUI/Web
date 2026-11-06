@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.nexustools.genericui.web

import com.nexustools.genericui.enums.document.PrintPageOrientation
import com.nexustools.genericui.enums.document.PrintPageSize

@JsFun("(iframePeer, title, css) => { " +
    "const doc = iframePeer.contentDocument || (iframePeer.contentWindow && iframePeer.contentWindow.document); " +
    "if (!doc || !doc.body) return false; " +
    "doc.title = title; " +
    "const head = doc.head; " +
    "if (head) { " +
    "  let styleEl = doc.getElementById('genericui-book-print-style'); " +
    "  if (!styleEl) { " +
    "    styleEl = doc.createElement('style'); " +
    "    styleEl.id = 'genericui-book-print-style'; " +
    "    styleEl.setAttribute('type', 'text/css'); " +
    "    head.appendChild(styleEl); " +
    "  } " +
    "  styleEl.textContent = css; " +
    "  if (!head.__genericUiHostStylesCopied && document && document.head) { " +
    "    const nodes = document.head.querySelectorAll(\"link[rel='stylesheet'], style\"); " +
    "    for (let i = 0; i < nodes.length; i++) { " +
    "      const clone = nodes[i].cloneNode(true); " +
    "      clone.setAttribute('data-genericui-cloned', 'true'); " +
    "      head.insertBefore(clone, styleEl); " +
    "    } " +
    "    head.__genericUiHostStylesCopied = true; " +
    "  } " +
    "} " +
    "return true; " +
    "}")
private external fun wasmPrepareIframeDoc(iframePeer: JsAny, title: String, css: String): Boolean

@JsFun("(iframePeer, childPeer) => { " +
    "const doc = iframePeer.contentDocument || (iframePeer.contentWindow && iframePeer.contentWindow.document); " +
    "if (doc && doc.body && childPeer.parentNode !== doc.body) { " +
    "  doc.body.appendChild(childPeer); " +
    "} " +
    "}")
private external fun wasmAppendChildToIframeBody(iframePeer: JsAny, childPeer: JsAny)

@JsFun("(iframePeer, childPeer) => { " +
    "const doc = iframePeer.contentDocument || (iframePeer.contentWindow && iframePeer.contentWindow.document); " +
    "if (doc && doc.body && childPeer.parentNode === doc.body) { " +
    "  doc.body.removeChild(childPeer); " +
    "} " +
    "}")
private external fun wasmRemoveChildFromIframeBody(iframePeer: JsAny, childPeer: JsAny)

@JsFun("(iframePeer) => { " +
    "const doc = iframePeer.contentDocument || (iframePeer.contentWindow && iframePeer.contentWindow.document); " +
    "if (doc && doc.body) { " +
    "  while (doc.body.firstChild) doc.body.removeChild(doc.body.firstChild); " +
    "} " +
    "}")
private external fun wasmClearIframeBody(iframePeer: JsAny)

@JsFun("(iframePeer, onLoad) => { " +
    "if (!iframePeer.__genericUiLoadBound) { " +
    "  iframePeer.__genericUiLoadBound = true; " +
    "  iframePeer.addEventListener('load', () => { onLoad(); }); " +
    "} " +
    "}")
private external fun wasmBindIframeLoadOnce(iframePeer: JsAny, onLoad: () -> Unit)

@JsFun("(iframePeer) => { " +
    "const win = iframePeer.contentWindow; " +
    "if (!win) return false; " +
    "if (typeof win.focus === 'function') win.focus(); " +
    "if (typeof win.print === 'function') { win.print(); return true; } " +
    "return false; " +
    "}")
private external fun wasmPrintIframeWindow(iframePeer: JsAny): Boolean

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
        val iframePeer = iframeElement.browserPeer as? JsAny ?: return
        val applyToIframe = {
            for (pageElement in iframeElement.children) {
                WebBrowserDomBridge.syncElementTree(pageElement)
            }
            val css = iframeElement.getAttribute("data-iframe-css") ?: ""
            val resolvedTitle = explicitTitle
                ?: iframeElement.getAttribute("title")
                ?: "Document"
            val ready = wasmPrepareIframeDoc(iframePeer, resolvedTitle, css)
            if (ready) {
                for (pageElement in iframeElement.children) {
                    val childPeer = pageElement.browserPeer as? JsAny ?: continue
                    wasmAppendChildToIframeBody(iframePeer, childPeer)
                }
            }
            ready
        }

        if (!applyToIframe()) {
            WebBrowserDomBridge.schedulePostLayoutPass {
                applyToIframe()
            }
        }
        wasmBindIframeLoadOnce(iframePeer) {
            syncMountedBookIframe(iframeElement)
        }
    }

    actual fun removeChildFromIframe(
        iframeElement: WebDomElement,
        childElement: WebDomElement
    ) {
        if (!WebBrowserDomBridge.isBrowserEnvironment) return
        val iframePeer = iframeElement.browserPeer as? JsAny ?: return
        val childPeer = childElement.browserPeer as? JsAny ?: return
        wasmRemoveChildFromIframeBody(iframePeer, childPeer)
    }

    actual fun clearIframeBody(iframeElement: WebDomElement) {
        if (!WebBrowserDomBridge.isBrowserEnvironment) return
        val iframePeer = iframeElement.browserPeer as? JsAny ?: return
        wasmClearIframeBody(iframePeer)
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

        val iframePeer = iframeElement.browserPeer as? JsAny ?: return false
        return wasmPrintIframeWindow(iframePeer)
    }
}
