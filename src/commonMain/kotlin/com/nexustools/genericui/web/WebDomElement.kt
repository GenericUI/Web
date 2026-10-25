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

    private val backingClassList = linkedSetOf<String>()

    val classList: MutableSet<String> = object : AbstractMutableSet<String>() {
        override val size: Int
            get() = backingClassList.size

        override fun add(element: String): Boolean {
            val added = backingClassList.add(element)
            if (added) {
                WebBrowserDomBridge.syncClassName(this@WebDomElement, className)
            }
            return added
        }

        override fun remove(element: String): Boolean {
            val removed = backingClassList.remove(element)
            if (removed) {
                WebBrowserDomBridge.syncClassName(this@WebDomElement, className)
            }
            return removed
        }

        override fun clear() {
            if (backingClassList.isNotEmpty()) {
                backingClassList.clear()
                WebBrowserDomBridge.syncClassName(this@WebDomElement, "")
            }
        }

        override fun iterator(): MutableIterator<String> {
            val delegate = backingClassList.iterator()
            return object : MutableIterator<String> {
                override fun hasNext(): Boolean = delegate.hasNext()
                override fun next(): String = delegate.next()
                override fun remove() {
                    delegate.remove()
                    WebBrowserDomBridge.syncClassName(this@WebDomElement, className)
                }
            }
        }
    }

    var className: String
        get() = backingClassList.joinToString(" ")
        set(value) {
            backingClassList.clear()
            value.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.forEach { backingClassList.add(it) }
            WebBrowserDomBridge.syncClassName(this, className)
        }

    private val backingStyle = linkedMapOf<String, String>()

    val style: MutableMap<String, String> = object : AbstractMutableMap<String, String>() {
        override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
            get() = backingStyle.entries

        override fun put(key: String, value: String): String? {
            val prev = backingStyle.put(key, value)
            if (prev != value) {
                WebBrowserDomBridge.syncStyle(this@WebDomElement, key, value)
            }
            return prev
        }

        override fun remove(key: String): String? {
            val prev = backingStyle.remove(key)
            WebBrowserDomBridge.syncStyle(this@WebDomElement, key, null)
            return prev
        }

        override fun clear() {
            backingStyle.clear()
            WebBrowserDomBridge.clearStyles(this@WebDomElement)
        }
    }

    fun setStyle(key: String, value: String?) {
        if (value == null) {
            style.remove(key)
        } else {
            style[key] = value
        }
    }

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
        listeners[type]?.remove(listener)
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
}
