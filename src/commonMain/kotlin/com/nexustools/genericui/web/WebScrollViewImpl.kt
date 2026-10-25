package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.layout.ScrollViewNativeImpl

/**
 * Web implementation of [ScrollViewNativeImpl] rendered as `<genericui:scrollview class="overflow-auto">`.
 */
class WebScrollViewImpl : WebBaseContainerNativeImpl("genericui:scrollview", "overflow-auto"), ScrollViewNativeImpl<WebDomElement> {
    override fun scrollTo(x: Int, y: Int) {
        nativeWidget.scrollLeft = x.toDouble()
        nativeWidget.scrollTop = y.toDouble()
    }

    override fun scrollToBottom() {
        nativeWidget.scrollTop = nativeWidget.scrollHeight.toDouble()
    }
}
