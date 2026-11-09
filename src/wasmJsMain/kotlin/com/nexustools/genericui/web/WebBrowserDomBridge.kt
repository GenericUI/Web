@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.nexustools.genericui.web

import com.nexustools.genericui.component.dialog.SelectedFile
import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.event.KeyEvent
import com.nexustools.genericui.event.PointerEvent
import com.nexustools.genericui.event.WheelEvent
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.res.DataUrlCodec

@JsFun("() => typeof document !== 'undefined' && typeof window !== 'undefined'")
private external fun wasmIsBrowserEnv(): Boolean

@JsFun("(peer) => { " +
    "const target = peer || (typeof document !== 'undefined' ? document.documentElement : null); " +
    "if (target && typeof target.requestFullscreen === 'function') { " +
    "  try { target.requestFullscreen(); return true; } catch (_e) { return false; } " +
    "} return false; }")
private external fun wasmRequestElementFullscreen(peer: JsAny?): Boolean

@JsFun("() => { " +
    "if (typeof document !== 'undefined' && document.fullscreenElement && typeof document.exitFullscreen === 'function') { " +
    "  try { document.exitFullscreen(); return true; } catch (_e) { return false; } " +
    "} return true; }")
private external fun wasmExitFullscreen(): Boolean

@JsFun("(onFullscreenChanged) => { " +
    "if (typeof document === 'undefined' || typeof document.addEventListener !== 'function') return; " +
    "const notify = () => { " +
    "  const fsEl = document.fullscreenElement || document.webkitFullscreenElement || null; " +
    "  onFullscreenChanged(!!fsEl); " +
    "}; " +
    "document.addEventListener('fullscreenchange', notify); " +
    "document.addEventListener('webkitfullscreenchange', notify); " +
    "}")
private external fun wasmRegisterFullscreenChange(onFullscreenChanged: (Boolean) -> Unit)

@JsFun("(peer, src, onLoaded, onError) => { " +
    "try { " +
    "  peer.style.display = 'none'; " +
    "  peer.style.position = 'fixed'; " +
    "  peer.style.width = '0px'; " +
    "  peer.style.height = '0px'; " +
    "  peer.style.pointerEvents = 'none'; " +
    "  peer.setAttribute('aria-hidden', 'true'); " +
    "  let settled = false; " +
    "  const finishLoad = () => { " +
    "    if (settled) return; settled = true; " +
    "    const w = (peer.naturalWidth && peer.naturalWidth > 0) ? (peer.naturalWidth | 0) : 64; " +
    "    const h = (peer.naturalHeight && peer.naturalHeight > 0) ? (peer.naturalHeight | 0) : 64; " +
    "    peer.setAttribute('data-complete', 'true'); " +
    "    peer.setAttribute('data-natural-width', String(w)); " +
    "    peer.setAttribute('data-natural-height', String(h)); " +
    "    onLoaded(w, h); " +
    "  }; " +
    "  const finishError = () => { " +
    "    if (settled) return; settled = true; " +
    "    peer.setAttribute('data-error', 'true'); " +
    "    onError('Failed to load HTMLImageElement: ' + src); " +
    "  }; " +
    "  peer.addEventListener('load', finishLoad); " +
    "  peer.addEventListener('error', finishError); " +
    "  peer.setAttribute('src', src); " +
    "  if ((peer.complete && peer.naturalWidth && peer.naturalWidth > 0) || src.toLowerCase().indexOf('data:') === 0) { " +
    "    finishLoad(); " +
    "  } " +
    "  return true; " +
    "} catch (_e) { return false; } " +
    "}")
private external fun wasmBindInvisibleImageLoader(
    peer: JsAny,
    src: String,
    onLoaded: (Int, Int) -> Unit,
    onError: (String) -> Unit
): Boolean

@JsFun("(tagName) => document.createElement(tagName)")
private external fun wasmCreateElement(tagName: String): JsAny

@JsFun("(oldPeer) => { " +
    "const doc = document; " +
    "const attach = () => { " +
    "  const loader = doc.querySelector('body > .loader'); " +
    "  if (loader && loader.parentNode) loader.parentNode.removeChild(loader); " +
    "  let el = (doc.getElementsByTagName('genericui:app') && doc.getElementsByTagName('genericui:app').item(0)) || doc.querySelector('genericui\\\\:app'); " +
    "  if (!el && doc.body) { el = doc.createElement('genericui:app'); doc.body.appendChild(el); } " +
    "  if (el) { " +
    "    if (typeof el.hasAttribute === 'function') { " +
    "      if (el.hasAttribute('id')) el.removeAttribute('id'); " +
    "      if (el.hasAttribute('data-bs-theme')) el.removeAttribute('data-bs-theme'); " +
    "    } " +
    "    if (oldPeer && oldPeer !== el) { " +
    "      while (oldPeer.firstChild) { el.appendChild(oldPeer.firstChild); } " +
    "    } " +
    "  } " +
    "}; " +
    "if (doc.body) attach(); else doc.addEventListener('DOMContentLoaded', attach); " +
    "return (doc.getElementsByTagName('genericui:app') && doc.getElementsByTagName('genericui:app').item(0)) || doc.querySelector('genericui\\\\:app') || oldPeer || doc.createElement('genericui:app'); " +
    "}")
private external fun wasmBindRoot(oldPeer: JsAny?): JsAny

@JsFun("(peer, peerId) => { peer.__genericUiPeerId = peerId; }")
private external fun wasmSetPeerId(peer: JsAny, peerId: Int)

@JsFun("(hasPopupFn, onDownFn, onUpFn, onClickFn) => { " +
    "try { " +
    "  const win = typeof window !== 'undefined' ? window : null; " +
    "  if (!win || win.__genericUiPopupCaptureInstalled) return; " +
    "  win.__genericUiPopupCaptureInstalled = true; " +
    "  const findPid = (t) => { " +
    "    let cur = t; " +
    "    while (cur) { " +
    "      if (typeof cur.__genericUiPeerId === 'number') return cur.__genericUiPeerId | 0; " +
    "      cur = cur.parentElement || cur.parentNode; " +
    "    } " +
    "    return 0; " +
    "  }; " +
    "  const stopEv = (ev) => { " +
    "    if (!ev) return; " +
    "    if (typeof ev.preventDefault === 'function') ev.preventDefault(); " +
    "    if (typeof ev.stopPropagation === 'function') ev.stopPropagation(); " +
    "    if (typeof ev.stopImmediatePropagation === 'function') ev.stopImmediatePropagation(); " +
    "  }; " +
    "  const handleDown = (ev) => { " +
    "    const cx = (ev && typeof ev.clientX === 'number') ? ev.clientX : ((ev && ev.touches && ev.touches[0]) ? (ev.touches[0].clientX || 0) : NaN); " +
    "    const cy = (ev && typeof ev.clientY === 'number') ? ev.clientY : ((ev && ev.touches && ev.touches[0]) ? (ev.touches[0].clientY || 0) : NaN); " +
    "    if (onDownFn(cx, cy, findPid(ev ? ev.target : null))) stopEv(ev); " +
    "  }; " +
    "  const handleUp = (ev) => { " +
    "    const cx = (ev && typeof ev.clientX === 'number') ? ev.clientX : NaN; " +
    "    const cy = (ev && typeof ev.clientY === 'number') ? ev.clientY : NaN; " +
    "    if (onUpFn(cx, cy, findPid(ev ? ev.target : null))) stopEv(ev); " +
    "  }; " +
    "  const handleClick = (ev) => { " +
    "    const cx = (ev && typeof ev.clientX === 'number') ? ev.clientX : NaN; " +
    "    const cy = (ev && typeof ev.clientY === 'number') ? ev.clientY : NaN; " +
    "    if (onClickFn(cx, cy, findPid(ev ? ev.target : null))) stopEv(ev); " +
    "  }; " +
    "  win.addEventListener('pointerdown', handleDown, true); " +
    "  win.addEventListener('mousedown', handleDown, true); " +
    "  win.addEventListener('touchstart', handleDown, true); " +
    "  win.addEventListener('contextmenu', handleDown, true); " +
    "  win.addEventListener('pointerup', handleUp, true); " +
    "  win.addEventListener('mouseup', handleUp, true); " +
    "  win.addEventListener('click', handleClick, true); " +
    "} catch (_e) {} }")
private external fun wasmEnsurePopupCaptureListenersInstalled(
    hasPopupFn: () -> Boolean,
    onDownFn: (Float, Float, Int) -> Boolean,
    onUpFn: (Float, Float, Int) -> Boolean,
    onClickFn: (Float, Float, Int) -> Boolean
)

@JsFun("(peer, onClick, onFocus, onBlur, onDblClick, onMouseEnter, onMouseLeave, onInput, onChange, onCancel, onWheel, onLoad, onError, onScroll) => { " +
    "const isEditable = (el) => { " +
    "  if (!el || !el.tagName) return false; " +
    "  if (el.isContentEditable) return true; " +
    "  const tag = String(el.tagName).toLowerCase(); " +
    "  if (tag === 'textarea' || tag === 'select') return true; " +
    "  if (tag === 'input') { " +
    "    const t = String(el.getAttribute('type') || el.type || 'text').toLowerCase(); " +
    "    return t !== 'range' && t !== 'button' && t !== 'checkbox' && t !== 'radio' && t !== 'submit' && t !== 'reset' && t !== 'file' && t !== 'color' && t !== 'image'; " +
    "  } " +
    "  return false; " +
    "}; " +
    "peer.addEventListener('click', (ev) => { " +
    "  if (ev && ev.__genericUiClickHandled) return; " +
    "  let cur = ev ? ev.target : null; let pid = 0; " +
    "  while (cur) { if (typeof cur.__genericUiPeerId === 'number') { pid = cur.__genericUiPeerId | 0; break; } cur = cur.parentElement || cur.parentNode; } " +
    "  const isDirect = !ev || !ev.target || ev.target === peer; " +
    "  if (onClick(pid, isDirect) && ev) { ev.__genericUiClickHandled = true; } " +
    "}); " +
    "peer.addEventListener('focus', () => { onFocus(isEditable(peer)); }); " +
    "peer.addEventListener('blur', () => { onBlur(isEditable(peer)); }); " +
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
    "peer.addEventListener('scroll', () => { " +
    "  onScroll(peer.scrollTop || 0, peer.scrollLeft || 0, peer.clientWidth || 0, peer.clientHeight || 0, peer.scrollWidth || 0, peer.scrollHeight || 0); " +
    "}); " +
    "}")
private external fun wasmAttachBasicListeners(
    peer: JsAny,
    onClick: (Int, Boolean) -> Boolean,
    onFocus: (Boolean) -> Unit,
    onBlur: (Boolean) -> Unit,
    onDblClick: () -> Unit,
    onMouseEnter: () -> Unit,
    onMouseLeave: () -> Unit,
    onInput: (String, Boolean) -> Unit,
    onChange: (String, Boolean) -> Unit,
    onCancel: () -> Unit,
    onWheel: (Float, Float, Float, Float) -> Unit,
    onLoad: () -> Unit,
    onError: () -> Unit,
    onScroll: (Double, Double, Int, Int, Int, Int) -> Unit
)

@JsFun("(peer, hasContextMenuListener, onContextMenu, onPointerDown, onPointerMove, onPointerUp, onKeyDown, onKeyUp) => { " +
    "const ptr = (ev, defBtn, cb) => { " +
    "  cb(ev.clientX || 0, ev.clientY || 0, typeof ev.button === 'number' ? ev.button : defBtn, ev.buttons || 0, !!ev.shiftKey, !!ev.ctrlKey, !!ev.altKey, !!ev.metaKey); " +
    "}; " +
    "const isEditable = (el) => { " +
    "  if (!el || !el.tagName) return false; " +
    "  if (el.isContentEditable) return true; " +
    "  const tag = String(el.tagName).toLowerCase(); " +
    "  if (tag === 'textarea' || tag === 'select') return true; " +
    "  if (tag === 'input') { " +
    "    const t = String(el.getAttribute('type') || el.type || 'text').toLowerCase(); " +
    "    return t !== 'range' && t !== 'button' && t !== 'checkbox' && t !== 'radio' && t !== 'submit' && t !== 'reset' && t !== 'file' && t !== 'color' && t !== 'image'; " +
    "  } " +
    "  return false; " +
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
    "  const ed = isEditable(ev ? ev.target : null) || (typeof document !== 'undefined' && isEditable(document.activeElement)); " +
    "  const win = typeof window !== 'undefined' ? window : globalThis; " +
    "  let eid = ev && typeof ev.__genericUiKeyEventId === 'number' ? (ev.__genericUiKeyEventId | 0) : 0; " +
    "  if (!eid) { " +
    "    eid = ((win.__genericUiNextKeyEventId || 0) + 1) | 0; " +
    "    if (eid <= 0) eid = 1; " +
    "    win.__genericUiNextKeyEventId = eid; " +
    "    try { if (ev) ev.__genericUiKeyEventId = eid; } catch (_e) {} " +
    "  } " +
    "  cb(eid, k, c, !!ev.shiftKey, !!ev.ctrlKey, !!ev.altKey, !!ev.metaKey, !!ev.repeat, ed); " +
    "}; " +
    "peer.addEventListener('keydown', (ev) => { if (peer.tagName && String(peer.tagName).toLowerCase() === 'genericui:window') ev.__genericUiHandledByWindowPeer = true; key(ev, onKeyDown); }); " +
    "peer.addEventListener('keyup', (ev) => { if (peer.tagName && String(peer.tagName).toLowerCase() === 'genericui:window') ev.__genericUiHandledByWindowPeer = true; key(ev, onKeyUp); }); " +
    "}")
private external fun wasmAttachInputListeners(
    peer: JsAny,
    hasContextMenuListener: () -> Boolean,
    onContextMenu: (Int, Int) -> Unit,
    onPointerDown: (Float, Float, Int, Int, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onPointerMove: (Float, Float, Int, Int, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onPointerUp: (Float, Float, Int, Int, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onKeyDown: (Int, String, String, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onKeyUp: (Int, String, String, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
)

@JsFun("(onResize) => { " +
    "const getW = () => { " +
    "  const iw = window.innerWidth || 0; " +
    "  const cw = (document.documentElement && document.documentElement.clientWidth) ? document.documentElement.clientWidth : 0; " +
    "  const vvw = (window.visualViewport && window.visualViewport.width) ? (window.visualViewport.width | 0) : 0; " +
    "  const arr = [iw, cw, vvw].filter((n) => n > 0); " +
    "  return arr.length > 0 ? Math.min.apply(null, arr) : 1280; " +
    "}; " +
    "const getH = () => { " +
    "  const ih = window.innerHeight || 0; " +
    "  const ch = (document.documentElement && document.documentElement.clientHeight) ? document.documentElement.clientHeight : 0; " +
    "  const vvh = (window.visualViewport && window.visualViewport.height) ? (window.visualViewport.height | 0) : 0; " +
    "  const arr = [ih, ch, vvh].filter((n) => n > 0); " +
    "  return arr.length > 0 ? Math.min.apply(null, arr) : 720; " +
    "}; " +
    "const notify = () => { onResize(getW(), getH()); }; " +
    "window.addEventListener('resize', notify); " +
    "window.addEventListener('orientationchange', notify); " +
    "if (window.visualViewport && typeof window.visualViewport.addEventListener === 'function') { " +
    "  window.visualViewport.addEventListener('resize', notify); " +
    "} " +
    "}")
private external fun wasmRegisterWindowResize(onResize: (Int, Int) -> Unit)

@JsFun("(block) => { " +
    "if (typeof window.requestAnimationFrame === 'function') window.requestAnimationFrame(() => block()); " +
    "else if (typeof window.setTimeout === 'function') window.setTimeout(() => block(), 0); " +
    "else block(); " +
    "}")
private external fun wasmSchedulePostLayout(block: () -> Unit)

@JsFun("(block) => { " +
    "if (typeof window !== 'undefined' && typeof window.requestAnimationFrame === 'function') { " +
    "  return window.requestAnimationFrame(() => block()) | 0; " +
    "} return -1; }")
private external fun wasmRequestAnimationFrame(block: () -> Unit): Int

@JsFun("(handle) => { " +
    "if (typeof window !== 'undefined' && typeof window.cancelAnimationFrame === 'function') { " +
    "  window.cancelAnimationFrame(handle); return true; " +
    "} return false; }")
private external fun wasmCancelAnimationFrame(handle: Int): Boolean

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

@JsFun("(peer, disabled) => { " +
    "peer.disabled = disabled; " +
    "if (disabled) { " +
    "  peer.setAttribute('aria-disabled', 'true'); " +
    "  if (typeof document !== 'undefined' && document.activeElement === peer && typeof peer.blur === 'function') peer.blur(); " +
    "} else { " +
    "  peer.removeAttribute('aria-disabled'); " +
    "} " +
    "}")
private external fun wasmSetDisabled(peer: JsAny, disabled: Boolean)

@JsFun("(peer, ro) => { peer.readOnly = ro; }")
private external fun wasmSetReadOnly(peer: JsAny, readOnly: Boolean)

@JsFun("(peer, mult) => { peer.multiple = mult; }")
private external fun wasmSetMultiple(peer: JsAny, multiple: Boolean)

@JsFun("(peer, dir) => { peer.webkitdirectory = dir; }")
private external fun wasmSetDirectory(peer: JsAny, directory: Boolean)

@JsFun("(peer, accept) => { peer.accept = accept; }")
private external fun wasmSetAccept(peer: JsAny, accept: String)

@JsFun("(peer, st) => { const cur = peer.scrollTop || 0; if (Math.abs(cur - st) > 1) peer.scrollTop = st; }")
private external fun wasmSetScrollTop(peer: JsAny, scrollTop: Double)

@JsFun("(peer, sl) => { const cur = peer.scrollLeft || 0; if (Math.abs(cur - sl) > 1) peer.scrollLeft = sl; }")
private external fun wasmSetScrollLeft(peer: JsAny, scrollLeft: Double)

@JsFun("(modalEl) => { " +
    "try { " +
    "  const win = window; const doc = document; " +
    "  if (!win.__genericUiModalStack) win.__genericUiModalStack = []; " +
    "  const stack = win.__genericUiModalStack; " +
    "  if (stack.length === 0) win.__genericUiPreModalFocus = doc.activeElement; " +
    "  const existingIdx = stack.indexOf(modalEl); " +
    "  if (existingIdx !== -1) stack.splice(existingIdx, 1); " +
    "  stack.push(modalEl); " +
    "  const mainWindows = doc.querySelectorAll('genericui\\\\:window:not([data-tool-modal=\"true\"])'); " +
    "  for (let i = 0; i < mainWindows.length; i++) { " +
    "    mainWindows[i].setAttribute('aria-hidden', 'true'); " +
    "    try { mainWindows[i].inert = true; } catch (_e) {} " +
    "  } " +
    "  const queryFocusables = (root) => { " +
    "    if (!root || typeof root.querySelectorAll !== 'function') return []; " +
    "    const nodes = root.querySelectorAll('button:not([disabled]), [href], input:not([disabled]):not([type=\"hidden\"]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex=\"-1\"]):not([disabled])'); " +
    "    const list = []; " +
    "    for (let j = 0; j < nodes.length; j++) { " +
    "      const el = nodes[j]; " +
    "      if (el.offsetParent !== null || (typeof el.getClientRects === 'function' && el.getClientRects().length > 0)) list.push(el); " +
    "    } " +
    "    return list; " +
    "  }; " +
    "  if (!win.__genericUiModalTrapInstalled) { " +
    "    win.__genericUiModalTrapInstalled = true; " +
    "    doc.addEventListener('keydown', (ev) => { " +
    "      const st = win.__genericUiModalStack; " +
    "      if (!st || st.length === 0 || ev.key !== 'Tab') return; " +
    "      const topModal = st[st.length - 1]; if (!topModal) return; " +
    "      const focusables = queryFocusables(topModal); " +
    "      if (focusables.length === 0) { ev.preventDefault(); if (typeof topModal.focus === 'function') topModal.focus(); return; } " +
    "      const first = focusables[0]; const last = focusables[focusables.length - 1]; const active = doc.activeElement; " +
    "      if (!topModal.contains(active)) { ev.preventDefault(); (ev.shiftKey ? last : first).focus(); } " +
    "      else if (!ev.shiftKey && active === last) { ev.preventDefault(); first.focus(); } " +
    "      else if (ev.shiftKey && (active === first || active === topModal)) { ev.preventDefault(); last.focus(); } " +
    "    }, true); " +
    "    doc.addEventListener('focusin', (ev) => { " +
    "      const st = win.__genericUiModalStack; " +
    "      if (!st || st.length === 0) return; " +
    "      const topModal = st[st.length - 1]; " +
    "      if (!topModal || !ev.target) return; " +
    "      if (!topModal.contains(ev.target)) { " +
    "        const focusables = queryFocusables(topModal); " +
    "        if (focusables.length > 0) focusables[0].focus(); else if (typeof topModal.focus === 'function') topModal.focus(); " +
    "      } " +
    "    }, true); " +
    "  } " +
    "  setTimeout(() => { " +
    "    const focusables = queryFocusables(modalEl); " +
    "    if (focusables.length > 0 && typeof focusables[0].focus === 'function') focusables[0].focus(); " +
    "    else if (typeof modalEl.focus === 'function') modalEl.focus(); " +
    "  }, 0); " +
    "} catch (_e) {} }")
private external fun wasmInstallModalFocusTrap(modalPeer: JsAny)

@JsFun("(modalEl) => { " +
    "try { " +
    "  const win = window; const doc = document; const stack = win.__genericUiModalStack; if (!stack) return; " +
    "  const idx = stack.indexOf(modalEl); if (idx !== -1) stack.splice(idx, 1); " +
    "  if (stack.length === 0) { " +
    "    const mainWindows = doc.querySelectorAll('genericui\\\\:window:not([data-tool-modal=\"true\"])'); " +
    "    for (let i = 0; i < mainWindows.length; i++) { " +
    "      mainWindows[i].removeAttribute('aria-hidden'); " +
    "      try { mainWindows[i].inert = false; } catch (_e) {} " +
    "    } " +
    "    const prev = win.__genericUiPreModalFocus; win.__genericUiPreModalFocus = null; " +
    "    if (prev && typeof prev.focus === 'function') { try { prev.focus(); } catch (_e) {} } " +
    "  } " +
    "} catch (_e) {} }")
private external fun wasmReleaseModalFocusTrap(modalPeer: JsAny)

@JsFun("(scrollEl, viewportEl) => { " +
    "try { " +
    "  if (!scrollEl || !viewportEl || scrollEl.__genericUiOverscrollAttached) return; " +
    "  scrollEl.__genericUiOverscrollAttached = true; " +
    "  scrollEl.style.overscrollBehavior = 'auto'; " +
    "  let startX = 0, startY = 0, activeTouch = false, wheelResetTimer = null; " +
    "  const isHoriz = () => scrollEl.style.overflowX === 'auto' && scrollEl.style.overflowY !== 'auto'; " +
    "  const calcScale = (delta) => { " +
    "    const abs = Math.abs(delta); if (abs <= 0.5) return 1.0; " +
    "    const norm = Math.min(4.0, abs / 220.0); " +
    "    return 1.0 + Math.min(0.14, (norm / (1.0 + norm)) * 0.14); " +
    "  }; " +
    "  const applyStretch = (delta) => { " +
    "    const h = isHoriz(); const s = calcScale(delta); " +
    "    if (s <= 1.0005) { " +
    "      viewportEl.style.transition = 'transform 260ms cubic-bezier(0.22, 1, 0.36, 1)'; " +
    "      viewportEl.style.transform = h ? 'scaleX(1)' : 'scaleY(1)'; " +
    "      scrollEl.setAttribute('data-overscroll-stretch', '1.0'); " +
    "      scrollEl.removeAttribute('data-overscroll-edge'); " +
    "      return; " +
    "    } " +
    "    viewportEl.style.transformOrigin = h ? (delta > 0 ? '0% 50%' : '100% 50%') : (delta > 0 ? '50% 0%' : '50% 100%'); " +
    "    viewportEl.style.transition = 'none'; " +
    "    viewportEl.style.transform = h ? ('scaleX(' + s.toFixed(4) + ')') : ('scaleY(' + s.toFixed(4) + ')'); " +
    "    scrollEl.setAttribute('data-overscroll-stretch', s.toFixed(4)); " +
    "    scrollEl.setAttribute('data-overscroll-edge', h ? (delta > 0 ? 'start' : 'end') : (delta > 0 ? 'top' : 'bottom')); " +
    "  }; " +
    "  const release = () => { " +
    "    activeTouch = false; const h = isHoriz(); " +
    "    viewportEl.style.transition = 'transform 260ms cubic-bezier(0.22, 1, 0.36, 1)'; " +
    "    viewportEl.style.transform = h ? 'scaleX(1)' : 'scaleY(1)'; " +
    "    scrollEl.setAttribute('data-overscroll-stretch', '1.0'); " +
    "    scrollEl.removeAttribute('data-overscroll-edge'); " +
    "  }; " +
    "  scrollEl.addEventListener('touchstart', (ev) => { " +
    "    if (!ev.touches || ev.touches.length !== 1) return; " +
    "    activeTouch = true; startX = ev.touches[0].clientX; startY = ev.touches[0].clientY; " +
    "  }, { passive: true }); " +
    "  scrollEl.addEventListener('touchmove', (ev) => { " +
    "    if (!activeTouch || !ev.touches || ev.touches.length !== 1) return; " +
    "    const h = isHoriz(); const curX = ev.touches[0].clientX; const curY = ev.touches[0].clientY; " +
    "    const delta = h ? (curX - startX) : (curY - startY); " +
    "    const pos = h ? scrollEl.scrollLeft : scrollEl.scrollTop; " +
    "    const client = h ? scrollEl.clientWidth : scrollEl.clientHeight; " +
    "    const total = h ? scrollEl.scrollWidth : scrollEl.scrollHeight; " +
    "    const atStart = pos <= 1; const atEnd = (pos + client) >= Math.max(0, total - 2); " +
    "    if ((atStart && delta > 0) || (atEnd && delta < 0)) { " +
    "      if (ev.cancelable) ev.preventDefault(); " +
    "      applyStretch(delta); " +
    "    } else { " +
    "      startX = curX; startY = curY; " +
    "      if (scrollEl.getAttribute('data-overscroll-stretch') !== '1.0') applyStretch(0); " +
    "    } " +
    "  }, { passive: false }); " +
    "  scrollEl.addEventListener('touchend', release, { passive: true }); " +
    "  scrollEl.addEventListener('touchcancel', release, { passive: true }); " +
    "} catch (_e) {} }")
private external fun wasmAttachOverscrollStretch(scrollPeer: JsAny, viewportPeer: JsAny)

@JsFun("(scrollEl, viewportEl, isHorizFn, emitCb) => { " +
    "try { " +
    "  if (!scrollEl || !viewportEl) return; " +
    "  scrollEl.__genericUiLazyEmitCb = emitCb; " +
    "  scrollEl.__genericUiLazyIsHorizFn = isHorizFn; " +
    "  if (scrollEl.__genericUiLazyViewportAttached) return; " +
    "  scrollEl.__genericUiLazyViewportAttached = true; " +
    "  const win = typeof window !== 'undefined' ? window : null; " +
    "  const doc = typeof document !== 'undefined' ? document : null; " +
    "  let lastScrollOffset = -1; " +
    "  let lastViewportExtent = -1; " +
    "  let calibratedSpan = false; " +
    "  const findClippingAncestor = (el, horiz) => { " +
    "    let cur = el ? el.parentElement : null; " +
    "    while (cur && cur !== (doc && doc.body) && cur !== (doc && doc.documentElement)) { " +
    "      const scrolled = horiz ? (cur.scrollLeft || 0) > 0 : (cur.scrollTop || 0) > 0; " +
    "      if (scrolled) return cur; " +
    "      if (win && typeof win.getComputedStyle === 'function') { " +
    "        const cs = win.getComputedStyle(cur); " +
    "        const ov = cs ? (horiz ? cs.overflowX : cs.overflowY) : ''; " +
    "        if (ov === 'auto' || ov === 'scroll' || ov === 'overlay' || ov === 'hidden') { " +
    "          const cSpan = horiz ? (cur.clientWidth || 0) : (cur.clientHeight || 0); " +
    "          if (cSpan > 0) return cur; " +
    "        } " +
    "      } " +
    "      cur = cur.parentElement; " +
    "    } " +
    "    return null; " +
    "  }; " +
    "  const sampleAndEmit = (fromScrollEvent) => { " +
    "    const cb = scrollEl.__genericUiLazyEmitCb; " +
    "    const hFn = scrollEl.__genericUiLazyIsHorizFn; " +
    "    if (typeof cb !== 'function') return; " +
    "    const isHoriz = typeof hFn === 'function' ? !!hFn() : false; " +
    "    const winW = (win && win.innerWidth) ? win.innerWidth : 1280; " +
    "    const winH = (win && win.innerHeight) ? win.innerHeight : 720; " +
    "    const ownScroll = isHoriz ? (scrollEl.scrollLeft || 0) : (scrollEl.scrollTop || 0); " +
    "    const ownClient = isHoriz ? (scrollEl.clientWidth || 0) : (scrollEl.clientHeight || 0); " +
    "    const ownTotal = isHoriz ? (scrollEl.scrollWidth || 0) : (scrollEl.scrollHeight || 0); " +
    "    const winSpan = isHoriz ? winW : winH; " +
    "    let scrollOffsetPx = 0; " +
    "    let viewportExtentPx = 600; " +
    "    const selfScrolling = ownScroll > 0 || (ownClient > 0 && ownTotal > ownClient + 2 && ownClient <= winSpan); " +
    "    if (selfScrolling) { " +
    "      scrollOffsetPx = Math.max(0, ownScroll); " +
    "      viewportExtentPx = Math.max(1, Math.round(ownClient || 600)); " +
    "    } else { " +
    "      const clipPeer = findClippingAncestor(scrollEl, isHoriz); " +
    "      const peerRect = typeof scrollEl.getBoundingClientRect === 'function' ? scrollEl.getBoundingClientRect() : null; " +
    "      const clipRect = (clipPeer && typeof clipPeer.getBoundingClientRect === 'function') " +
    "        ? clipPeer.getBoundingClientRect() " +
    "        : { top: 0, left: 0, bottom: winH, right: winW, height: winH, width: winW }; " +
    "      if (peerRect) { " +
    "        if (isHoriz) { " +
    "          scrollOffsetPx = Math.max(0, (clipRect.left - peerRect.left) + (scrollEl.scrollLeft || 0)); " +
    "          const cw = Math.round(clipRect.width || winW || 600); " +
    "          viewportExtentPx = Math.max(1, (ownClient > 0 && ownClient < cw) ? ownClient : cw); " +
    "        } else { " +
    "          scrollOffsetPx = Math.max(0, (clipRect.top - peerRect.top) + (scrollEl.scrollTop || 0)); " +
    "          const ch = Math.round(clipRect.height || winH || 600); " +
    "          viewportExtentPx = Math.max(1, (ownClient > 0 && ownClient < ch) ? ownClient : ch); " +
    "        } " +
    "      } else { " +
    "        scrollOffsetPx = Math.max(0, ownScroll); " +
    "        viewportExtentPx = Math.max(1, Math.round(ownClient || winSpan || 600)); " +
    "      } " +
    "    } " +
    "    const measuredViewportSpanPx = Math.max(0, Math.round( " +
    "      isHoriz " +
    "        ? (viewportEl.clientWidth || viewportEl.offsetWidth || 0) " +
    "        : (viewportEl.clientHeight || viewportEl.offsetHeight || 0) " +
    "    )); " +
    "    const renderedChildCount = (viewportEl.children && typeof viewportEl.children.length === 'number') " +
    "      ? (viewportEl.children.length | 0) " +
    "      : 0; " +
    "    const scrollChanged = Math.abs(scrollOffsetPx - lastScrollOffset) >= 0.5; " +
    "    const extentChanged = Math.abs(viewportExtentPx - lastViewportExtent) >= 1; " +
    "    const needsInitialCalibration = !calibratedSpan && renderedChildCount > 0 && measuredViewportSpanPx > 0; " +
    "    if (!fromScrollEvent && !scrollChanged && !extentChanged && !needsInitialCalibration) return; " +
    "    if (renderedChildCount > 0 && measuredViewportSpanPx > 0) calibratedSpan = true; " +
    "    lastScrollOffset = scrollOffsetPx; " +
    "    lastViewportExtent = viewportExtentPx; " +
    "    cb( " +
    "      scrollOffsetPx, " +
    "      viewportExtentPx | 0, " +
    "      measuredViewportSpanPx | 0, " +
    "      renderedChildCount | 0, " +
    "      (scrollEl.scrollWidth || 0) | 0, " +
    "      (scrollEl.scrollHeight || 0) | 0 " +
    "    ); " +
    "  }; " +
    "  const onScrollHandler = (ev) => { " +
    "    const target = ev ? ev.target : null; " +
    "    if ( " +
    "      !target || " +
    "      target === scrollEl || " +
    "      target === doc || " +
    "      target === (doc && doc.documentElement) || " +
    "      target === (doc && doc.body) || " +
    "      target === win || " +
    "      (typeof target.contains === 'function' && target.contains(scrollEl)) " +
    "    ) { " +
    "      sampleAndEmit(true); " +
    "    } " +
    "  }; " +
    "  scrollEl.addEventListener('scroll', onScrollHandler, { passive: true }); " +
    "  scrollEl.addEventListener('wheel', (ev) => { " +
    "    if (!ev) return; " +
    "    const hFn = scrollEl.__genericUiLazyIsHorizFn; " +
    "    const isHoriz = typeof hFn === 'function' ? !!hFn() : false; " +
    "    const rawDelta = isHoriz ? (ev.deltaX || ev.deltaY || 0) : (ev.deltaY || 0); " +
    "    if (!rawDelta) return; " +
    "    let deltaPx = rawDelta; " +
    "    if (ev.deltaMode === 1) deltaPx = rawDelta * 40; " +
    "    else if (ev.deltaMode === 2) deltaPx = rawDelta * ((isHoriz ? scrollEl.clientWidth : scrollEl.clientHeight) || 400); " +
    "    const ownClient = isHoriz ? (scrollEl.clientWidth || 0) : (scrollEl.clientHeight || 0); " +
    "    const ownTotal = isHoriz ? (scrollEl.scrollWidth || 0) : (scrollEl.scrollHeight || 0); " +
    "    if (ownClient > 0 && ownTotal > ownClient + 2) { " +
    "      const prevOwn = isHoriz ? (scrollEl.scrollLeft || 0) : (scrollEl.scrollTop || 0); " +
    "      if (win && typeof win.requestAnimationFrame === 'function') { " +
    "        win.requestAnimationFrame(() => { " +
    "          const afterOwn = isHoriz ? (scrollEl.scrollLeft || 0) : (scrollEl.scrollTop || 0); " +
    "          if (Math.abs(afterOwn - prevOwn) < 0.5) { " +
    "            if (isHoriz) scrollEl.scrollLeft = Math.max(0, Math.min(ownTotal - ownClient, prevOwn + deltaPx)); " +
    "            else scrollEl.scrollTop = Math.max(0, Math.min(ownTotal - ownClient, prevOwn + deltaPx)); " +
    "          } " +
    "          sampleAndEmit(true); " +
    "        }); " +
    "      } else { sampleAndEmit(true); } " +
    "    } else { " +
    "      const clipPeer = findClippingAncestor(scrollEl, isHoriz); " +
    "      if (clipPeer) { " +
    "        const peerClient = isHoriz ? (clipPeer.clientWidth || 0) : (clipPeer.clientHeight || 0); " +
    "        const peerTotal = isHoriz ? (clipPeer.scrollWidth || 0) : (clipPeer.scrollHeight || 0); " +
    "        if (peerClient > 0 && peerTotal > peerClient + 2) { " +
    "          const prevPeer = isHoriz ? (clipPeer.scrollLeft || 0) : (clipPeer.scrollTop || 0); " +
    "          if (win && typeof win.requestAnimationFrame === 'function') { " +
    "            win.requestAnimationFrame(() => { " +
    "              const afterPeer = isHoriz ? (clipPeer.scrollLeft || 0) : (clipPeer.scrollTop || 0); " +
    "              if (Math.abs(afterPeer - prevPeer) < 0.5) { " +
    "                if (isHoriz) clipPeer.scrollLeft = Math.max(0, Math.min(peerTotal - peerClient, prevPeer + deltaPx)); " +
    "                else clipPeer.scrollTop = Math.max(0, Math.min(peerTotal - peerClient, prevPeer + deltaPx)); " +
    "              } " +
    "              sampleAndEmit(true); " +
    "            }); " +
    "          } else { sampleAndEmit(true); } " +
    "        } " +
    "      } " +
    "    } " +
    "  }, { passive: true }); " +
    "  if (win) { " +
    "    win.addEventListener('scroll', onScrollHandler, { capture: true, passive: true }); " +
    "    win.addEventListener('resize', () => { sampleAndEmit(false); }, { passive: true }); " +
    "  } " +
    "  if (typeof ResizeObserver !== 'undefined') { " +
    "    try { " +
    "      const ro = new ResizeObserver(() => { sampleAndEmit(false); }); " +
    "      ro.observe(scrollEl); " +
    "      ro.observe(viewportEl); " +
    "    } catch (_e) {} " +
    "  } " +
    "} catch (_e) {} }")
private external fun wasmAttachLazyViewportObserver(
    scrollPeer: JsAny,
    viewportPeer: JsAny,
    isHorizFn: () -> Boolean,
    emitCb: (Double, Int, Int, Int, Int, Int) -> Unit
)

@JsFun("(containerEl, rangeEl, startCb, moveCb, endCb) => { " +
    "try { " +
    "  if (!containerEl || containerEl.__genericUiSeekBarScrubAttached) return; " +
    "  containerEl.__genericUiSeekBarScrubAttached = true; " +
    "  containerEl.style.touchAction = 'none'; " +
    "  if (rangeEl && rangeEl.style) rangeEl.style.pointerEvents = 'none'; " +
    "  const win = typeof window !== 'undefined' ? window : null; " +
    "  let scrubbing = false; let activePointerId = null; let lastFraction = 0.0; let lastClientX = null; " +
    "  const calcFraction = (ev) => { " +
    "    const trackEl = (typeof containerEl.querySelector === 'function' ? containerEl.querySelector('.genericui-seekbar-track') : null) || containerEl; " +
    "    const rect = typeof trackEl.getBoundingClientRect === 'function' ? trackEl.getBoundingClientRect() : null; " +
    "    const w = (rect && rect.width > 0) ? rect.width : (trackEl.clientWidth || containerEl.clientWidth || 100); " +
    "    const left = rect ? rect.left : 0; " +
    "    const clientX = (ev && typeof ev.clientX === 'number') ? ev.clientX : null; " +
    "    if (clientX === null) return lastFraction; " +
    "    lastFraction = Math.max(0.0, Math.min(1.0, (clientX - left) / w)); " +
    "    return lastFraction; " +
    "  }; " +
    "  const handleMove = (ev) => { " +
    "    if (!scrubbing) return; " +
    "    if (activePointerId !== null && ev && ev.pointerId !== undefined && ev.pointerId !== activePointerId) return; " +
    "    if (ev && typeof ev.preventDefault === 'function') ev.preventDefault(); " +
    "    if (ev && typeof ev.stopPropagation === 'function') ev.stopPropagation(); " +
    "    if (ev && typeof ev.clientX === 'number') { " +
    "      if (lastClientX === null || Math.abs(ev.clientX - lastClientX) > 0.5) { " +
    "        lastClientX = ev.clientX; " +
    "        lastFraction = calcFraction(ev); " +
    "      } " +
    "    } " +
    "    moveCb(lastFraction); " +
    "  }; " +
    "  const finishScrub = (ev) => { " +
    "    if (!scrubbing) return; " +
    "    if (activePointerId !== null && ev && ev.pointerId !== undefined && ev.pointerId !== activePointerId) return; " +
    "    scrubbing = false; const pid = activePointerId; activePointerId = null; " +
    "    if (ev && typeof ev.stopPropagation === 'function') ev.stopPropagation(); " +
    "    try { if (typeof containerEl.releasePointerCapture === 'function' && pid !== null && pid !== undefined) containerEl.releasePointerCapture(pid); } catch (_e) {} " +
    "    if (ev && typeof ev.clientX === 'number' && lastClientX !== null && Math.abs(ev.clientX - lastClientX) > 0.5) { " +
    "      lastClientX = ev.clientX; " +
    "      lastFraction = calcFraction(ev); " +
    "    } " +
    "    lastClientX = null; " +
    "    endCb(lastFraction); " +
    "  }; " +
    "  containerEl.addEventListener('pointerdown', (ev) => { " +
    "    if (ev.button !== undefined && ev.button !== 0) return; " +
    "    scrubbing = true; activePointerId = ev.pointerId !== undefined ? ev.pointerId : null; " +
    "    lastClientX = (ev && typeof ev.clientX === 'number') ? ev.clientX : null; " +
    "    lastFraction = calcFraction(ev); " +
    "    if (typeof ev.preventDefault === 'function') ev.preventDefault(); " +
    "    if (typeof ev.stopPropagation === 'function') ev.stopPropagation(); " +
    "    try { if (typeof containerEl.setPointerCapture === 'function' && ev.pointerId !== undefined) containerEl.setPointerCapture(ev.pointerId); } catch (_e) {} " +
    "    startCb(lastFraction); " +
    "  }); " +
    "  containerEl.addEventListener('pointermove', handleMove); " +
    "  containerEl.addEventListener('pointerup', finishScrub); " +
    "  containerEl.addEventListener('pointercancel', finishScrub); " +
    "  if (win) { " +
    "    win.addEventListener('pointermove', handleMove, true); " +
    "    win.addEventListener('mousemove', handleMove, true); " +
    "    win.addEventListener('pointerup', finishScrub, true); " +
    "    win.addEventListener('mouseup', finishScrub, true); " +
    "    win.addEventListener('pointercancel', finishScrub, true); " +
    "    win.addEventListener('blur', finishScrub); " +
    "  } " +
    "} catch (_e) {} }")
private external fun wasmAttachSeekBarScrubbing(
    containerPeer: JsAny,
    rangePeer: JsAny?,
    onScrubStart: (Double) -> Unit,
    onScrubMove: (Double) -> Unit,
    onScrubEnd: (Double) -> Unit
)

@JsFun("(onMove, onUp) => { " +
    "try { " +
    "  const win = typeof window !== 'undefined' ? window : null; " +
    "  if (!win || win.__genericUiGlobalPointerDragInstalled) return; " +
    "  win.__genericUiGlobalPointerDragInstalled = true; " +
    "  const ptr = (ev, cb) => { cb(ev.clientX || 0, ev.clientY || 0, typeof ev.button === 'number' ? ev.button : 0, ev.buttons || 0, !!ev.shiftKey, !!ev.ctrlKey, !!ev.altKey, !!ev.metaKey); }; " +
    "  const moveHandler = (ev) => ptr(ev, onMove); " +
    "  const upHandler = (ev) => ptr(ev, onUp); " +
    "  win.addEventListener('pointermove', moveHandler, true); " +
    "  win.addEventListener('mousemove', moveHandler, true); " +
    "  win.addEventListener('pointerup', upHandler, true); " +
    "  win.addEventListener('mouseup', upHandler, true); " +
    "  win.addEventListener('pointercancel', upHandler, true); " +
    "} catch (_e) {} }")
private external fun wasmEnsureGlobalWindowPointerDragBridge(
    onMove: (Float, Float, Int, Int, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onUp: (Float, Float, Int, Int, Boolean, Boolean, Boolean, Boolean) -> Unit
)

@JsFun("(onKeyDown, onKeyUp) => { " +
    "try { " +
    "  const win = typeof window !== 'undefined' ? window : null; if (!win) return; " +
    "  win.__genericUiWindowKeyDownCb = onKeyDown; " +
    "  win.__genericUiWindowKeyUpCb = onKeyUp; " +
    "  if (win.__genericUiWindowKeysInstalled) return; " +
    "  win.__genericUiWindowKeysInstalled = true; " +
    "  const isEditableTarget = (el) => { " +
    "    if (!el || !el.tagName) return false; " +
    "    if (el.isContentEditable) return true; " +
    "    const tag = String(el.tagName).toLowerCase(); " +
    "    if (tag === 'textarea' || tag === 'select') return true; " +
    "    if (tag === 'input') { " +
    "      const t = String(el.getAttribute('type') || el.type || 'text').toLowerCase(); " +
    "      return t !== 'range' && t !== 'button' && t !== 'checkbox' && t !== 'radio'; " +
    "    } " +
    "    return false; " +
    "  }; " +
    "  const isModalActive = () => { " +
    "    if (win.__genericUiModalStack && win.__genericUiModalStack.length > 0) return true; " +
    "    if (typeof document !== 'undefined') { " +
    "      const openModal = document.querySelector('genericui\\\\:window[data-tool-modal=\"true\"]:not(.d-none)'); " +
    "      if (openModal && openModal.style && openModal.style.display !== 'none') return true; " +
    "    } " +
    "    return false; " +
    "  }; " +
    "  const isMediaPlayerKey = (ev) => { " +
    "    if (ev.ctrlKey || ev.altKey || ev.metaKey) return false; " +
    "    const k = String(ev.key || ''); const c = String(ev.code || ''); " +
    "    return k === ' ' || c === 'Space' || k === 'ArrowLeft' || k === 'ArrowRight' || k === 'ArrowUp' || k === 'ArrowDown'; " +
    "  }; " +
    "  const emit = (ev, cb) => { " +
    "    if (typeof cb !== 'function') return; " +
    "    const k = typeof ev.key === 'string' ? ev.key : ''; " +
    "    const c = typeof ev.code === 'string' ? ev.code : k; " +
    "    const ed = isEditableTarget(ev ? ev.target : null) || (typeof document !== 'undefined' && isEditableTarget(document.activeElement)); " +
    "    let eid = ev && typeof ev.__genericUiKeyEventId === 'number' ? (ev.__genericUiKeyEventId | 0) : 0; " +
    "    if (!eid) { " +
    "      eid = ((win.__genericUiNextKeyEventId || 0) + 1) | 0; " +
    "      if (eid <= 0) eid = 1; " +
    "      win.__genericUiNextKeyEventId = eid; " +
    "      try { if (ev) ev.__genericUiKeyEventId = eid; } catch (_e) {} " +
    "    } " +
    "    cb(eid, k, c, !!ev.shiftKey, !!ev.ctrlKey, !!ev.altKey, !!ev.metaKey, !!ev.repeat, ed); " +
    "  }; " +
    "  win.addEventListener('keydown', (ev) => { " +
    "    if (isModalActive() || isEditableTarget(ev.target)) return; " +
    "    if (isMediaPlayerKey(ev) && typeof ev.preventDefault === 'function') ev.preventDefault(); " +
    "  }, true); " +
    "  win.addEventListener('keydown', (ev) => { " +
    "    if (ev.__genericUiHandledByWindowPeer || isModalActive() || isEditableTarget(ev.target)) return; " +
    "    emit(ev, win.__genericUiWindowKeyDownCb); " +
    "  }, false); " +
    "  win.addEventListener('keyup', (ev) => { " +
    "    if (ev.__genericUiHandledByWindowPeer || isModalActive() || isEditableTarget(ev.target)) return; " +
    "    emit(ev, win.__genericUiWindowKeyUpCb); " +
    "  }, false); " +
    "} catch (_e) {} }")
private external fun wasmRegisterWindowKeyEvents(
    onKeyDown: (Int, String, String, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onKeyUp: (Int, String, String, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
)

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

@JsFun("(fallback) => { " +
    "const iw = window.innerWidth || 0; " +
    "const cw = (document.documentElement && document.documentElement.clientWidth) ? document.documentElement.clientWidth : 0; " +
    "const vvw = (window.visualViewport && window.visualViewport.width) ? (window.visualViewport.width | 0) : 0; " +
    "const arr = [iw, cw, vvw].filter((n) => n > 0); " +
    "return arr.length > 0 ? Math.min.apply(null, arr) : fallback; }")
private external fun wasmGetViewportWidth(fallback: Int): Int

@JsFun("(fallback) => { " +
    "const ih = window.innerHeight || 0; " +
    "const ch = (document.documentElement && document.documentElement.clientHeight) ? document.documentElement.clientHeight : 0; " +
    "const vvh = (window.visualViewport && window.visualViewport.height) ? (window.visualViewport.height | 0) : 0; " +
    "const arr = [ih, ch, vvh].filter((n) => n > 0); " +
    "return arr.length > 0 ? Math.min.apply(null, arr) : fallback; }")
private external fun wasmGetViewportHeight(fallback: Int): Int

@JsFun("(title) => { document.title = title; }")
private external fun wasmSetDocumentTitle(title: String)

@JsFun("(theme) => { const setTheme = (t) => { document.documentElement.setAttribute('data-bs-theme', t); }; if (document.documentElement) setTheme(theme); }")
private external fun wasmSetDocumentTheme(theme: String)

@JsFun("(theme) => { const setTheme = (t) => { document.documentElement.setAttribute('data-bs-theme', t); }; if (document.documentElement) setTheme(theme); }")
private external fun wasmSyncBootstrapTheme(theme: String)

@JsFun("(id, href) => { " +
    "const head = document.head; if (!head) return; " +
    "const norm = (u) => { const s = String(u || '').split('?')[0].split('#')[0]; const idx = s.lastIndexOf('/'); return idx >= 0 ? s.substring(idx + 1) : s; }; " +
    "let link = document.getElementById(id); " +
    "if (!link) { " +
    "  const targetBase = norm(href); " +
    "  const links = head.querySelectorAll(\"link[rel='stylesheet']\"); " +
    "  for (let i = 0; i < links.length; i++) { " +
    "    const c = links.item(i); const ch = c.getAttribute('href') || ''; " +
    "    if (ch === href || (targetBase && norm(ch) === targetBase)) { link = c; if (!link.id) link.id = id; break; } " +
    "  } " +
    "} " +
    "if (!link) { link = document.createElement('link'); link.id = id; link.setAttribute('rel', 'stylesheet'); link.setAttribute('href', href); head.appendChild(link); } " +
    "else { const eh = link.getAttribute('href') || ''; if (!eh || norm(eh) !== norm(href) || (href.indexOf('?') !== -1 && eh.indexOf('?') === -1)) link.setAttribute('href', href); } " +
    "}")
private external fun wasmAddStyleSheetLink(id: String, href: String)

@JsFun("(id) => { " +
    "const link = document.getElementById(id); if (link && link.parentNode) link.parentNode.removeChild(link); " +
    "if (id === 'genericui-bootstrap-css') { const jsEl = document.getElementById('genericui-bootstrap-js'); if (jsEl && jsEl.parentNode) jsEl.parentNode.removeChild(jsEl); } " +
    "}")
private external fun wasmRemoveStyleSheetLink(id: String)

@JsFun("(id, cssText) => { " +
    "const head = document.head; if (!head) return; " +
    "let el = document.getElementById(id); " +
    "if (!cssText || !String(cssText).trim()) { if (el && el.parentNode) el.parentNode.removeChild(el); return; } " +
    "if (!el) { el = document.createElement('style'); el.id = id; head.appendChild(el); } " +
    "el.textContent = cssText; " +
    "if (el.parentNode !== head) head.appendChild(el); " +
    "}")
private external fun wasmSetHeadStyleElement(id: String, cssText: String)

@JsFun("() => { if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return true; const isDarkMode = window.matchMedia('(prefers-color-scheme: dark)').matches; return !!isDarkMode; }")
private external fun wasmQueryDarkMode(): Boolean

@JsFun("(cb) => { if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return; const mql = window.matchMedia('(prefers-color-scheme: dark)'); if (mql && typeof mql.addEventListener === 'function') { mql.addEventListener('change', (ev) => { const isDarkMode = ev ? !!ev.matches : window.matchMedia('(prefers-color-scheme: dark)').matches; cb(isDarkMode); }); } }")
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

@JsFun("(routeHash, historyIndex, title, stateJson, hasStateJson) => { try { if (typeof window !== 'undefined' && window.history && typeof window.history.pushState === 'function') { const st = { genericUiRoute: routeHash, genericUiIndex: historyIndex, genericUiStateJson: hasStateJson ? stateJson : null }; const url = routeHash ? ('#' + routeHash) : '#'; window.history.pushState(st, title, url); } } catch (e) {} }")
private external fun wasmHistoryPushState(
    routeHash: String,
    historyIndex: Int,
    title: String,
    stateJson: String,
    hasStateJson: Boolean
)

@JsFun("(routeHash, historyIndex, title, stateJson, hasStateJson) => { try { if (typeof window !== 'undefined' && window.history && typeof window.history.replaceState === 'function') { const st = { genericUiRoute: routeHash, genericUiIndex: historyIndex, genericUiStateJson: hasStateJson ? stateJson : null }; const url = routeHash ? ('#' + routeHash) : '#'; window.history.replaceState(st, title, url); } } catch (e) {} }")
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

@JsFun("(key) => { try { const g = typeof window !== 'undefined' ? window : globalThis; const s = g && g.__GENERICUI_PRELOADED_RESOURCES__; const e = s && s[key]; return (e && typeof e.dataUrl === 'string') ? e.dataUrl : null; } catch (err) { return null; } }")
private external fun wasmGetPreloadedDataUrl(key: String): String?

@JsFun("(key) => { try { const g = typeof window !== 'undefined' ? window : globalThis; const s = g && g.__GENERICUI_PRELOADED_RESOURCES__; const e = s && s[key]; return (e && typeof e.text === 'string') ? e.text : null; } catch (err) { return null; } }")
private external fun wasmGetPreloadedText(key: String): String?

@JsFun("(key) => { try { const g = typeof window !== 'undefined' ? window : globalThis; const s = g && g.__GENERICUI_PRELOADED_RESOURCES__; return !!(s && s[key]); } catch (err) { return false; } }")
private external fun wasmHasPreloadedResource(key: String): Boolean

@JsFun("(key, mimeType, dataUrl, text, hasText) => { try { const g = typeof window !== 'undefined' ? window : globalThis; if (!g.__GENERICUI_PRELOADED_RESOURCES__) g.__GENERICUI_PRELOADED_RESOURCES__ = {}; const rec = { id: key, path: key, name: key, mimeType: mimeType, dataUrl: dataUrl, text: hasText ? text : null }; if (typeof mimeType === 'string' && mimeType.indexOf('image/') === 0 && typeof Image === 'function') { const img = new Image(); img.decoding = 'async'; img.src = dataUrl; rec.imageElement = img; } g.__GENERICUI_PRELOADED_RESOURCES__[key] = rec; } catch (err) {} }")
private external fun wasmSetPreloadedResource(
    key: String,
    mimeType: String,
    dataUrl: String,
    text: String,
    hasText: Boolean
)

@JsFun("() => { try { const g = typeof window !== 'undefined' ? window : globalThis; if (g) g.__GENERICUI_PRELOADED_RESOURCES__ = {}; } catch (err) {} }")
private external fun wasmClearPreloadedResources()

@JsFun("(key) => { " +
    "try { " +
    "  const g = typeof window !== 'undefined' ? window : globalThis; " +
    "  const s = g && g.__GENERICUI_PRELOADED_RESOURCES__; " +
    "  return (s && s[key]) ? s[key] : null; " +
    "} catch (_e) { return null; } }")
private external fun wasmFindPreloadedRecordByKey(key: String): JsAny?

@JsFun("(byteLen, getByteAt) => { " +
    "try { " +
    "  const g = typeof window !== 'undefined' ? window : globalThis; " +
    "  const store = g && g.__GENERICUI_PRELOADED_RESOURCES__; " +
    "  if (!store || byteLen <= 0) return null; " +
    "  const keys = Object.keys(store); " +
    "  const headCount = Math.min(16, byteLen); " +
    "  const tailStart = Math.max(0, byteLen - 16); " +
    "  for (let k = 0; k < keys.length; k++) { " +
    "    const rec = store[keys[k]]; " +
    "    if (!rec || !rec.bytes) continue; " +
    "    const b = rec.bytes; " +
    "    const len = typeof b.length === 'number' ? b.length : (typeof b.byteLength === 'number' ? b.byteLength : -1); " +
    "    if (len !== byteLen) continue; " +
    "    let match = true; " +
    "    for (let i = 0; i < headCount; i++) { " +
    "      if ((b[i] & 0xFF) !== (getByteAt(i) & 0xFF)) { match = false; break; } " +
    "    } " +
    "    if (!match) continue; " +
    "    for (let i = tailStart; i < byteLen; i++) { " +
    "      if ((b[i] & 0xFF) !== (getByteAt(i) & 0xFF)) { match = false; break; } " +
    "    } " +
    "    if (match) return rec; " +
    "  } " +
    "  return null; " +
    "} catch (_e) { return null; } }")
private external fun wasmFindPreloadedRecordByBytes(
    byteLen: Int,
    getByteAt: (Int) -> Int
): JsAny?

@JsFun("(rec) => { " +
    "try { " +
    "  if (!rec) return 0; " +
    "  let img = rec.imageElement; " +
    "  if ((!img || !img.complete || !(img.naturalWidth > 0)) && typeof rec.dataUrl === 'string' && rec.dataUrl.indexOf('data:image/') === 0 && typeof Image === 'function') { " +
    "    img = new Image(); img.src = rec.dataUrl; rec.imageElement = img; " +
    "  } " +
    "  if (img && img.complete && img.naturalWidth > 0) return img.naturalWidth | 0; " +
    "  return 0; " +
    "} catch (_e) { return 0; } }")
private external fun wasmGetPreloadedRecordWidth(rec: JsAny): Int

@JsFun("(rec) => { " +
    "try { " +
    "  if (!rec) return 0; " +
    "  const img = rec.imageElement; " +
    "  if (img && img.complete && img.naturalHeight > 0) return img.naturalHeight | 0; " +
    "  return 0; " +
    "} catch (_e) { return 0; } }")
private external fun wasmGetPreloadedRecordHeight(rec: JsAny): Int

@JsFun("(rec, outW, outH) => { " +
    "try { " +
    "  if (!rec || typeof document === 'undefined') return null; " +
    "  const img = rec.imageElement; " +
    "  if (!img || !img.complete || !(img.naturalWidth > 0)) return null; " +
    "  const canvas = document.createElement('canvas'); " +
    "  canvas.width = outW; canvas.height = outH; " +
    "  const ctx = canvas.getContext('2d'); " +
    "  if (!ctx) return null; " +
    "  ctx.drawImage(img, 0, 0, outW, outH); " +
    "  return canvas; " +
    "} catch (_e) { return null; } }")
private external fun wasmRenderPreloadedRecordToCanvas(rec: JsAny, outW: Int, outH: Int): JsAny?

@JsFun("(peer, mimeType) => { " +
    "try { " +
    "  if (!peer) return null; " +
    "  if (typeof peer.toDataURL === 'function') { " +
    "    const url = peer.toDataURL(mimeType); " +
    "    return (typeof url === 'string' && url.indexOf('data:') === 0) ? url : null; " +
    "  } " +
    "  if (peer.complete && peer.naturalWidth > 0 && peer.naturalHeight > 0 && typeof document !== 'undefined') { " +
    "    const c = document.createElement('canvas'); " +
    "    c.width = peer.naturalWidth | 0; c.height = peer.naturalHeight | 0; " +
    "    const ctx = c.getContext('2d'); " +
    "    if (!ctx) return null; " +
    "    ctx.drawImage(peer, 0, 0, c.width, c.height); " +
    "    const url = c.toDataURL(mimeType); " +
    "    return (typeof url === 'string' && url.indexOf('data:') === 0) ? url : null; " +
    "  } " +
    "  return null; " +
    "} catch (_e) { return null; } }")
private external fun wasmSurfacePeerToDataUrl(peer: JsAny, mimeType: String): String?

@JsFun("(canvasPeer, outW, outH, emitFourBytes) => { " +
    "try { " +
    "  const ctx = canvasPeer.getContext('2d'); " +
    "  if (!ctx) return false; " +
    "  const imgData = ctx.getImageData(0, 0, outW, outH); " +
    "  const d = imgData.data; " +
    "  const total = outW * outH * 4; " +
    "  for (let i = 0; i < total; i += 4) { " +
    "    const packed = ((d[i] & 0xFF) << 24) | ((d[i + 1] & 0xFF) << 16) | ((d[i + 2] & 0xFF) << 8) | (d[i + 3] & 0xFF); " +
    "    emitFourBytes(i, packed); " +
    "  } " +
    "  return true; " +
    "} catch (_e) { return false; } }")
private external fun wasmReadCanvasRgbaPixels(
    canvasPeer: JsAny,
    outW: Int,
    outH: Int,
    emitFourBytes: (Int, Int) -> Unit
): Boolean

@JsFun("(peer, onPlay, onPlaying, onPause, onEnded, onWaiting, onCanPlay, onLoadedData, onSeeked, onTimeUpdate, onDurationChange, onLoadedMetadata, onBufferedSync, onProgress, onError) => { " +
    "try { " +
    "  if (peer.__genericUiMediaAttached) return; " +
    "  peer.__genericUiMediaAttached = true; " +
    "  const syncBuffered = (fireProgress) => { " +
    "    try { " +
    "      const b = peer.buffered; " +
    "      if (b && b.length > 0) { " +
    "        const parts = []; " +
    "        let maxEnd = 0; " +
    "        for (let i = 0; i < b.length; i++) { " +
    "          const s = Math.max(0, b.start(i) || 0); " +
    "          const e = Math.max(s, b.end(i) || 0); " +
    "          if (!isNaN(s) && !isNaN(e) && e > s) { " +
    "            parts.push(s + '-' + e); " +
    "            if (e > maxEnd) maxEnd = e; " +
    "          } " +
    "        } " +
    "        if (parts.length > 0) { " +
    "          onBufferedSync(parts.join(','), maxEnd); " +
    "        } " +
    "        if (fireProgress && typeof maxEnd === 'number' && !isNaN(maxEnd) && maxEnd >= 0) { " +
    "          onProgress(maxEnd); " +
    "        } " +
    "      } " +
    "    } catch (_e) {} " +
    "  }; " +
    "  peer.addEventListener('play', () => { onPlay(); }); " +
    "  peer.addEventListener('playing', () => { onPlaying(); }); " +
    "  peer.addEventListener('pause', () => { onPause(); }); " +
    "  peer.addEventListener('ended', () => { onEnded(); }); " +
    "  peer.addEventListener('waiting', () => { onWaiting(); }); " +
    "  peer.addEventListener('canplay', () => { syncBuffered(false); onCanPlay(); }); " +
    "  peer.addEventListener('loadeddata', () => { syncBuffered(false); onLoadedData(); }); " +
    "  peer.addEventListener('seeked', () => { syncBuffered(false); onSeeked(); }); " +
    "  peer.addEventListener('timeupdate', () => { " +
    "    syncBuffered(false); " +
    "    const ct = typeof peer.currentTime === 'number' && !isNaN(peer.currentTime) ? peer.currentTime : 0; " +
    "    if (ct >= 0) onTimeUpdate(ct); " +
    "  }); " +
    "  const syncDur = () => { " +
    "    const d = typeof peer.duration === 'number' && isFinite(peer.duration) ? peer.duration : 0; " +
    "    if (d > 0) onDurationChange(d); " +
    "  }; " +
    "  peer.addEventListener('durationchange', syncDur); " +
    "  peer.addEventListener('loadedmetadata', () => { syncDur(); syncBuffered(false); onLoadedMetadata(); }); " +
    "  peer.addEventListener('progress', () => { syncBuffered(true); }); " +
    "  peer.addEventListener('error', () => { " +
    "    const errObj = peer.error; " +
    "    const msg = (errObj && errObj.message) ? String(errObj.message) : (errObj && errObj.code ? ('MediaError code ' + errObj.code) : 'Media playback error'); " +
    "    onError(msg); " +
    "  }); " +
    "} catch (_e) {} }")
private external fun wasmAttachMediaListeners(
    peer: JsAny,
    onPlay: () -> Unit,
    onPlaying: () -> Unit,
    onPause: () -> Unit,
    onEnded: () -> Unit,
    onWaiting: () -> Unit,
    onCanPlay: () -> Unit,
    onLoadedData: () -> Unit,
    onSeeked: () -> Unit,
    onTimeUpdate: (Double) -> Unit,
    onDurationChange: (Double) -> Unit,
    onLoadedMetadata: () -> Unit,
    onBufferedSync: (String, Double) -> Unit,
    onProgress: (Double) -> Unit,
    onError: (String) -> Unit
)

@JsFun("(peer, tagName, mimeType) => { " +
    "try { " +
    "  const tag = (tagName && String(tagName).toLowerCase() === 'audio') ? 'audio' : 'video'; " +
    "  const el = (peer && typeof peer.canPlayType === 'function') ? peer : (typeof document !== 'undefined' ? document.createElement(tag) : null); " +
    "  if (!el || typeof el.canPlayType !== 'function') return false; " +
    "  const res = String(el.canPlayType(mimeType) || '').trim().toLowerCase(); " +
    "  return res === 'probably' || res === 'maybe'; " +
    "} catch (_e) { return false; } }")
private external fun wasmMediaCanPlayType(peer: JsAny?, tagName: String, mimeType: String): Boolean

@JsFun("(peer, seekOffsetSeconds) => { " +
    "try { " +
    "  const preloadAttr = String((typeof peer.getAttribute === 'function' && peer.getAttribute('preload')) || peer.preload || '').trim().toLowerCase(); " +
    "  if (preloadAttr !== 'none') { peer.preload = 'auto'; } " +
    "  if (typeof peer.load === 'function') peer.load(); " +
    "  const isVideo = String(peer.tagName || '').toLowerCase() === 'video'; " +
    "  const rawSrc = String((typeof peer.getAttribute === 'function' && peer.getAttribute('src')) || peer.src || ''); " +
    "  const isTranscodeSrc = /(?:[?&]mode=transcode|\\/v1\\/transcode\\/|[?&](?:start|start_seconds|tv|ta|tc)=)/i.test(rawSrc); " +
    "  const applySeek = () => { " +
    "    try { " +
    "      if (seekOffsetSeconds > 0) { peer.currentTime = seekOffsetSeconds; } " +
    "      else if (isVideo && !isTranscodeSrc && peer.paused !== false && preloadAttr !== 'none') { peer.currentTime = 0.001; } " +
    "    } catch (_e) {} " +
    "  }; " +
    "  if ((peer.readyState || 0) >= 1) applySeek(); " +
    "  else { " +
    "    let applied = false; " +
    "    const listener = () => { if (!applied) { applied = true; peer.removeEventListener('loadedmetadata', listener); applySeek(); } }; " +
    "    peer.addEventListener('loadedmetadata', listener); " +
    "  } " +
    "  return true; " +
    "} catch (_e) { return false; } }")
private external fun wasmMediaLoad(peer: JsAny, seekOffsetSeconds: Double): Boolean

@JsFun("(peer, onRejected) => { " +
    "try { " +
    "  if (typeof peer.play !== 'function') return false; " +
    "  const p = peer.play(); " +
    "  if (p && typeof p.catch === 'function') { " +
    "    p.catch((err) => { " +
    "      const errName = (err && err.name) ? String(err.name) : ''; " +
    "      const msg = (err && err.message) ? String(err.message) : String(err || 'Playback blocked'); " +
    "      const low = msg.toLowerCase(); " +
    "      if (errName === 'AbortError' || low.indexOf('aborterror') !== -1 || low.indexOf('interrupted by a call to pause') !== -1 || low.indexOf('interrupted by a new load request') !== -1) return; " +
    "      onRejected(msg); " +
    "    }); " +
    "  } " +
    "  return true; " +
    "} catch (e) { onRejected((e && e.message) ? String(e.message) : String(e || 'Playback failed')); return false; } }")
private external fun wasmMediaPlay(peer: JsAny, onRejected: (String) -> Unit): Boolean

@JsFun("(peer) => { try { if (typeof peer.pause === 'function') { peer.pause(); return true; } } catch (_e) {} return false; }")
private external fun wasmMediaPause(peer: JsAny): Boolean

@JsFun("(peer, seconds) => { " +
    "try { " +
    "  const s = seconds < 0 ? 0 : seconds; " +
    "  if ((peer.readyState || 0) >= 1) { peer.currentTime = s; } " +
    "  else { " +
    "    let applied = false; " +
    "    const listener = () => { if (!applied) { applied = true; peer.removeEventListener('loadedmetadata', listener); try { peer.currentTime = s; } catch (_e) {} } }; " +
    "    peer.addEventListener('loadedmetadata', listener); " +
    "  } " +
    "  return true; " +
    "} catch (_e) { return false; } }")
private external fun wasmMediaSeekTo(peer: JsAny, seconds: Double): Boolean

@JsFun("(peer, volume) => { try { peer.volume = Math.max(0, Math.min(1, volume)); return true; } catch (_e) { return false; } }")
private external fun wasmMediaSetVolume(peer: JsAny, volume: Float): Boolean

@JsFun("(peer, muted) => { try { peer.muted = !!muted; return true; } catch (_e) { return false; } }")
private external fun wasmMediaSetMuted(peer: JsAny, muted: Boolean): Boolean

@JsFun("(peer, rate) => { try { peer.playbackRate = Math.max(0.25, Math.min(4, rate)); return true; } catch (_e) { return false; } }")
private external fun wasmMediaSetPlaybackRate(peer: JsAny, rate: Float): Boolean

@JsFun("(text, typeSuffix) => { try { const g = typeof window !== 'undefined' ? window : globalThis; if (g && g.GenericUiSplash) { if (typeof g.GenericUiSplash.setStatus === 'function') g.GenericUiSplash.setStatus(text, typeSuffix); else if (typeof g.GenericUiSplash.setApplicationStatus === 'function') g.GenericUiSplash.setApplicationStatus(text, typeSuffix); } } catch (_e) {} }")
private external fun wasmSetSplashStatus(text: String, typeSuffix: String)

@JsFun("(pct, typeSuffix) => { try { const g = typeof window !== 'undefined' ? window : globalThis; if (g && g.GenericUiSplash && typeof g.GenericUiSplash.setProgress === 'function') g.GenericUiSplash.setProgress(pct, typeSuffix); } catch (_e) {} }")
private external fun wasmSetSplashProgress(pct: Int, typeSuffix: String)

@JsFun("(msg, stack, title) => { try { const g = typeof window !== 'undefined' ? window : globalThis; if (g && g.GenericUiSplash) { if (typeof g.GenericUiSplash.setError === 'function') g.GenericUiSplash.setError(msg, stack, title); else if (typeof g.GenericUiSplash.showError === 'function') { const e = new Error(msg); e.name = title; if (stack) e.stack = stack; g.GenericUiSplash.showError(e, title); } } } catch (_e) {} }")
private external fun wasmSetSplashError(msg: String, stack: String, title: String)

@JsFun("(fadeDurationMs) => { try { const g = typeof window !== 'undefined' ? window : globalThis; if (g && g.GenericUiSplash && typeof g.GenericUiSplash.finish === 'function') g.GenericUiSplash.finish(fadeDurationMs); } catch (_e) {} }")
private external fun wasmFinishSplash(fadeDurationMs: Int)

@JsFun("(peer, onFileRead, onAllDone) => { " +
    "try { " +
    "  const files = peer.files; " +
    "  const len = (files && typeof files.length === 'number') ? (files.length | 0) : 0; " +
    "  if (len <= 0) { onAllDone(); return; } " +
    "  let remaining = len; " +
    "  for (let i = 0; i < len; i++) { " +
    "    const f = files[i]; " +
    "    const name = (f && typeof f.name === 'string') ? f.name : 'file'; " +
    "    const path = (f && typeof f.webkitRelativePath === 'string' && f.webkitRelativePath.length > 0) ? f.webkitRelativePath : name; " +
    "    const size = (f && typeof f.size === 'number') ? f.size : 0; " +
    "    const mime = (f && typeof f.type === 'string') ? f.type : ''; " +
    "    const reader = new FileReader(); " +
    "    reader.onload = () => { " +
    "      const res = typeof reader.result === 'string' ? reader.result : ''; " +
    "      onFileRead(name, path, size, mime, res); " +
    "      remaining--; if (remaining === 0) onAllDone(); " +
    "    }; " +
    "    reader.onerror = () => { " +
    "      remaining--; if (remaining === 0) onAllDone(); " +
    "    }; " +
    "    reader.readAsDataURL(f); " +
    "  } " +
    "} catch (_e) { onAllDone(); } }")
private external fun wasmReadFileInputFiles(
    peer: JsAny,
    onFileRead: (String, String, Double, String, String) -> Unit,
    onAllDone: () -> Unit
)

@JsFun("(fileName, mimeType, dataUrl) => { " +
    "try { " +
    "  const commaIdx = dataUrl.indexOf(','); " +
    "  const b64 = commaIdx >= 0 ? dataUrl.substring(commaIdx + 1) : dataUrl; " +
    "  const binStr = typeof atob === 'function' ? atob(b64) : ''; " +
    "  const u8 = new Uint8Array(binStr.length); " +
    "  for (let i = 0; i < binStr.length; i++) u8[i] = binStr.charCodeAt(i) & 0xFF; " +
    "  const blob = new Blob([u8], { type: mimeType || 'application/octet-stream' }); " +
    "  const url = URL.createObjectURL(blob); " +
    "  const a = document.createElement('a'); " +
    "  a.href = url; a.download = fileName || 'download.bin'; a.style.display = 'none'; " +
    "  if (document.body) document.body.appendChild(a); " +
    "  a.click(); " +
    "  if (a.parentNode) a.parentNode.removeChild(a); " +
    "  setTimeout(() => { URL.revokeObjectURL(url); }, 1000); " +
    "  return true; " +
    "} catch (_e) { return false; } }")
private external fun wasmTriggerBrowserFileDownload(
    fileName: String,
    mimeType: String,
    dataUrl: String
): Boolean

actual object WebBrowserDomBridge {
    private var nextPeerId: Int = 1
    private val peerById = HashMap<Int, WebDomElement>()
    private var popupCaptureInstalled: Boolean = false
    private var lastDomKeyEventId: Int = 0
    private var lastDomKeyEvent: KeyEvent? = null

    private fun toKeyEvent(
        eventId: Int,
        key: String,
        code: String,
        shiftKey: Boolean,
        ctrlKey: Boolean,
        altKey: Boolean,
        metaKey: Boolean,
        isRepeat: Boolean,
        isTargetTextInput: Boolean
    ): KeyEvent {
        com.nexustools.genericui.event.InputFocusManager.isNativeTextInputFocused = isTargetTextInput
        val existing = lastDomKeyEvent
        if (eventId > 0 && eventId == lastDomKeyEventId && existing != null) {
            return existing
        }
        val created = KeyEvent(
            key = key,
            code = code,
            shiftKey = shiftKey,
            ctrlKey = ctrlKey,
            altKey = altKey,
            metaKey = metaKey,
            isRepeat = isRepeat,
            isTargetTextInput = isTargetTextInput
        )
        if (eventId > 0) {
            lastDomKeyEventId = eventId
            lastDomKeyEvent = created
        }
        return created
    }

    actual val isBrowserEnvironment: Boolean
        get() = wasmIsBrowserEnv()

    private fun ensurePopupCaptureInstalled() {
        if (popupCaptureInstalled || !isBrowserEnvironment) return
        popupCaptureInstalled = true
        wasmEnsurePopupCaptureListenersInstalled(
            hasPopupFn = { com.nexustools.genericui.event.PopupMouseCaptureManager.hasActivePopup },
            onDownFn = { cx, cy, pid ->
                WebDomElement.interceptPopupPointerDown(cx, cy, peerById[pid])
            },
            onUpFn = { cx, cy, pid ->
                WebDomElement.interceptPopupPointerUp(cx, cy, peerById[pid])
            },
            onClickFn = { cx, cy, pid ->
                WebDomElement.interceptPopupClick(cx, cy, peerById[pid])
            }
        )
    }

    actual fun createPeer(element: WebDomElement) {
        if (!isBrowserEnvironment) return
        val peer = wasmCreateElement(element.tagName)
        val pid = nextPeerId++
        peerById[pid] = element
        wasmSetPeerId(peer, pid)
        element.browserPeer = peer
        ensurePopupCaptureInstalled()
        attachInteractiveListeners(element, peer)
    }

    private fun attachInteractiveListeners(model: WebDomElement, peer: JsAny) {
        wasmAttachBasicListeners(
            peer = peer,
            onClick = { originPid, isDirectTarget ->
                if (!model.disabled) {
                    val originModel = peerById[originPid] ?: model
                    val isModalContainer = model.tagName.equals("genericui:dialogwindow", ignoreCase = true) ||
                        model.tagName.equals("genericui:modal", ignoreCase = true) ||
                        model.classList.contains("modal") ||
                        model.enumClasses.contains("modal")
                    if (!isModalContainer || isDirectTarget || originModel === model) {
                        model.dispatchFromBrowser("click", originModel)
                        model.hasActiveClickListener()
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
            onFocus = { isEditable ->
                if (isEditable) {
                    com.nexustools.genericui.event.InputFocusManager.isNativeTextInputFocused = true
                }
                model.dispatchFromBrowser("focus", null)
            },
            onBlur = { isEditable ->
                if (isEditable) {
                    com.nexustools.genericui.event.InputFocusManager.isNativeTextInputFocused = false
                }
                model.dispatchFromBrowser("blur", null)
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
                if (model.tagName.equals("input", ignoreCase = true) && model.getAttribute("type") == "file") {
                    handleWasmFileInputChange(model, peer)
                } else {
                    model.dispatchFromBrowser("change", model.value)
                }
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
            },
            onScroll = { st, sl, cw, ch, sw, sh ->
                model.updateScrollFromBrowser(st, sl, cw, ch, sw, sh)
                model.dispatchFromBrowser("scroll", null)
            }
        )
        wasmAttachInputListeners(
            peer = peer,
            hasContextMenuListener = { "contextmenu" in model.eventListeners },
            onContextMenu = { cx, cy ->
                if (!model.disabled && "contextmenu" in model.eventListeners) {
                    model.dispatchEvent("contextmenu", "$cx,$cy")
                }
            },
            onPointerDown = { x, y, btn, btns, shift, ctrl, alt, meta ->
                if (!model.disabled) {
                    wasmEnsureGlobalWindowPointerDragBridge(
                        onMove = { mx, my, mbtn, mbtns, mshift, mctrl, malt, mmeta ->
                            if (com.nexustools.genericui.event.PointerDragManager.isDragging) {
                                com.nexustools.genericui.event.PointerDragManager.notifyGlobalPointerMove(
                                    PointerEvent(x = mx, y = my, button = mbtn, buttonsMask = mbtns, shiftKey = mshift, ctrlKey = mctrl, altKey = malt, metaKey = mmeta)
                                )
                            }
                        },
                        onUp = { ux, uy, ubtn, ubtns, ushift, uctrl, ualt, umeta ->
                            if (com.nexustools.genericui.event.PointerDragManager.isDragging) {
                                com.nexustools.genericui.event.PointerDragManager.notifyGlobalPointerUp(
                                    PointerEvent(x = ux, y = uy, button = ubtn, buttonsMask = ubtns, shiftKey = ushift, ctrlKey = uctrl, altKey = ualt, metaKey = umeta)
                                )
                            }
                        }
                    )
                    model.dispatchFromBrowser(
                        "pointerdown",
                        PointerEvent(x = x, y = y, button = btn, buttonsMask = btns, shiftKey = shift, ctrlKey = ctrl, altKey = alt, metaKey = meta)
                    )
                }
            },
            onPointerMove = { x, y, btn, btns, shift, ctrl, alt, meta ->
                if (!model.disabled) {
                    model.dispatchFromBrowser(
                        "pointermove",
                        PointerEvent(x = x, y = y, button = btn, buttonsMask = btns, shiftKey = shift, ctrlKey = ctrl, altKey = alt, metaKey = meta)
                    )
                }
            },
            onPointerUp = { x, y, btn, btns, shift, ctrl, alt, meta ->
                if (!model.disabled) {
                    model.dispatchFromBrowser(
                        "pointerup",
                        PointerEvent(x = x, y = y, button = btn, buttonsMask = btns, shiftKey = shift, ctrlKey = ctrl, altKey = alt, metaKey = meta)
                    )
                }
            },
            onKeyDown = { eventId, key, code, shift, ctrl, alt, meta, repeat, isEditable ->
                if (!model.disabled) {
                    val isWindowTag = model.tagName.equals("genericui:window", ignoreCase = true)
                    val hasListener = isWindowTag ||
                        (model.ownerNativeImpl as? WebBaseNativeImpl)?.hasKeyDownListener() == true ||
                        model.eventListeners["keydown"]?.isNotEmpty() == true
                    if (hasListener) {
                        model.dispatchFromBrowser(
                            "keydown",
                            toKeyEvent(eventId, key, code, shift, ctrl, alt, meta, repeat, isEditable)
                        )
                    } else {
                        com.nexustools.genericui.event.InputFocusManager.isNativeTextInputFocused = isEditable
                    }
                }
            },
            onKeyUp = { eventId, key, code, shift, ctrl, alt, meta, repeat, isEditable ->
                if (!model.disabled) {
                    val isWindowTag = model.tagName.equals("genericui:window", ignoreCase = true)
                    val hasListener = isWindowTag ||
                        (model.ownerNativeImpl as? WebBaseNativeImpl)?.hasKeyUpListener() == true ||
                        model.eventListeners["keyup"]?.isNotEmpty() == true
                    if (hasListener) {
                        model.dispatchFromBrowser(
                            "keyup",
                            toKeyEvent(eventId, key, code, shift, ctrl, alt, meta, repeat, isEditable)
                        )
                    } else {
                        com.nexustools.genericui.event.InputFocusManager.isNativeTextInputFocused = isEditable
                    }
                }
            }
        )
    }

    private fun handleWasmFileInputChange(element: WebDomElement, peer: JsAny) {
        val selected = ArrayList<SelectedFile>()
        wasmReadFileInputFiles(
            peer = peer,
            onFileRead = { name, path, size, mime, dataUrl ->
                val bytes = DataUrlCodec.decodeBytes(dataUrl) ?: ByteArray(0)
                selected.add(
                    SelectedFile(
                        name = name,
                        pathOrUri = path,
                        sizeBytes = size.toLong(),
                        mimeType = mime.takeIf { it.isNotEmpty() },
                        isDirectory = false,
                        bytesReader = { bytes }
                    )
                )
            },
            onAllDone = {
                element.dispatchFromBrowser("change", selected)
            }
        )
    }

    actual fun bindApplicationRoot(element: WebDomElement) {
        if (!isBrowserEnvironment) return
        ensurePopupCaptureInstalled()
        val oldPeer = element.browserPeer as? JsAny
        val newPeer = wasmBindRoot(oldPeer)
        val pid = nextPeerId++
        peerById[pid] = element
        wasmSetPeerId(newPeer, pid)
        element.browserPeer = newPeer
    }

    actual fun syncElementTree(element: WebDomElement) {
        if (!isBrowserEnvironment) return
        syncElementTreeNodesRecursive(element)
        WebPrintIframeBridge.syncMountedSubtreeIframes(element)
    }

    private fun syncElementTreeNodesRecursive(element: WebDomElement) {
        val parentPeer = element.browserPeer as? JsAny
        if (parentPeer != null) {
            for ((index, child) in element.children.withIndex()) {
                val childPeer = child.browserPeer as? JsAny ?: continue
                wasmInsertChildAt(parentPeer, index, childPeer)
            }
        }
        for (child in element.children) {
            syncElementTreeNodesRecursive(child)
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

    actual fun syncScrollTop(element: WebDomElement, scrollTop: Double) {
        val peer = element.browserPeer as? JsAny ?: return
        wasmSetScrollTop(peer, scrollTop)
    }

    actual fun syncScrollLeft(element: WebDomElement, scrollLeft: Double) {
        val peer = element.browserPeer as? JsAny ?: return
        wasmSetScrollLeft(peer, scrollLeft)
    }

    actual fun appendChild(parent: WebDomElement, child: WebDomElement) {
        val parentPeer = parent.browserPeer as? JsAny
        val childPeer = child.browserPeer as? JsAny
        if (parentPeer != null && childPeer != null) {
            wasmAppendChild(parentPeer, childPeer)
        }
        WebPrintIframeBridge.syncMountedSubtreeIframes(child)
    }

    actual fun insertChildAt(parent: WebDomElement, index: Int, child: WebDomElement) {
        val parentPeer = parent.browserPeer as? JsAny
        val childPeer = child.browserPeer as? JsAny
        if (parentPeer != null && childPeer != null) {
            wasmInsertChildAt(parentPeer, index, childPeer)
        }
        WebPrintIframeBridge.syncMountedSubtreeIframes(child)
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

    actual fun setHeadStyleElement(id: String, cssText: String?) {
        if (isBrowserEnvironment) wasmSetHeadStyleElement(id, cssText.orEmpty())
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

    actual fun requestAnimationFrame(callback: () -> Unit): Int {
        if (isBrowserEnvironment) {
            val id = wasmRequestAnimationFrame(callback)
            if (id >= 0) return id
        }
        return setTimeoutId(16, callback)
    }

    actual fun cancelAnimationFrame(handle: Int) {
        if (handle < 0) return
        if (!isBrowserEnvironment || !wasmCancelAnimationFrame(handle)) {
            clearTimeoutId(handle)
        }
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

    actual fun localStorageGetItem(key: String): String? {
        if (hasLocalStorage) {
            wasmLocalStorageGetItem(key)?.let { return it }
        }
        return headlessLocalStorage[key]
    }

    actual fun localStorageSetItem(key: String, value: String): Boolean {
        headlessLocalStorage[key] = value
        return if (hasLocalStorage) wasmLocalStorageSetItem(key, value) else true
    }

    actual fun localStorageRemoveItem(key: String): Boolean {
        val existedInHeadless = headlessLocalStorage.remove(key) != null
        return if (hasLocalStorage) wasmLocalStorageRemoveItem(key) else existedInHeadless
    }

    actual fun localStorageClear(): Boolean {
        headlessLocalStorage.clear()
        return if (hasLocalStorage) wasmLocalStorageClear() else true
    }

    actual fun localStorageKeys(): List<String> {
        val out = LinkedHashSet<String>()
        if (hasLocalStorage) {
            val len = wasmLocalStorageLength()
            for (i in 0 until len) {
                val k = wasmLocalStorageKeyAt(i)
                if (k != null) out.add(k)
            }
        }
        out.addAll(headlessLocalStorage.keys)
        return out.toList()
    }

    actual fun registerStorageListener(onStorageChanged: (key: String?, newValue: String?) -> Unit) {
        headlessStorageListeners.add(onStorageChanged)
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

    actual fun getPreloadedResourceDataUrl(key: String): String? {
        getPreloadedDataUrlFromMemory(key)?.let { return it }
        for (candidate in expandPreloadedResourceLookupKeys(key)) {
            val found = wasmGetPreloadedDataUrl(candidate)
            if (!found.isNullOrBlank()) return found
        }
        return null
    }

    actual fun getPreloadedResourceBytes(key: String): ByteArray? {
        getPreloadedBytesFromMemory(key)?.let { return it }
        val dataUrl = getPreloadedResourceDataUrl(key)
        if (!dataUrl.isNullOrBlank()) {
            DataUrlCodec.decodeBytes(dataUrl)?.let { return it }
        }
        for (candidate in expandPreloadedResourceLookupKeys(key)) {
            val text = wasmGetPreloadedText(candidate)
            if (text != null) return text.encodeToByteArray()
        }
        return null
    }

    actual fun getPreloadedResourceText(key: String): String? {
        getPreloadedTextFromMemory(key)?.let { return it }
        for (candidate in expandPreloadedResourceLookupKeys(key)) {
            val text = wasmGetPreloadedText(candidate)
            if (text != null) return text
        }
        val dataUrl = getPreloadedResourceDataUrl(key)
        if (!dataUrl.isNullOrBlank()) {
            return DataUrlCodec.decodeUtf8Text(dataUrl)
        }
        return null
    }

    actual fun hasPreloadedResource(key: String): Boolean {
        if (hasPreloadedResourceInMemory(key)) return true
        for (candidate in expandPreloadedResourceLookupKeys(key)) {
            if (wasmHasPreloadedResource(candidate)) return true
        }
        return false
    }

    actual fun registerPreloadedResource(key: String, mimeType: String, bytes: ByteArray, text: String?) {
        val (resolvedMime, dataUrl, resolvedText) = registerPreloadedResourceInMemory(key, mimeType, bytes, text)
        for (candidate in expandPreloadedResourceLookupKeys(key)) {
            wasmSetPreloadedResource(
                key = candidate,
                mimeType = resolvedMime,
                dataUrl = dataUrl,
                text = resolvedText ?: "",
                hasText = resolvedText != null
            )
        }
    }

    actual fun clearPreloadedResources() {
        clearPreloadedResourcesInMemory()
        wasmClearPreloadedResources()
    }

    actual fun surfacePeerToDataUrl(peer: Any?, mimeType: String): String? {
        if (!isBrowserEnvironment || peer == null) return null
        val rawPeer = (if (peer is WebDomElement) peer.browserPeer else peer) as? JsAny ?: return null
        return wasmSurfacePeerToDataUrl(rawPeer, mimeType)
    }

    actual fun decodePreloadedBrowserImage(
        uriOrKey: String?,
        encodedBytes: ByteArray?,
        targetWidth: Int?,
        targetHeight: Int?,
        preserveAspectRatio: Boolean
    ): ImageBuffer? {
        if (!isBrowserEnvironment) return null
        var record: JsAny? = null
        if (!uriOrKey.isNullOrBlank()) {
            for (candidate in expandPreloadedResourceLookupKeys(uriOrKey)) {
                val found = wasmFindPreloadedRecordByKey(candidate)
                if (found != null) {
                    record = found
                    break
                }
            }
        }
        if (record == null && encodedBytes != null && encodedBytes.isNotEmpty()) {
            record = wasmFindPreloadedRecordByBytes(encodedBytes.size) { idx ->
                encodedBytes[idx].toInt() and 0xFF
            }
        }
        if (record == null) return null

        val natW = wasmGetPreloadedRecordWidth(record)
        val natH = wasmGetPreloadedRecordHeight(record)
        if (natW <= 0 || natH <= 0) return null

        val (outW, outH) = resolvePreloadedImageCanvasDimensions(
            naturalWidth = natW,
            naturalHeight = natH,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            preserveAspectRatio = preserveAspectRatio
        )
        val canvasPeer = wasmRenderPreloadedRecordToCanvas(record, outW, outH) ?: return null

        return createLazyBrowserCanvasImageBuffer(
            width = outW,
            height = outH,
            canvasPeer = canvasPeer,
            toDataUrlFn = { mimeType ->
                wasmSurfacePeerToDataUrl(canvasPeer, mimeType)
            },
            readRgbaPixelsFn = {
                val loadedBytes = ByteArray(outW * outH * 4)
                wasmReadCanvasRgbaPixels(canvasPeer, outW, outH) { offset, packed ->
                    if (offset + 3 < loadedBytes.size) {
                        loadedBytes[offset] = ((packed ushr 24) and 0xFF).toByte()
                        loadedBytes[offset + 1] = ((packed ushr 16) and 0xFF).toByte()
                        loadedBytes[offset + 2] = ((packed ushr 8) and 0xFF).toByte()
                        loadedBytes[offset + 3] = (packed and 0xFF).toByte()
                    }
                }
                loadedBytes
            }
        )
    }

    actual fun requestElementFullscreen(element: WebDomElement?): Boolean {
        activeFullscreenElement = element
        if (!isBrowserEnvironment) return true
        return wasmRequestElementFullscreen(element?.browserPeer as? JsAny)
    }

    actual fun exitFullscreen(): Boolean {
        activeFullscreenElement = null
        if (!isBrowserEnvironment) return true
        return wasmExitFullscreen()
    }

    actual fun registerFullscreenChange(onFullscreenChanged: (Boolean) -> Unit) {
        headlessFullscreenListeners.add(onFullscreenChanged)
        if (!isBrowserEnvironment) return
        wasmRegisterFullscreenChange { isFs ->
            if (!isFs) {
                activeFullscreenElement = null
            }
            onFullscreenChanged(isFs)
        }
    }

    actual fun bindInvisibleHtmlImageLoader(
        imgElement: WebDomElement,
        src: String,
        onLoaded: (naturalWidth: Int, naturalHeight: Int) -> Unit,
        onError: (String) -> Unit
    ): Boolean {
        if (!isBrowserEnvironment) return false
        val peer = imgElement.browserPeer as? JsAny ?: return false
        imgElement.setAttribute("src", src)
        return wasmBindInvisibleImageLoader(
            peer = peer,
            src = src,
            onLoaded = { natW, natH ->
                imgElement.setAttribute("data-complete", "true")
                imgElement.setAttribute("data-natural-width", natW.toString())
                imgElement.setAttribute("data-natural-height", natH.toString())
                onLoaded(natW, natH)
            },
            onError = { err ->
                imgElement.setAttribute("data-error", "true")
                onError(err)
            }
        )
    }

    actual fun attachMediaElementListeners(element: WebDomElement) {
        if (!isBrowserEnvironment) return
        val peer = element.browserPeer as? JsAny ?: return
        wasmAttachMediaListeners(
            peer = peer,
            onPlay = { element.dispatchFromBrowser("play", null) },
            onPlaying = { element.dispatchFromBrowser("playing", null) },
            onPause = { element.dispatchFromBrowser("pause", null) },
            onEnded = { element.dispatchFromBrowser("ended", null) },
            onWaiting = { element.dispatchFromBrowser("waiting", null) },
            onCanPlay = { element.dispatchFromBrowser("canplay", null) },
            onLoadedData = { element.dispatchFromBrowser("loadeddata", null) },
            onSeeked = { element.dispatchFromBrowser("seeked", null) },
            onTimeUpdate = { ctSeconds ->
                element.setAttribute("data-current-time", ctSeconds.toString())
                element.dispatchFromBrowser("timeupdate", ctSeconds)
            },
            onDurationChange = { durSeconds ->
                element.setAttribute("data-duration", durSeconds.toString())
                element.dispatchFromBrowser("durationchange", durSeconds)
            },
            onLoadedMetadata = {
                element.dispatchFromBrowser("loadedmetadata", null)
            },
            onBufferedSync = { rangesStr, maxEnd ->
                element.setAttribute("data-buffered-ranges", rangesStr)
                element.setAttribute("data-buffered-time", maxEnd.toString())
            },
            onProgress = { endSec ->
                element.setAttribute("data-buffered-time", endSec.toString())
                element.dispatchFromBrowser("progress", endSec)
            },
            onError = { errMsg ->
                element.dispatchFromBrowser("error", errMsg)
            }
        )
    }

    actual fun mediaCanPlayType(element: WebDomElement, mimeType: String): Boolean {
        if (!isBrowserEnvironment) return false
        return wasmMediaCanPlayType(element.browserPeer as? JsAny, element.tagName, mimeType)
    }

    actual fun mediaLoad(element: WebDomElement, seekOffsetSeconds: Double): Boolean {
        element.removeAttribute("data-last-seek-sec")
        val preloadAttr = element.getAttribute("preload")?.trim()?.lowercase()
        if (preloadAttr != "none") {
            element.setAttribute("preload", preloadAttr ?: "auto")
        }
        if (!isBrowserEnvironment) return false
        val peer = element.browserPeer as? JsAny ?: return false
        return wasmMediaLoad(peer, seekOffsetSeconds)
    }

    actual fun mediaPlay(element: WebDomElement, onRejected: ((String) -> Unit)?): Boolean {
        if (!isBrowserEnvironment) return false
        val peer = element.browserPeer as? JsAny ?: return false
        return wasmMediaPlay(peer) { err ->
            onRejected?.invoke(err)
        }
    }

    actual fun mediaPause(element: WebDomElement): Boolean {
        if (!isBrowserEnvironment) return false
        val peer = element.browserPeer as? JsAny ?: return false
        return wasmMediaPause(peer)
    }

    actual fun mediaSeekTo(element: WebDomElement, seconds: Double): Boolean {
        val clamped = seconds.coerceAtLeast(0.0)
        element.setAttribute("data-last-seek-sec", clamped.toString())
        if (!isBrowserEnvironment) return false
        val peer = element.browserPeer as? JsAny ?: return false
        return wasmMediaSeekTo(peer, clamped)
    }

    actual fun mediaSetVolume(element: WebDomElement, volume: Float): Boolean {
        if (!isBrowserEnvironment) return false
        val peer = element.browserPeer as? JsAny ?: return false
        return wasmMediaSetVolume(peer, volume)
    }

    actual fun mediaSetMuted(element: WebDomElement, muted: Boolean): Boolean {
        if (!isBrowserEnvironment) return false
        val peer = element.browserPeer as? JsAny ?: return false
        return wasmMediaSetMuted(peer, muted)
    }

    actual fun mediaSetPlaybackRate(element: WebDomElement, rate: Float): Boolean {
        if (!isBrowserEnvironment) return false
        val peer = element.browserPeer as? JsAny ?: return false
        return wasmMediaSetPlaybackRate(peer, rate)
    }

    actual fun installModalFocusTrap(modalElement: WebDomElement) {
        registerHeadlessModalFocusTrap(modalElement)
        if (!isBrowserEnvironment) return
        val peer = modalElement.browserPeer as? JsAny ?: return
        wasmInstallModalFocusTrap(peer)
    }

    actual fun releaseModalFocusTrap(modalElement: WebDomElement) {
        unregisterHeadlessModalFocusTrap(modalElement)
        if (!isBrowserEnvironment) return
        val peer = modalElement.browserPeer as? JsAny ?: return
        wasmReleaseModalFocusTrap(peer)
    }

    actual fun attachOverscrollStretch(scrollElement: WebDomElement, contentViewport: WebDomElement) {
        attachHeadlessOverscrollStretch(scrollElement, contentViewport)
        if (!isBrowserEnvironment) return
        val scrollPeer = scrollElement.browserPeer as? JsAny ?: return
        val viewportPeer = contentViewport.browserPeer as? JsAny ?: return
        wasmAttachOverscrollStretch(scrollPeer, viewportPeer)
    }

    actual fun attachLazyViewportObserver(
        scrollElement: WebDomElement,
        viewportElement: WebDomElement,
        isHorizontal: () -> Boolean,
        onViewportChanged: (
            scrollOffsetPx: Double,
            viewportExtentPx: Int,
            measuredViewportSpanPx: Int,
            renderedChildCount: Int
        ) -> Unit
    ) {
        scrollElement.setAttribute("data-lazy-viewport-observer", "true")
        if (!isBrowserEnvironment) return
        val scrollPeer = scrollElement.browserPeer as? JsAny ?: return
        val viewportPeer = viewportElement.browserPeer as? JsAny ?: return
        wasmAttachLazyViewportObserver(
            scrollPeer = scrollPeer,
            viewportPeer = viewportPeer,
            isHorizFn = isHorizontal
        ) { scrollOffsetPx, viewportExtentPx, measuredSpanPx, childCount, scrollW, scrollH ->
            if (isHorizontal()) {
                scrollElement.updateScrollFromBrowser(
                    newScrollTop = scrollElement.scrollTop,
                    newScrollLeft = scrollOffsetPx,
                    clientW = viewportExtentPx,
                    clientH = scrollElement.clientHeight,
                    scrollW = scrollW,
                    scrollH = scrollH
                )
            } else {
                scrollElement.updateScrollFromBrowser(
                    newScrollTop = scrollOffsetPx,
                    newScrollLeft = scrollElement.scrollLeft,
                    clientW = scrollElement.clientWidth,
                    clientH = viewportExtentPx,
                    scrollW = scrollW,
                    scrollH = scrollH
                )
            }
            onViewportChanged(scrollOffsetPx, viewportExtentPx, measuredSpanPx, childCount)
        }
    }

    actual fun attachSeekBarScrubbing(
        containerElement: WebDomElement,
        rangeInputElement: WebDomElement,
        onScrubStart: (fraction: Double) -> Unit,
        onScrubMove: (fraction: Double) -> Unit,
        onScrubEnd: (fraction: Double) -> Unit
    ) {
        if (!isBrowserEnvironment) {
            attachHeadlessSeekBarScrubbing(containerElement, onScrubStart, onScrubMove, onScrubEnd)
            return
        }
        val containerPeer = containerElement.browserPeer as? JsAny ?: return
        val rangePeer = rangeInputElement.browserPeer as? JsAny
        wasmAttachSeekBarScrubbing(containerPeer, rangePeer, onScrubStart, onScrubMove, onScrubEnd)
    }

    actual fun registerWindowKeyEvents(
        onKeyDown: (com.nexustools.genericui.event.KeyEvent) -> Unit,
        onKeyUp: (com.nexustools.genericui.event.KeyEvent) -> Unit
    ) {
        if (!isBrowserEnvironment) return
        wasmRegisterWindowKeyEvents(
            onKeyDown = { eventId, key, code, shift, ctrl, alt, meta, repeat, isEditable ->
                onKeyDown(toKeyEvent(eventId, key, code, shift, ctrl, alt, meta, repeat, isEditable))
            },
            onKeyUp = { eventId, key, code, shift, ctrl, alt, meta, repeat, isEditable ->
                onKeyUp(toKeyEvent(eventId, key, code, shift, ctrl, alt, meta, repeat, isEditable))
            }
        )
    }

    actual fun setWebSplashStatus(statusText: String, type: BootstrapType) {
        val trimmed = statusText.trim().ifEmpty { WebSplashConfig.STATUS_INITIALIZING }
        lastWebSplashStatus = trimmed
        lastWebSplashStatusType = type
        if (!isBrowserEnvironment) return
        wasmSetSplashStatus(trimmed, type.cssSuffix)
    }

    actual fun setWebSplashProgress(progressPercent: Int, type: BootstrapType) {
        val clamped = progressPercent.coerceIn(0, 100)
        lastWebSplashProgress = clamped
        lastWebSplashProgressType = type
        if (!isBrowserEnvironment) return
        wasmSetSplashProgress(clamped, type.cssSuffix)
    }

    actual fun setWebSplashError(message: String, stackTrace: String?, title: String) {
        val resolvedTitle = title.trim().ifEmpty { WebSplashConfig.STATUS_ERROR }
        lastWebSplashStatus = WebSplashConfig.STATUS_ERROR
        lastWebSplashStatusType = BootstrapType.DANGER
        lastWebSplashProgress = 100
        lastWebSplashProgressType = BootstrapType.DANGER
        lastWebSplashErrorMessage = message
        lastWebSplashErrorStack = stackTrace
        lastWebSplashErrorTitle = resolvedTitle
        if (!isBrowserEnvironment) return
        wasmSetSplashError(message, stackTrace ?: "", resolvedTitle)
    }

    actual fun finishWebSplash(fadeDurationMs: Int) {
        lastWebSplashStatus = WebSplashConfig.STATUS_READY
        lastWebSplashStatusType = BootstrapType.SUCCESS
        lastWebSplashProgress = 100
        isWebSplashFinished = true
        if (!isBrowserEnvironment) return
        wasmFinishSplash(fadeDurationMs.coerceAtLeast(0))
    }

    actual fun triggerBrowserFileDownload(fileName: String, mimeType: String, bytes: ByteArray): Boolean {
        if (!isBrowserEnvironment) return true
        val resolvedMime = mimeType.ifBlank { "application/octet-stream" }
        val resolvedName = fileName.ifBlank { "download.bin" }
        val dataUrl = DataUrlCodec.encodeBytes(bytes, resolvedMime)
        return wasmTriggerBrowserFileDownload(resolvedName, resolvedMime, dataUrl)
    }
}
