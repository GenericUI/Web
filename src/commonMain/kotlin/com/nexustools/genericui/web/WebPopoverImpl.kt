package com.nexustools.genericui.web

import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.enums.ui.Placement
import com.nexustools.genericui.nativeimpl.overlay.PopoverNativeImpl

/**
 * Web implementation of [PopoverNativeImpl].
 */
class WebPopoverImpl : WebBaseContainerNativeImpl("div", "popover"), PopoverNativeImpl<WebDomElement> {
    private var placement: Placement = Placement.BOTTOM

    override fun setTitle(title: String?) {
        if (title != null) nativeWidget.setAttribute("data-popover-title", title) else nativeWidget.removeAttribute("data-popover-title")
    }

    override fun setPlacement(placement: Placement) {
        this.placement = placement
        nativeWidget.setAttribute("data-bs-placement", placement.name.lowercase())
    }

    override fun showAnchored(anchor: NativeImpl<*>?, x: Int, y: Int) {
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
