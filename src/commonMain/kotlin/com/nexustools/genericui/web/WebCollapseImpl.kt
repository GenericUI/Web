package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.surface.CollapseNativeImpl

/**
 * Web implementation of [CollapseNativeImpl] rendered as `<genericui:collapsiblepanel class="collapse">`.
 */
class WebCollapseImpl : WebBaseContainerNativeImpl("genericui:collapsiblepanel", "collapse"), CollapseNativeImpl<WebDomElement> {
    private var expandedState: Boolean = false
    private var onExpandedChangedListener: ((Boolean) -> Unit)? = null

    override fun setTitle(title: String) {
        nativeWidget.setAttribute("data-collapse-title", title)
    }

    override fun setExpanded(expanded: Boolean) {
        expandedState = expanded
        if (expanded) nativeWidget.classList.add("show") else nativeWidget.classList.remove("show")
        onExpandedChangedListener?.invoke(expanded)
    }

    override fun isExpanded(): Boolean = expandedState

    override fun setOnExpandedChanged(listener: ((Boolean) -> Unit)?) {
        onExpandedChangedListener = listener
    }
}
