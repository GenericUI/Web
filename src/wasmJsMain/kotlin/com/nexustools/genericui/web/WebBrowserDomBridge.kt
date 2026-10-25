@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.nexustools.genericui.web

import com.nexustools.genericui.event.KeyEvent
import com.nexustools.genericui.event.PointerEvent
import com.nexustools.genericui.event.WheelEvent

@JsFun("() => typeof document !== 'undefined' && typeof window !== 'undefined'")
private external fun wasmIsBrowserEnv(): Boolean

@JsFun("(tagName) => document.createElement(tagName)")
private external fun wasmCreateElement(tagName: String): JsAny

@JsFun("(oldPeer) => { " +
    "const doc = document; " +
    "const attach = () => { " +
    "  const loader = doc.querySelector('body > .loader'); " +
    "  if (loader && loader.parentNode) loader.parentNode.removeChild(loader); " +
    "  let el = (doc.getElementsByTagName('genericui:root') && doc.getElementsByTagName('genericui:root').item(0)) || doc.querySelector('genericui\\\\:root'); " +
    "  if (!el && doc.body) { el = doc.createElement('genericui:root'); doc.body.appendChild(el); } " +
    "  if (el) { " +
    "    if (typeof el.hasAttribute === 'function') { " +
    "      if (el.hasAttribute('id')) el.removeAttribute('id'); " +
    "      if (el.hasAttribute('data-bs-theme')) el.removeAttribute('data-bs-theme'); " +
    "    } " +
    "    if (oldPeer && oldPeer !== el) { " +
    "      while (oldPeer.firstChild) { el.appendChild(oldPeer.firstChild); } " +
    "    } " +
    "  } " +
    "  if (window.GenericUiSplash && typeof window.GenericUiSplash.finish === 'function') window.GenericUiSplash.finish(); " +
    "}; " +
    "if (doc.body) attach(); else doc.addEventListener('DOMContentLoaded', attach); " +
    "return (doc.getElementsByTagName('genericui:root') && doc.getElementsByTagName('genericui:root').item(0)) || doc.querySelector('genericui\\\\:root') || oldPeer || doc.createElement('genericui:root'); " +
    "}")
private external fun wasmBindRoot(oldPeer: JsAny?): JsAny

@JsFun("(peer, onClick, onDblClick, onMouseEnter, onMouseLeave, onInput, onChange, onCancel, onWheel, onLoad, onError) => { " +
    "peer.addEventListener('click', () => { onClick(); }); " +
    "peer.addEventListener('dblclick', () => { onDblClick(); }); " +
    "peer.addEventListener('mouseenter', () => { onMouseEnter(); }); " +
    "peer.addEventListener('mouseleave', () => { onMouseLeave(); }); " +
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
    "peer.addEventListener('load', () => { onLoad(); }); " +
    "peer.addEventListener('error', () => { onError(); }); " +
    "}")
private external fun wasmAttachBasicListeners(
    peer: JsAny,
    onClick: () -> Unit,
    onDblClick: () -> Unit,
    onMouseEnter: () -> Unit,
    onMouseLeave: () -> Unit,
    onInput: (String, Boolean) -> Unit,
    onChange: (String, Boolean) -> Unit,
    onCancel: () -> Unit,
    onWheel: (Float, Float, Float, Float) -> Unit,
    onLoad: () -> Unit,
    onError: () -> Unit
)

@JsFun("(peer, hasContextMenuListener, onContextMenu, onPointerDown, onPointerMove, onPointerUp, onKeyDown, onKeyUp) => { " +
    "const ptr = (ev, defBtn, cb) => { " +
    "  cb(ev.clientX || 0, ev.clientY || 0, typeof ev.button === 'number' ? ev.button : defBtn, ev.buttons || 0, !!ev.shiftKey, !!ev.ctrlKey, !!ev.altKey, !!ev.metaKey); " +
    "}; " +
    "let lpTimer = null; let lpFired = false; let startX = 0; let startY = 0; " +
    "const cancelLp = () => { if (lpTimer !== null) { window.clearTimeout(lpTimer); lpTimer = null; } }; " +
    "peer.addEventListener('contextmenu', (ev) => { " +
    "  if (!peer.disabled && hasContextMenuListener()) { " +
    "    if (typeof ev.preventDefault === 'function') ev.preventDefault(); " +
    "    if (typeof ev.stopPropagation === 'function') ev.stopPropagation(); " +
    "    if (lpFired) { lpFired = false; } else { onContextMenu(ev.clientX || 0, ev.clientY || 0); } " +
    "  } " +
    "}); " +
    "peer.addEventListener('touchstart', (ev) => { " +
    "  cancelLp(); lpFired = false; " +
    "  if (!peer.disabled && hasContextMenuListener() && ev.touches && ev.touches.length === 1) { " +
    "    startX = ev.touches[0].clientX || 0; startY = ev.touches[0].clientY || 0; " +
    "    lpTimer = window.setTimeout(() => { " +
    "      lpTimer = null; " +
    "      if (!peer.disabled && hasContextMenuListener()) { lpFired = true; onContextMenu(startX | 0, startY | 0); } " +
    "    }, 500); " +
    "  } " +
    "}); " +
    "peer.addEventListener('touchmove', (ev) => { " +
    "  if (lpTimer !== null) { " +
    "    if (!ev.touches || ev.touches.length !== 1) { cancelLp(); } else { " +
    "      const dx = (ev.touches[0].clientX || 0) - startX; const dy = (ev.touches[0].clientY || 0) - startY; " +
    "      if (dx * dx + dy * dy > 100) cancelLp(); " +
    "    } " +
    "  } " +
    "}); " +
    "peer.addEventListener('touchend', (ev) => { " +
    "  cancelLp(); " +
    "  if (lpFired) { if (typeof ev.preventDefault === 'function') ev.preventDefault(); lpFired = false; } " +
    "}); " +
    "peer.addEventListener('touchcancel', () => { cancelLp(); lpFired = false; }); " +
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
    hasContextMenuListener: () -> Boolean,
    onContextMenu: (Int, Int) -> Unit,
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

@JsFun("(delayMs, block) => { " +
    "if (typeof window !== 'undefined' && typeof window.setTimeout === 'function') { " +
    "  window.setTimeout(() => block(), delayMs); return true; " +
    "} return false; }")
private external fun wasmScheduleTimeout(delayMs: Int, block: () -> Unit): Boolean

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

@JsFun("(parent, child) => { " +
    "const target = (parent.tagName && parent.tagName.toLowerCase() === 'iframe') ? ((parent.contentDocument || (parent.contentWindow && parent.contentWindow.document)) && (parent.contentDocument || parent.contentWindow.document).body) : parent; " +
    "if (target) target.appendChild(child); " +
    "}")
private external fun wasmAppendChild(parent: JsAny, child: JsAny)

@JsFun("(parent, index, child) => { " +
    "const target = (parent.tagName && parent.tagName.toLowerCase() === 'iframe') ? ((parent.contentDocument || (parent.contentWindow && parent.contentWindow.document)) && (parent.contentDocument || parent.contentWindow.document).body) : parent; " +
    "if (!target) return; " +
    "const children = target.children; " +
    "const len = children ? children.length : 0; " +
    "if (index < 0 || index >= len) target.appendChild(child); " +
    "else target.insertBefore(child, children[index]); " +
    "}")
private external fun wasmInsertChildAt(parent: JsAny, index: Int, child: JsAny)

@JsFun("(parent, child) => { " +
    "const target = (parent.tagName && parent.tagName.toLowerCase() === 'iframe') ? ((parent.contentDocument || (parent.contentWindow && parent.contentWindow.document)) && (parent.contentDocument || parent.contentWindow.document).body) : parent; " +
    "if (target && child.parentNode === target) target.removeChild(child); " +
    "}")
private external fun wasmRemoveChild(parent: JsAny, child: JsAny)

@JsFun("(parent) => { " +
    "const target = (parent.tagName && parent.tagName.toLowerCase() === 'iframe') ? ((parent.contentDocument || (parent.contentWindow && parent.contentWindow.document)) && (parent.contentDocument || parent.contentWindow.document).body) : parent; " +
    "if (target) { while (target.firstChild) target.removeChild(target.firstChild); } " +
    "}")
private external fun wasmClearChildren(parent: JsAny)

@JsFun("(peer, fallback) => { const w = peer.clientWidth || peer.offsetWidth || 0; return w > 0 ? w : fallback; }")
private external fun wasmGetClientWidth(peer: JsAny, fallback: Int): Int

@JsFun("(peer, fallback) => { const h = peer.clientHeight || peer.offsetHeight || 0; return h > 0 ? h : fallback; }")
private external fun wasmGetClientHeight(peer: JsAny, fallback: Int): Int

@JsFun("(peer, fallback) => { return typeof peer.offsetLeft === 'number' ? (peer.offsetLeft | 0) : fallback; }")
private external fun wasmGetOffsetLeft(peer: JsAny, fallback: Int): Int

@JsFun("(peer, fallback) => { return typeof peer.offsetTop === 'number' ? (peer.offsetTop | 0) : fallback; }")
private external fun wasmGetOffsetTop(peer: JsAny, fallback: Int): Int

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

@JsFun("() => { try { return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined' && window.localStorage !== null; } catch (e) { return false; } }")
private external fun wasmHasLocalStorage(): Boolean

@JsFun("(key) => { try { return window.localStorage.getItem(key); } catch (e) { return null; } }")
private external fun wasmLocalStorageGetItem(key: String): String?

@JsFun("(key, val) => { try { window.localStorage.setItem(key, val); return true; } catch (e) { return false; } }")
private external fun wasmLocalStorageSetItem(key: String, value: String): Boolean

@JsFun("(key) => { try { window.localStorage.removeItem(key); return true; } catch (e) { return false; } }")
private external fun wasmLocalStorageRemoveItem(key: String): Boolean

@JsFun("() => { try { window.localStorage.clear(); return true; } catch (e) { return false; } }")
private external fun wasmLocalStorageClear(): Boolean

@JsFun("() => { try { return window.localStorage.length || 0; } catch (e) { return 0; } }")
private external fun wasmLocalStorageLength(): Int

@JsFun("(idx) => { try { return window.localStorage.key(idx); } catch (e) { return null; } }")
private external fun wasmLocalStorageKeyAt(idx: Int): String?

@JsFun("(cb) => { if (typeof window !== 'undefined') { window.addEventListener('storage', (ev) => { cb(ev.key || '', ev.key !== null && ev.key !== undefined, ev.newValue || '', ev.newValue !== null && ev.newValue !== undefined); }); } }")
private external fun wasmRegisterStorageListener(
    cb: (String, Boolean, String, Boolean) -> Unit
)

actual object WebBrowserDomBridge {
    actual val isBrowserEnvironment: Boolean
        get() = wasmIsBrowserEnv()

    actual fun createPeer(element: WebDomElement) {
        if (!isBrowserEnvironment) return
        val peer = wasmCreateElement(element.tagName)
        element.browserPeer = peer
        attachInteractiveListeners(element, peer)
    }

    private fun attachInteractiveListeners(model: WebDomElement, peer: JsAny) {
        wasmAttachBasicListeners(
            peer = peer,
            onClick = {
                if (!model.disabled) {
                    model.dispatchFromBrowser("click", null)
                }
            },
            onDblClick = {
                if (!model.disabled) {
                    model.dispatchFromBrowser("dblclick", null)
                }
            },
            onMouseEnter = {
                if (!model.disabled) {
                    model.dispatchFromBrowser("mouseenter", null)
                }
            },
            onMouseLeave = {
                if (!model.disabled) {
                    model.dispatchFromBrowser("mouseleave", null)
                }
            },
            onInput = { v, c ->
                model.updateValueFromBrowser(v)
                model.updateCheckedFromBrowser(c)
                model.dispatchFromBrowser("input", model.value)
            },
            onChange = { v, c ->
                model.updateValueFromBrowser(v)
                model.updateCheckedFromBrowser(c)
                model.dispatchFromBrowser("change", model.value)
            },
            onCancel = {
                model.dispatchFromBrowser("cancel", null)
            },
            onWheel = { dx, dy, cx, cy ->
                model.dispatchFromBrowser("wheel", WheelEvent(dx, dy, cx, cy))
            },
            onLoad = {
                model.dispatchFromBrowser("load", null)
            },
            onError = {
                model.dispatchFromBrowser("error", null)
            }
        )
        wasmAttachInputListeners(
            peer = peer,
            hasContextMenuListener = { "contextmenu" in model.eventListeners },
            onContextMenu = { cx, cy ->
                if ("contextmenu" in model.eventListeners) {
                    model.dispatchEvent("contextmenu", "$cx,$cy")
                }
            },
            onPointerDown = { x, y, btn, btns, shift, ctrl, alt, meta ->
                model.dispatchFromBrowser(
                    "pointerdown",
                    PointerEvent(x = x, y = y, button = btn, buttonsMask = btns, shiftKey = shift, ctrlKey = ctrl, altKey = alt, metaKey = meta)
                )
            },
            onPointerMove = { x, y, btn, btns, shift, ctrl, alt, meta ->
                model.dispatchFromBrowser(
                    "pointermove",
                    PointerEvent(x = x, y = y, button = btn, buttonsMask = btns, shiftKey = shift, ctrlKey = ctrl, altKey = alt, metaKey = meta)
                )
            },
            onPointerUp = { x, y, btn, btns, shift, ctrl, alt, meta ->
                model.dispatchFromBrowser(
                    "pointerup",
                    PointerEvent(x = x, y = y, button = btn, buttonsMask = btns, shiftKey = shift, ctrlKey = ctrl, altKey = alt, metaKey = meta)
                )
            },
            onKeyDown = { key, code, shift, ctrl, alt, meta, repeat ->
                model.dispatchFromBrowser(
                    "keydown",
                    KeyEvent(key = key, code = code, shiftKey = shift, ctrlKey = ctrl, altKey = alt, metaKey = meta, isRepeat = repeat)
                )
            },
            onKeyUp = { key, code, shift, ctrl, alt, meta, repeat ->
                model.dispatchFromBrowser(
                    "keyup",
                    KeyEvent(key = key, code = code, shiftKey = shift, ctrlKey = ctrl, altKey = alt, metaKey = meta, isRepeat = repeat)
                )
            }
        )
    }

    actual fun bindApplicationRoot(element: WebDomElement) {
        if (!isBrowserEnvironment) return
        val oldPeer = element.browserPeer as? JsAny
        element.browserPeer = wasmBindRoot(oldPeer)
    }

    actual fun syncElementTree(element: WebDomElement) {
        if (!isBrowserEnvironment) return
        val parentPeer = element.browserPeer as? JsAny ?: return
        for ((index, child) in element.children.withIndex()) {
            val childPeer = child.browserPeer as? JsAny ?: continue
            wasmInsertChildAt(parentPeer, index, childPeer)
        }
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

    actual fun getOffsetLeft(element: WebDomElement, fallback: Int): Int {
        val peer = element.browserPeer as? JsAny ?: return fallback
        return wasmGetOffsetLeft(peer, fallback)
    }

    actual fun getOffsetTop(element: WebDomElement, fallback: Int): Int {
        val peer = element.browserPeer as? JsAny ?: return fallback
        return wasmGetOffsetTop(peer, fallback)
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

    actual fun scheduleTimeout(delayMs: Int, block: () -> Unit): Boolean =
        if (isBrowserEnvironment) wasmScheduleTimeout(delayMs.coerceAtLeast(0), block) else false

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

    actual val hasLocalStorage: Boolean
        get() = isBrowserEnvironment && wasmHasLocalStorage()

    actual fun localStorageGetItem(key: String): String? =
        if (hasLocalStorage) wasmLocalStorageGetItem(key) else null

    actual fun localStorageSetItem(key: String, value: String): Boolean =
        if (hasLocalStorage) wasmLocalStorageSetItem(key, value) else false

    actual fun localStorageRemoveItem(key: String): Boolean =
        if (hasLocalStorage) wasmLocalStorageRemoveItem(key) else false

    actual fun localStorageClear(): Boolean =
        if (hasLocalStorage) wasmLocalStorageClear() else false

    actual fun localStorageKeys(): List<String> {
        if (!hasLocalStorage) return emptyList()
        val len = wasmLocalStorageLength()
        val out = ArrayList<String>(len)
        for (i in 0 until len) {
            val k = wasmLocalStorageKeyAt(i)
            if (k != null) out.add(k)
        }
        return out
    }

    actual fun registerStorageListener(onStorageChanged: (key: String?, newValue: String?) -> Unit) {
        if (!isBrowserEnvironment) return
        wasmRegisterStorageListener { key, hasKey, newValue, hasNewValue ->
            onStorageChanged(
                if (hasKey) key else null,
                if (hasNewValue) newValue else null
            )
        }
    }
}
