package com.nexustools.genericui.web

import com.nexustools.genericui.component.dialog.SelectedFile
import com.nexustools.genericui.event.KeyEvent
import com.nexustools.genericui.event.PointerEvent
import com.nexustools.genericui.event.WheelEvent

actual object WebBrowserDomBridge {

    actual val isBrowserEnvironment: Boolean
        get() = js("typeof document !== 'undefined' && typeof window !== 'undefined'") as Boolean

    actual fun createPeer(element: WebDomElement) {
        if (!isBrowserEnvironment) return
        val doc: dynamic = js("document")
        val peer: dynamic = doc.createElement(element.tagName)
        element.browserPeer = peer
        attachInteractiveListeners(element, peer)
    }

    actual fun bindApplicationRoot(element: WebDomElement) {
        if (!isBrowserEnvironment) return
        val doc: dynamic = js("document")
        val attachToDocument = {
            // Remove any legacy standalone .loader element directly inside body outside genericui:root
            val bodyLoader: dynamic = doc.querySelector("body > .loader")
            if (bodyLoader != null && bodyLoader.parentNode != null) {
                bodyLoader.parentNode.removeChild(bodyLoader)
            }

            val existing: dynamic = doc.getElementsByTagName("genericui:root")?.item(0)
                ?: doc.querySelector("genericui\\:root")
            val host: dynamic = existing ?: doc.createElement("genericui:root")
            if (host != null) {
                if (js("typeof host.hasAttribute === 'function'") as Boolean) {
                    if (host.hasAttribute("id") as Boolean) host.removeAttribute("id")
                    if (host.hasAttribute("data-bs-theme") as Boolean) host.removeAttribute("data-bs-theme")
                }
                val oldPeer: dynamic = element.browserPeer
                if (oldPeer != null && oldPeer !== host) {
                    while (oldPeer.firstChild != null) {
                        host.appendChild(oldPeer.firstChild)
                    }
                }
                element.browserPeer = host
                if (host.parentNode == null && doc.body != null) {
                    doc.body.appendChild(host)
                }
            }
            val win: dynamic = js("window")
            if (win.GenericUiSplash != null && js("typeof win.GenericUiSplash.finish === 'function'") as Boolean) {
                win.GenericUiSplash.finish()
            }
        }

        if (doc.body != null) {
            attachToDocument()
        } else {
            doc.addEventListener("DOMContentLoaded", {
                attachToDocument()
            })
        }
    }

    actual fun syncElementTree(element: WebDomElement) {
        if (!isBrowserEnvironment) return
        val parentPeer: dynamic = resolveContainerTarget(element) ?: return
        for ((index, child) in element.children.withIndex()) {
            val childPeer: dynamic = child.browserPeer ?: continue
            val currentAtIdx: dynamic = parentPeer.children?.get(index)
            if (currentAtIdx !== childPeer) {
                if (currentAtIdx != null) {
                    parentPeer.insertBefore(childPeer, currentAtIdx)
                } else {
                    parentPeer.appendChild(childPeer)
                }
            }
        }
    }

    private fun attachInteractiveListeners(model: WebDomElement, peer: dynamic) {
        peer.addEventListener("click", { _: dynamic ->
            if (!model.disabled) {
                model.dispatchFromBrowser("click", null)
            }
        })
        peer.addEventListener("dblclick", { _: dynamic ->
            if (!model.disabled) {
                model.dispatchFromBrowser("dblclick", null)
            }
        })
        peer.addEventListener("mouseenter", { _: dynamic ->
            if (!model.disabled) {
                model.dispatchFromBrowser("mouseenter", null)
            }
        })
        peer.addEventListener("mouseleave", { _: dynamic ->
            if (!model.disabled) {
                model.dispatchFromBrowser("mouseleave", null)
            }
        })
        peer.addEventListener("input", { _: dynamic ->
            val v = peer.value
            if (v is String) {
                model.updateValueFromBrowser(v)
            }
            val c = peer.checked
            if (c is Boolean) {
                model.updateCheckedFromBrowser(c)
            }
            model.dispatchFromBrowser("input", model.value)
        })
        peer.addEventListener("change", { _: dynamic ->
            val v = peer.value
            if (v is String) {
                model.updateValueFromBrowser(v)
            }
            val c = peer.checked
            if (c is Boolean) {
                model.updateCheckedFromBrowser(c)
            }
            if (model.tagName.equals("input", ignoreCase = true) && model.getAttribute("type") == "file") {
                handleFileInputChange(model, peer)
            } else {
                model.dispatchFromBrowser("change", model.value)
            }
        })
        peer.addEventListener("cancel", { _: dynamic ->
            model.dispatchFromBrowser("cancel", null)
        })
        var longPressTimer: dynamic = null
        var longPressTriggered = false
        var touchStartX = 0f
        var touchStartY = 0f
        val cancelLongPress = {
            if (longPressTimer != null) {
                val win: dynamic = js("window")
                win.clearTimeout(longPressTimer)
                longPressTimer = null
            }
        }
        peer.addEventListener("contextmenu", { event: dynamic ->
            if (!model.disabled && "contextmenu" in model.eventListeners) {
                if (js("typeof event.preventDefault === 'function'") as Boolean) event.preventDefault()
                if (js("typeof event.stopPropagation === 'function'") as Boolean) event.stopPropagation()
                if (longPressTriggered) {
                    longPressTriggered = false
                } else {
                    val cx = (event.clientX as? Number)?.toInt() ?: 0
                    val cy = (event.clientY as? Number)?.toInt() ?: 0
                    model.dispatchEvent("contextmenu", "$cx,$cy")
                }
            }
        })
        peer.addEventListener("touchstart", { event: dynamic ->
            cancelLongPress()
            longPressTriggered = false
            val touchCount = (event.touches?.length as? Number)?.toInt() ?: 0
            if (!model.disabled && "contextmenu" in model.eventListeners && touchCount == 1) {
                val touch: dynamic = event.touches?.get(0)
                if (touch != null) {
                    touchStartX = (touch.clientX as? Number)?.toFloat() ?: 0f
                    touchStartY = (touch.clientY as? Number)?.toFloat() ?: 0f
                    val win: dynamic = js("window")
                    longPressTimer = win.setTimeout({
                        longPressTimer = null
                        if (!model.disabled && "contextmenu" in model.eventListeners) {
                            longPressTriggered = true
                            model.dispatchEvent("contextmenu", "${touchStartX.toInt()},${touchStartY.toInt()}")
                        }
                    }, WebDomElement.LONG_PRESS_THRESHOLD_MS)
                }
            }
        })
        peer.addEventListener("touchmove", { event: dynamic ->
            if (longPressTimer != null) {
                val touchCount = (event.touches?.length as? Number)?.toInt() ?: 0
                val touch: dynamic = event.touches?.get(0)
                if (touchCount != 1 || touch == null) {
                    cancelLongPress()
                } else {
                    val dx = ((touch.clientX as? Number)?.toFloat() ?: 0f) - touchStartX
                    val dy = ((touch.clientY as? Number)?.toFloat() ?: 0f) - touchStartY
                    if (dx * dx + dy * dy > 100f) {
                        cancelLongPress()
                    }
                }
            }
        })
        peer.addEventListener("touchend", { event: dynamic ->
            cancelLongPress()
            if (longPressTriggered) {
                if (js("typeof event.preventDefault === 'function'") as Boolean) event.preventDefault()
                longPressTriggered = false
            }
        })
        peer.addEventListener("touchcancel", { _: dynamic ->
            cancelLongPress()
            longPressTriggered = false
        })
        peer.addEventListener("pointerdown", { ev: dynamic ->
            val pe = toPointerEvent(ev, defaultButton = 0)
            model.dispatchFromBrowser("pointerdown", pe)
        })
        peer.addEventListener("pointermove", { ev: dynamic ->
            val pe = toPointerEvent(ev, defaultButton = 0)
            model.dispatchFromBrowser("pointermove", pe)
        })
        peer.addEventListener("pointerup", { ev: dynamic ->
            val pe = toPointerEvent(ev, defaultButton = 0)
            model.dispatchFromBrowser("pointerup", pe)
        })
        peer.addEventListener("wheel", { ev: dynamic ->
            val dx = (ev.deltaX as? Number)?.toFloat() ?: 0f
            val dy = (ev.deltaY as? Number)?.toFloat() ?: 0f
            val cx = (ev.clientX as? Number)?.toFloat() ?: 0f
            val cy = (ev.clientY as? Number)?.toFloat() ?: 0f
            model.dispatchFromBrowser("wheel", WheelEvent(dx, dy, cx, cy))
        })
        peer.addEventListener("keydown", { ev: dynamic ->
            val ke = toKeyEvent(ev)
            model.dispatchFromBrowser("keydown", ke)
        })
        peer.addEventListener("keyup", { ev: dynamic ->
            val ke = toKeyEvent(ev)
            model.dispatchFromBrowser("keyup", ke)
        })
        peer.addEventListener("load", { _: dynamic ->
            model.dispatchFromBrowser("load", null)
        })
        peer.addEventListener("error", { _: dynamic ->
            model.dispatchFromBrowser("error", null)
        })
    }

    private fun handleFileInputChange(element: WebDomElement, peer: dynamic) {
        val files: dynamic = peer.files
        val len = (files?.length as? Number)?.toInt() ?: 0
        if (len <= 0) {
            element.dispatchFromBrowser("change", emptyList<SelectedFile>())
            return
        }
        val selected = ArrayList<SelectedFile>(len)
        var remaining = len
        for (i in 0 until len) {
            val f: dynamic = files[i]
            val name = (f.name as? String) ?: "file"
            val path = (f.webkitRelativePath as? String)?.takeIf { it.isNotEmpty() } ?: name
            val size = (f.size as? Number)?.toLong() ?: 0L
            val mime = (f.type as? String)?.takeIf { it.isNotEmpty() }
            val reader: dynamic = js("new FileReader()")
            reader.onload = { _: dynamic ->
                val arrayBuffer: dynamic = reader.result
                val uint8: dynamic = js("new Uint8Array(arrayBuffer)")
                val byteLen = (uint8.length as? Number)?.toInt() ?: 0
                val bytes = ByteArray(byteLen) { idx -> ((uint8[idx] as Number).toInt() and 0xFF).toByte() }
                selected.add(
                    SelectedFile(
                        name = name,
                        pathOrUri = path,
                        sizeBytes = size,
                        mimeType = mime,
                        isDirectory = false,
                        bytesReader = { bytes }
                    )
                )
                remaining--
                if (remaining == 0) {
                    element.dispatchFromBrowser("change", selected)
                }
            }
            reader.onerror = { _: dynamic ->
                remaining--
                if (remaining == 0) {
                    element.dispatchFromBrowser("change", selected)
                }
            }
            reader.readAsArrayBuffer(f)
        }
    }

    private fun toPointerEvent(ev: dynamic, defaultButton: Int): PointerEvent {
        val cx = (ev.clientX as? Number)?.toFloat() ?: 0f
        val cy = (ev.clientY as? Number)?.toFloat() ?: 0f
        val btn = (ev.button as? Number)?.toInt() ?: defaultButton
        val btns = (ev.buttons as? Number)?.toInt() ?: 0
        val shift = (ev.shiftKey as? Boolean) ?: false
        val ctrl = (ev.ctrlKey as? Boolean) ?: false
        val alt = (ev.altKey as? Boolean) ?: false
        val meta = (ev.metaKey as? Boolean) ?: false
        return PointerEvent(
            x = cx,
            y = cy,
            button = btn,
            buttonsMask = btns,
            shiftKey = shift,
            ctrlKey = ctrl,
            altKey = alt,
            metaKey = meta
        )
    }

    private fun toKeyEvent(ev: dynamic): KeyEvent {
        val key = (ev.key as? String) ?: ""
        val code = (ev.code as? String) ?: key
        val shift = (ev.shiftKey as? Boolean) ?: false
        val ctrl = (ev.ctrlKey as? Boolean) ?: false
        val alt = (ev.altKey as? Boolean) ?: false
        val meta = (ev.metaKey as? Boolean) ?: false
        val repeat = (ev.repeat as? Boolean) ?: false
        return KeyEvent(
            key = key,
            code = code,
            shiftKey = shift,
            ctrlKey = ctrl,
            altKey = alt,
            metaKey = meta,
            isRepeat = repeat
        )
    }

    actual fun syncId(element: WebDomElement, id: String) {
        val peer: dynamic = element.browserPeer ?: return
        peer.id = id
    }

    actual fun syncClassName(element: WebDomElement, className: String) {
        val peer: dynamic = element.browserPeer ?: return
        peer.className = className
    }

    actual fun syncStyle(element: WebDomElement, property: String, value: String?) {
        val peer: dynamic = element.browserPeer ?: return
        if (value == null) {
            peer.style.removeProperty(property)
        } else {
            peer.style.setProperty(property, value)
        }
    }

    actual fun clearStyles(element: WebDomElement) {
        val peer: dynamic = element.browserPeer ?: return
        peer.removeAttribute("style")
    }

    actual fun syncAttribute(element: WebDomElement, name: String, value: String?) {
        val peer: dynamic = element.browserPeer ?: return
        if (value == null) {
            peer.removeAttribute(name)
        } else {
            peer.setAttribute(name, value)
        }
    }

    actual fun syncTextContent(element: WebDomElement, text: String) {
        val peer: dynamic = element.browserPeer ?: return
        if (element.children.isEmpty()) {
            peer.textContent = text
        }
    }

    actual fun syncInnerHTML(element: WebDomElement, html: String) {
        val peer: dynamic = element.browserPeer ?: return
        peer.innerHTML = html
    }

    actual fun syncValue(element: WebDomElement, value: String) {
        val peer: dynamic = element.browserPeer ?: return
        peer.value = value
    }

    actual fun syncChecked(element: WebDomElement, checked: Boolean) {
        val peer: dynamic = element.browserPeer ?: return
        peer.checked = checked
    }

    actual fun syncDisabled(element: WebDomElement, disabled: Boolean) {
        val peer: dynamic = element.browserPeer ?: return
        peer.disabled = disabled
    }

    actual fun syncReadOnly(element: WebDomElement, readOnly: Boolean) {
        val peer: dynamic = element.browserPeer ?: return
        peer.readOnly = readOnly
    }

    actual fun syncMultiple(element: WebDomElement, multiple: Boolean) {
        val peer: dynamic = element.browserPeer ?: return
        peer.multiple = multiple
    }

    actual fun syncDirectory(element: WebDomElement, directory: Boolean) {
        val peer: dynamic = element.browserPeer ?: return
        peer.webkitdirectory = directory
    }

    actual fun syncAccept(element: WebDomElement, accept: String) {
        val peer: dynamic = element.browserPeer ?: return
        peer.accept = accept
    }

    private fun resolveContainerTarget(parent: WebDomElement): dynamic {
        val parentPeer: dynamic = parent.browserPeer ?: return null
        if (parent.tagName.equals("iframe", ignoreCase = true)) {
            val iframeDoc: dynamic = parentPeer.contentDocument ?: parentPeer.contentWindow?.document
            val body: dynamic = iframeDoc?.body
            if (body != null) return body
            return null
        }
        return parentPeer
    }

    actual fun appendChild(parent: WebDomElement, child: WebDomElement) {
        val parentPeer: dynamic = resolveContainerTarget(parent) ?: return
        val childPeer: dynamic = child.browserPeer ?: return
        parentPeer.appendChild(childPeer)
    }

    actual fun insertChildAt(parent: WebDomElement, index: Int, child: WebDomElement) {
        val parentPeer: dynamic = resolveContainerTarget(parent) ?: return
        val childPeer: dynamic = child.browserPeer ?: return
        val children: dynamic = parentPeer.children
        val len = (children?.length as? Number)?.toInt() ?: 0
        if (index < 0 || index >= len) {
            parentPeer.appendChild(childPeer)
        } else {
            val refNode: dynamic = children[index]
            if (refNode != null) {
                parentPeer.insertBefore(childPeer, refNode)
            } else {
                parentPeer.appendChild(childPeer)
            }
        }
    }

    actual fun removeChild(parent: WebDomElement, child: WebDomElement) {
        val parentPeer: dynamic = resolveContainerTarget(parent) ?: return
        val childPeer: dynamic = child.browserPeer ?: return
        if (childPeer.parentNode === parentPeer) {
            parentPeer.removeChild(childPeer)
        }
    }

    actual fun clearChildren(parent: WebDomElement) {
        val parentPeer: dynamic = resolveContainerTarget(parent) ?: return
        while (parentPeer.firstChild != null) {
            parentPeer.removeChild(parentPeer.firstChild)
        }
    }

    actual fun getClientWidth(element: WebDomElement, fallback: Int): Int {
        val peer: dynamic = element.browserPeer ?: return fallback
        val cw = (peer.clientWidth as? Number)?.toInt() ?: 0
        if (cw > 0) return cw
        val ow = (peer.offsetWidth as? Number)?.toInt() ?: 0
        return if (ow > 0) ow else fallback
    }

    actual fun getClientHeight(element: WebDomElement, fallback: Int): Int {
        val peer: dynamic = element.browserPeer ?: return fallback
        val ch = (peer.clientHeight as? Number)?.toInt() ?: 0
        if (ch > 0) return ch
        val oh = (peer.offsetHeight as? Number)?.toInt() ?: 0
        return if (oh > 0) oh else fallback
    }

    actual fun getOffsetLeft(element: WebDomElement, fallback: Int): Int {
        val peer: dynamic = element.browserPeer ?: return fallback
        return (peer.offsetLeft as? Number)?.toInt() ?: fallback
    }

    actual fun getOffsetTop(element: WebDomElement, fallback: Int): Int {
        val peer: dynamic = element.browserPeer ?: return fallback
        return (peer.offsetTop as? Number)?.toInt() ?: fallback
    }

    actual fun getViewportWidth(fallback: Int): Int {
        if (!isBrowserEnvironment) return fallback
        val win: dynamic = js("window")
        val iw = (win.innerWidth as? Number)?.toInt() ?: 0
        return if (iw > 0) iw else fallback
    }

    actual fun getViewportHeight(fallback: Int): Int {
        if (!isBrowserEnvironment) return fallback
        val win: dynamic = js("window")
        val ih = (win.innerHeight as? Number)?.toInt() ?: 0
        return if (ih > 0) ih else fallback
    }

    actual fun setDocumentTitle(title: String) {
        if (!isBrowserEnvironment) return
        val doc: dynamic = js("document")
        doc.title = title
    }

    actual fun setDocumentTheme(theme: String) {
        if (!isBrowserEnvironment) return
        val doc: dynamic = js("document")
        doc.documentElement?.setAttribute("data-bs-theme", theme)
    }

    actual fun triggerClick(element: WebDomElement): Boolean {
        if (!isBrowserEnvironment) return false
        val peer: dynamic = element.browserPeer ?: return false
        if (js("typeof peer.click === 'function'") as Boolean) {
            peer.click()
            return true
        }
        return false
    }

    actual fun registerWindowResize(onResize: (Int, Int) -> Unit) {
        if (!isBrowserEnvironment) return
        val win: dynamic = js("window")
        win.addEventListener("resize", {
            val w = getViewportWidth(1280)
            val h = getViewportHeight(720)
            onResize(w, h)
        })
    }

    actual fun schedulePostLayoutPass(block: () -> Unit) {
        if (!isBrowserEnvironment) return
        val win: dynamic = js("window")
        if (js("typeof win.requestAnimationFrame === 'function'") as Boolean) {
            win.requestAnimationFrame({
                block()
            })
        } else if (js("typeof win.setTimeout === 'function'") as Boolean) {
            win.setTimeout({
                block()
            }, 0)
        }
    }

    actual fun scheduleTimeout(delayMs: Int, block: () -> Unit): Boolean {
        if (!isBrowserEnvironment) return false
        val win: dynamic = js("window")
        if (js("typeof win.setTimeout === 'function'") as Boolean) {
            win.setTimeout({
                block()
            }, delayMs.coerceAtLeast(0))
            return true
        }
        return false
    }

    actual fun showAlert(message: String) {
        if (!isBrowserEnvironment) return
        val win: dynamic = js("window")
        if (js("typeof win.alert === 'function'") as Boolean) {
            win.alert(message)
        }
    }

    actual fun showConfirm(message: String): Boolean {
        if (!isBrowserEnvironment) return true
        val win: dynamic = js("window")
        if (js("typeof win.confirm === 'function'") as Boolean) {
            return (win.confirm(message) as? Boolean) ?: true
        }
        return true
    }

    actual fun showPrompt(message: String, defaultValue: String): String? {
        if (!isBrowserEnvironment) return defaultValue
        val win: dynamic = js("window")
        if (js("typeof win.prompt === 'function'") as Boolean) {
            return win.prompt(message, defaultValue) as? String
        }
        return defaultValue
    }

    actual fun sendHttpRequest(
        url: String,
        method: String,
        contentType: String?,
        body: String?,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isBrowserEnvironment || !(js("typeof XMLHttpRequest !== 'undefined'") as Boolean)) {
            onError("XMLHttpRequest is not available")
            return
        }
        val request: dynamic = js("new XMLHttpRequest()")
        request.addEventListener("load", {
            val responseText = (request.responseText as? String) ?: ""
            onSuccess(responseText)
        })
        request.addEventListener("error", {
            onError("Network request failed for $url")
        })
        request.open(method, url)
        if (contentType != null) {
            request.setRequestHeader("Content-Type", contentType)
        }
        if (body != null) {
            request.send(body)
        } else {
            request.send()
        }
    }

    actual val hasLocalStorage: Boolean
        get() = isBrowserEnvironment && (js("typeof window.localStorage !== 'undefined' && window.localStorage !== null") as Boolean)

    actual fun localStorageGetItem(key: String): String? {
        if (!hasLocalStorage) return null
        return runCatching {
            val ls: dynamic = js("window.localStorage")
            ls.getItem(key) as? String
        }.getOrNull()
    }

    actual fun localStorageSetItem(key: String, value: String): Boolean {
        if (!hasLocalStorage) return false
        return runCatching {
            val ls: dynamic = js("window.localStorage")
            ls.setItem(key, value)
            true
        }.getOrDefault(false)
    }

    actual fun localStorageRemoveItem(key: String): Boolean {
        if (!hasLocalStorage) return false
        return runCatching {
            val ls: dynamic = js("window.localStorage")
            ls.removeItem(key)
            true
        }.getOrDefault(false)
    }

    actual fun localStorageClear(): Boolean {
        if (!hasLocalStorage) return false
        return runCatching {
            val ls: dynamic = js("window.localStorage")
            ls.clear()
            true
        }.getOrDefault(false)
    }

    actual fun localStorageKeys(): List<String> {
        if (!hasLocalStorage) return emptyList()
        return runCatching {
            val ls: dynamic = js("window.localStorage")
            val len = (ls.length as? Number)?.toInt() ?: 0
            val out = ArrayList<String>(len)
            for (i in 0 until len) {
                val k = ls.key(i) as? String
                if (k != null) out.add(k)
            }
            out
        }.getOrDefault(emptyList())
    }

    actual fun registerStorageListener(onStorageChanged: (key: String?, newValue: String?) -> Unit) {
        if (!isBrowserEnvironment) return
        val win: dynamic = js("window")
        win.addEventListener("storage", { ev: dynamic ->
            val k = ev.key as? String
            val nv = ev.newValue as? String
            onStorageChanged(k, nv)
        })
    }
}
