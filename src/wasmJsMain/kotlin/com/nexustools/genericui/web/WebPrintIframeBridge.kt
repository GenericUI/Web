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
    "} " +
    "return true; " +
    "}")
private external fun wasmPrepareIframeDoc(iframePeer: JsAny, title: String, css: String): Boolean

@JsFun("(iframePeer, joinedHrefs) => { " +
    "const doc = iframePeer.contentDocument || (iframePeer.contentWindow && iframePeer.contentWindow.document); " +
    "if (!doc || !doc.head) return; " +
    "const head = doc.head; " +
    "const desired = joinedHrefs ? joinedHrefs.split('\\n').map(s => s.trim()).filter(s => s.length > 0) : []; " +
    "const existingNodes = head.querySelectorAll(\"link[data-genericui-book-style='true']\"); " +
    "const existingByHref = new Map(); " +
    "for (let i = 0; i < existingNodes.length; i++) { " +
    "  const node = existingNodes[i]; " +
    "  const href = (node.getAttribute('href') || '').trim(); " +
    "  if (!href || desired.indexOf(href) === -1 || existingByHref.has(href)) { " +
    "    head.removeChild(node); " +
    "  } else { " +
    "    existingByHref.set(href, node); " +
    "  } " +
    "} " +
    "const printStyleEl = doc.getElementById('genericui-book-print-style'); " +
    "for (let i = 0; i < desired.length; i++) { " +
    "  const href = desired[i]; " +
    "  if (existingByHref.has(href)) continue; " +
    "  const linkEl = doc.createElement('link'); " +
    "  linkEl.setAttribute('rel', 'stylesheet'); " +
    "  linkEl.setAttribute('data-genericui-book-style', 'true'); " +
    "  linkEl.setAttribute('href', href); " +
    "  if (printStyleEl && printStyleEl.parentNode === head) { " +
    "    head.insertBefore(linkEl, printStyleEl); " +
    "  } else { " +
    "    head.appendChild(linkEl); " +
    "  } " +
    "} " +
    "}")
private external fun wasmSyncBookStyleSheets(iframePeer: JsAny, joinedHrefs: String)

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
        val iframePeer = iframeElement.browserPeer as? JsAny ?: return
        val applyToIframe = {
            for (pageElement in iframeElement.children) {
                WebBrowserDomBridge.syncElementTree(pageElement)
            }
            val css = iframeElement.getAttribute("data-iframe-css") ?: ""
            val resolvedTitle = explicitTitle
                ?: iframeElement.getAttribute("title")
                ?: "Document"
            val effectiveHrefs = styleSheetHrefs ?: parseBookStyleSheetHrefs(iframeElement)
            val ready = wasmPrepareIframeDoc(iframePeer, resolvedTitle, css)
            if (ready) {
                wasmSyncBookStyleSheets(iframePeer, effectiveHrefs.joinToString("\n"))
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
