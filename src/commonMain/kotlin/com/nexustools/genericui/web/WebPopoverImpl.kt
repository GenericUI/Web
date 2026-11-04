package com.nexustools.genericui.web

import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.enums.ui.Placement
import com.nexustools.genericui.nativeimpl.overlay.PopoverNativeImpl

/**
 * Web implementation of [PopoverNativeImpl] rendered as a Bootstrap 5 `<genericui:popover class="popover fade">`
 * with `.popover-header` and `.popover-body`.
 */
class WebPopoverImpl : WebBaseContainerNativeImpl("genericui:popover", "popover fade"), PopoverNativeImpl<WebDomElement> {
    private var placement: Placement = Placement.BOTTOM

    val headerElement = WebDomElement("h3").apply {
        classList.addAll(listOf("popover-header", "d-none"))
    }

    val bodyElement = WebDomElement("div").apply {
        classList.add("popover-body")
    }

    init {
        nativeWidget.setAttribute("role", "tooltip")
        nativeWidget.appendChild(headerElement)
        nativeWidget.appendChild(bodyElement)
    }

    override fun addChildNative(childImpl: NativeImpl<*>, index: Int) {
        val dom = childImpl.nativeWidget as? WebDomElement ?: return
        bodyElement.insertChildAt(index, dom)
    }

    override fun removeChildNative(childImpl: NativeImpl<*>) {
        val dom = childImpl.nativeWidget as? WebDomElement ?: return
        bodyElement.removeChild(dom)
    }

    override fun clearChildrenNative() {
        bodyElement.clearChildren()
    }

    override fun setTitle(title: String?) {
        if (title != null && title.isNotEmpty()) {
            nativeWidget.setAttribute("data-popover-title", title)
            headerElement.textContent = title
            headerElement.classList.remove("d-none")
        } else {
            nativeWidget.removeAttribute("data-popover-title")
            headerElement.textContent = ""
            headerElement.classList.add("d-none")
        }
    }

    override fun setPlacement(placement: Placement) {
        this.placement = placement
        val placementName = placement.name.lowercase()
        nativeWidget.setAttribute("data-bs-placement", placementName)
        nativeWidget.classList.removeAll(listOf("bs-popover-top", "bs-popover-bottom", "bs-popover-start", "bs-popover-end", "bs-popover-auto"))
        val bsClass = when (placement) {
            Placement.TOP, Placement.TOP_START, Placement.TOP_END -> "bs-popover-top"
            Placement.BOTTOM, Placement.BOTTOM_START, Placement.BOTTOM_END -> "bs-popover-bottom"
            Placement.START -> "bs-popover-start"
            Placement.END -> "bs-popover-end"
        }
        nativeWidget.classList.add(bsClass)
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
