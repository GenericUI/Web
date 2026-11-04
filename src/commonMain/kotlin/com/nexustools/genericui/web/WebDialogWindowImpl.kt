package com.nexustools.genericui.web

import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.enums.dialog.DialogPresentationStyle
import com.nexustools.genericui.geometry.Size
import com.nexustools.genericui.nativeimpl.dialog.DialogWindowNativeImpl
import com.nexustools.genericui.nativeimpl.shell.WindowNativeImpl
import com.nexustools.genericui.style.css.ComponentStyle
import com.nexustools.genericui.style.css.ComputedStyle

/**
 * Web implementation of [DialogWindowNativeImpl] rendered as a centered Bootstrap 5 modal dialog
 * (`<genericui:dialogwindow class="modal fade">` -> `.modal-dialog.modal-dialog-centered.modal-dialog-scrollable`
 * -> `.modal-content` -> `.modal-header` + `.modal-body`).
 *
 * Enforces:
 * - Full-viewport centered flex positioning (`position: fixed; inset: 0; display: flex; align-items: center; justify-content: center;`)
 *   above all application and player overlays (`z-index >= 2200`).
 * - Semi-transparent black backdrop (`rgba(0, 0, 0, 0.55)`) and Tab/Shift+Tab focus trapping
 *   ([WebBrowserDomBridge.installModalFocusTrap]) when [modalFlag] is `true`.
 * - Responsive small-screen sizing where `preferredSize` constrains `.modal-dialog` (`max-width: min(W, calc(100vw - 1rem))`)
 *   and `.modal-content` (`max-height: min(H, calc(100dvh - 1rem))`) instead of shrinking the outer `.modal` container.
 */
class WebDialogWindowImpl :
    WebBaseContainerNativeImpl("genericui:dialogwindow", "modal fade"),
    DialogWindowNativeImpl<WebDomElement> {

    private var titleText: String = ""
    private var presentationStyle: DialogPresentationStyle = DialogPresentationStyle.WINDOW_MODAL
    private var modalFlag: Boolean = true
    private var fullscreenFlag: Boolean = false
    private var onClosedListener: (() -> Unit)? = null
    private var preferredDialogSize: Size? = null
    private var isDialogShowing: Boolean = false

    val modalDialog = WebDomElement("div").apply {
        classList.addAll(listOf("modal-dialog", "modal-dialog-centered"))
        style["width"] = "fit-content"
        style["min-width"] = "0"
        style["max-width"] = "calc(100vw - 2rem)"
        style["height"] = "auto"
        style["max-height"] = "calc(100dvh - 2rem)"
        style["margin"] = "auto"
        style["pointer-events"] = "auto"
    }

    val modalContent = WebDomElement("div").apply {
        classList.addAll(listOf("modal-content", "shadow-lg"))
        style["width"] = "fit-content"
        style["min-width"] = "100%"
        style["max-width"] = "calc(100vw - 2rem)"
        style["height"] = "auto"
        style["max-height"] = "calc(100dvh - 2rem)"
        style["overflow"] = "hidden"
        style["display"] = "flex"
        style["flex-direction"] = "column"
    }

    val modalHeader = WebDomElement("div").apply {
        classList.addAll(listOf("modal-header", "py-2", "px-3"))
        style["flex-shrink"] = "0"
        style["gap"] = "16px"
    }

    val modalTitleElement = WebDomElement("h5").apply {
        classList.addAll(listOf("modal-title", "fs-6", "fw-semibold", "mb-0"))
        style["white-space"] = "nowrap"
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
        style["min-height"] = "0"
    }

    private val backdropElement = WebDomElement("div").apply {
        classList.addAll(listOf("modal-backdrop", "fade", "show"))
        style["position"] = "fixed"
        style["inset"] = "0"
        style["width"] = "100vw"
        style["height"] = "100dvh"
        style["background-color"] = "rgba(0, 0, 0, 0.55)"
        style["z-index"] = "2190"
    }

    init {
        nativeWidget.setAttribute("tabindex", "-1")
        nativeWidget.setAttribute("role", "dialog")
        nativeWidget.setAttribute("aria-modal", "true")
        applyModalOverlayStyles()

        closeButton.addEventListener("click") {
            closeDialog()
        }
        // Clicking the backdrop outside modalDialog closes the modal if dismissible
        backdropElement.addEventListener("click") {
            if (modalFlag) {
                closeDialog()
            }
        }
        nativeWidget.addEventListener("click") { ev ->
            if (ev === nativeWidget && modalFlag) {
                closeDialog()
            }
        }

        modalHeader.appendChild(modalTitleElement)
        modalHeader.appendChild(closeButton)
        modalContent.appendChild(modalHeader)
        modalContent.appendChild(modalBody)
        modalDialog.appendChild(modalContent)
        nativeWidget.appendChild(modalDialog)
        setVisible(false)
    }

    private fun applyModalOverlayStyles() {
        nativeWidget.setStyle("position", "fixed", true)
        nativeWidget.setStyle("inset", "0", true)
        nativeWidget.setStyle("top", "0", true)
        nativeWidget.setStyle("left", "0", true)
        nativeWidget.setStyle("width", "100vw", true)
        nativeWidget.setStyle("height", "100dvh", true)
        nativeWidget.setStyle("max-width", "100vw", true)
        nativeWidget.setStyle("max-height", "100dvh", true)
        nativeWidget.setStyle("margin", "0", true)
        nativeWidget.setStyle("padding", if (fullscreenFlag) "0" else "0.5rem", true)
        nativeWidget.setStyle("box-sizing", "border-box", true)
        nativeWidget.setStyle("align-items", "center", true)
        nativeWidget.setStyle("justify-content", "center", true)
        nativeWidget.setStyle("overflow-x", "hidden", true)
        nativeWidget.setStyle("overflow-y", "auto", true)
        if (modalFlag) {
            nativeWidget.setStyle("background-color", "rgba(0, 0, 0, 0.55)", true)
            nativeWidget.setStyle("pointer-events", "auto", true)
        } else {
            nativeWidget.setStyle("background-color", "transparent", true)
            nativeWidget.setStyle("pointer-events", "none", true)
        }
        updateDialogDimensions()
    }

    private fun updateDialogDimensions() {
        if (fullscreenFlag) {
            modalDialog.style["width"] = "100vw"
            modalDialog.style.remove("min-width")
            modalDialog.style["max-width"] = "100vw"
            modalDialog.style["height"] = "100dvh"
            modalDialog.style["max-height"] = "100dvh"
            modalDialog.style["margin"] = "0"
            modalContent.style["width"] = "100%"
            modalContent.style.remove("min-width")
            modalContent.style.remove("max-width")
            modalContent.style["height"] = "100dvh"
            modalContent.style["max-height"] = "100dvh"
        } else {
            val pref = preferredDialogSize
            if (pref == null) {
                modalDialog.style["width"] = "fit-content"
                modalDialog.style["min-width"] = "0"
                modalDialog.style["max-width"] = "calc(100vw - 2rem)"
                modalDialog.style["height"] = "auto"
                modalDialog.style["max-height"] = "calc(100dvh - 2rem)"
                modalDialog.style["margin"] = "auto"
                modalContent.style["width"] = "fit-content"
                modalContent.style["min-width"] = "100%"
                modalContent.style["max-width"] = "calc(100vw - 2rem)"
                modalContent.style["height"] = "auto"
                modalContent.style["max-height"] = "calc(100dvh - 2rem)"
            } else {
                val w = pref.width.coerceAtLeast(280)
                val h = pref.height.coerceAtLeast(200)
                modalDialog.style["width"] = "100%"
                modalDialog.style.remove("min-width")
                modalDialog.style["max-width"] = "min(${w}px, calc(100vw - 2rem))"
                modalDialog.style.remove("height")
                modalDialog.style.remove("max-height")
                modalDialog.style["margin"] = "auto"
                modalContent.style["width"] = "100%"
                modalContent.style.remove("min-width")
                modalContent.style.remove("max-width")
                modalContent.style.remove("height")
                modalContent.style["max-height"] = "min(${h}px, calc(100dvh - 2rem))"
            }
        }
    }

    override fun setPreferredSize(size: Size?) {
        preferredDialogSize = size
        updateDialogDimensions()
        // Keep nativeWidget full-screen; never shrink the .modal overlay container to preferredSize
        nativeWidget.setStyle("width", "100vw", true)
        nativeWidget.setStyle("height", "100dvh", true)
    }

    override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        setPreferredSize(Size(width, height))
    }

    override fun applyComponentStyle(style: ComponentStyle) {
        super<WebBaseContainerNativeImpl>.applyComponentStyle(style)
        // Re-assert full-viewport modal overlay styles on nativeWidget so component styles never break centering
        applyModalOverlayStyles()
        if (isDialogShowing) {
            nativeWidget.setStyle("display", "flex", true)
        } else {
            nativeWidget.setStyle("display", "none", false)
        }
    }

    override fun applyComputedStyle(style: ComputedStyle) {
        super<WebBaseContainerNativeImpl>.applyComputedStyle(style)
        applyModalOverlayStyles()
        if (isDialogShowing) {
            nativeWidget.setStyle("display", "flex", true)
        } else {
            nativeWidget.setStyle("display", "none", false)
        }
    }

    override fun setVisible(visible: Boolean) {
        isDialogShowing = visible
        if (visible) {
            nativeWidget.setStyle("display", "flex", true)
        } else {
            nativeWidget.setStyle("display", "none", false)
        }
    }

    override fun addChildNative(childImpl: NativeImpl<*>, index: Int) {
        val dom = childImpl.nativeWidget as? WebDomElement ?: return
        if (dom.classList.contains("modal-footer") || dom.enumClasses.contains("modal-footer")) {
            dom.style["flex-shrink"] = "0"
            if ("py-2" !in dom.classList) dom.classList.add("py-2")
            if ("px-3" !in dom.classList) dom.classList.add("px-3")
            modalBody.removeChild(dom)
            modalContent.appendChild(dom)
        } else {
            val targetIdx = if (index < 0) -1 else index.coerceAtMost(modalBody.children.size)
            modalBody.insertChildAt(targetIdx, dom)
        }
    }

    override fun removeChildNative(childImpl: NativeImpl<*>) {
        val dom = childImpl.nativeWidget as? WebDomElement ?: return
        if (dom.parent === modalContent) {
            modalContent.removeChild(dom)
        } else {
            modalBody.removeChild(dom)
        }
    }

    override fun clearChildrenNative() {
        modalBody.clearChildren()
        val footerNodes = modalContent.children.filter {
            it !== modalHeader && it !== modalBody && (it.classList.contains("modal-footer") || it.enumClasses.contains("modal-footer"))
        }
        for (footer in footerNodes) {
            modalContent.removeChild(footer)
        }
    }

    override fun setTitle(title: String) {
        titleText = title
        nativeWidget.setAttribute("data-dialog-title", title)
        modalTitleElement.textContent = title
    }

    override fun getTitle(): String = titleText

    override fun setPresentationStyle(style: DialogPresentationStyle) {
        presentationStyle = style
        nativeWidget.setAttribute("data-presentation-style", style.name.lowercase())
    }

    override fun getPresentationStyle(): DialogPresentationStyle = presentationStyle

    override fun setModal(modal: Boolean) {
        modalFlag = modal
        nativeWidget.setAttribute("aria-modal", modal.toString())
        applyModalOverlayStyles()
    }

    override fun setFullscreen(fullscreen: Boolean) {
        fullscreenFlag = fullscreen
        if (fullscreen) {
            nativeWidget.classList.add("modal-fullscreen")
            modalDialog.classList.add("modal-fullscreen")
        } else {
            nativeWidget.classList.remove("modal-fullscreen")
            modalDialog.classList.remove("modal-fullscreen")
        }
        applyModalOverlayStyles()
    }

    override fun showDialog(ownerWindow: WindowNativeImpl<*>?) {
        val appRoot = WebApplicationImpl.activeApplication?.nativeApplication
        val modalZ = WebWindowImpl.allocateNextModalZIndex()
        backdropElement.style["z-index"] = (modalZ - 1).toString()
        nativeWidget.setStyle("z-index", modalZ.toString(), true)

        if (appRoot != null) {
            if (modalFlag) {
                backdropElement.remove()
                appRoot.appendChild(backdropElement)
            }
            nativeWidget.remove()
            appRoot.appendChild(nativeWidget)
        }
        applyModalOverlayStyles()
        nativeWidget.classList.add("show")
        setVisible(true)
        if (modalFlag) {
            WebBrowserDomBridge.installModalFocusTrap(nativeWidget)
        }
    }

    override fun closeDialog() {
        WebBrowserDomBridge.releaseModalFocusTrap(nativeWidget)
        nativeWidget.classList.remove("show")
        backdropElement.remove()
        setVisible(false)
        onClosedListener?.invoke()
    }

    override fun setOnDialogClosed(listener: (() -> Unit)?) {
        onClosedListener = listener
    }

    override fun dispose() {
        WebBrowserDomBridge.releaseModalFocusTrap(nativeWidget)
        backdropElement.remove()
        super.dispose()
    }
}
