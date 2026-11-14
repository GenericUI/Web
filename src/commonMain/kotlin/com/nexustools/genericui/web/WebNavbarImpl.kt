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
    private var brandIconSpec: String? = null
    private var showBrandOverride: Boolean? = null

    val containerFluid = WebDomElement("div").apply {
        classList.addAll(listOf("container-fluid", "px-0", "d-flex", "align-items-center", "justify-content-between", "w-100"))
    }

    val brandIconElement = WebDomElement("img").apply {
        classList.addAll(listOf("navbar-brand-icon", "me-2", "d-none"))
        setAttribute("alt", "")
        style["width"] = "24px"
        style["height"] = "24px"
        style["object-fit"] = "contain"
        style["flex-shrink"] = "0"
    }

    val brandElement = WebDomElement("span").apply {
        classList.addAll(listOf("navbar-brand", "mb-0", "h6", "me-3", "d-none"))
        style["flex-shrink"] = "0"
    }

    val navItemsSlot = WebDomElement("div").apply {
        classList.addAll(listOf("navbar-nav", "d-flex", "flex-row", "align-items-center", "gap-2", "flex-grow-1", "w-100"))
    }

    init {
        nativeWidget.setAttribute("role", "navigation")
        containerFluid.appendChild(brandIconElement)
        containerFluid.appendChild(brandElement)
        containerFluid.appendChild(navItemsSlot)
        nativeWidget.appendChild(containerFluid)
    }

    private fun syncBrandDomVisibility() {
        val visible = isBrandVisible()
        val resolvedIcon = brandIconSpec?.takeIf { it.isNotBlank() }?.trim()?.let { raw ->
            if (raw.startsWith("res://", ignoreCase = true)) {
                WebBrowserDomBridge.getPreloadedResourceDataUrl(raw) ?: raw
            } else {
                raw
            }
        }
        if (visible && !resolvedIcon.isNullOrBlank()) {
            brandIconElement.setAttribute("src", resolvedIcon)
            brandIconElement.classList.remove("d-none")
        } else {
            brandIconElement.removeAttribute("src")
            brandIconElement.classList.add("d-none")
        }
        brandElement.textContent = brandTitleText
        if (visible && brandTitleText.isNotEmpty()) {
            brandElement.classList.remove("d-none")
        } else {
            brandElement.classList.add("d-none")
        }
    }

    override fun setBrandTitle(title: String) {
        brandTitleText = title
        nativeWidget.setAttribute("data-brand-title", title)
        syncBrandDomVisibility()
    }

    override fun getBrandTitle(): String = brandTitleText

    override fun setBrandIcon(icon: String?) {
        brandIconSpec = icon
        if (icon != null) {
            nativeWidget.setAttribute("data-brand-icon", icon)
        } else {
            nativeWidget.removeAttribute("data-brand-icon")
        }
        syncBrandDomVisibility()
    }

    override fun getBrandIcon(): String? = brandIconSpec

    override fun setShowBrand(show: Boolean?) {
        showBrandOverride = show
        syncBrandDomVisibility()
    }

    override fun getShowBrand(): Boolean? = showBrandOverride

    override fun isBrandVisible(): Boolean = showBrandOverride ?: true

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
