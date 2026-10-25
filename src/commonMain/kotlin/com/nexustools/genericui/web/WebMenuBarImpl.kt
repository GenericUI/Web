package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.shell.MenuBarNativeImpl

/**
 * Web implementation of [MenuBarNativeImpl] rendered as
 * `<genericui:menubar class="navbar navbar-expand genericui-menubar" role="menubar">`.
 */
class WebMenuBarImpl :
    WebBaseContainerNativeImpl("genericui:menubar", "navbar navbar-expand genericui-menubar"),
    MenuBarNativeImpl<WebDomElement> {

    init {
        nativeWidget.setAttribute("role", "menubar")
    }

    fun closeAllMenus() {
        for (child in nativeWidget.children) {
            WebMenuImpl.findMenuForDom(child)?.close()
        }
    }
}
