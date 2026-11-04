package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.navigation.BottomNavigationBarNativeImpl

/**
 * Web implementation of [BottomNavigationBarNativeImpl] rendered as a Bootstrap 5 bottom navigation bar
 * (`<genericui:bottomnavigationbar class="navbar navbar-expand navbar-bottom border-top bg-body-tertiary">`)
 * containing a `.navbar-nav.nav-pills.nav-justified` list of `.nav-item` / `.nav-link` buttons.
 */
class WebBottomNavigationBarImpl :
    WebBaseNativeImpl(
        "genericui:bottomnavigationbar",
        "navbar navbar-expand navbar-bottom border-top bg-body-tertiary px-2 py-1"
    ),
    BottomNavigationBarNativeImpl<WebDomElement> {

    private var itemsList: List<Triple<String, String, String?>> = emptyList()
    private var selectedId: String? = null
    private var onItemSelectedListener: ((String) -> Unit)? = null

    private val navList = WebDomElement("ul").apply {
        classList.addAll(
            listOf(
                "navbar-nav",
                "nav-pills",
                "nav-justified",
                "w-100",
                "d-flex",
                "flex-row",
                "align-items-center",
                "justify-content-around",
                "mb-0",
                "gap-1"
            )
        )
        setAttribute("role", "tablist")
    }

    private val itemButtons = linkedMapOf<String, WebDomElement>()

    init {
        nativeWidget.setAttribute("role", "navigation")
        nativeWidget.appendChild(navList)
    }

    override fun setItems(items: List<Triple<String, String, String?>>) {
        itemsList = items
        if (selectedId == null && items.isNotEmpty()) {
            selectedId = items.first().first
        }
        rebuildNavButtons()
    }

    override fun setSelectedItemId(itemId: String) {
        val changed = (selectedId != itemId)
        selectedId = itemId
        syncActiveButtonStates()
        if (changed) {
            onItemSelectedListener?.invoke(itemId)
        }
    }

    override fun getSelectedItemId(): String? = selectedId

    override fun setOnItemSelected(listener: ((String) -> Unit)?) {
        onItemSelectedListener = listener
    }

    private fun rebuildNavButtons() {
        navList.clearChildren()
        itemButtons.clear()
        for ((id, label, icon) in itemsList) {
            val li = WebDomElement("li").apply {
                classList.addAll(listOf("nav-item", "flex-fill"))
                setAttribute("role", "presentation")
            }
            val btn = WebDomElement("button").apply {
                setAttribute("type", "button")
                setAttribute("role", "tab")
                setAttribute("data-nav-item-id", id)
                classList.addAll(
                    listOf(
                        "nav-link",
                        "w-100",
                        "d-flex",
                        "flex-column",
                        "align-items-center",
                        "justify-content-center",
                        "py-1",
                        "px-2",
                        "small"
                    )
                )
                if (!icon.isNullOrBlank()) {
                    setAttribute("data-nav-icon", icon)
                }
                textContent = label
                addEventListener("click") {
                    setSelectedItemId(id)
                }
            }
            itemButtons[id] = btn
            li.appendChild(btn)
            navList.appendChild(li)
        }
        syncActiveButtonStates()
    }

    private fun syncActiveButtonStates() {
        for ((id, btn) in itemButtons) {
            val isActive = (id == selectedId)
            if (isActive) {
                btn.classList.add("active")
                btn.setAttribute("aria-selected", "true")
            } else {
                btn.classList.remove("active")
                btn.setAttribute("aria-selected", "false")
            }
        }
    }
}
