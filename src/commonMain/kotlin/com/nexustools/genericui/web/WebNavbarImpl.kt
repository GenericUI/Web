package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.navigation.NavbarNativeImpl

/**
 * Web implementation of [NavbarNativeImpl] rendered as `<nav class="navbar">`.
 */
class WebNavbarImpl : WebBaseContainerNativeImpl("nav", "navbar"), NavbarNativeImpl<WebDomElement> {
    private var brandTitleText: String = ""

    override fun setBrandTitle(title: String) {
        brandTitleText = title
        nativeWidget.setAttribute("data-brand-title", title)
    }

    override fun getBrandTitle(): String = brandTitleText
}
