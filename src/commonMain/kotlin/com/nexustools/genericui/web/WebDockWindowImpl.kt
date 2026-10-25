package com.nexustools.genericui.web

import com.nexustools.genericui.geometry.Rect
import com.nexustools.genericui.nativeimpl.dock.DockWindowNativeImpl
import com.nexustools.genericui.nativeimpl.shell.WindowNativeImpl

/**
 * Web implementation of [DockWindowNativeImpl] rendered as a floating `<genericui:panelwindow class="dock-window">` DOM utility overlay.
 */
class WebDockWindowImpl : WebBaseContainerNativeImpl("genericui:panelwindow", "dock-window"), DockWindowNativeImpl<WebDomElement> {
    private var closeListener: (() -> Unit)? = null
    private var ownerWindowRef: WebWindowImpl? = null

    init {
        nativeWidget.setAttribute("data-tool-modal", "true")
        nativeWidget.setAttribute("data-taskbar-entry", "false")
        nativeWidget.setAttribute("data-minimizable", "false")
        nativeWidget.setAttribute("data-always-on-top-of-owner", "true")
        nativeWidget.addEventListener("pointerdown") {
            bringToFront()
        }
    }

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
        bringToFront()
    }

    override fun hideDockWindow() {
        setVisible(false)
    }

    override fun setOnDockWindowCloseRequested(listener: (() -> Unit)?) {
        closeListener = listener
    }

    override fun attachToOwnerWindow(ownerWindow: WindowNativeImpl<*>?) {
        ownerWindowRef?.attachedDockWindows?.remove(this)
        val webOwner = ownerWindow as? WebWindowImpl
        ownerWindowRef = webOwner
        if (webOwner != null) {
            webOwner.attachedDockWindows.add(this)
            val ownerId = webOwner.nativeWidget.id.ifEmpty { "main-window" }
            nativeWidget.setAttribute("data-owner-window", ownerId)
        }
        bringToFront()
    }

    override fun bringToFront() {
        nativeWidget.style["z-index"] = WebWindowImpl.allocateNextZIndex().toString()
    }

    override fun dispose() {
        ownerWindowRef?.attachedDockWindows?.remove(this)
        ownerWindowRef = null
        super.dispose()
    }
}
