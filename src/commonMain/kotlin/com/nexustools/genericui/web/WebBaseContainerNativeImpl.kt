package com.nexustools.genericui.web

import com.nexustools.genericui.core.AbstractContainerNativeImpl
import com.nexustools.genericui.core.NativeImpl

/**
 * Base container [AbstractContainerNativeImpl] for the `:web` backend.
 */
open class WebBaseContainerNativeImpl(
    tagName: String = "div",
    componentClassPrefix: String = "container"
) : WebBaseNativeImpl(tagName, componentClassPrefix), AbstractContainerNativeImpl<WebDomElement> {

    override fun addChildNative(childImpl: NativeImpl<*>, index: Int) {
        val childDom = childImpl.nativeWidget as? WebDomElement ?: return
        nativeWidget.insertChildAt(index, childDom)
    }

    override fun removeChildNative(childImpl: NativeImpl<*>) {
        val childDom = childImpl.nativeWidget as? WebDomElement ?: return
        nativeWidget.removeChild(childDom)
    }

    override fun clearChildrenNative() {
        nativeWidget.clearChildren()
    }

    override fun enableHardwareLayerCache(enabled: Boolean) {
        if (enabled) {
            nativeWidget.style["will-change"] = "transform, opacity"
        } else {
            nativeWidget.style.remove("will-change")
        }
    }
}
