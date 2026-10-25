package com.nexustools.genericui.web

import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.nativeimpl.shell.MenuNativeImpl

/**
 * Web implementation of [MenuNativeImpl] rendered as a Bootstrap 5 dropdown inside
 * `<genericui:menu class="nav-item dropdown genericui-menu">`.
 */
class WebMenuImpl :
    WebBaseContainerNativeImpl("genericui:menu", "nav-item dropdown genericui-menu"),
    MenuNativeImpl<WebDomElement> {

    private var labelText: String = ""
    private var iconName: String? = null
    private var openState: Boolean = false

    val toggleButton: WebDomElement = WebDomElement("button").apply {
        className = "btn btn-sm nav-link dropdown-toggle genericui-menu-toggle"
        type = "button"
        setAttribute("type", "button")
        setAttribute("data-bs-toggle", "dropdown")
        setAttribute("aria-expanded", "false")
    }

    val dropdownMenu: WebDomElement = WebDomElement("div").apply {
        className = "dropdown-menu genericui-dropdown-menu"
        setAttribute("role", "menu")
        style["display"] = "none"
    }

    val isOpen: Boolean
        get() = openState

    init {
        menuRegistry[nativeWidget] = this
        dropdownMenuRegistry[dropdownMenu] = this
        nativeWidget.appendChild(toggleButton)
        nativeWidget.appendChild(dropdownMenu)

        toggleButton.addEventListener("click") {
            if (isEnabled()) {
                if (openState) {
                    close()
                } else {
                    open()
                }
            }
        }

        nativeWidget.addEventListener("mouseenter") {
            if (!isEnabled()) return@addEventListener
            val parentDom = nativeWidget.parent ?: return@addEventListener
            if (parentDom.tagName.equals("genericui:menubar", ignoreCase = true)) {
                val anySiblingOpen = parentDom.children.any { sibling ->
                    sibling !== nativeWidget && (sibling.classList.contains("show") || menuRegistry[sibling]?.isOpen == true)
                }
                if (anySiblingOpen && !openState) {
                    open()
                }
            } else if (nativeWidget.classList.contains("genericui-submenu")) {
                open()
            }
        }

        dropdownMenu.addEventListener("click") {
            closeMenuHierarchy()
        }
    }

    override fun setLabel(label: String) {
        labelText = label
        nativeWidget.setAttribute("data-menu-label", label)
        toggleButton.textContent = label
    }

    override fun getLabel(): String = labelText

    override fun setIcon(icon: String?) {
        iconName = icon
        if (icon != null) {
            nativeWidget.setAttribute("data-icon", icon)
            toggleButton.setAttribute("data-icon", icon)
        } else {
            nativeWidget.removeAttribute("data-icon")
            toggleButton.removeAttribute("data-icon")
        }
    }

    override fun getIcon(): String? = iconName

    fun open() {
        val parentDom = nativeWidget.parent
        if (parentDom != null) {
            for (sibling in parentDom.children) {
                if (sibling !== nativeWidget) {
                    menuRegistry[sibling]?.close()
                }
            }
        }
        openState = true
        nativeWidget.classList.add("show")
        toggleButton.classList.add("show")
        toggleButton.setAttribute("aria-expanded", "true")
        dropdownMenu.classList.add("show")
        dropdownMenu.style["display"] = "block"
    }

    fun close() {
        for (child in dropdownMenu.children) {
            menuRegistry[child]?.close()
        }
        openState = false
        nativeWidget.classList.remove("show")
        toggleButton.classList.remove("show")
        toggleButton.setAttribute("aria-expanded", "false")
        dropdownMenu.classList.remove("show")
        dropdownMenu.style["display"] = "none"
    }

    fun toggle() {
        if (openState) close() else open()
    }

    internal fun closeMenuHierarchy() {
        var rootMenu: WebMenuImpl = this
        while (true) {
            val parentContainer = rootMenu.nativeWidget.parent ?: break
            val parentMenu = dropdownMenuRegistry[parentContainer] ?: break
            rootMenu = parentMenu
        }
        rootMenu.close()
        val hostContextMenu = findEnclosingContextMenu(rootMenu.nativeWidget.parent)
        hostContextMenu?.hide()
    }

    override fun addChildNative(childImpl: NativeImpl<*>, index: Int) {
        val childDom = childImpl.nativeWidget as? WebDomElement ?: return
        if (childImpl is WebMenuImpl || childDom.tagName.equals("genericui:menu", ignoreCase = true)) {
            childDom.classList.add("dropend")
            childDom.classList.add("genericui-submenu")
            val subMenu = (childImpl as? WebMenuImpl) ?: menuRegistry[childDom]
            subMenu?.toggleButton?.classList?.add("dropdown-item")
        }
        dropdownMenu.insertChildAt(index, childDom)
        WebWindowImpl.scheduleLayoutRefresh()
    }

    override fun removeChildNative(childImpl: NativeImpl<*>) {
        val childDom = childImpl.nativeWidget as? WebDomElement ?: return
        dropdownMenu.removeChild(childDom)
    }

    override fun clearChildrenNative() {
        dropdownMenu.clearChildren()
    }

    override fun dispose() {
        menuRegistry.remove(nativeWidget)
        dropdownMenuRegistry.remove(dropdownMenu)
        super.dispose()
    }

    companion object {
        private val menuRegistry = mutableMapOf<WebDomElement, WebMenuImpl>()
        private val dropdownMenuRegistry = mutableMapOf<WebDomElement, WebMenuImpl>()

        internal fun findMenuForDom(dom: WebDomElement?): WebMenuImpl? =
            if (dom != null) menuRegistry[dom] else null

        internal fun findMenuForDropdownContainer(dom: WebDomElement?): WebMenuImpl? =
            if (dom != null) dropdownMenuRegistry[dom] else null

        internal fun findEnclosingContextMenu(startDom: WebDomElement?): WebContextMenuImpl? {
            var curr = startDom
            while (curr != null) {
                WebContextMenuImpl.findContextMenuForDom(curr)?.let { return it }
                curr = curr.parent
            }
            return null
        }

        internal fun closeAllTopLevelMenus() {
            for (menu in menuRegistry.values.toList()) {
                if (menu.isOpen && !menu.nativeWidget.classList.contains("genericui-submenu")) {
                    menu.close()
                }
            }
        }
    }
}
