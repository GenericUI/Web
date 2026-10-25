package com.nexustools.genericui.web

import com.nexustools.genericui.enums.dock.DockPosition
import com.nexustools.genericui.nativeimpl.dock.DockAreaNativeImpl

/**
 * Web implementation of [DockAreaNativeImpl] rendered as `<genericui:dockarea class="dock-area">`.
 */
class WebDockAreaImpl : WebBaseContainerNativeImpl("genericui:dockarea", "dock-area"), DockAreaNativeImpl<WebDomElement> {
    private var position: DockPosition = DockPosition.RIGHT
    private var collapsed: Boolean = false

    override fun setPosition(position: DockPosition) {
        this.position = position
        nativeWidget.setAttribute("data-dock-position", position.name.lowercase())
    }

    override fun setCollapsed(collapsed: Boolean) {
        this.collapsed = collapsed
        if (collapsed) nativeWidget.classList.add("collapsed") else nativeWidget.classList.remove("collapsed")
    }

    override fun isCollapsed(): Boolean = collapsed
}
