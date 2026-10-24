package com.nexustools.genericui.web

import com.nexustools.genericui.component.dock.DockHeaderAction
import com.nexustools.genericui.nativeimpl.dock.DockPanelNativeImpl

/**
 * Web implementation of [DockPanelNativeImpl].
 */
class WebDockPanelImpl : WebBaseContainerNativeImpl("section", "dock-panel"), DockPanelNativeImpl<WebDomElement> {
    private var titleText: String = ""
    private var iconName: String? = null
    private var collapsedState: Boolean = false
    private var floatingState: Boolean = false
    private var headerActions: List<DockHeaderAction> = emptyList()

    override fun setTitle(title: String) {
        titleText = title
        nativeWidget.setAttribute("data-panel-title", title)
    }

    override fun getTitle(): String = titleText

    override fun setIcon(icon: String?) {
        iconName = icon
    }

    override fun setCollapsed(collapsed: Boolean) {
        collapsedState = collapsed
    }

    override fun isCollapsed(): Boolean = collapsedState

    override fun setFloating(floating: Boolean) {
        floatingState = floating
    }

    override fun isFloating(): Boolean = floatingState

    override fun setHeaderActions(actions: List<DockHeaderAction>) {
        headerActions = actions
    }
}
