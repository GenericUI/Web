package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.layout.ScrollViewNativeImpl

/**
 * Web implementation of [ScrollViewNativeImpl].
 */
class WebScrollViewImpl : WebBaseContainerNativeImpl("div", "overflow-auto"), ScrollViewNativeImpl<WebDomElement> {
    override fun scrollTo(x: Int, y: Int) {
        nativeWidget.scrollLeft = x.toDouble()
        nativeWidget.scrollTop = y.toDouble()
    }

    override fun scrollToBottom() {
        nativeWidget.scrollTop = nativeWidget.scrollHeight.toDouble()
    }
}
