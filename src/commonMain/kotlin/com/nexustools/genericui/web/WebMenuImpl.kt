package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.shell.MenuNativeImpl

/**
 * Web implementation of [MenuNativeImpl].
 */
class WebMenuImpl : WebBaseContainerNativeImpl("div", "dropdown"), MenuNativeImpl<WebDomElement> {
    private var labelText: String = ""

    override fun setLabel(label: String) {
        labelText = label
        nativeWidget.setAttribute("data-menu-label", label)
    }

    override fun getLabel(): String = labelText
}
