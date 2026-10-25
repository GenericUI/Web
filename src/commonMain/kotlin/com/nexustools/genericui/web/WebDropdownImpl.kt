package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.action.DropdownNativeImpl

/**
 * Web implementation of [DropdownNativeImpl] rendered as `<genericui:dropdown class="dropdown">`.
 */
class WebDropdownImpl : WebBaseContainerNativeImpl("genericui:dropdown", "dropdown"), DropdownNativeImpl<WebDomElement> {
    private var onItemSelectedListener: ((String) -> Unit)? = null

    override fun setLabel(label: String) {
        nativeWidget.setAttribute("data-dropdown-label", label)
    }

    override fun setItems(items: List<Pair<String, String>>) {
        nativeWidget.setAttribute("data-item-count", items.size.toString())
    }

    override fun setOnItemSelected(listener: ((String) -> Unit)?) {
        onItemSelectedListener = listener
    }
}
