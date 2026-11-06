package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.action.DropdownNativeImpl

/**
 * Web implementation of [DropdownNativeImpl] rendered as a Bootstrap 5 `.dropdown`
 * with a `.btn.dropdown-toggle` button and a `.dropdown-menu` list of `.dropdown-item` buttons.
 */
class WebDropdownImpl : WebBaseContainerNativeImpl("genericui:dropdown", "dropdown"), DropdownNativeImpl<WebDomElement> {
    private var onItemSelectedListener: ((String) -> Unit)? = null
    private var menuOpen: Boolean = false

    val toggleButton = WebDomElement("button").apply {
        setAttribute("type", "button")
        classList.addAll(listOf("btn", "btn-secondary", "dropdown-toggle"))
        setAttribute("data-bs-toggle", "dropdown")
        setAttribute("aria-expanded", "false")
    }

    val menuList = WebDomElement("ul").apply {
        classList.add("dropdown-menu")
    }

    init {
        toggleButton.addEventListener("click") {
            setMenuOpen(!menuOpen)
        }
        nativeWidget.appendChild(toggleButton)
        nativeWidget.appendChild(menuList)
    }

    private fun setMenuOpen(open: Boolean) {
        menuOpen = open
        toggleButton.setAttribute("aria-expanded", open.toString())
        if (open) {
            toggleButton.classList.add("show")
            menuList.classList.add("show")
            com.nexustools.genericui.event.PopupMouseCaptureManager.registerNativePopup(
                nativePopup = this,
                hitTester = { _, _, target -> nativeWidget.containsDomElement(target) },
                onDismiss = { setMenuOpen(false) }
            )
        } else {
            com.nexustools.genericui.event.PopupMouseCaptureManager.unregisterNativePopup(this)
            toggleButton.classList.remove("show")
            menuList.classList.remove("show")
        }
    }

    override fun dispose() {
        com.nexustools.genericui.event.PopupMouseCaptureManager.unregisterNativePopup(this)
        super.dispose()
    }

    override fun setLabel(label: String) {
        nativeWidget.setAttribute("data-dropdown-label", label)
        toggleButton.textContent = label
    }

    override fun setItems(items: List<Pair<String, String>>) {
        nativeWidget.setAttribute("data-item-count", items.size.toString())
        menuList.clearChildren()
        for ((itemId, itemLabel) in items) {
            val li = WebDomElement("li")
            val itemBtn = WebDomElement("button").apply {
                setAttribute("type", "button")
                classList.add("dropdown-item")
                setAttribute("data-dropdown-item-id", itemId)
                textContent = itemLabel
                addEventListener("click") {
                    setMenuOpen(false)
                    onItemSelectedListener?.invoke(itemId)
                }
            }
            li.appendChild(itemBtn)
            menuList.appendChild(li)
        }
    }

    override fun setOnItemSelected(listener: ((String) -> Unit)?) {
        onItemSelectedListener = listener
    }
}
