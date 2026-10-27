package com.nexustools.genericui.web

/**
 * Lightweight, cross-platform DOM Element abstraction used by `:web` native implementations.
 *
 * Mirrors standard HTML5 `HTMLElement` properties (`tagName`, `id`, `classList`, `style`, `attributes`,
 * `children`, `textContent`, `innerHTML`, `value`, `checked`, `disabled`, `addEventListener`, `click`, `remove`)
 * so that `:web` widgets can be tested deterministically in common/JS/Wasm tests while mapping 1:1 to
 * browser DOM elements via [WebBrowserDomBridge].
 */
class WebDomElement(val tagName: String) {
    internal var browserPeer: Any? = null

    /**
     * Returns the underlying browser DOM `HTMLElement` / `HTMLCanvasElement` peer when running
     * in a browser environment, or `null` in headless unit tests.
     */
    val nativeBrowserPeer: Any?
        get() = browserPeer

    var id: String = ""
        set(value) {
            field = value
            WebBrowserDomBridge.syncId(this, value)
        }

    private val userClassList: MutableSet<String> = linkedSetOf()
    private val backingEnumClassList: MutableSet<String> = linkedSetOf()

    val classList: MutableSet<String> = object : AbstractMutableSet<String>() {
        override val size: Int
            get() = userClassList.size

        override fun add(element: String): Boolean {
            val trimmed = element.trim()
            if (trimmed.isEmpty()) return false
            val added = userClassList.add(trimmed)
            if (added) {
                WebBrowserDomBridge.syncClassName(this@WebDomElement, getEffectiveDomClassName())
            }
            return added
        }

        override fun remove(element: String): Boolean {
            val removed = userClassList.remove(element.trim())
            if (removed) {
                WebBrowserDomBridge.syncClassName(this@WebDomElement, getEffectiveDomClassName())
            }
            return removed
        }

        override fun clear() {
            if (userClassList.isNotEmpty()) {
                userClassList.clear()
                WebBrowserDomBridge.syncClassName(this@WebDomElement, getEffectiveDomClassName())
            }
        }

        override fun iterator(): MutableIterator<String> {
            val delegate = userClassList.iterator()
            return object : MutableIterator<String> {
                override fun hasNext(): Boolean = delegate.hasNext()
                override fun next(): String = delegate.next()
                override fun remove() {
                    delegate.remove()
                    WebBrowserDomBridge.syncClassName(this@WebDomElement, getEffectiveDomClassName())
                }
            }
        }
    }

    val enumClasses: Set<String>
        get() = backingEnumClassList.toSet()

    val enumClassList: Set<String>
        get() = backingEnumClassList.toSet()

    fun setEnumClasses(classes: Collection<String>) {
        backingEnumClassList.clear()
        for (raw in classes) {
            for (token in raw.trim().split(Regex("\\s+"))) {
                if (token.isNotEmpty()) {
                    backingEnumClassList.add(token)
                }
            }
        }
        WebBrowserDomBridge.syncClassName(this, getEffectiveDomClassName())
    }

    fun getEffectiveDomClassName(): String =
        (backingEnumClassList + userClassList).joinToString(" ")

    fun addClass(className: String) {
        var changed = false
        for (token in className.trim().split(Regex("\\s+"))) {
            if (token.isNotEmpty() && userClassList.add(token)) {
                changed = true
            }
        }
        if (changed) {
            WebBrowserDomBridge.syncClassName(this, getEffectiveDomClassName())
        }
    }

    fun removeClass(className: String) {
        var changed = false
        for (token in className.trim().split(Regex("\\s+"))) {
            if (token.isNotEmpty() && userClassList.remove(token)) {
                changed = true
            }
        }
        if (changed) {
            WebBrowserDomBridge.syncClassName(this, getEffectiveDomClassName())
        }
    }

    fun hasClass(className: String): Boolean {
        val trimmed = className.trim()
        return trimmed.isNotEmpty() && trimmed in userClassList
    }

    fun setClassName(value: String) {
        userClassList.clear()
        value.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.forEach { userClassList.add(it) }
        WebBrowserDomBridge.syncClassName(this, getEffectiveDomClassName())
    }

    var className: String
        get() = userClassList.joinToString(" ")
        set(value) {
            setClassName(value)
        }

    private val backingStyle = linkedMapOf<String, String>()
    private val styleImportantProps = mutableSetOf<String>()

    val style: MutableMap<String, String> = object : AbstractMutableMap<String, String>() {
        override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
            get() = backingStyle.entries

        override fun put(key: String, value: String): String? {
            val prev = backingStyle.put(key, value)
            styleImportantProps.remove(key.trim().lowercase())
            if (prev != value) {
                WebBrowserDomBridge.syncStyle(this@WebDomElement, key, value, false)
            }
            return prev
        }

        override fun remove(key: String): String? {
            val prev = backingStyle.remove(key)
            styleImportantProps.remove(key.trim().lowercase())
            WebBrowserDomBridge.syncStyle(this@WebDomElement, key, null, false)
            return prev
        }

        override fun clear() {
            backingStyle.clear()
            styleImportantProps.clear()
            WebBrowserDomBridge.clearStyles(this@WebDomElement)
        }
    }

    fun setStyle(property: String, value: String?, important: Boolean = false) {
        val cleanProp = property.trim()
        val lowerProp = cleanProp.lowercase()
        if (value == null) {
            backingStyle.remove(cleanProp)
            backingStyle.remove(lowerProp)
            styleImportantProps.remove(lowerProp)
            WebBrowserDomBridge.syncStyle(this, cleanProp, null, false)
        } else {
            backingStyle[cleanProp] = value
            if (important) {
                styleImportantProps.add(lowerProp)
            } else {
                styleImportantProps.remove(lowerProp)
            }
            WebBrowserDomBridge.syncStyle(this, cleanProp, value, important)
        }
    }

    fun getStyle(property: String): String? =
        backingStyle[property] ?: backingStyle[property.trim().lowercase()]

    fun isStyleImportant(property: String): Boolean =
        property.trim().lowercase() in styleImportantProps

    val attributes: MutableMap<String, String> = linkedMapOf()
    val children: MutableList<WebDomElement> = mutableListOf()
    var parent: WebDomElement? = null

    var textContent: String = ""
        set(value) {
            field = value
            WebBrowserDomBridge.syncTextContent(this, value)
        }

    var innerHTML: String = ""
        set(value) {
            field = value
            WebBrowserDomBridge.syncInnerHTML(this, value)
        }

    var value: String = ""
        set(value) {
            field = value
            WebBrowserDomBridge.syncValue(this, value)
        }

    var checked: Boolean = false
        set(value) {
            field = value
            WebBrowserDomBridge.syncChecked(this, value)
        }

    var disabled: Boolean = false
        set(value) {
            field = value
            WebBrowserDomBridge.syncDisabled(this, value)
        }

    var readOnly: Boolean = false
        set(value) {
            field = value
            WebBrowserDomBridge.syncReadOnly(this, value)
        }

    var multiple: Boolean = false
        set(value) {
            field = value
            WebBrowserDomBridge.syncMultiple(this, value)
        }

    var webkitdirectory: Boolean = false
        set(value) {
            field = value
            WebBrowserDomBridge.syncDirectory(this, value)
        }

    var directory: Boolean = false
        set(value) {
            field = value
            WebBrowserDomBridge.syncDirectory(this, value)
        }

    var accept: String = ""
        set(value) {
            field = value
            WebBrowserDomBridge.syncAccept(this, value)
        }

    var type: String = ""
    var tabIndex: Int = 0
    var scrollTop: Double = 0.0
    var scrollLeft: Double = 0.0
    var scrollHeight: Int = 600
    var scrollWidth: Int = 800

    var parentElement: WebDomElement?
        get() = parent
        set(value) {
            parent = value
        }

    private var explicitClientWidth: Int = 800
    private var explicitClientHeight: Int = 600
    private var explicitOffsetLeft: Int = 0
    private var explicitOffsetTop: Int = 0

    var clientWidth: Int
        get() = WebBrowserDomBridge.getClientWidth(this, explicitClientWidth)
        set(value) {
            explicitClientWidth = value
        }

    var clientHeight: Int
        get() = WebBrowserDomBridge.getClientHeight(this, explicitClientHeight)
        set(value) {
            explicitClientHeight = value
        }

    var offsetLeft: Int
        get() = WebBrowserDomBridge.getOffsetLeft(this, explicitOffsetLeft)
        set(value) {
            explicitOffsetLeft = value
        }

    var offsetTop: Int
        get() = WebBrowserDomBridge.getOffsetTop(this, explicitOffsetTop)
        set(value) {
            explicitOffsetTop = value
        }

    private val listeners = linkedMapOf<String, MutableList<(Any?) -> Unit>>()

    val eventListeners: Map<String, List<(Any?) -> Unit>>
        get() = listeners

    init {
        WebBrowserDomBridge.createPeer(this)
    }

    internal fun updateValueFromBrowser(newValue: String) {
        this.value = newValue
    }

    internal fun updateCheckedFromBrowser(newChecked: Boolean) {
        this.checked = newChecked
    }

    internal fun dispatchFromBrowser(type: String, payload: Any? = null) {
        listeners[type]?.toList()?.forEach { it(payload) }
    }

    fun setAttribute(name: String, value: String) {
        attributes[name] = value
        WebBrowserDomBridge.syncAttribute(this, name, value)
    }

    fun getAttribute(name: String): String? = attributes[name]

    fun removeAttribute(name: String) {
        attributes.remove(name)
        WebBrowserDomBridge.syncAttribute(this, name, null)
    }

    fun hasAttribute(name: String): Boolean = name in attributes

    fun appendChild(child: WebDomElement) {
        child.parent?.removeChild(child)
        child.parent = this
        children.add(child)
        WebBrowserDomBridge.appendChild(this, child)
    }

    fun insertChildAt(index: Int, child: WebDomElement) {
        child.parent?.removeChild(child)
        child.parent = this
        if (index < 0 || index >= children.size) {
            children.add(child)
            WebBrowserDomBridge.appendChild(this, child)
        } else {
            children.add(index, child)
            WebBrowserDomBridge.insertChildAt(this, index, child)
        }
    }

    fun removeChild(child: WebDomElement): Boolean {
        val removed = children.remove(child)
        if (removed) {
            child.parent = null
            WebBrowserDomBridge.removeChild(this, child)
        }
        return removed
    }

    fun clearChildren() {
        children.forEach { it.parent = null }
        children.clear()
        WebBrowserDomBridge.clearChildren(this)
    }

    fun remove() {
        parent?.removeChild(this)
    }

    fun addEventListener(type: String, listener: (Any?) -> Unit) {
        listeners.getOrPut(type) { mutableListOf() }.add(listener)
    }

    fun removeEventListener(type: String, listener: (Any?) -> Unit) {
        val list = listeners[type] ?: return
        list.remove(listener)
        if (list.isEmpty()) {
            listeners.remove(type)
        }
    }

    fun dispatchEvent(type: String, payload: Any? = null) {
        listeners[type]?.toList()?.forEach { it(payload) }
    }

    fun click() {
        if (!disabled) {
            if (!WebBrowserDomBridge.triggerClick(this)) {
                dispatchEvent("click")
            }
        }
    }

    /**
     * Simulates a mouse right-click (`contextmenu`) activation at viewport coordinates `(x, y)`.
     */
    fun simulateRightClick(x: Int = 0, y: Int = 0) {
        if (!disabled && "contextmenu" in listeners) {
            dispatchEvent("contextmenu", "$x,$y")
        }
    }

    /**
     * Simulates a mobile touch long-press gesture at viewport coordinates `(x, y)`.
     * Activates `"contextmenu"` when [durationMs] is at least [LONG_PRESS_THRESHOLD_MS] (500ms)
     * and touch movement stays within the 10px threshold.
     */
    fun simulateLongPress(
        x: Int = 0,
        y: Int = 0,
        durationMs: Int = LONG_PRESS_THRESHOLD_MS,
        moveDeltaX: Int = 0,
        moveDeltaY: Int = 0
    ): Boolean {
        if (disabled || "contextmenu" !in listeners) return false
        dispatchEvent("touchstart", "$x,$y")
        if (moveDeltaX != 0 || moveDeltaY != 0) {
            dispatchEvent("touchmove", "${x + moveDeltaX},${y + moveDeltaY}")
        }
        val movedTooFar = moveDeltaX * moveDeltaX + moveDeltaY * moveDeltaY > 100
        val triggered = !movedTooFar && durationMs >= LONG_PRESS_THRESHOLD_MS
        if (triggered) {
            dispatchEvent("contextmenu", "$x,$y")
        }
        dispatchEvent("touchend", "$x,$y")
        return triggered
    }

    fun toOuterHtml(indent: String = ""): String = buildString {
        val attrs = mutableListOf<String>()
        if (id.isNotEmpty()) {
            attrs.add("id=\"${escapeHtmlAttr(id)}\"")
        }
        val effectiveClasses = getEffectiveDomClassName()
        if (effectiveClasses.isNotEmpty()) {
            attrs.add("class=\"${escapeHtmlAttr(effectiveClasses)}\"")
        }
        if (backingStyle.isNotEmpty()) {
            val inlineCss = backingStyle.entries.joinToString("; ") { (k, v) ->
                if (isStyleImportant(k)) "$k: $v !important" else "$k: $v"
            }
            attrs.add("style=\"${escapeHtmlAttr(inlineCss)}\"")
        }
        for ((k, v) in attributes) {
            if (k != "id" && k != "class" && k != "style") {
                attrs.add("$k=\"${escapeHtmlAttr(v)}\"")
            }
        }
        if (disabled && !attributes.containsKey("disabled")) {
            attrs.add("disabled=\"disabled\"")
        }

        val attrSuffix = if (attrs.isEmpty()) "" else " " + attrs.joinToString(" ")
        append("$indent<$tagName$attrSuffix>")

        if (innerHTML.isNotEmpty()) {
            append(innerHTML)
        } else if (children.isNotEmpty()) {
            for (child in children) {
                append(child.toOuterHtml())
            }
        } else if (textContent.isNotEmpty()) {
            append(escapeHtmlText(textContent))
        }

        append("</$tagName>")
    }

    private fun escapeHtmlText(text: String): String =
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    private fun escapeHtmlAttr(value: String): String =
        escapeHtmlText(value).replace("\"", "&quot;")

    companion object {
        const val LONG_PRESS_THRESHOLD_MS: Int = 500
    }
}
