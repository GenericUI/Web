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

@JsFun("(delayMs, block) => { " +
    "if (typeof window !== 'undefined' && typeof window.setTimeout === 'function') { " +
    "  return window.setTimeout(() => block(), delayMs) | 0; " +
    "} return -1; }")
private external fun wasmSetTimeoutId(delayMs: Int, block: () -> Unit): Int

@JsFun("(intervalMs, block) => { " +
    "if (typeof window !== 'undefined' && typeof window.setInterval === 'function') { " +
    "  return window.setInterval(() => block(), intervalMs) | 0; " +
    "} return -1; }")
private external fun wasmSetIntervalId(intervalMs: Int, block: () -> Unit): Int

@JsFun("(id) => { " +
    "if (typeof window !== 'undefined' && typeof window.clearTimeout === 'function') { " +
    "  window.clearTimeout(id); return true; " +
    "} return false; }")
private external fun wasmClearTimeoutId(id: Int): Boolean

@JsFun("(id) => { " +
    "if (typeof window !== 'undefined' && typeof window.clearInterval === 'function') { " +
    "  window.clearInterval(id); return true; " +
    "} return false; }")
private external fun wasmClearIntervalId(id: Int): Boolean

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

@JsFun("(peer, prop, val, priority) => { peer.style.setProperty(prop, val, priority); }")
private external fun wasmSetStyle(peer: JsAny, prop: String, value: String, priority: String)

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

@JsFun("(theme) => { if (document.documentElement) document.documentElement.setAttribute('data-bs-theme', theme); if (document.body) document.body.setAttribute('data-bs-theme', theme); }")
private external fun wasmSyncBootstrapTheme(theme: String)

@JsFun("(id, href) => { const head = document.head; if (!head) return; let link = document.getElementById(id); if (!link) { link = document.createElement('link'); link.id = id; link.setAttribute('rel', 'stylesheet'); link.setAttribute('href', href); head.appendChild(link); } else { link.setAttribute('href', href); } }")
private external fun wasmAddStyleSheetLink(id: String, href: String)

@JsFun("(id) => { const link = document.getElementById(id); if (link && link.parentNode) link.parentNode.removeChild(link); }")
private external fun wasmRemoveStyleSheetLink(id: String)

@JsFun("() => { if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return true; const mql = window.matchMedia('(prefers-color-scheme: dark)'); return mql ? !!mql.matches : true; }")
private external fun wasmQueryDarkMode(): Boolean

@JsFun("(cb) => { if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return; const mql = window.matchMedia('(prefers-color-scheme: dark)'); if (mql && typeof mql.addEventListener === 'function') { mql.addEventListener('change', (ev) => { cb(ev ? !!ev.matches : true); }); } }")
private external fun wasmObserveDarkMode(cb: (Boolean) -> Unit)

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

@JsFun("() => { try { const h = (typeof window !== 'undefined' && window.location && window.location.hash) ? window.location.hash : ''; return h.startsWith('#') ? h.substring(1) : h; } catch (e) { return ''; } }")
private external fun wasmGetLocationHash(): String

@JsFun("(hash) => { try { if (typeof window !== 'undefined' && window.location) { window.location.hash = hash ? ('#' + hash) : ''; } } catch (e) {} }")
private external fun wasmSetLocationHash(hash: String)

@JsFun("(routeHash, historyIndex, title, stateJson, hasStateJson) => { try { if (typeof window !== 'undefined' && window.history && typeof window.history.pushState === 'function') { const st = { genericUiRoute: routeHash, genericUiIndex: historyIndex, genericUiStateJson: hasStateJson ? stateJson : null }; const url = routeHash ? ('#' + routeHash) : '#'; window.history.pushState(st, title, url); if (title && typeof document !== 'undefined') document.title = title; } } catch (e) {} }")
private external fun wasmHistoryPushState(
    routeHash: String,
    historyIndex: Int,
    title: String,
    stateJson: String,
    hasStateJson: Boolean
)

@JsFun("(routeHash, historyIndex, title, stateJson, hasStateJson) => { try { if (typeof window !== 'undefined' && window.history && typeof window.history.replaceState === 'function') { const st = { genericUiRoute: routeHash, genericUiIndex: historyIndex, genericUiStateJson: hasStateJson ? stateJson : null }; const url = routeHash ? ('#' + routeHash) : '#'; window.history.replaceState(st, title, url); if (title && typeof document !== 'undefined') document.title = title; } } catch (e) {} }")
private external fun wasmHistoryReplaceState(
    routeHash: String,
    historyIndex: Int,
    title: String,
    stateJson: String,
    hasStateJson: Boolean
)

@JsFun("() => { try { if (typeof window !== 'undefined' && window.history && typeof window.history.back === 'function') { window.history.back(); return true; } } catch (e) {} return false; }")
private external fun wasmHistoryBack(): Boolean

@JsFun("() => { try { if (typeof window !== 'undefined' && window.history && typeof window.history.forward === 'function') { window.history.forward(); return true; } } catch (e) {} return false; }")
private external fun wasmHistoryForward(): Boolean

@JsFun("() => { try { const st = (typeof window !== 'undefined' && window.history) ? window.history.state : null; return st && typeof st.genericUiIndex === 'number'; } catch (e) { return false; } }")
private external fun wasmHasCurrentHistoryIndex(): Boolean

@JsFun("() => { try { const st = (typeof window !== 'undefined' && window.history) ? window.history.state : null; return (st && typeof st.genericUiIndex === 'number') ? (st.genericUiIndex | 0) : 0; } catch (e) { return 0; } }")
private external fun wasmGetCurrentHistoryIndex(): Int

@JsFun("() => { try { const st = (typeof window !== 'undefined' && window.history) ? window.history.state : null; return (st && typeof st.genericUiStateJson === 'string') ? st.genericUiStateJson : null; } catch (e) { return null; } }")
private external fun wasmGetCurrentHistoryStateJson(): String?

@JsFun("(cb) => { if (typeof window !== 'undefined') { window.addEventListener('popstate', (ev) => { const st = ev && ev.state ? ev.state : null; const rawHash = (window.location && window.location.hash) ? window.location.hash : ''; const fallbackRoute = rawHash.startsWith('#') ? rawHash.substring(1) : rawHash; const route = (st && typeof st.genericUiRoute === 'string') ? st.genericUiRoute : fallbackRoute; const hasIdx = !!(st && typeof st.genericUiIndex === 'number'); const idx = hasIdx ? (st.genericUiIndex | 0) : 0; const hasJson = !!(st && typeof st.genericUiStateJson === 'string'); const json = hasJson ? st.genericUiStateJson : ''; cb(route, hasIdx, idx, hasJson, json); }); } }")
private external fun wasmRegisterPopStateListener(
    cb: (String, Boolean, Int, Boolean, String) -> Unit
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
        wasmSetClassName(peer, element.getEffectiveDomClassName())
    }

    actual fun syncStyle(element: WebDomElement, property: String, value: String?, important: Boolean) {
        val peer = element.browserPeer as? JsAny ?: return
        if (value == null) {
            wasmRemoveStyle(peer, property)
        } else {
            wasmSetStyle(peer, property, value, if (important) "important" else "")
        }
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

    actual fun syncBootstrapTheme(themeCssValue: String) {
        if (isBrowserEnvironment) wasmSyncBootstrapTheme(themeCssValue)
    }

    actual fun addStyleSheetLink(id: String, href: String) {
        if (isBrowserEnvironment) wasmAddStyleSheetLink(id, href)
    }

    actual fun removeStyleSheetLink(id: String) {
        if (isBrowserEnvironment) wasmRemoveStyleSheetLink(id)
    }

    actual fun isBrowserDarkMode(onSystemThemeChanged: ((Boolean) -> Unit)?): Boolean {
        if (!isBrowserEnvironment) return true
        if (onSystemThemeChanged != null) {
            wasmObserveDarkMode(onSystemThemeChanged)
        }
        return wasmQueryDarkMode()
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

    actual fun setTimeoutId(delayMs: Int, block: () -> Unit): Int =
        if (isBrowserEnvironment) wasmSetTimeoutId(delayMs.coerceAtLeast(0), block) else -1

    actual fun setIntervalId(intervalMs: Int, block: () -> Unit): Int =
        if (isBrowserEnvironment) wasmSetIntervalId(intervalMs.coerceAtLeast(1), block) else -1

    actual fun clearTimeoutId(timerId: Int): Boolean =
        if (isBrowserEnvironment) wasmClearTimeoutId(timerId) else false

    actual fun clearIntervalId(timerId: Int): Boolean =
        if (isBrowserEnvironment) wasmClearIntervalId(timerId) else false

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

    private val headlessHistoryEntries = mutableListOf<Triple<String, Int, String?>>()
    private var headlessHistoryCursor: Int = -1
    private var headlessCurrentHash: String = ""
    private val headlessPopStateListeners = mutableListOf<(String, Int?, String?) -> Unit>()

    actual fun getLocationHash(): String {
        if (!isBrowserEnvironment) return headlessCurrentHash
        return wasmGetLocationHash()
    }

    actual fun setLocationHash(hash: String) {
        val clean = hash.removePrefix("#")
        headlessCurrentHash = clean
        if (!isBrowserEnvironment) {
            val state = getCurrentHistoryState()
            for (listener in headlessPopStateListeners.toList()) {
                listener(clean, state.first, state.second)
            }
            return
        }
        wasmSetLocationHash(clean)
    }

    actual fun historyPushState(routeHash: String, historyIndex: Int, title: String, stateJson: String?) {
        val clean = routeHash.removePrefix("#")
        headlessCurrentHash = clean
        while (headlessHistoryEntries.size > headlessHistoryCursor + 1 && headlessHistoryEntries.isNotEmpty()) {
            headlessHistoryEntries.removeAt(headlessHistoryEntries.lastIndex)
        }
        headlessHistoryEntries.add(Triple(clean, historyIndex, stateJson))
        headlessHistoryCursor = headlessHistoryEntries.lastIndex
        if (!isBrowserEnvironment) return
        wasmHistoryPushState(
            routeHash = clean,
            historyIndex = historyIndex,
            title = title,
            stateJson = stateJson ?: "",
            hasStateJson = stateJson != null
        )
    }

    actual fun historyReplaceState(routeHash: String, historyIndex: Int, title: String, stateJson: String?) {
        val clean = routeHash.removePrefix("#")
        headlessCurrentHash = clean
        if (headlessHistoryCursor in headlessHistoryEntries.indices) {
            headlessHistoryEntries[headlessHistoryCursor] = Triple(clean, historyIndex, stateJson)
        } else {
            headlessHistoryEntries.add(Triple(clean, historyIndex, stateJson))
            headlessHistoryCursor = headlessHistoryEntries.lastIndex
        }
        if (!isBrowserEnvironment) return
        wasmHistoryReplaceState(
            routeHash = clean,
            historyIndex = historyIndex,
            title = title,
            stateJson = stateJson ?: "",
            hasStateJson = stateJson != null
        )
    }

    actual fun historyBack(): Boolean {
        if (!isBrowserEnvironment) {
            if (headlessHistoryCursor <= 0) return false
            headlessHistoryCursor -= 1
            val entry = headlessHistoryEntries[headlessHistoryCursor]
            headlessCurrentHash = entry.first
            for (listener in headlessPopStateListeners.toList()) {
                listener(entry.first, entry.second, entry.third)
            }
            return true
        }
        return wasmHistoryBack()
    }

    actual fun historyForward(): Boolean {
        if (!isBrowserEnvironment) {
            if (headlessHistoryCursor + 1 >= headlessHistoryEntries.size) return false
            headlessHistoryCursor += 1
            val entry = headlessHistoryEntries[headlessHistoryCursor]
            headlessCurrentHash = entry.first
            for (listener in headlessPopStateListeners.toList()) {
                listener(entry.first, entry.second, entry.third)
            }
            return true
        }
        return wasmHistoryForward()
    }

    actual fun getCurrentHistoryState(): Pair<Int?, String?> {
        if (!isBrowserEnvironment) {
            val entry = headlessHistoryEntries.getOrNull(headlessHistoryCursor) ?: return null to null
            return entry.second to entry.third
        }
        val idx = if (wasmHasCurrentHistoryIndex()) wasmGetCurrentHistoryIndex() else null
        val json = wasmGetCurrentHistoryStateJson()
        if (idx == null && json == null) {
            val entry = headlessHistoryEntries.getOrNull(headlessHistoryCursor)
            return (entry?.second) to (entry?.third)
        }
        return idx to json
    }

    actual fun registerPopStateListener(onPopState: (routeHash: String, historyIndex: Int?, stateJson: String?) -> Unit) {
        headlessPopStateListeners.add(onPopState)
        if (!isBrowserEnvironment) return
        wasmRegisterPopStateListener { route, hasIdx, idx, hasJson, json ->
            headlessCurrentHash = route
            onPopState(
                route,
                if (hasIdx) idx else null,
                if (hasJson) json else null
            )
        }
    }
}
