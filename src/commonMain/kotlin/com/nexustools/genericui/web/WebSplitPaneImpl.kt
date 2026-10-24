package com.nexustools.genericui.web

import com.nexustools.genericui.enums.ui.Orientation
import com.nexustools.genericui.nativeimpl.shell.SplitPaneNativeImpl

/**
 * Web implementation of [SplitPaneNativeImpl] using CSS flex/grid split sizing.
 */
class WebSplitPaneImpl : WebBaseContainerNativeImpl("div", "split-pane"), SplitPaneNativeImpl<WebDomElement> {
    private var orientation: Orientation = Orientation.HORIZONTAL
    private var dividerRatio: Float = 0.5f

    override fun setOrientation(orientation: Orientation) {
        this.orientation = orientation
        nativeWidget.style["display"] = "flex"
        nativeWidget.style["flex-direction"] = if (orientation == Orientation.HORIZONTAL) "row" else "column"
    }

    override fun getOrientation(): Orientation = orientation

    override fun setDividerPosition(ratio: Float) {
        dividerRatio = ratio.coerceIn(0.05f, 0.95f)
        nativeWidget.setAttribute("data-divider-ratio", dividerRatio.toString())
    }

    override fun getDividerPosition(): Float = dividerRatio
}
