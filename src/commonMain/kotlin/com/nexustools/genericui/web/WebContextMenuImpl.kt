package com.nexustools.genericui.web

import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.nativeimpl.shell.ContextMenuNativeImpl

/**
 * Web implementation of [ContextMenuNativeImpl] rendered as
 * `<genericui:contextmenu class="dropdown-menu genericui-context-menu">`
 * positioned at `(x, y)` in viewport coordinates.
 */
class WebContextMenuImpl :
    WebBaseContainerNativeImpl("genericui:contextmenu", "dropdown-menu genericui-context-menu"),
    ContextMenuNativeImpl<WebDomElement> {

    init {
        contextMenuRegistry[nativeWidget] = this
        nativeWidget.setAttribute("role", "menu")
        nativeWidget.style["display"] = "none"
        nativeWidget.addEventListener("click") {
            hide()
        }
    }

    override fun addChildNative(childImpl: NativeImpl<*>, index: Int) {
        val childDom = childImpl.nativeWidget as? WebDomElement ?: return
        if (childImpl is WebMenuImpl || childDom.tagName.equals("genericui:menu", ignoreCase = true)) {
            childDom.classList.add("dropend")
            childDom.classList.add("genericui-submenu")
            val subMenu = (childImpl as? WebMenuImpl) ?: WebMenuImpl.findMenuForDom(childDom)
            subMenu?.toggleButton?.classList?.add("dropdown-item")
        }
        nativeWidget.insertChildAt(index, childDom)
        WebWindowImpl.scheduleLayoutRefresh()
    }

    override fun removeChildNative(childImpl: NativeImpl<*>) {
        val childDom = childImpl.nativeWidget as? WebDomElement ?: return
        nativeWidget.removeChild(childDom)
    }

    override fun clearChildrenNative() {
        nativeWidget.clearChildren()
    }

    override fun showAt(anchor: NativeImpl<*>?, x: Int, y: Int) {
        hideAllExcept(this)
        WebMenuImpl.closeAllTopLevelMenus()
        val appRoot = WebApplicationImpl.activeApplication?.nativeApplication
        if (nativeWidget.parent == null && appRoot != null) {
            appRoot.appendChild(nativeWidget)
        }
        nativeWidget.classList.add("show")
        nativeWidget.style["position"] = "fixed"
        nativeWidget.style["left"] = "${x}px"
        nativeWidget.style["top"] = "${y}px"
        nativeWidget.style["display"] = "block"
        nativeWidget.style["z-index"] = "2050"
    }

    override fun hide() {
        for (child in nativeWidget.children) {
            WebMenuImpl.findMenuForDom(child)?.close()
        }
        nativeWidget.classList.remove("show")
        nativeWidget.style["display"] = "none"
    }

    override fun dispose() {
        contextMenuRegistry.remove(nativeWidget)
        super.dispose()
    }

    companion object {
        private val contextMenuRegistry = mutableMapOf<WebDomElement, WebContextMenuImpl>()

        internal fun findContextMenuForDom(dom: WebDomElement?): WebContextMenuImpl? =
            if (dom != null) contextMenuRegistry[dom] else null

        internal fun hideAllExcept(active: WebContextMenuImpl? = null) {
            for (menu in contextMenuRegistry.values.toList()) {
                if (menu !== active) {
                    menu.hide()
                }
            }
        }
    }
}
