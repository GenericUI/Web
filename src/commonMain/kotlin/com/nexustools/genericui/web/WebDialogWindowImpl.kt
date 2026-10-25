package com.nexustools.genericui.web

import com.nexustools.genericui.enums.dialog.DialogPresentationStyle
import com.nexustools.genericui.nativeimpl.dialog.DialogWindowNativeImpl
import com.nexustools.genericui.nativeimpl.shell.WindowNativeImpl

/**
 * Web implementation of [DialogWindowNativeImpl] rendered as `<genericui:dialogwindow class="modal">`.
 */
class WebDialogWindowImpl : WebBaseContainerNativeImpl("genericui:dialogwindow", "modal"), DialogWindowNativeImpl<WebDomElement> {
    private var titleText: String = ""
    private var presentationStyle: DialogPresentationStyle = DialogPresentationStyle.WINDOW_MODAL
    private var modalFlag: Boolean = true
    private var fullscreenFlag: Boolean = false
    private var onClosedListener: (() -> Unit)? = null

    override fun setTitle(title: String) {
        titleText = title
        nativeWidget.setAttribute("data-dialog-title", title)
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
    }

    override fun setFullscreen(fullscreen: Boolean) {
        fullscreenFlag = fullscreen
        if (fullscreen) nativeWidget.classList.add("modal-fullscreen") else nativeWidget.classList.remove("modal-fullscreen")
    }

    override fun showDialog(ownerWindow: WindowNativeImpl<*>?) {
        nativeWidget.classList.add("show")
        setVisible(true)
    }

    override fun closeDialog() {
        nativeWidget.classList.remove("show")
        setVisible(false)
        onClosedListener?.invoke()
    }

    override fun setOnDialogClosed(listener: (() -> Unit)?) {
        onClosedListener = listener
    }
}
