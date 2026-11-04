package com.nexustools.genericui.web

import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.nativeimpl.navigation.NavbarNativeImpl

/**
 * Web implementation of [NavbarNativeImpl] rendered as a Bootstrap 5 `<genericui:navbar class="navbar navbar-expand">`.
 */
class WebNavbarImpl :
    WebBaseContainerNativeImpl("genericui:navbar", "navbar navbar-expand bg-body-tertiary border-bottom px-3"),
    NavbarNativeImpl<WebDomElement> {

    private var brandTitleText: String = ""

    val containerFluid = WebDomElement("div").apply {
        classList.addAll(listOf("container-fluid", "px-0", "d-flex", "align-items-center", "justify-content-between", "w-100"))
    }

    val brandElement = WebDomElement("span").apply {
        classList.addAll(listOf("navbar-brand", "mb-0", "h6", "d-none"))
    }

    val navItemsSlot = WebDomElement("div").apply {
        classList.addAll(listOf("navbar-nav", "d-flex", "flex-row", "align-items-center", "gap-2", "flex-grow-1", "w-100"))
    }

    init {
        nativeWidget.setAttribute("role", "navigation")
        containerFluid.appendChild(brandElement)
        containerFluid.appendChild(navItemsSlot)
        nativeWidget.appendChild(containerFluid)
    }

    override fun setBrandTitle(title: String) {
        brandTitleText = title
        nativeWidget.setAttribute("data-brand-title", title)
        brandElement.textContent = title
        if (title.isNotEmpty()) {
            brandElement.classList.remove("d-none")
        } else {
            brandElement.classList.add("d-none")
        }
    }

    override fun getBrandTitle(): String = brandTitleText

    override fun addChildNative(childImpl: NativeImpl<*>, index: Int) {
        val dom = childImpl.nativeWidget as? WebDomElement ?: return
        if (containerFluid.parent == nativeWidget) {
            navItemsSlot.insertChildAt(index, dom)
        } else {
            nativeWidget.insertChildAt(index, dom)
        }
    }

    override fun removeChildNative(childImpl: NativeImpl<*>) {
        val dom = childImpl.nativeWidget as? WebDomElement ?: return
        if (containerFluid.parent == nativeWidget) {
            navItemsSlot.removeChild(dom)
        } else {
            nativeWidget.removeChild(dom)
        }
    }

    override fun clearChildrenNative() {
        if (containerFluid.parent == nativeWidget) {
            navItemsSlot.clearChildren()
        } else {
            nativeWidget.clearChildren()
        }
    }
}
