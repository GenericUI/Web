package com.nexustools.genericui.web

/**
 * Lightweight, multiplatform-safe (`js` + `wasmJs`) HTML DOM element representation for the `:web` backend.
 * Mirrors standard W3C `HTMLElement` / `HTMLInputElement` / `HTMLCanvasElement` properties, CSS `classList`,
 * inline `style`, attributes, scroll offsets, and DOM event listeners.
 */
class WebDomElement(
    val tagName: String
) {
    var id: String = ""
    var textContent: String = ""
    var innerHTML: String = ""
    var value: String = ""
    var checked: Boolean = false
    var disabled: Boolean = false
    var readOnly: Boolean = false
    var multiple: Boolean = false
    var webkitdirectory: Boolean = false
    var directory: Boolean = false
    var accept: String = ""
    var type: String = ""
    var tabIndex: Int = 0

    var scrollTop: Double = 0.0
    var scrollLeft: Double = 0.0
    var clientWidth: Int = 800
    var clientHeight: Int = 600
    var scrollHeight: Int = 600
    var scrollWidth: Int = 800

    val classList: MutableSet<String> = linkedSetOf()
    val style: MutableMap<String, String> = linkedMapOf()
    val attributes: MutableMap<String, String> = linkedMapOf()

    var parentElement: WebDomElement? = null
        private set

    private val _children = mutableListOf<WebDomElement>()
    val children: List<WebDomElement>
        get() = _children

    private val eventListeners = linkedMapOf<String, MutableList<(Any?) -> Unit>>()

    var className: String
        get() = classList.joinToString(" ")
        set(value) {
            classList.clear()
            value.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.forEach { classList.add(it) }
        }

    fun setAttribute(name: String, value: String) {
        attributes[name] = value
        when (name) {
            "id" -> id = value
            "type" -> type = value
            "accept" -> accept = value
            "tabindex" -> tabIndex = value.toIntOrNull() ?: 0
            "webkitdirectory" -> webkitdirectory = value != "false"
            "directory" -> directory = value != "false"
            "multiple" -> multiple = value != "false"
        }
    }

    fun getAttribute(name: String): String? = attributes[name]

    fun removeAttribute(name: String) {
        attributes.remove(name)
    }

    fun appendChild(child: WebDomElement): WebDomElement {
        child.parentElement?.removeChild(child)
        child.parentElement = this
        _children.add(child)
        return child
    }

    fun insertChildAt(index: Int, child: WebDomElement): WebDomElement {
        child.parentElement?.removeChild(child)
        child.parentElement = this
        if (index < 0 || index >= _children.size) {
            _children.add(child)
        } else {
            _children.add(index, child)
        }
        return child
    }

    fun removeChild(child: WebDomElement): Boolean {
        val removed = _children.remove(child)
        if (removed) {
            child.parentElement = null
        }
        return removed
    }

    fun clearChildren() {
        for (child in _children) {
            child.parentElement = null
        }
        _children.clear()
    }

    fun remove() {
        parentElement?.removeChild(this)
    }

    fun addEventListener(eventType: String, listener: (Any?) -> Unit) {
        eventListeners.getOrPut(eventType) { mutableListOf() }.add(listener)
    }

    fun removeEventListener(eventType: String, listener: (Any?) -> Unit) {
        eventListeners[eventType]?.remove(listener)
    }

    fun dispatchEvent(eventType: String, payload: Any? = null) {
        eventListeners[eventType]?.toList()?.forEach { it(payload) }
    }

    fun click() {
        dispatchEvent("click", null)
    }
}
