package com.nexustools.genericui.web

import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.geometry.Rect
import com.nexustools.genericui.nativeimpl.dock.DockWindowNativeImpl
import com.nexustools.genericui.nativeimpl.shell.WindowNativeImpl

/**
 * Web implementation of [DockWindowNativeImpl] rendered as a centered Bootstrap 5 modal dialog
 * (`<genericui:panelwindow class="dock-window modal fade show">` -> `.modal-dialog.modal-dialog-centered.modal-dialog-scrollable`
 * -> `.modal-content` -> `.modal-header` + `.modal-body`).
 */
class WebDockWindowImpl : WebBaseContainerNativeImpl("genericui:panelwindow", "dock-window"), DockWindowNativeImpl<WebDomElement> {
    private var closeListener: (() -> Unit)? = null
    private var ownerWindowRef: WebWindowImpl? = null

    val modalDialog = WebDomElement("div").apply {
        classList.addAll(listOf("modal-dialog", "modal-dialog-centered", "modal-dialog-scrollable"))
        style["width"] = "100%"
        style["max-width"] = "min(560px, calc(100vw - 1rem))"
        style["margin"] = "auto"
    }

    val modalContent = WebDomElement("div").apply {
        classList.addAll(listOf("modal-content", "shadow-lg"))
        style["width"] = "100%"
        style["max-height"] = "calc(100dvh - 1rem)"
        style["overflow"] = "hidden"
        style["display"] = "flex"
        style["flex-direction"] = "column"
    }

    val modalHeader = WebDomElement("div").apply {
        classList.addAll(listOf("modal-header", "py-2", "px-3"))
    }

    val modalTitleElement = WebDomElement("h5").apply {
        classList.addAll(listOf("modal-title", "fs-6", "fw-semibold", "mb-0"))
    }

    val closeButton = WebDomElement("button").apply {
        setAttribute("type", "button")
        classList.add("btn-close")
        setAttribute("data-bs-dismiss", "modal")
        setAttribute("aria-label", "Close")
    }

    val modalBody = WebDomElement("div").apply {
        classList.add("modal-body")
        style["overflow-y"] = "auto"
        style["overflow-x"] = "hidden"
        style["flex"] = "1 1 auto"
    }

    init {
        nativeWidget.classList.addAll(listOf("modal", "fade"))
        nativeWidget.setAttribute("data-tool-modal", "true")
        nativeWidget.setAttribute("data-taskbar-entry", "false")
        nativeWidget.setAttribute("data-minimizable", "false")
        nativeWidget.setAttribute("data-always-on-top-of-owner", "true")
        nativeWidget.setAttribute("tabindex", "-1")
        nativeWidget.setAttribute("role", "dialog")
        nativeWidget.setAttribute("aria-modal", "true")

        nativeWidget.style["position"] = "fixed"
        nativeWidget.style["inset"] = "0"
        nativeWidget.style["width"] = "100vw"
        nativeWidget.style["height"] = "100dvh"
        nativeWidget.style["display"] = "flex"
        nativeWidget.style["align-items"] = "center"
        nativeWidget.style["justify-content"] = "center"
        nativeWidget.style["background-color"] = "rgba(0, 0, 0, 0.55)"

        closeButton.addEventListener("click") {
            hideDockWindow()
            closeListener?.invoke()
        }
        modalHeader.appendChild(modalTitleElement)
        modalHeader.appendChild(closeButton)
        modalContent.appendChild(modalHeader)
        modalContent.appendChild(modalBody)
        modalDialog.appendChild(modalContent)
        nativeWidget.appendChild(modalDialog)

        nativeWidget.addEventListener("pointerdown") {
            bringToFront()
        }
    }

    override fun addChildNative(childImpl: NativeImpl<*>, index: Int) {
        val dom = childImpl.nativeWidget as? WebDomElement ?: return
        modalBody.insertChildAt(index, dom)
    }

    override fun removeChildNative(childImpl: NativeImpl<*>) {
        val dom = childImpl.nativeWidget as? WebDomElement ?: return
        modalBody.removeChild(dom)
    }

    override fun clearChildrenNative() {
        modalBody.clearChildren()
    }

    override fun setTitle(title: String) {
        nativeWidget.setAttribute("data-dock-window-title", title)
        modalTitleElement.textContent = title
    }

    override fun setWindowBounds(bounds: Rect) {
        nativeWidget.style["position"] = "fixed"
        nativeWidget.style["left"] = "${bounds.x}px"
        nativeWidget.style["top"] = "${bounds.y}px"
        nativeWidget.style["width"] = "${bounds.width}px"
        nativeWidget.style["height"] = "${bounds.height}px"
        modalDialog.style["max-width"] = "min(${bounds.width}px, calc(100vw - 1rem))"
        modalContent.style["max-height"] = "min(${bounds.height}px, calc(100dvh - 1rem))"
    }

    override fun showDockWindow() {
        val appRoot = WebApplicationImpl.activeApplication?.nativeApplication
        if (appRoot != null && nativeWidget.parent == null) {
            appRoot.appendChild(nativeWidget)
        }
        nativeWidget.classList.add("show")
        setVisible(true)
        nativeWidget.style["display"] = "flex"
        bringToFront()
        WebBrowserDomBridge.installModalFocusTrap(nativeWidget)
    }

    override fun hideDockWindow() {
        WebBrowserDomBridge.releaseModalFocusTrap(nativeWidget)
        nativeWidget.classList.remove("show")
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
        WebBrowserDomBridge.releaseModalFocusTrap(nativeWidget)
        ownerWindowRef?.attachedDockWindows?.remove(this)
        ownerWindowRef = null
        super.dispose()
    }
}
