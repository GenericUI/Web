package com.nexustools.genericui.web

import com.nexustools.genericui.geometry.Rect
import com.nexustools.genericui.nativeimpl.dock.DockWindowNativeImpl

/**
 * Web implementation of [DockWindowNativeImpl] rendered as a floating `<genericui:panelwindow class="dock-window">` DOM utility overlay.
 */
class WebDockWindowImpl : WebBaseContainerNativeImpl("genericui:panelwindow", "dock-window"), DockWindowNativeImpl<WebDomElement> {
    private var closeListener: (() -> Unit)? = null

    override fun setTitle(title: String) {
        nativeWidget.setAttribute("data-dock-window-title", title)
    }

    override fun setWindowBounds(bounds: Rect) {
        nativeWidget.style["position"] = "fixed"
        nativeWidget.style["left"] = "${bounds.x}px"
        nativeWidget.style["top"] = "${bounds.y}px"
        nativeWidget.style["width"] = "${bounds.width}px"
        nativeWidget.style["height"] = "${bounds.height}px"
    }

    override fun showDockWindow() {
        setVisible(true)
    }

    override fun hideDockWindow() {
        setVisible(false)
    }

    override fun setOnDockWindowCloseRequested(listener: (() -> Unit)?) {
        closeListener = listener
    }
}
