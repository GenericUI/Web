package com.nexustools.genericui.web

import com.nexustools.genericui.event.KeyShortcut
import com.nexustools.genericui.nativeimpl.shell.MenuItemNativeImpl

/**
 * Web implementation of [MenuItemNativeImpl] supporting standard (`<genericui:menuitem>`),
 * checkable (`<genericui:checkmenuitem>`), and radio (`<genericui:radiomenuitem>`) menu items.
 */
class WebMenuItemImpl(
    val checkable: Boolean = false,
    val radio: Boolean = false
) : WebBaseNativeImpl(
    when {
        radio -> "genericui:radiomenuitem"
        checkable -> "genericui:checkmenuitem"
        else -> "genericui:menuitem"
    },
    "dropdown-item"
), MenuItemNativeImpl<WebDomElement> {
    private var labelText: String = ""
    private var iconName: String? = null
    private var shortcutValue: KeyShortcut? = null
    private var checkedState: Boolean = false

    override fun setLabel(label: String) {
        labelText = label
        nativeWidget.textContent = label
    }

    override fun getLabel(): String = labelText

    override fun setIcon(icon: String?) {
        iconName = icon
        if (icon != null) nativeWidget.setAttribute("data-icon", icon) else nativeWidget.removeAttribute("data-icon")
    }

    override fun setShortcut(shortcut: KeyShortcut?) {
        shortcutValue = shortcut
        if (shortcut != null) {
            nativeWidget.setAttribute("data-shortcut", shortcut.toString())
        } else {
            nativeWidget.removeAttribute("data-shortcut")
        }
    }

    override fun setChecked(checked: Boolean) {
        checkedState = checked
        nativeWidget.checked = checked
        nativeWidget.setAttribute("aria-checked", checked.toString())
    }

    override fun isChecked(): Boolean = checkedState
}
