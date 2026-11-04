package com.nexustools.genericui.web

import com.nexustools.genericui.component.dialog.SelectedFile
import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.event.KeyEvent
import com.nexustools.genericui.event.PointerEvent
import com.nexustools.genericui.event.WheelEvent
import com.nexustools.genericui.res.DataUrlCodec

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
            // Remove any legacy standalone .loader element directly inside body outside genericui:app
            val bodyLoader: dynamic = doc.querySelector("body > .loader")
            if (bodyLoader != null && bodyLoader.parentNode != null) {
                bodyLoader.parentNode.removeChild(bodyLoader)
            }

            val existing: dynamic = doc.getElementsByTagName("genericui:app")?.item(0)
                ?: doc.querySelector("genericui\\:app")
            val host: dynamic = existing ?: doc.createElement("genericui:app")
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
            val currentAtIdx: dynamic = parentPeer.children?.item(index)
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
                val touch: dynamic = event.touches?.item(0)
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
                val touch: dynamic = event.touches?.item(0)
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
            if (!model.disabled) {
                ensureGlobalWindowPointerDragBridge()
                val pe = toPointerEvent(ev, defaultButton = 0)
                model.dispatchFromBrowser("pointerdown", pe)
            }
        })
        peer.addEventListener("pointermove", { ev: dynamic ->
            if (!model.disabled) {
                val pe = toPointerEvent(ev, defaultButton = 0)
                model.dispatchFromBrowser("pointermove", pe)
            }
        })
        peer.addEventListener("pointerup", { ev: dynamic ->
            if (!model.disabled) {
                val pe = toPointerEvent(ev, defaultButton = 0)
                model.dispatchFromBrowser("pointerup", pe)
            }
        })
        peer.addEventListener("wheel", { ev: dynamic ->
            val dx = (ev.deltaX as? Number)?.toFloat() ?: 0f
            val dy = (ev.deltaY as? Number)?.toFloat() ?: 0f
            val cx = (ev.clientX as? Number)?.toFloat() ?: 0f
            val cy = (ev.clientY as? Number)?.toFloat() ?: 0f
            model.dispatchFromBrowser("wheel", WheelEvent(dx, dy, cx, cy))
        })
        peer.addEventListener("keydown", { ev: dynamic ->
            if (!model.disabled) {
                if (model.tagName.equals("genericui:window", ignoreCase = true)) {
                    ev.__genericUiHandledByWindowPeer = true
                }
                val ke = toKeyEvent(ev)
                model.dispatchFromBrowser("keydown", ke)
            }
        })
        peer.addEventListener("keyup", { ev: dynamic ->
            if (!model.disabled) {
                if (model.tagName.equals("genericui:window", ignoreCase = true)) {
                    ev.__genericUiHandledByWindowPeer = true
                }
                val ke = toKeyEvent(ev)
                model.dispatchFromBrowser("keyup", ke)
            }
        })
        peer.addEventListener("load", { _: dynamic ->
            model.dispatchFromBrowser("load", null)
        })
        peer.addEventListener("error", { _: dynamic ->
            model.dispatchFromBrowser("error", null)
        })
        peer.addEventListener("scroll", { _: dynamic ->
            val st = (peer.scrollTop as? Number)?.toDouble() ?: 0.0
            val sl = (peer.scrollLeft as? Number)?.toDouble() ?: 0.0
            val cw = (peer.clientWidth as? Number)?.toInt() ?: 0
            val ch = (peer.clientHeight as? Number)?.toInt() ?: 0
            val sw = (peer.scrollWidth as? Number)?.toInt() ?: 0
            val sh = (peer.scrollHeight as? Number)?.toInt() ?: 0
            model.updateScrollFromBrowser(st, sl, cw, ch, sw, sh)
            model.dispatchFromBrowser("scroll", null)
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
        peer.className = element.getEffectiveDomClassName()
    }

    actual fun syncStyle(element: WebDomElement, property: String, value: String?, important: Boolean) {
        val peer: dynamic = element.browserPeer ?: return
        if (value == null) {
            peer.style.removeProperty(property)
        } else {
            val priority = if (important) "important" else ""
            peer.style.setProperty(property, value, priority)
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
        if (disabled) {
            peer.setAttribute("aria-disabled", "true")
            if (js("typeof document !== 'undefined' && document.activeElement === peer && typeof peer.blur === 'function'") as Boolean) {
                peer.blur()
            }
        } else {
            peer.removeAttribute("aria-disabled")
        }
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

    actual fun syncScrollTop(element: WebDomElement, scrollTop: Double) {
        val peer: dynamic = element.browserPeer ?: return
        val current = (peer.scrollTop as? Number)?.toDouble() ?: 0.0
        if (kotlin.math.abs(current - scrollTop) > 1.0) {
            peer.scrollTop = scrollTop
        }
    }

    actual fun syncScrollLeft(element: WebDomElement, scrollLeft: Double) {
        val peer: dynamic = element.browserPeer ?: return
        val current = (peer.scrollLeft as? Number)?.toDouble() ?: 0.0
        if (kotlin.math.abs(current - scrollLeft) > 1.0) {
            peer.scrollLeft = scrollLeft
        }
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
        val doc: dynamic = js("document")
        val iw = (win.innerWidth as? Number)?.toInt() ?: 0
        val cw = (doc.documentElement?.clientWidth as? Number)?.toInt() ?: 0
        val vvw = (win.visualViewport?.width as? Number)?.toInt() ?: 0
        val candidates = listOf(iw, cw, vvw).filter { it > 0 }
        return if (candidates.isNotEmpty()) candidates.min() else fallback
    }

    actual fun getViewportHeight(fallback: Int): Int {
        if (!isBrowserEnvironment) return fallback
        val win: dynamic = js("window")
        val doc: dynamic = js("document")
        val ih = (win.innerHeight as? Number)?.toInt() ?: 0
        val ch = (doc.documentElement?.clientHeight as? Number)?.toInt() ?: 0
        val vvh = (win.visualViewport?.height as? Number)?.toInt() ?: 0
        val candidates = listOf(ih, ch, vvh).filter { it > 0 }
        return if (candidates.isNotEmpty()) candidates.min() else fallback
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

    actual fun syncBootstrapTheme(themeCssValue: String) {
        if (!isBrowserEnvironment) return
        val doc: dynamic = js("document")
        doc.documentElement?.setAttribute("data-bs-theme", themeCssValue)
    }

    actual fun addStyleSheetLink(id: String, href: String) {
        if (!isBrowserEnvironment) return
        val doc: dynamic = js("document")
        val head: dynamic = doc.head ?: return
        var link: dynamic = doc.getElementById(id)
        if (link == null) {
            val normalizedTarget = href.substringBefore('?').substringBefore('#').substringAfterLast('/')
            val existingLinks: dynamic = head.querySelectorAll("link[rel='stylesheet']")
            val len = (existingLinks?.length as? Number)?.toInt() ?: 0
            for (i in 0 until len) {
                val candidate: dynamic = existingLinks.item(i)
                val candHref = (candidate?.getAttribute("href") as? String).orEmpty()
                val normalizedCand = candHref.substringBefore('?').substringBefore('#').substringAfterLast('/')
                if (candHref == href || (normalizedTarget.isNotEmpty() && normalizedCand == normalizedTarget)) {
                    link = candidate
                    val existingId = (link.id as? String).orEmpty()
                    if (existingId.isEmpty()) {
                        link.id = id
                    }
                    break
                }
            }
        }
        if (link == null) {
            link = doc.createElement("link")
            link.id = id
            link.setAttribute("rel", "stylesheet")
            link.setAttribute("href", href)
            head.appendChild(link)
        } else {
            val existingHref = (link.getAttribute("href") as? String).orEmpty()
            val normalizedExisting = existingHref.substringBefore('?').substringBefore('#').substringAfterLast('/')
            val normalizedNew = href.substringBefore('?').substringBefore('#').substringAfterLast('/')
            if (existingHref.isEmpty() || normalizedExisting != normalizedNew || ('?' in href && '?' !in existingHref)) {
                link.setAttribute("href", href)
            }
        }
    }

    actual fun removeStyleSheetLink(id: String) {
        if (!isBrowserEnvironment) return
        val doc: dynamic = js("document")
        val link: dynamic = doc.getElementById(id)
        if (link != null && link.parentNode != null) {
            link.parentNode.removeChild(link)
        }
        if (id == "genericui-bootstrap-css") {
            val jsEl: dynamic = doc.getElementById("genericui-bootstrap-js")
            if (jsEl != null && jsEl.parentNode != null) {
                jsEl.parentNode.removeChild(jsEl)
            }
        }
    }

    actual fun isBrowserDarkMode(onSystemThemeChanged: ((Boolean) -> Unit)?): Boolean {
        if (!isBrowserEnvironment) return true
        val win: dynamic = js("window")
        if (!(js("typeof win.matchMedia === 'function'") as Boolean)) return true
        val mql: dynamic = win.matchMedia("(prefers-color-scheme: dark)") ?: return true
        val isDarkMode = (mql.matches as? Boolean) ?: true
        if (onSystemThemeChanged != null && js("typeof mql.addEventListener === 'function'") as Boolean) {
            mql.addEventListener("change", { ev: dynamic ->
                val matches = (ev?.matches as? Boolean) ?: true
                onSystemThemeChanged(matches)
            })
        }
        return isDarkMode
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
        val notify = {
            val w = getViewportWidth(1280)
            val h = getViewportHeight(720)
            onResize(w, h)
        }
        win.addEventListener("resize", { notify() })
        win.addEventListener("orientationchange", { notify() })
        val vv: dynamic = win.visualViewport
        if (vv != null && js("typeof vv.addEventListener === 'function'") as Boolean) {
            vv.addEventListener("resize", { notify() })
        }
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

    actual fun setTimeoutId(delayMs: Int, block: () -> Unit): Int {
        if (!isBrowserEnvironment) return -1
        val win: dynamic = js("window")
        if (js("typeof win.setTimeout === 'function'") as Boolean) {
            return (win.setTimeout({
                block()
            }, delayMs.coerceAtLeast(0)) as? Number)?.toInt() ?: -1
        }
        return -1
    }

    actual fun setIntervalId(intervalMs: Int, block: () -> Unit): Int {
        if (!isBrowserEnvironment) return -1
        val win: dynamic = js("window")
        if (js("typeof win.setInterval === 'function'") as Boolean) {
            return (win.setInterval({
                block()
            }, intervalMs.coerceAtLeast(1)) as? Number)?.toInt() ?: -1
        }
        return -1
    }

    actual fun clearTimeoutId(timerId: Int): Boolean {
        if (!isBrowserEnvironment) return false
        val win: dynamic = js("window")
        if (js("typeof win.clearTimeout === 'function'") as Boolean) {
            win.clearTimeout(timerId)
            return true
        }
        return false
    }

    actual fun clearIntervalId(timerId: Int): Boolean {
        if (!isBrowserEnvironment) return false
        val win: dynamic = js("window")
        if (js("typeof win.clearInterval === 'function'") as Boolean) {
            win.clearInterval(timerId)
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
        if (hasLocalStorage) {
            runCatching {
                val ls: dynamic = js("window.localStorage")
                ls.getItem(key) as? String
            }.getOrNull()?.let { return it }
        }
        return headlessLocalStorage[key]
    }

    actual fun localStorageSetItem(key: String, value: String): Boolean {
        headlessLocalStorage[key] = value
        if (!hasLocalStorage) return true
        return runCatching {
            val ls: dynamic = js("window.localStorage")
            ls.setItem(key, value)
            true
        }.getOrDefault(true)
    }

    actual fun localStorageRemoveItem(key: String): Boolean {
        val existedInHeadless = headlessLocalStorage.remove(key) != null
        if (!hasLocalStorage) return existedInHeadless
        return runCatching {
            val ls: dynamic = js("window.localStorage")
            ls.removeItem(key)
            true
        }.getOrDefault(existedInHeadless)
    }

    actual fun localStorageClear(): Boolean {
        headlessLocalStorage.clear()
        if (!hasLocalStorage) return true
        return runCatching {
            val ls: dynamic = js("window.localStorage")
            ls.clear()
            true
        }.getOrDefault(true)
    }

    actual fun localStorageKeys(): List<String> {
        val out = LinkedHashSet<String>()
        if (hasLocalStorage) {
            runCatching {
                val ls: dynamic = js("window.localStorage")
                val len = (ls.length as? Number)?.toInt() ?: 0
                for (i in 0 until len) {
                    val k = ls.key(i) as? String
                    if (k != null) out.add(k)
                }
            }
        }
        out.addAll(headlessLocalStorage.keys)
        return out.toList()
    }

    actual fun registerStorageListener(onStorageChanged: (key: String?, newValue: String?) -> Unit) {
        headlessStorageListeners.add(onStorageChanged)
        if (!isBrowserEnvironment) return
        val win: dynamic = js("window")
        win.addEventListener("storage", { ev: dynamic ->
            val k = ev.key as? String
            val nv = ev.newValue as? String
            onStorageChanged(k, nv)
        })
    }

    private val headlessHistoryEntries = mutableListOf<Triple<String, Int, String?>>()
    private var headlessHistoryCursor: Int = -1
    private var headlessCurrentHash: String = ""
    private val headlessPopStateListeners = mutableListOf<(String, Int?, String?) -> Unit>()

    actual fun getLocationHash(): String {
        if (!isBrowserEnvironment) return headlessCurrentHash
        return runCatching {
            val raw = (js("window.location.hash") as? String).orEmpty()
            raw.removePrefix("#")
        }.getOrDefault(headlessCurrentHash)
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
        runCatching {
            val win: dynamic = js("window")
            win.location.hash = if (clean.isEmpty()) "" else "#$clean"
        }
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
        runCatching {
            val win: dynamic = js("window")
            val stateObj: dynamic = js("({})")
            stateObj.genericUiRoute = clean
            stateObj.genericUiIndex = historyIndex
            stateObj.genericUiStateJson = stateJson
            val urlHash = if (clean.isEmpty()) "#" else "#$clean"
            win.history.pushState(stateObj, title, urlHash)
        }
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
        runCatching {
            val win: dynamic = js("window")
            val stateObj: dynamic = js("({})")
            stateObj.genericUiRoute = clean
            stateObj.genericUiIndex = historyIndex
            stateObj.genericUiStateJson = stateJson
            val urlHash = if (clean.isEmpty()) "#" else "#$clean"
            win.history.replaceState(stateObj, title, urlHash)
        }
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
        return runCatching {
            val win: dynamic = js("window")
            win.history.back()
            true
        }.getOrDefault(false)
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
        return runCatching {
            val win: dynamic = js("window")
            win.history.forward()
            true
        }.getOrDefault(false)
    }

    actual fun getCurrentHistoryState(): Pair<Int?, String?> {
        if (!isBrowserEnvironment) {
            val entry = headlessHistoryEntries.getOrNull(headlessHistoryCursor) ?: return null to null
            return entry.second to entry.third
        }
        return runCatching {
            val st: dynamic = js("window.history && window.history.state")
            if (st == null || st == undefined) {
                val entry = headlessHistoryEntries.getOrNull(headlessHistoryCursor)
                return@runCatching (entry?.second) to (entry?.third)
            }
            val idx = (st.genericUiIndex as? Number)?.toInt()
            val json = st.genericUiStateJson as? String
            idx to json
        }.getOrDefault(null to null)
    }

    actual fun registerPopStateListener(onPopState: (routeHash: String, historyIndex: Int?, stateJson: String?) -> Unit) {
        headlessPopStateListeners.add(onPopState)
        if (!isBrowserEnvironment) return
        runCatching {
            val win: dynamic = js("window")
            win.addEventListener("popstate", { ev: dynamic ->
                val st = ev.state
                val route = (st?.genericUiRoute as? String)
                    ?: ((win.location.hash as? String).orEmpty().removePrefix("#"))
                val idx = (st?.genericUiIndex as? Number)?.toInt()
                val json = st?.genericUiStateJson as? String
                headlessCurrentHash = route
                onPopState(route, idx, json)
            })
            win.addEventListener("hashchange", { _: dynamic ->
                val route = (win.location.hash as? String).orEmpty().removePrefix("#")
                if (route != headlessCurrentHash) {
                    headlessCurrentHash = route
                    val st = win.history?.state
                    val idx = (st?.genericUiIndex as? Number)?.toInt()
                    val json = st?.genericUiStateJson as? String
                    onPopState(route, idx, json)
                }
            })
        }
    }

    private fun getJsPreloadedStore(): dynamic = runCatching {
        js("(typeof window !== 'undefined' && window.__GENERICUI_PRELOADED_RESOURCES__) || (typeof globalThis !== 'undefined' && globalThis.__GENERICUI_PRELOADED_RESOURCES__) || null")
    }.getOrNull()

    private fun ensureJsPreloadedStore(): dynamic = runCatching {
        js(
            "(() => { " +
                "const g = typeof window !== 'undefined' ? window : globalThis; " +
                "if (!g.__GENERICUI_PRELOADED_RESOURCES__) g.__GENERICUI_PRELOADED_RESOURCES__ = {}; " +
                "return g.__GENERICUI_PRELOADED_RESOURCES__; " +
                "})()"
        )
    }.getOrNull()

    private fun findJsPreloadedEntry(key: String): dynamic {
        val store: dynamic = getJsPreloadedStore() ?: return null
        for (candidate in expandPreloadedResourceLookupKeys(key)) {
            val found: dynamic = store[candidate]
            if (found != null && found !== undefined) {
                return found
            }
        }
        return null
    }

    actual fun getPreloadedResourceDataUrl(key: String): String? {
        getPreloadedDataUrlFromMemory(key)?.let { return it }
        val entry: dynamic = findJsPreloadedEntry(key) ?: return null
        val directDataUrl = entry.dataUrl as? String
        if (!directDataUrl.isNullOrBlank()) {
            return directDataUrl
        }
        val bytes = extractJsEntryBytes(entry)
        val mime = (entry.mimeType as? String)?.takeIf { it.isNotBlank() } ?: DataUrlCodec.guessMimeType(key)
        if (bytes != null) {
            return DataUrlCodec.encodeBytes(bytes, mime)
        }
        val text = entry.text as? String
        if (text != null) {
            return DataUrlCodec.encodeUtf8Text(text, mime)
        }
        return null
    }

    private fun extractJsEntryBytes(entry: dynamic): ByteArray? {
        val rawBytes: dynamic = entry.bytes
        if (rawBytes != null && rawBytes !== undefined) {
            val len = (rawBytes.length as? Number)?.toInt() ?: (rawBytes.byteLength as? Number)?.toInt() ?: -1
            if (len >= 0) {
                return ByteArray(len) { idx -> ((rawBytes[idx] as Number).toInt() and 0xFF).toByte() }
            }
        }
        val dataUrl = entry.dataUrl as? String
        if (!dataUrl.isNullOrBlank()) {
            DataUrlCodec.decodeBytes(dataUrl)?.let { return it }
        }
        val text = entry.text as? String
        if (text != null) {
            return text.encodeToByteArray()
        }
        return null
    }

    actual fun getPreloadedResourceBytes(key: String): ByteArray? {
        getPreloadedBytesFromMemory(key)?.let { return it }
        val entry: dynamic = findJsPreloadedEntry(key) ?: return null
        return extractJsEntryBytes(entry)
    }

    actual fun getPreloadedResourceText(key: String): String? {
        getPreloadedTextFromMemory(key)?.let { return it }
        val entry: dynamic = findJsPreloadedEntry(key) ?: return null
        val text = entry.text as? String
        if (text != null) return text
        return extractJsEntryBytes(entry)?.decodeToString()
    }

    actual fun hasPreloadedResource(key: String): Boolean {
        if (hasPreloadedResourceInMemory(key)) return true
        return findJsPreloadedEntry(key) != null
    }

    actual fun registerPreloadedResource(key: String, mimeType: String, bytes: ByteArray, text: String?) {
        val (resolvedMime, dataUrl, resolvedText) = registerPreloadedResourceInMemory(key, mimeType, bytes, text)
        runCatching {
            val store: dynamic = ensureJsPreloadedStore() ?: return@runCatching
            val uint8: dynamic = js("new Uint8Array(bytes.length)")
            for (i in bytes.indices) {
                uint8[i] = bytes[i].toInt() and 0xFF
            }
            val record: dynamic = js("({})")
            record.id = key
            record.name = key.substringAfterLast('/').substringBeforeLast('.')
            record.path = key
            record.mimeType = resolvedMime
            record.bytes = uint8
            record.dataUrl = dataUrl
            record.text = resolvedText
            for (candidate in expandPreloadedResourceLookupKeys(key)) {
                store[candidate] = record
            }
        }
    }

    actual fun clearPreloadedResources() {
        clearPreloadedResourcesInMemory()
        runCatching {
            js(
                "(() => { " +
                    "const g = typeof window !== 'undefined' ? window : globalThis; " +
                    "g.__GENERICUI_PRELOADED_RESOURCES__ = {}; " +
                    "})()"
            )
        }
    }

    actual fun requestElementFullscreen(element: WebDomElement?): Boolean {
        activeFullscreenElement = element
        if (!isBrowserEnvironment) return true
        return runCatching {
            val doc: dynamic = js("document")
            val target: dynamic = element?.browserPeer ?: doc.documentElement
            if (target != null && js("typeof target.requestFullscreen === 'function'") as Boolean) {
                target.requestFullscreen()
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    actual fun exitFullscreen(): Boolean {
        activeFullscreenElement = null
        if (!isBrowserEnvironment) return true
        return runCatching {
            val doc: dynamic = js("document")
            if (doc.fullscreenElement != null && js("typeof doc.exitFullscreen === 'function'") as Boolean) {
                doc.exitFullscreen()
            }
            true
        }.getOrDefault(false)
    }

    actual fun bindInvisibleHtmlImageLoader(
        imgElement: WebDomElement,
        src: String,
        onLoaded: (naturalWidth: Int, naturalHeight: Int) -> Unit,
        onError: (String) -> Unit
    ): Boolean {
        if (!isBrowserEnvironment) return false
        val peer: dynamic = imgElement.browserPeer ?: return false
        return runCatching {
            peer.style.display = "none"
            peer.style.position = "fixed"
            peer.style.width = "0px"
            peer.style.height = "0px"
            peer.style.pointerEvents = "none"
            peer.setAttribute("aria-hidden", "true")
            var settled = false
            val finishLoad = {
                if (!settled) {
                    settled = true
                    val natW = ((peer.naturalWidth as? Number)?.toInt() ?: 0).let { if (it > 0) it else 64 }
                    val natH = ((peer.naturalHeight as? Number)?.toInt() ?: 0).let { if (it > 0) it else 64 }
                    imgElement.setAttribute("data-complete", "true")
                    imgElement.setAttribute("data-natural-width", natW.toString())
                    imgElement.setAttribute("data-natural-height", natH.toString())
                    onLoaded(natW, natH)
                }
            }
            val finishError = {
                if (!settled) {
                    settled = true
                    imgElement.setAttribute("data-error", "true")
                    onError("Failed to load HTMLImageElement: $src")
                }
            }
            peer.addEventListener("load", { _: dynamic -> finishLoad() })
            peer.addEventListener("error", { _: dynamic -> finishError() })
            imgElement.setAttribute("src", src)
            val isComplete = (peer.complete as? Boolean) == true
            val hasDimensions = ((peer.naturalWidth as? Number)?.toInt() ?: 0) > 0
            if ((isComplete && hasDimensions) || src.startsWith("data:", ignoreCase = true)) {
                finishLoad()
            }
            true
        }.getOrDefault(false)
    }

    actual fun attachMediaElementListeners(element: WebDomElement) {
        if (!isBrowserEnvironment) return
        val peer: dynamic = element.browserPeer ?: return
        runCatching {
            if ((peer.__genericUiMediaAttached as? Boolean) == true) return
            peer.__genericUiMediaAttached = true
            peer.addEventListener("play", { _: dynamic ->
                element.dispatchFromBrowser("play", null)
            })
            peer.addEventListener("playing", { _: dynamic ->
                element.dispatchFromBrowser("playing", null)
            })
            peer.addEventListener("pause", { _: dynamic ->
                element.dispatchFromBrowser("pause", null)
            })
            peer.addEventListener("ended", { _: dynamic ->
                element.dispatchFromBrowser("ended", null)
            })
            peer.addEventListener("waiting", { _: dynamic ->
                element.dispatchFromBrowser("waiting", null)
            })
            val syncBufferedRanges = { dispatchProgress: Boolean ->
                val buffered: dynamic = peer.buffered
                val len = (buffered?.length as? Number)?.toInt() ?: 0
                if (len > 0) {
                    val rangesBuilder = StringBuilder()
                    var maxEnd = 0.0
                    for (i in 0 until len) {
                        val startSec = ((buffered.start(i) as? Number)?.toDouble() ?: 0.0).coerceAtLeast(0.0)
                        val endSec = ((buffered.end(i) as? Number)?.toDouble() ?: 0.0).coerceAtLeast(startSec)
                        if (!startSec.isNaN() && !endSec.isNaN() && endSec > startSec) {
                            if (rangesBuilder.isNotEmpty()) rangesBuilder.append(',')
                            rangesBuilder.append(startSec).append('-').append(endSec)
                            if (endSec > maxEnd) maxEnd = endSec
                        }
                    }
                    val rangesStr = rangesBuilder.toString()
                    if (rangesStr.isNotEmpty()) {
                        element.setAttribute("data-buffered-ranges", rangesStr)
                        element.setAttribute("data-buffered-time", maxEnd.toString())
                    }
                    if (dispatchProgress && !maxEnd.isNaN() && maxEnd >= 0.0) {
                        element.dispatchFromBrowser("progress", maxEnd)
                    }
                }
            }
            peer.addEventListener("canplay", { _: dynamic ->
                syncBufferedRanges(false)
                element.dispatchFromBrowser("canplay", null)
            })
            peer.addEventListener("timeupdate", { _: dynamic ->
                syncBufferedRanges(false)
                val ct = (peer.currentTime as? Number)?.toDouble() ?: 0.0
                if (!ct.isNaN() && ct >= 0.0) {
                    element.setAttribute("data-current-time", ct.toString())
                    element.dispatchFromBrowser("timeupdate", ct)
                }
            })
            val syncDuration = {
                val dur = (peer.duration as? Number)?.toDouble() ?: 0.0
                if (!dur.isNaN() && dur.isFinite() && dur > 0.0) {
                    element.setAttribute("data-duration", dur.toString())
                    element.dispatchFromBrowser("durationchange", dur)
                }
            }
            peer.addEventListener("durationchange", { _: dynamic -> syncDuration() })
            peer.addEventListener("loadedmetadata", { _: dynamic ->
                syncDuration()
                syncBufferedRanges(false)
                element.dispatchFromBrowser("loadedmetadata", null)
            })
            peer.addEventListener("progress", { _: dynamic ->
                syncBufferedRanges(true)
            })
            peer.addEventListener("error", { _: dynamic ->
                val errCode = (peer.error?.code as? Number)?.toInt()
                val errMsg = (peer.error?.message as? String)?.takeIf { it.isNotBlank() }
                    ?: if (errCode != null) "MediaError code $errCode" else "Media playback error"
                element.dispatchFromBrowser("error", errMsg)
            })
        }
    }

    actual fun mediaCanPlayType(element: WebDomElement, mimeType: String): Boolean {
        if (!isBrowserEnvironment) return false
        return runCatching {
            val peer: dynamic = element.browserPeer
                ?: js("typeof document !== 'undefined' ? document.createElement('video') : null")
            if (peer != null && js("typeof peer.canPlayType === 'function'") as Boolean) {
                val res = (peer.canPlayType(mimeType) as? String)?.trim()?.lowercase().orEmpty()
                res == "probably" || res == "maybe"
            } else {
                false
            }
        }.getOrDefault(false)
    }

    actual fun mediaLoad(element: WebDomElement, seekOffsetSeconds: Double): Boolean {
        if (!isBrowserEnvironment) return false
        val peer: dynamic = element.browserPeer ?: return false
        return runCatching {
            if (js("typeof peer.load === 'function'") as Boolean) {
                peer.load()
            }
            if (seekOffsetSeconds > 0.0) {
                val applySeek = {
                    runCatching { peer.currentTime = seekOffsetSeconds }
                }
                val readyState = (peer.readyState as? Number)?.toInt() ?: 0
                if (readyState >= 1) {
                    applySeek()
                } else {
                    var applied = false
                    var listener: dynamic = null
                    listener = { _: dynamic ->
                        if (!applied) {
                            applied = true
                            peer.removeEventListener("loadedmetadata", listener)
                            applySeek()
                        }
                    }
                    peer.addEventListener("loadedmetadata", listener)
                }
            }
            true
        }.getOrDefault(false)
    }

    actual fun mediaPlay(element: WebDomElement, onRejected: ((String) -> Unit)?): Boolean {
        if (!isBrowserEnvironment) return false
        val peer: dynamic = element.browserPeer ?: return false
        return runCatching {
            if (js("typeof peer.play === 'function'") as Boolean) {
                val promise: dynamic = peer.play()
                if (promise != null && js("typeof promise.catch === 'function'") as Boolean) {
                    promise.catch({ err: dynamic ->
                        val errName = (err?.name as? String).orEmpty()
                        val msg = (err?.message as? String) ?: err?.toString() ?: "Playback blocked"
                        val isAbort = errName == "AbortError" ||
                            msg.contains("AbortError", ignoreCase = true) ||
                            msg.contains("interrupted by a call to pause", ignoreCase = true) ||
                            msg.contains("interrupted by a new load request", ignoreCase = true)
                        if (!isAbort) {
                            onRejected?.invoke(msg)
                        }
                    })
                }
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    actual fun mediaPause(element: WebDomElement): Boolean {
        if (!isBrowserEnvironment) return false
        val peer: dynamic = element.browserPeer ?: return false
        return runCatching {
            if (js("typeof peer.pause === 'function'") as Boolean) {
                peer.pause()
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    actual fun mediaSeekTo(element: WebDomElement, seconds: Double): Boolean {
        if (!isBrowserEnvironment) return false
        val peer: dynamic = element.browserPeer ?: return false
        return runCatching {
            val clamped = seconds.coerceAtLeast(0.0)
            val readyState = (peer.readyState as? Number)?.toInt() ?: 0
            if (readyState >= 1) {
                peer.currentTime = clamped
            } else {
                var applied = false
                var listener: dynamic = null
                listener = { _: dynamic ->
                    if (!applied) {
                        applied = true
                        peer.removeEventListener("loadedmetadata", listener)
                        runCatching { peer.currentTime = clamped }
                    }
                }
                peer.addEventListener("loadedmetadata", listener)
            }
            true
        }.getOrDefault(false)
    }

    actual fun mediaSetVolume(element: WebDomElement, volume: Float): Boolean {
        if (!isBrowserEnvironment) return false
        val peer: dynamic = element.browserPeer ?: return false
        return runCatching {
            peer.volume = volume.coerceIn(0f, 1f).toDouble()
            true
        }.getOrDefault(false)
    }

    actual fun mediaSetMuted(element: WebDomElement, muted: Boolean): Boolean {
        if (!isBrowserEnvironment) return false
        val peer: dynamic = element.browserPeer ?: return false
        return runCatching {
            peer.muted = muted
            true
        }.getOrDefault(false)
    }

    actual fun mediaSetPlaybackRate(element: WebDomElement, rate: Float): Boolean {
        if (!isBrowserEnvironment) return false
        val peer: dynamic = element.browserPeer ?: return false
        return runCatching {
            peer.playbackRate = rate.coerceIn(0.25f, 4f).toDouble()
            true
        }.getOrDefault(false)
    }

    actual fun installModalFocusTrap(modalElement: WebDomElement) {
        registerHeadlessModalFocusTrap(modalElement)
        if (!isBrowserEnvironment) return
        val peer: dynamic = modalElement.browserPeer ?: return
        js(
            """
            (function(modalEl) {
                var win = window;
                var doc = document;
                if (!win.__genericUiModalStack) {
                    win.__genericUiModalStack = [];
                }
                var stack = win.__genericUiModalStack;
                if (stack.length === 0) {
                    win.__genericUiPreModalFocus = doc.activeElement;
                }
                var existingIdx = stack.indexOf(modalEl);
                if (existingIdx !== -1) {
                    stack.splice(existingIdx, 1);
                }
                stack.push(modalEl);
                var mainWindows = doc.querySelectorAll('genericui\\:window:not([data-tool-modal="true"])');
                for (var i = 0; i < mainWindows.length; i++) {
                    mainWindows[i].setAttribute('aria-hidden', 'true');
                    try { mainWindows[i].inert = true; } catch (_e) {}
                }
                var queryFocusables = function(root) {
                    if (!root || typeof root.querySelectorAll !== 'function') return [];
                    var nodes = root.querySelectorAll('button:not([disabled]), [href], input:not([disabled]):not([type="hidden"]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"]):not([disabled])');
                    var list = [];
                    for (var j = 0; j < nodes.length; j++) {
                        var el = nodes[j];
                        if (el.offsetParent !== null || (typeof el.getClientRects === 'function' && el.getClientRects().length > 0)) {
                            list.push(el);
                        }
                    }
                    return list;
                };
                if (!win.__genericUiModalTrapInstalled) {
                    win.__genericUiModalTrapInstalled = true;
                    doc.addEventListener('keydown', function(ev) {
                        var st = win.__genericUiModalStack;
                        if (!st || st.length === 0) return;
                        if (ev.key !== 'Tab') return;
                        var topModal = st[st.length - 1];
                        if (!topModal) return;
                        var focusables = queryFocusables(topModal);
                        if (focusables.length === 0) {
                            ev.preventDefault();
                            if (typeof topModal.focus === 'function') topModal.focus();
                            return;
                        }
                        var first = focusables[0];
                        var last = focusables[focusables.length - 1];
                        var active = doc.activeElement;
                        if (!topModal.contains(active)) {
                            ev.preventDefault();
                            (ev.shiftKey ? last : first).focus();
                        } else if (!ev.shiftKey && active === last) {
                            ev.preventDefault();
                            first.focus();
                        } else if (ev.shiftKey && (active === first || active === topModal)) {
                            ev.preventDefault();
                            last.focus();
                        }
                    }, true);
                    doc.addEventListener('focusin', function(ev) {
                        var st = win.__genericUiModalStack;
                        if (!st || st.length === 0) return;
                        var topModal = st[st.length - 1];
                        if (!topModal || !ev.target) return;
                        if (!topModal.contains(ev.target)) {
                            var focusables = queryFocusables(topModal);
                            if (focusables.length > 0) {
                                focusables[0].focus();
                            } else if (typeof topModal.focus === 'function') {
                                topModal.focus();
                            }
                        }
                    }, true);
                }
                setTimeout(function() {
                    var focusables = queryFocusables(modalEl);
                    if (focusables.length > 0 && typeof focusables[0].focus === 'function') {
                        focusables[0].focus();
                    } else if (typeof modalEl.focus === 'function') {
                        modalEl.focus();
                    }
                }, 0);
            })(peer)
            """
        )
    }

    actual fun releaseModalFocusTrap(modalElement: WebDomElement) {
        unregisterHeadlessModalFocusTrap(modalElement)
        if (!isBrowserEnvironment) return
        val peer: dynamic = modalElement.browserPeer ?: return
        js(
            """
            (function(modalEl) {
                var win = window;
                var doc = document;
                var stack = win.__genericUiModalStack;
                if (!stack) return;
                var idx = stack.indexOf(modalEl);
                if (idx !== -1) {
                    stack.splice(idx, 1);
                }
                if (stack.length === 0) {
                    var mainWindows = doc.querySelectorAll('genericui\\:window:not([data-tool-modal="true"])');
                    for (var i = 0; i < mainWindows.length; i++) {
                        mainWindows[i].removeAttribute('aria-hidden');
                        try { mainWindows[i].inert = false; } catch (_e) {}
                    }
                    var prev = win.__genericUiPreModalFocus;
                    win.__genericUiPreModalFocus = null;
                    if (prev && typeof prev.focus === 'function') {
                        try { prev.focus(); } catch (_e) {}
                    }
                }
            })(peer)
            """
        )
    }

    actual fun attachOverscrollStretch(scrollElement: WebDomElement, contentViewport: WebDomElement) {
        attachHeadlessOverscrollStretch(scrollElement, contentViewport)
        if (!isBrowserEnvironment) return
        val scrollPeer: dynamic = scrollElement.browserPeer ?: return
        val viewportPeer: dynamic = contentViewport.browserPeer ?: return
        js(
            """
            (function(scrollEl, viewportEl) {
                if (!scrollEl || !viewportEl || scrollEl.__genericUiOverscrollAttached) return;
                scrollEl.__genericUiOverscrollAttached = true;
                scrollEl.style.overscrollBehavior = 'auto';
                var startX = 0, startY = 0, activeTouch = false, wheelResetTimer = null;
                var isHoriz = function() {
                    return scrollEl.style.overflowX === 'auto' && scrollEl.style.overflowY !== 'auto';
                };
                var calcScale = function(delta) {
                    var abs = Math.abs(delta);
                    if (abs <= 0.5) return 1.0;
                    var norm = Math.min(4.0, abs / 220.0);
                    var stretch = Math.min(0.14, (norm / (1.0 + norm)) * 0.14);
                    return 1.0 + stretch;
                };
                var applyStretch = function(delta) {
                    var h = isHoriz();
                    var s = calcScale(delta);
                    if (s <= 1.0005) {
                        viewportEl.style.transition = 'transform 260ms cubic-bezier(0.22, 1, 0.36, 1)';
                        viewportEl.style.transform = h ? 'scaleX(1)' : 'scaleY(1)';
                        scrollEl.setAttribute('data-overscroll-stretch', '1.0');
                        scrollEl.removeAttribute('data-overscroll-edge');
                        return;
                    }
                    var origin = h ? (delta > 0 ? '0% 50%' : '100% 50%') : (delta > 0 ? '50% 0%' : '50% 100%');
                    viewportEl.style.transformOrigin = origin;
                    viewportEl.style.transition = 'none';
                    viewportEl.style.transform = h ? ('scaleX(' + s.toFixed(4) + ')') : ('scaleY(' + s.toFixed(4) + ')');
                    scrollEl.setAttribute('data-overscroll-stretch', s.toFixed(4));
                    scrollEl.setAttribute('data-overscroll-edge', h ? (delta > 0 ? 'start' : 'end') : (delta > 0 ? 'top' : 'bottom'));
                };
                var release = function() {
                    activeTouch = false;
                    var h = isHoriz();
                    viewportEl.style.transition = 'transform 260ms cubic-bezier(0.22, 1, 0.36, 1)';
                    viewportEl.style.transform = h ? 'scaleX(1)' : 'scaleY(1)';
                    scrollEl.setAttribute('data-overscroll-stretch', '1.0');
                    scrollEl.removeAttribute('data-overscroll-edge');
                };
                scrollEl.addEventListener('touchstart', function(ev) {
                    if (!ev.touches || ev.touches.length !== 1) return;
                    activeTouch = true;
                    startX = ev.touches[0].clientX;
                    startY = ev.touches[0].clientY;
                }, { passive: true });
                scrollEl.addEventListener('touchmove', function(ev) {
                    if (!activeTouch || !ev.touches || ev.touches.length !== 1) return;
                    var h = isHoriz();
                    var curX = ev.touches[0].clientX;
                    var curY = ev.touches[0].clientY;
                    var delta = h ? (curX - startX) : (curY - startY);
                    var pos = h ? scrollEl.scrollLeft : scrollEl.scrollTop;
                    var client = h ? scrollEl.clientWidth : scrollEl.clientHeight;
                    var total = h ? scrollEl.scrollWidth : scrollEl.scrollHeight;
                    var atStart = pos <= 1;
                    var atEnd = (pos + client) >= Math.max(0, total - 2);
                    if ((atStart && delta > 0) || (atEnd && delta < 0)) {
                        if (ev.cancelable) ev.preventDefault();
                        applyStretch(delta);
                    } else {
                        startX = curX;
                        startY = curY;
                        if (scrollEl.getAttribute('data-overscroll-stretch') !== '1.0') {
                            applyStretch(0);
                        }
                    }
                }, { passive: false });
                scrollEl.addEventListener('touchend', release, { passive: true });
                scrollEl.addEventListener('touchcancel', release, { passive: true });
            })(scrollPeer, viewportPeer)
            """
        )
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
        val scrollPeer: dynamic = scrollElement.browserPeer ?: return
        val viewportPeer: dynamic = viewportElement.browserPeer ?: return
        val dispatchMetrics: (Double, Int, Int, Int, Int, Int) -> Unit = { scrollOffsetPx, viewportExtentPx, measuredSpanPx, childCount, scrollW, scrollH ->
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
        js(
            """
            (function(scrollEl, viewportEl, isHorizFn, emitCb) {
                if (!scrollEl || !viewportEl) return;
                scrollEl.__genericUiLazyEmitCb = emitCb;
                scrollEl.__genericUiLazyIsHorizFn = isHorizFn;
                if (scrollEl.__genericUiLazyViewportAttached) return;
                scrollEl.__genericUiLazyViewportAttached = true;
                var win = typeof window !== 'undefined' ? window : null;
                var doc = typeof document !== 'undefined' ? document : null;
                var lastScrollOffset = -1;
                var lastViewportExtent = -1;
                var calibratedSpan = false;
                var findClippingAncestor = function(el, horiz) {
                    var cur = el ? el.parentElement : null;
                    while (cur && cur !== doc.body && cur !== doc.documentElement) {
                        var scrolled = horiz ? (cur.scrollLeft || 0) > 0 : (cur.scrollTop || 0) > 0;
                        if (scrolled) return cur;
                        if (win && typeof win.getComputedStyle === 'function') {
                            var cs = win.getComputedStyle(cur);
                            var ov = cs ? (horiz ? cs.overflowX : cs.overflowY) : '';
                            if (ov === 'auto' || ov === 'scroll' || ov === 'overlay' || ov === 'hidden') {
                                var cSpan = horiz ? (cur.clientWidth || 0) : (cur.clientHeight || 0);
                                if (cSpan > 0) return cur;
                            }
                        }
                        cur = cur.parentElement;
                    }
                    return null;
                };
                var sampleAndEmit = function(fromScrollEvent) {
                    var cb = scrollEl.__genericUiLazyEmitCb;
                    var hFn = scrollEl.__genericUiLazyIsHorizFn;
                    if (typeof cb !== 'function') return;
                    var isHoriz = typeof hFn === 'function' ? !!hFn() : false;
                    var winW = (win && win.innerWidth) ? win.innerWidth : 1280;
                    var winH = (win && win.innerHeight) ? win.innerHeight : 720;
                    var ownScroll = isHoriz ? (scrollEl.scrollLeft || 0) : (scrollEl.scrollTop || 0);
                    var ownClient = isHoriz ? (scrollEl.clientWidth || 0) : (scrollEl.clientHeight || 0);
                    var ownTotal = isHoriz ? (scrollEl.scrollWidth || 0) : (scrollEl.scrollHeight || 0);
                    var winSpan = isHoriz ? winW : winH;
                    var scrollOffsetPx = 0;
                    var viewportExtentPx = 600;
                    var selfScrolling = ownScroll > 0 || (ownClient > 0 && ownTotal > ownClient + 2 && ownClient <= winSpan);
                    if (selfScrolling) {
                        scrollOffsetPx = Math.max(0, ownScroll);
                        viewportExtentPx = Math.max(1, Math.round(ownClient || 600));
                    } else {
                        var clipPeer = findClippingAncestor(scrollEl, isHoriz);
                        var peerRect = typeof scrollEl.getBoundingClientRect === 'function' ? scrollEl.getBoundingClientRect() : null;
                        var clipRect = (clipPeer && typeof clipPeer.getBoundingClientRect === 'function')
                            ? clipPeer.getBoundingClientRect()
                            : { top: 0, left: 0, bottom: winH, right: winW, height: winH, width: winW };
                        if (peerRect) {
                            if (isHoriz) {
                                scrollOffsetPx = Math.max(0, (clipRect.left - peerRect.left) + (scrollEl.scrollLeft || 0));
                                var cw = Math.round(clipRect.width || winW || 600);
                                viewportExtentPx = Math.max(1, (ownClient > 0 && ownClient < cw) ? ownClient : cw);
                            } else {
                                scrollOffsetPx = Math.max(0, (clipRect.top - peerRect.top) + (scrollEl.scrollTop || 0));
                                var ch = Math.round(clipRect.height || winH || 600);
                                viewportExtentPx = Math.max(1, (ownClient > 0 && ownClient < ch) ? ownClient : ch);
                            }
                        } else {
                            scrollOffsetPx = Math.max(0, ownScroll);
                            viewportExtentPx = Math.max(1, Math.round(ownClient || winSpan || 600));
                        }
                    }
                    var measuredViewportSpanPx = Math.max(0, Math.round(
                        isHoriz
                            ? (viewportEl.clientWidth || viewportEl.offsetWidth || 0)
                            : (viewportEl.clientHeight || viewportEl.offsetHeight || 0)
                    ));
                    var renderedChildCount = (viewportEl.children && typeof viewportEl.children.length === 'number')
                        ? (viewportEl.children.length | 0)
                        : 0;
                    var scrollChanged = Math.abs(scrollOffsetPx - lastScrollOffset) >= 0.5;
                    var extentChanged = Math.abs(viewportExtentPx - lastViewportExtent) >= 1;
                    var needsInitialCalibration = !calibratedSpan && renderedChildCount > 0 && measuredViewportSpanPx > 0;
                    if (!fromScrollEvent && !scrollChanged && !extentChanged && !needsInitialCalibration) {
                        return;
                    }
                    if (renderedChildCount > 0 && measuredViewportSpanPx > 0) {
                        calibratedSpan = true;
                    }
                    lastScrollOffset = scrollOffsetPx;
                    lastViewportExtent = viewportExtentPx;
                    cb(
                        scrollOffsetPx,
                        viewportExtentPx | 0,
                        measuredViewportSpanPx | 0,
                        renderedChildCount | 0,
                        (scrollEl.scrollWidth || 0) | 0,
                        (scrollEl.scrollHeight || 0) | 0
                    );
                };
                var onScrollHandler = function(ev) {
                    var target = ev ? ev.target : null;
                    if (
                        !target ||
                        target === scrollEl ||
                        target === doc ||
                        target === (doc && doc.documentElement) ||
                        target === (doc && doc.body) ||
                        target === win ||
                        (typeof target.contains === 'function' && target.contains(scrollEl))
                    ) {
                        sampleAndEmit(true);
                    }
                };
                scrollEl.addEventListener('scroll', onScrollHandler, { passive: true });
                scrollEl.addEventListener('wheel', function(ev) {
                    if (!ev) return;
                    var hFn = scrollEl.__genericUiLazyIsHorizFn;
                    var isHoriz = typeof hFn === 'function' ? !!hFn() : false;
                    var rawDelta = isHoriz ? (ev.deltaX || ev.deltaY || 0) : (ev.deltaY || 0);
                    if (!rawDelta) return;
                    var deltaPx = rawDelta;
                    if (ev.deltaMode === 1) deltaPx = rawDelta * 40;
                    else if (ev.deltaMode === 2) deltaPx = rawDelta * ((isHoriz ? scrollEl.clientWidth : scrollEl.clientHeight) || 400);
                    var ownClient = isHoriz ? (scrollEl.clientWidth || 0) : (scrollEl.clientHeight || 0);
                    var ownTotal = isHoriz ? (scrollEl.scrollWidth || 0) : (scrollEl.scrollHeight || 0);
                    if (ownClient > 0 && ownTotal > ownClient + 2) {
                        var prevOwn = isHoriz ? (scrollEl.scrollLeft || 0) : (scrollEl.scrollTop || 0);
                        if (win && typeof win.requestAnimationFrame === 'function') {
                            win.requestAnimationFrame(function() {
                                var afterOwn = isHoriz ? (scrollEl.scrollLeft || 0) : (scrollEl.scrollTop || 0);
                                if (Math.abs(afterOwn - prevOwn) < 0.5) {
                                    if (isHoriz) scrollEl.scrollLeft = Math.max(0, Math.min(ownTotal - ownClient, prevOwn + deltaPx));
                                    else scrollEl.scrollTop = Math.max(0, Math.min(ownTotal - ownClient, prevOwn + deltaPx));
                                }
                                sampleAndEmit(true);
                            });
                        } else {
                            sampleAndEmit(true);
                        }
                    } else {
                        var clipPeer = findClippingAncestor(scrollEl, isHoriz);
                        if (clipPeer) {
                            var peerClient = isHoriz ? (clipPeer.clientWidth || 0) : (clipPeer.clientHeight || 0);
                            var peerTotal = isHoriz ? (clipPeer.scrollWidth || 0) : (clipPeer.scrollHeight || 0);
                            if (peerClient > 0 && peerTotal > peerClient + 2) {
                                var prevPeer = isHoriz ? (clipPeer.scrollLeft || 0) : (clipPeer.scrollTop || 0);
                                if (win && typeof win.requestAnimationFrame === 'function') {
                                    win.requestAnimationFrame(function() {
                                        var afterPeer = isHoriz ? (clipPeer.scrollLeft || 0) : (clipPeer.scrollTop || 0);
                                        if (Math.abs(afterPeer - prevPeer) < 0.5) {
                                            if (isHoriz) clipPeer.scrollLeft = Math.max(0, Math.min(peerTotal - peerClient, prevPeer + deltaPx));
                                            else clipPeer.scrollTop = Math.max(0, Math.min(peerTotal - peerClient, prevPeer + deltaPx));
                                        }
                                        sampleAndEmit(true);
                                    });
                                } else {
                                    sampleAndEmit(true);
                                }
                            }
                        }
                    }
                }, { passive: true });
                if (win) {
                    win.addEventListener('scroll', onScrollHandler, { capture: true, passive: true });
                    win.addEventListener('resize', function() { sampleAndEmit(false); }, { passive: true });
                }
                if (typeof ResizeObserver !== 'undefined') {
                    try {
                        var ro = new ResizeObserver(function() { sampleAndEmit(false); });
                        ro.observe(scrollEl);
                        ro.observe(viewportEl);
                    } catch (_e) {}
                }
            })(scrollPeer, viewportPeer, isHorizontal, dispatchMetrics)
            """
        )
    }

    private var globalWindowPointerDragBridgeInstalled: Boolean = false

    private fun ensureGlobalWindowPointerDragBridge() {
        if (globalWindowPointerDragBridgeInstalled || !isBrowserEnvironment) return
        globalWindowPointerDragBridgeInstalled = true
        val win: dynamic = js("window")
        val handleMove: (dynamic) -> Unit = { ev ->
            if (com.nexustools.genericui.event.PointerDragManager.isDragging) {
                val pe = toPointerEvent(ev, defaultButton = 0)
                com.nexustools.genericui.event.PointerDragManager.notifyGlobalPointerMove(pe)
            }
        }
        val handleUp: (dynamic) -> Unit = { ev ->
            if (com.nexustools.genericui.event.PointerDragManager.isDragging) {
                val pe = toPointerEvent(ev, defaultButton = 0)
                com.nexustools.genericui.event.PointerDragManager.notifyGlobalPointerUp(pe)
            }
        }
        win.addEventListener("pointermove", handleMove, true)
        win.addEventListener("mousemove", handleMove, true)
        win.addEventListener("pointerup", handleUp, true)
        win.addEventListener("mouseup", handleUp, true)
        win.addEventListener("pointercancel", handleUp, true)
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
        val containerPeer: dynamic = containerElement.browserPeer ?: return
        val rangePeer: dynamic = rangeInputElement.browserPeer
        js(
            """
            (function(containerEl, rangeEl, startCb, moveCb, endCb) {
                if (!containerEl || containerEl.__genericUiSeekBarScrubAttached) return;
                containerEl.__genericUiSeekBarScrubAttached = true;
                containerEl.style.touchAction = 'none';
                if (rangeEl && rangeEl.style) {
                    rangeEl.style.pointerEvents = 'none';
                }
                var win = typeof window !== 'undefined' ? window : null;
                var scrubbing = false;
                var activePointerId = null;
                var lastFraction = 0.0;
                var lastClientX = null;
                var calcFraction = function(ev) {
                    var trackEl = (typeof containerEl.querySelector === 'function' ? containerEl.querySelector('.genericui-seekbar-track') : null) || containerEl;
                    var rect = typeof trackEl.getBoundingClientRect === 'function' ? trackEl.getBoundingClientRect() : null;
                    var w = (rect && rect.width > 0) ? rect.width : (trackEl.clientWidth || containerEl.clientWidth || 100);
                    var left = rect ? rect.left : 0;
                    var clientX = (ev && typeof ev.clientX === 'number') ? ev.clientX : null;
                    if (clientX === null) return lastFraction;
                    var f = (clientX - left) / w;
                    lastFraction = Math.max(0.0, Math.min(1.0, f));
                    return lastFraction;
                };
                var handleMove = function(ev) {
                    if (!scrubbing) return;
                    if (activePointerId !== null && ev && ev.pointerId !== undefined && ev.pointerId !== activePointerId) return;
                    if (ev && typeof ev.preventDefault === 'function') ev.preventDefault();
                    if (ev && typeof ev.stopPropagation === 'function') ev.stopPropagation();
                    if (ev && typeof ev.clientX === 'number') {
                        if (lastClientX === null || Math.abs(ev.clientX - lastClientX) > 0.5) {
                            lastClientX = ev.clientX;
                            lastFraction = calcFraction(ev);
                        }
                    }
                    moveCb(lastFraction);
                };
                var finishScrub = function(ev) {
                    if (!scrubbing) return;
                    if (activePointerId !== null && ev && ev.pointerId !== undefined && ev.pointerId !== activePointerId) return;
                    scrubbing = false;
                    var pid = activePointerId;
                    activePointerId = null;
                    if (ev && typeof ev.stopPropagation === 'function') ev.stopPropagation();
                    try {
                        if (typeof containerEl.releasePointerCapture === 'function' && pid !== null && pid !== undefined) {
                            containerEl.releasePointerCapture(pid);
                        }
                    } catch (_e) {}
                    if (ev && typeof ev.clientX === 'number' && lastClientX !== null && Math.abs(ev.clientX - lastClientX) > 0.5) {
                        lastClientX = ev.clientX;
                        lastFraction = calcFraction(ev);
                    }
                    lastClientX = null;
                    endCb(lastFraction);
                };
                containerEl.addEventListener('pointerdown', function(ev) {
                    if (ev.button !== undefined && ev.button !== 0) return;
                    scrubbing = true;
                    activePointerId = ev.pointerId !== undefined ? ev.pointerId : null;
                    lastClientX = (ev && typeof ev.clientX === 'number') ? ev.clientX : null;
                    lastFraction = calcFraction(ev);
                    if (typeof ev.preventDefault === 'function') ev.preventDefault();
                    if (typeof ev.stopPropagation === 'function') ev.stopPropagation();
                    try {
                        if (typeof containerEl.setPointerCapture === 'function' && ev.pointerId !== undefined) {
                            containerEl.setPointerCapture(ev.pointerId);
                        }
                    } catch (_e) {}
                    startCb(lastFraction);
                });
                containerEl.addEventListener('pointermove', handleMove);
                containerEl.addEventListener('pointerup', finishScrub);
                containerEl.addEventListener('pointercancel', finishScrub);
                if (win) {
                    win.addEventListener('pointermove', handleMove, true);
                    win.addEventListener('mousemove', handleMove, true);
                    win.addEventListener('pointerup', finishScrub, true);
                    win.addEventListener('mouseup', finishScrub, true);
                    win.addEventListener('pointercancel', finishScrub, true);
                    win.addEventListener('blur', finishScrub);
                }
            })(containerPeer, rangePeer, onScrubStart, onScrubMove, onScrubEnd)
            """
        )
    }

    actual fun registerWindowKeyEvents(
        onKeyDown: (com.nexustools.genericui.event.KeyEvent) -> Unit,
        onKeyUp: (com.nexustools.genericui.event.KeyEvent) -> Unit
    ) {
        if (!isBrowserEnvironment) return
        val dispatchDown: (dynamic) -> Unit = { ev ->
            onKeyDown(toKeyEvent(ev))
        }
        val dispatchUp: (dynamic) -> Unit = { ev ->
            onKeyUp(toKeyEvent(ev))
        }
        js(
            """
            (function(downCb, upCb) {
                var win = typeof window !== 'undefined' ? window : null;
                if (!win) return;
                win.__genericUiWindowKeyDownCb = downCb;
                win.__genericUiWindowKeyUpCb = upCb;
                if (win.__genericUiWindowKeysInstalled) return;
                win.__genericUiWindowKeysInstalled = true;
                var isEditableTarget = function(el) {
                    if (!el || !el.tagName) return false;
                    if (el.isContentEditable) return true;
                    var tag = String(el.tagName).toLowerCase();
                    if (tag === 'textarea' || tag === 'select') return true;
                    if (tag === 'input') {
                        var t = String(el.getAttribute('type') || el.type || 'text').toLowerCase();
                        return t !== 'range' && t !== 'button' && t !== 'checkbox' && t !== 'radio';
                    }
                    return false;
                };
                var isModalActive = function() {
                    if (win.__genericUiModalStack && win.__genericUiModalStack.length > 0) return true;
                    if (typeof document !== 'undefined') {
                        var openModal = document.querySelector('genericui\\:window[data-tool-modal="true"]:not(.d-none)');
                        if (openModal && openModal.style && openModal.style.display !== 'none') return true;
                    }
                    return false;
                };
                var isMediaPlayerKey = function(ev) {
                    if (ev.ctrlKey || ev.altKey || ev.metaKey) return false;
                    var k = String(ev.key || '');
                    var c = String(ev.code || '');
                    return k === ' ' || c === 'Space' ||
                        k === 'ArrowLeft' || k === 'ArrowRight' || k === 'ArrowUp' || k === 'ArrowDown';
                };
                win.addEventListener('keydown', function(ev) {
                    if (isModalActive() || isEditableTarget(ev.target)) return;
                    if (isMediaPlayerKey(ev)) {
                        if (typeof ev.preventDefault === 'function') ev.preventDefault();
                    }
                }, true);
                win.addEventListener('keydown', function(ev) {
                    if (ev.__genericUiHandledByWindowPeer) return;
                    if (isModalActive() || isEditableTarget(ev.target)) return;
                    if (typeof win.__genericUiWindowKeyDownCb === 'function') {
                        win.__genericUiWindowKeyDownCb(ev);
                    }
                }, false);
                win.addEventListener('keyup', function(ev) {
                    if (ev.__genericUiHandledByWindowPeer) return;
                    if (isModalActive() || isEditableTarget(ev.target)) return;
                    if (typeof win.__genericUiWindowKeyUpCb === 'function') {
                        win.__genericUiWindowKeyUpCb(ev);
                    }
                }, false);
            })(dispatchDown, dispatchUp)
            """
        )
    }

    actual fun setWebSplashStatus(statusText: String, type: BootstrapType) {
        val trimmed = statusText.trim().ifEmpty { WebSplashConfig.STATUS_INITIALIZING }
        lastWebSplashStatus = trimmed
        lastWebSplashStatusType = type
        if (!isBrowserEnvironment) return
        runCatching {
            val typeSuffix = type.cssSuffix
            js(
                """
                (function(text, t) {
                    var g = typeof window !== 'undefined' ? window : globalThis;
                    if (g && g.GenericUiSplash) {
                        if (typeof g.GenericUiSplash.setStatus === 'function') {
                            g.GenericUiSplash.setStatus(text, t);
                        } else if (typeof g.GenericUiSplash.setApplicationStatus === 'function') {
                            g.GenericUiSplash.setApplicationStatus(text, t);
                        }
                    }
                })(trimmed, typeSuffix)
                """
            )
        }
    }

    actual fun setWebSplashProgress(progressPercent: Int, type: BootstrapType) {
        val clamped = progressPercent.coerceIn(0, 100)
        lastWebSplashProgress = clamped
        lastWebSplashProgressType = type
        if (!isBrowserEnvironment) return
        runCatching {
            val typeSuffix = type.cssSuffix
            js(
                """
                (function(pct, t) {
                    var g = typeof window !== 'undefined' ? window : globalThis;
                    if (g && g.GenericUiSplash && typeof g.GenericUiSplash.setProgress === 'function') {
                        g.GenericUiSplash.setProgress(pct, t);
                    }
                })(clamped, typeSuffix)
                """
            )
        }
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
        runCatching {
            val resolvedStack = stackTrace ?: ""
            js(
                """
                (function(msg, stack, errTitle) {
                    var g = typeof window !== 'undefined' ? window : globalThis;
                    if (g && g.GenericUiSplash) {
                        if (typeof g.GenericUiSplash.setError === 'function') {
                            g.GenericUiSplash.setError(msg, stack, errTitle);
                        } else if (typeof g.GenericUiSplash.showError === 'function') {
                            var e = new Error(msg);
                            e.name = errTitle;
                            if (stack) e.stack = stack;
                            g.GenericUiSplash.showError(e, errTitle);
                        }
                    }
                })(message, resolvedStack, resolvedTitle)
                """
            )
        }
    }

    actual fun finishWebSplash(fadeDurationMs: Int) {
        lastWebSplashStatus = WebSplashConfig.STATUS_READY
        lastWebSplashStatusType = BootstrapType.SUCCESS
        lastWebSplashProgress = 100
        isWebSplashFinished = true
        if (!isBrowserEnvironment) return
        runCatching {
            val dur = fadeDurationMs.coerceAtLeast(0)
            js(
                """
                (function(d) {
                    var g = typeof window !== 'undefined' ? window : globalThis;
                    if (g && g.GenericUiSplash && typeof g.GenericUiSplash.finish === 'function') {
                        g.GenericUiSplash.finish(d);
                    }
                })(dur)
                """
            )
        }
    }

    actual fun triggerBrowserFileDownload(fileName: String, mimeType: String, bytes: ByteArray): Boolean {
        if (!isBrowserEnvironment) return true
        return runCatching {
            val len = bytes.size
            val u8: dynamic = js("new Uint8Array(len)")
            for (i in 0 until len) {
                u8[i] = bytes[i].toInt() and 0xFF
            }
            val resolvedMime = mimeType.ifBlank { "application/octet-stream" }
            val resolvedName = fileName.ifBlank { "download.bin" }
            js(
                """
                (function(data, mime, name) {
                    var blob = new Blob([data], { type: mime });
                    var url = URL.createObjectURL(blob);
                    var a = document.createElement('a');
                    a.href = url;
                    a.download = name;
                    a.style.display = 'none';
                    if (document.body) {
                        document.body.appendChild(a);
                    }
                    a.click();
                    if (a.parentNode) {
                        a.parentNode.removeChild(a);
                    }
                    setTimeout(function() {
                        URL.revokeObjectURL(url);
                    }, 1000);
                })(u8, resolvedMime, resolvedName)
                """
            )
            true
        }.getOrDefault(false)
    }
}
