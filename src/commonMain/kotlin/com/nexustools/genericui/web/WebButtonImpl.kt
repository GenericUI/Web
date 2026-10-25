package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.action.ButtonNativeImpl

/**
 * Web implementation of [ButtonNativeImpl] rendered as `<button type="button" class="btn ...">` per the Bootstrap `.btn` standard.
 */
class WebButtonImpl(
    classPrefix: String = "btn"
) : WebBaseNativeImpl("button", classPrefix), ButtonNativeImpl<WebDomElement> {
    private var iconName: String? = null

    init {
        nativeWidget.setAttribute("type", "button")
    }

    override fun setText(text: String) {
        nativeWidget.textContent = text
    }

    override fun getText(): String = nativeWidget.textContent

    override fun setIcon(icon: String?) {
        iconName = icon
        if (icon != null) nativeWidget.setAttribute("data-icon", icon) else nativeWidget.removeAttribute("data-icon")
    }

    override fun getIcon(): String? = iconName
}
