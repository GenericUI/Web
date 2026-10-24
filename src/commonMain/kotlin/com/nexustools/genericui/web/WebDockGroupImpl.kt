package com.nexustools.genericui.web

import com.nexustools.genericui.enums.dock.DockGroupMode
import com.nexustools.genericui.nativeimpl.dock.DockGroupNativeImpl

/**
 * Web implementation of [DockGroupNativeImpl].
 */
class WebDockGroupImpl : WebBaseContainerNativeImpl("div", "dock-group"), DockGroupNativeImpl<WebDomElement> {
    private var groupMode: DockGroupMode = DockGroupMode.TABBED
    private var activeIndex: Int = 0

    override fun setGroupMode(mode: DockGroupMode) {
        groupMode = mode
        nativeWidget.setAttribute("data-group-mode", mode.name.lowercase())
    }

    override fun getGroupMode(): DockGroupMode = groupMode

    override fun setActivePanelIndex(index: Int) {
        activeIndex = index
    }

    override fun getActivePanelIndex(): Int = activeIndex
}
