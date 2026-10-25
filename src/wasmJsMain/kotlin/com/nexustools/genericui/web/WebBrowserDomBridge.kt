@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.nexustools.genericui.web

import com.nexustools.genericui.event.KeyEvent
import com.nexustools.genericui.event.PointerEvent
import com.nexustools.genericui.event.WheelEvent

@JsFun("() => typeof document !== 'undefined' && typeof window !== 'undefined'")
private external fun wasmIsBrowserEnv(): Boolean

@JsFun("(tagName) => document.createElement(tagName)")
private external fun wasmCreateElement(tagName: String): JsAny

@JsFun("(oldPeer, rootId, theme) => { " +
    "const doc = document; " +
    "const attach = () => { " +
    "  const loader = doc.querySelector('body > .loader'); " +
    "  if (loader && loader.parentNode) loader.parentNode.removeChild(loader); " +
    "  let el = doc.getElementById(rootId); " +
    "  if (!el && doc.body) { el = doc.createElement('div'); el.id = rootId; doc.body.appendChild(el); } " +
    "  if (el) { " +
    "    if (oldPeer && oldPeer !== el) { " +
    "      while (oldPeer.firstChild) { el.appendChild(oldPeer.firstChild); } " +
    "    } " +
    "    el.setAttribute('data-bs-theme', theme); " +
    "  } " +
    "  if (window.GenericUiSplash && typeof window.GenericUiSplash.finish === 'function') window.GenericUiSplash.finish(); " +
    "}; " +
    "if (doc.body) attach(); else doc.addEventListener('DOMContentLoaded', attach); " +
    "return doc.getElementById(rootId) || oldPeer || doc.createElement('div'); " +
    "}")
private external fun wasmBindRoot(oldPeer: JsAny?, rootId: String, theme: String): JsAny

@JsFun("(peer, onClick, onDblClick, onInput, onChange, onCancel, onWheel) => { " +
    "peer.addEventListener('click', () => { onClick(); }); " +
    "peer.addEventListener('dblclick', () => { onDblClick(); }); " +
    "peer.addEventListener('input', () => { " +
    "  const v = typeof peer.value === 'string' ? peer.value : ''; " +
    "  const c = typeof peer.checked === 'boolean' ? peer.checked : false; " +
    "  onInput(v, c); " +
    "}); " +
    "peer.addEventListener('change', () => { " +
    "  const v = typeof peer.value === 'string' ? peer.value : ''; " +
    "  const c = typeof peer.checked === 'boolean' ? peer.checked : false; " +
    "  onChange(v, c); " +
    "}); " +
    "peer.addEventListener('cancel', () => { onCancel(); }); " +
    "peer.addEventListener('wheel', (ev) => { " +
    "  onWheel(ev.deltaX || 0, ev.deltaY || 0, ev.clientX || 0, ev.clientY || 0); " +
    "}); " +
    "}")
private external fun wasmAttachBasicListeners(
    peer: JsAny,
    onClick: () -> Unit,
    onDblClick: () -> Unit,
    onInput: (String, Boolean) -> Unit,
    onChange: (String, Boolean) -> Unit,
    onCancel: () -> Unit,
    onWheel: (Float, Float, Float, Float) -> Unit
)

@JsFun("(peer, onContextMenu, onPointerDown, onPointerMove, onPointerUp, onKeyDown, onKeyUp) => { " +
    "const ptr = (ev, defBtn, cb) => { " +
    "  cb(ev.clientX || 0, ev.clientY || 0, typeof ev.button === 'number' ? ev.button : defBtn, ev.buttons || 0, !!ev.shiftKey, !!ev.ctrlKey, !!ev.altKey, !!ev.metaKey); " +
    "}; " +
    "peer.addEventListener('contextmenu', (ev) => ptr(ev, 2, onContextMenu)); " +
    "peer.addEventListener('pointerdown', (ev) => ptr(ev, 0, onPointerDown)); " +
    "peer.addEventListener('pointermove', (ev) => ptr(ev, 0, onPointerMove)); " +
    "peer.addEventListener('pointerup', (ev) => ptr(ev, 0, onPointerUp)); " +
    "const key = (ev, cb) => { " +
    "  const k = typeof ev.key === 'string' ? ev.key : ''; " +
    "  const c = typeof ev.code === 'string' ? ev.code : k; " +
    "  cb(k, c, !!ev.shiftKey, !!ev.ctrlKey, !!ev.altKey, !!ev.metaKey, !!ev.repeat); " +
    "}; " +
    "peer.addEventListener('keydown', (ev) => key(ev, onKeyDown)); " +
    "peer.addEventListener('keyup', (ev) => key(ev, onKeyUp)); " +
    "}")
private external fun wasmAttachInputListeners(
    peer: JsAny,
    onContextMenu: (Float, Float, Int, Int, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onPointerDown: (Float, Float, Int, Int, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onPointerMove: (Float, Float, Int, Int, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onPointerUp: (Float, Float, Int, Int, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onKeyDown: (String, String, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onKeyUp: (String, String, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
)

@JsFun("(onResize) => { " +
    "window.addEventListener('resize', () => { " +
    "  onResize(window.innerWidth || 1280, window.innerHeight || 720); " +
    "}); " +
    "}")
private external fun wasmRegisterWindowResize(onResize: (Int, Int) -> Unit)

@JsFun("(block) => { " +
    "if (typeof window.requestAnimationFrame === 'function') window.requestAnimationFrame(() => block()); " +
    "else if (typeof window.setTimeout === 'function') window.setTimeout(() => block(), 0); " +
    "else block(); " +
    "}")
private external fun wasmSchedulePostLayout(block: () -> Unit)

@JsFun("(url, method, contentType, hasContentType, body, hasBody, onSuccess, onError) => { " +
    "if (typeof XMLHttpRequest === 'undefined') { onError('XMLHttpRequest is not available'); return; } " +
    "const req = new XMLHttpRequest(); " +
    "req.addEventListener('load', () => { onSuccess(typeof req.responseText === 'string' ? req.responseText : ''); }); " +
    "req.addEventListener('error', () => { onError('Network request failed for ' + url); }); " +
    "req.open(method, url); " +
    "if (hasContentType) req.setRequestHeader('Content-Type', contentType); " +
    "if (hasBody) req.send(body); else req.send(); " +
    "}")
private external fun wasmSendHttpRequest(
    url: String,
    method: String,
    contentType: String,
    hasContentType: Boolean,
    body: String,
    hasBody: Boolean,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
)

@JsFun("(peer, id) => { peer.id = id; }")
private external fun wasmSetId(peer: JsAny, id: String)

@JsFun("(peer, cls) => { peer.className = cls; }")
private external fun wasmSetClassName(peer: JsAny, cls: String)

@JsFun("(peer, prop, val) => { peer.style.setProperty(prop, val); }")
private external fun wasmSetStyle(peer: JsAny, prop: String, value: String)

@JsFun("(peer, prop) => { peer.style.removeProperty(prop); }")
private external fun wasmRemoveStyle(peer: JsAny, prop: String)

@JsFun("(peer) => { peer.removeAttribute('style'); }")
private external fun wasmClearStyles(peer: JsAny)

@JsFun("(peer, name, val) => { peer.setAttribute(name, val); }")
private external fun wasmSetAttr(peer: JsAny, name: String, value: String)

@JsFun("(peer, name) => { peer.removeAttribute(name); }")
private external fun wasmRemoveAttr(peer: JsAny, name: String)

@JsFun("(peer, text) => { peer.textContent = text; }")
private external fun wasmSetTextContent(peer: JsAny, text: String)

@JsFun("(peer, html) => { peer.innerHTML = html; }")
private external fun wasmSetInnerHtml(peer: JsAny, html: String)

@JsFun("(peer, val) => { peer.value = val; }")
private external fun wasmSetValue(peer: JsAny, value: String)

@JsFun("(peer, checked) => { peer.checked = checked; }")
private external fun wasmSetChecked(peer: JsAny, checked: Boolean)

@JsFun("(peer, disabled) => { peer.disabled = disabled; }")
private external fun wasmSetDisabled(peer: JsAny, disabled: Boolean)

@JsFun("(peer, ro) => { peer.readOnly = ro; }")
private external fun wasmSetReadOnly(peer: JsAny, readOnly: Boolean)

@JsFun("(peer, mult) => { peer.multiple = mult; }")
private external fun wasmSetMultiple(peer: JsAny, multiple: Boolean)

@JsFun("(peer, dir) => { peer.webkitdirectory = dir; }")
private external fun wasmSetDirectory(peer: JsAny, directory: Boolean)

@JsFun("(peer, accept) => { peer.accept = accept; }")
private external fun wasmSetAccept(peer: JsAny, accept: String)

@JsFun("(parent, child) => { parent.appendChild(child); }")
private external fun wasmAppendChild(parent: JsAny, child: JsAny)

@JsFun("(parent, index, child) => { " +
    "const children = parent.children; " +
    "const len = children ? children.length : 0; " +
    "if (index < 0 || index >= len) parent.appendChild(child); " +
    "else parent.insertBefore(child, children[index]); " +
    "}")
private external fun wasmInsertChildAt(parent: JsAny, index: Int, child: JsAny)

@JsFun("(parent, child) => { if (child.parentNode === parent) parent.removeChild(child); }")
private external fun wasmRemoveChild(parent: JsAny, child: JsAny)

@JsFun("(parent) => { while (parent.firstChild) parent.removeChild(parent.firstChild); }")
private external fun wasmClearChildren(parent: JsAny)

@JsFun("(peer, fallback) => { const w = peer.clientWidth || peer.offsetWidth || 0; return w > 0 ? w : fallback; }")
private external fun wasmGetClientWidth(peer: JsAny, fallback: Int): Int

@JsFun("(peer, fallback) => { const h = peer.clientHeight || peer.offsetHeight || 0; return h > 0 ? h : fallback; }")
private external fun wasmGetClientHeight(peer: JsAny, fallback: Int): Int

@JsFun("(fallback) => { const w = window.innerWidth || 0; return w > 0 ? w : fallback; }")
private external fun wasmGetViewportWidth(fallback: Int): Int

@JsFun("(fallback) => { const h = window.innerHeight || 0; return h > 0 ? h : fallback; }")
private external fun wasmGetViewportHeight(fallback: Int): Int

@JsFun("(title) => { document.title = title; }")
private external fun wasmSetDocumentTitle(title: String)

@JsFun("(theme) => { if (document.documentElement) document.documentElement.setAttribute('data-bs-theme', theme); }")
private external fun wasmSetDocumentTheme(theme: String)

@JsFun("(peer) => { if (typeof peer.click === 'function') { peer.click(); return true; } return false; }")
private external fun wasmTriggerClick(peer: JsAny): Boolean

@JsFun("(msg) => { if (typeof window.alert === 'function') window.alert(msg); }")
private external fun wasmAlert(msg: String)

@JsFun("(msg) => { return typeof window.confirm === 'function' ? !!window.confirm(msg) : true; }")
private external fun wasmConfirm(msg: String): Boolean

actual object WebBrowserDomBridge {
    actual val isBrowserEnvironment: Boolean
        get() = wasmIsBrowserEnv()

    actual fun createPeer(element: WebDomElement) {
        if (!isBrowserEnvironment) return
        val peer = wasmCreateElement(element.tagName)
        element.browserPeer = peer
        wasmAttachBasicListeners(
            peer = peer,
            onClick = {
                if (!element.disabled) {
                    element.dispatchFromBrowser("click", null)
                }
            },
            onDblClick = {
                if (!element.disabled) {
                    element.dispatchFromBrowser("dblclick", null)
                }
            },
            onInput = { v, c ->
                element.updateValueFromBrowser(v)
                element.updateCheckedFromBrowser(c)
                element.dispatchFromBrowser("input", element.value)
            },
            onChange = { v, c ->
                element.updateValueFromBrowser(v)
                element.updateCheckedFromBrowser(c)
                element.dispatchFromBrowser("change", element.value)
            },
            onCancel = {
                element.dispatchFromBrowser("cancel", null)
            },
            onWheel = { dx, dy, cx, cy ->
                element.dispatchFromBrowser("wheel", WheelEvent(dx, dy, cx, cy))
            }
        )
        wasmAttachInputListeners(
            peer = peer,
            onContextMenu = { x, y, btn, btns, shift, ctrl, alt, meta ->
                element.dispatchFromBrowser(
                    "contextmenu",
                    PointerEvent(x = x, y = y, button = btn, buttonsMask = btns, shiftKey = shift, ctrlKey = ctrl, altKey = alt, metaKey = meta)
                )
            },
            onPointerDown = { x, y, btn, btns, shift, ctrl, alt, meta ->
                element.dispatchFromBrowser(
                    "pointerdown",
                    PointerEvent(x = x, y = y, button = btn, buttonsMask = btns, shiftKey = shift, ctrlKey = ctrl, altKey = alt, metaKey = meta)
                )
            },
            onPointerMove = { x, y, btn, btns, shift, ctrl, alt, meta ->
                element.dispatchFromBrowser(
                    "pointermove",
                    PointerEvent(x = x, y = y, button = btn, buttonsMask = btns, shiftKey = shift, ctrlKey = ctrl, altKey = alt, metaKey = meta)
                )
            },
            onPointerUp = { x, y, btn, btns, shift, ctrl, alt, meta ->
                element.dispatchFromBrowser(
                    "pointerup",
                    PointerEvent(x = x, y = y, button = btn, buttonsMask = btns, shiftKey = shift, ctrlKey = ctrl, altKey = alt, metaKey = meta)
                )
            },
            onKeyDown = { key, code, shift, ctrl, alt, meta, repeat ->
                element.dispatchFromBrowser(
                    "keydown",
                    KeyEvent(key = key, code = code, shiftKey = shift, ctrlKey = ctrl, altKey = alt, metaKey = meta, isRepeat = repeat)
                )
            },
            onKeyUp = { key, code, shift, ctrl, alt, meta, repeat ->
                element.dispatchFromBrowser(
                    "keyup",
                    KeyEvent(key = key, code = code, shiftKey = shift, ctrlKey = ctrl, altKey = alt, metaKey = meta, isRepeat = repeat)
                )
            }
        )
    }

    actual fun bindApplicationRoot(element: WebDomElement) {
        if (!isBrowserEnvironment) return
        val rootId = element.id.ifEmpty { "genericui-app" }
        val theme = element.getAttribute("data-bs-theme") ?: "dark"
        val oldPeer = element.browserPeer as? JsAny
        element.browserPeer = wasmBindRoot(oldPeer, rootId, theme)
    }

    actual fun syncId(element: WebDomElement, id: String) {
        val peer = element.browserPeer as? JsAny ?: return
        wasmSetId(peer, id)
    }

    actual fun syncClassName(element: WebDomElement, className: String) {
        val peer = element.browserPeer as? JsAny ?: return
        wasmSetClassName(peer, className)
    }

    actual fun syncStyle(element: WebDomElement, property: String, value: String?) {
        val peer = element.browserPeer as? JsAny ?: return
        if (value == null) wasmRemoveStyle(peer, property) else wasmSetStyle(peer, property, value)
    }

    actual fun clearStyles(element: WebDomElement) {
        val peer = element.browserPeer as? JsAny ?: return
        wasmClearStyles(peer)
    }

    actual fun syncAttribute(element: WebDomElement, name: String, value: String?) {
        val peer = element.browserPeer as? JsAny ?: return
        if (value == null) wasmRemoveAttr(peer, name) else wasmSetAttr(peer, name, value)
    }

    actual fun syncTextContent(element: WebDomElement, text: String) {
        val peer = element.browserPeer as? JsAny ?: return
        if (element.children.isEmpty()) wasmSetTextContent(peer, text)
    }

    actual fun syncInnerHTML(element: WebDomElement, html: String) {
        val peer = element.browserPeer as? JsAny ?: return
        wasmSetInnerHtml(peer, html)
    }

    actual fun syncValue(element: WebDomElement, value: String) {
        val peer = element.browserPeer as? JsAny ?: return
        wasmSetValue(peer, value)
    }

    actual fun syncChecked(element: WebDomElement, checked: Boolean) {
        val peer = element.browserPeer as? JsAny ?: return
        wasmSetChecked(peer, checked)
    }

    actual fun syncDisabled(element: WebDomElement, disabled: Boolean) {
        val peer = element.browserPeer as? JsAny ?: return
        wasmSetDisabled(peer, disabled)
    }

    actual fun syncReadOnly(element: WebDomElement, readOnly: Boolean) {
        val peer = element.browserPeer as? JsAny ?: return
        wasmSetReadOnly(peer, readOnly)
    }

    actual fun syncMultiple(element: WebDomElement, multiple: Boolean) {
        val peer = element.browserPeer as? JsAny ?: return
        wasmSetMultiple(peer, multiple)
    }

    actual fun syncDirectory(element: WebDomElement, directory: Boolean) {
        val peer = element.browserPeer as? JsAny ?: return
        wasmSetDirectory(peer, directory)
    }

    actual fun syncAccept(element: WebDomElement, accept: String) {
        val peer = element.browserPeer as? JsAny ?: return
        wasmSetAccept(peer, accept)
    }

    actual fun appendChild(parent: WebDomElement, child: WebDomElement) {
        val parentPeer = parent.browserPeer as? JsAny ?: return
        val childPeer = child.browserPeer as? JsAny ?: return
        wasmAppendChild(parentPeer, childPeer)
    }

    actual fun insertChildAt(parent: WebDomElement, index: Int, child: WebDomElement) {
        val parentPeer = parent.browserPeer as? JsAny ?: return
        val childPeer = child.browserPeer as? JsAny ?: return
        wasmInsertChildAt(parentPeer, index, childPeer)
    }

    actual fun removeChild(parent: WebDomElement, child: WebDomElement) {
        val parentPeer = parent.browserPeer as? JsAny ?: return
        val childPeer = child.browserPeer as? JsAny ?: return
        wasmRemoveChild(parentPeer, childPeer)
    }

    actual fun clearChildren(parent: WebDomElement) {
        val parentPeer = parent.browserPeer as? JsAny ?: return
        wasmClearChildren(parentPeer)
    }

    actual fun getClientWidth(element: WebDomElement, fallback: Int): Int {
        val peer = element.browserPeer as? JsAny ?: return fallback
        return wasmGetClientWidth(peer, fallback)
    }

    actual fun getClientHeight(element: WebDomElement, fallback: Int): Int {
        val peer = element.browserPeer as? JsAny ?: return fallback
        return wasmGetClientHeight(peer, fallback)
    }

    actual fun getViewportWidth(fallback: Int): Int =
        if (isBrowserEnvironment) wasmGetViewportWidth(fallback) else fallback

    actual fun getViewportHeight(fallback: Int): Int =
        if (isBrowserEnvironment) wasmGetViewportHeight(fallback) else fallback

    actual fun setDocumentTitle(title: String) {
        if (isBrowserEnvironment) wasmSetDocumentTitle(title)
    }

    actual fun setDocumentTheme(theme: String) {
        if (isBrowserEnvironment) wasmSetDocumentTheme(theme)
    }

    actual fun triggerClick(element: WebDomElement): Boolean {
        if (!isBrowserEnvironment) return false
        val peer = element.browserPeer as? JsAny ?: return false
        return wasmTriggerClick(peer)
    }

    actual fun registerWindowResize(onResize: (Int, Int) -> Unit) {
        if (!isBrowserEnvironment) return
        wasmRegisterWindowResize(onResize)
    }

    actual fun schedulePostLayoutPass(block: () -> Unit) {
        if (!isBrowserEnvironment) return
        wasmSchedulePostLayout(block)
    }

    actual fun showAlert(message: String) {
        if (isBrowserEnvironment) wasmAlert(message)
    }

    actual fun showConfirm(message: String): Boolean =
        if (isBrowserEnvironment) wasmConfirm(message) else true

    actual fun showPrompt(message: String, defaultValue: String): String? = defaultValue

    actual fun sendHttpRequest(
        url: String,
        method: String,
        contentType: String?,
        body: String?,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isBrowserEnvironment) {
            onError("Not running in a browser environment")
            return
        }
        wasmSendHttpRequest(
            url = url,
            method = method,
            contentType = contentType ?: "",
            hasContentType = contentType != null,
            body = body ?: "",
            hasBody = body != null,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
