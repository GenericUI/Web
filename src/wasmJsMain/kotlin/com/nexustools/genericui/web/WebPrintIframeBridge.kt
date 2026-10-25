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

        val iframePeer = iframeElement.browserPeer as? JsAny ?: return
        val applyToIframe = {
            val ready = wasmPrepareIframeDoc(iframePeer, documentTitle, css)
            if (ready) {
                for (child in iframeElement.children) {
                    val childPeer = child.browserPeer as? JsAny ?: continue
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
            applyToIframe()
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
