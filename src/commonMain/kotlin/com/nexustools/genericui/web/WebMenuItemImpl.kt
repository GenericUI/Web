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
    private var inClickToggleGuard: Boolean = false
    private var checkedBeforeClick: Boolean = false
    private var clickActionCallback: (() -> Unit)? = null

    var onActionListener: (() -> Unit)? = null

    init {
        menuItemRegistry[nativeWidget] = this
        val role = when {
            radio -> "menuitemradio"
            checkable -> "menuitemcheckbox"
            else -> "menuitem"
        }
        nativeWidget.setAttribute("role", role)
        nativeWidget.setAttribute("aria-disabled", "false")
        if (checkable || radio) {
            nativeWidget.setAttribute("aria-checked", "false")
        }
        nativeWidget.style["cursor"] = "pointer"
        nativeWidget.style["user-select"] = "none"

        super.setOnClick {
            handleItemActivated()
        }
    }

    private fun handleItemActivated() {
        if (!isEnabled()) return
        val role = nativeWidget.getAttribute("role")
        checkedBeforeClick = checkedState
        if (role == "menuitemcheckbox") {
            applyCheckedState(!checkedBeforeClick)
            inClickToggleGuard = true
        } else if (role == "menuitemradio") {
            val parentDom = nativeWidget.parent
            if (parentDom != null) {
                for (sibling in parentDom.children) {
                    if (sibling !== nativeWidget && sibling.getAttribute("role") == "menuitemradio") {
                        menuItemRegistry[sibling]?.applyCheckedState(false) ?: run {
                            sibling.checked = false
                            sibling.setAttribute("aria-checked", "false")
                            sibling.classList.remove("active")
                        }
                    }
                }
            }
            applyCheckedState(true)
        }
        try {
            onActionListener?.invoke()
            clickActionCallback?.invoke()
        } finally {
            inClickToggleGuard = false
        }
        closeEnclosingMenus()
    }

    private fun closeEnclosingMenus() {
        val parentDom = nativeWidget.parent ?: return
        WebMenuImpl.findMenuForDropdownContainer(parentDom)?.closeMenuHierarchy()
        WebMenuImpl.findEnclosingContextMenu(parentDom)?.hide()
    }

    override fun setOnClick(listener: (() -> Unit)?) {
        clickActionCallback = listener
    }

    fun setOnAction(listener: (() -> Unit)?) {
        onActionListener = listener
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        nativeWidget.setAttribute("aria-disabled", (!enabled).toString())
        nativeWidget.style["cursor"] = if (enabled) "pointer" else "default"
        if (enabled) {
            nativeWidget.classList.remove("disabled")
        } else {
            nativeWidget.classList.add("disabled")
        }
    }

    override fun setLabel(label: String) {
        labelText = label
        nativeWidget.textContent = label
    }

    override fun getLabel(): String = labelText

    override fun setIcon(icon: String?) {
        iconName = icon
        if (icon != null) nativeWidget.setAttribute("data-icon", icon) else nativeWidget.removeAttribute("data-icon")
    }

    override fun getIcon(): String? = iconName

    override fun setShortcut(shortcut: KeyShortcut?) {
        shortcutValue = shortcut
        if (shortcut != null) {
            nativeWidget.setAttribute("data-shortcut", shortcut.toString())
        } else {
            nativeWidget.removeAttribute("data-shortcut")
        }
    }

    override fun getShortcut(): KeyShortcut? = shortcutValue

    private fun applyCheckedState(checked: Boolean) {
        checkedState = checked
        nativeWidget.checked = checked
        nativeWidget.setAttribute("aria-checked", checked.toString())
        if (checked) {
            nativeWidget.classList.add("active")
        } else {
            nativeWidget.classList.remove("active")
        }
    }

    override fun setChecked(checked: Boolean) {
        if (inClickToggleGuard && checked == checkedBeforeClick) {
            inClickToggleGuard = false
            return
        }
        inClickToggleGuard = false
        applyCheckedState(checked)
    }

    override fun isChecked(): Boolean = checkedState

    override fun dispose() {
        menuItemRegistry.remove(nativeWidget)
        super.dispose()
    }

    companion object {
        private val menuItemRegistry = mutableMapOf<WebDomElement, WebMenuItemImpl>()
    }
}
