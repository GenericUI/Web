package com.nexustools.genericui.web

import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.nativeimpl.shell.ContextMenuNativeImpl

/**
 * Web implementation of [ContextMenuNativeImpl] positioned at `(x, y)` in viewport coordinates.
 */
class WebContextMenuImpl : WebBaseContainerNativeImpl("div", "dropdown-menu"), ContextMenuNativeImpl<WebDomElement> {
    override fun showAt(anchor: NativeImpl<*>?, x: Int, y: Int) {
        nativeWidget.style["position"] = "fixed"
        nativeWidget.style["left"] = "${x}px"
        nativeWidget.style["top"] = "${y}px"
        nativeWidget.classList.add("show")
        setVisible(true)
    }

    override fun hide() {
        nativeWidget.classList.remove("show")
        setVisible(false)
    }
}
