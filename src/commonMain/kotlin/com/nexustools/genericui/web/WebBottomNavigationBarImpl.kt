package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.navigation.BottomNavigationBarNativeImpl

/**
 * Web implementation of [BottomNavigationBarNativeImpl].
 */
class WebBottomNavigationBarImpl : WebBaseNativeImpl("nav", "navbar-bottom"), BottomNavigationBarNativeImpl<WebDomElement> {
    private var itemsList: List<Triple<String, String, String?>> = emptyList()
    private var selectedId: String? = null
    private var onItemSelectedListener: ((String) -> Unit)? = null

    override fun setItems(items: List<Triple<String, String, String?>>) {
        itemsList = items
        if (selectedId == null && items.isNotEmpty()) {
            selectedId = items.first().first
        }
    }

    override fun setSelectedItemId(itemId: String) {
        selectedId = itemId
        onItemSelectedListener?.invoke(itemId)
    }

    override fun getSelectedItemId(): String? = selectedId

    override fun setOnItemSelected(listener: ((String) -> Unit)?) {
        onItemSelectedListener = listener
    }
}
