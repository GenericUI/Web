package com.nexustools.genericui.web

import com.nexustools.genericui.component.layout.AspectRatioLayout
import com.nexustools.genericui.component.layout.DefaultAspectRatioLayoutImpl
import kotlin.math.roundToInt

/**
 * Web (`js` + `wasmJs`) implementation of [DefaultAspectRatioLayoutImpl] that maps [AspectRatioLayout.host]
 * directly to Bootstrap 5's `.ratio` and `.ratio-1x1` / `.ratio-4x3` / `.ratio-16x9` / `.ratio-21x9` classes
 * (plus `--bs-aspect-ratio` for custom aspect ratios).
 */
class WebAspectRatioLayoutImpl(
    layout: AspectRatioLayout
) : DefaultAspectRatioLayoutImpl(layout) {

    override fun syncAspectRatio() {
        super.syncAspectRatio()
        syncBootstrapAspectRatioVar()
    }

    override fun syncLayout(
        viewportWidthDp: Float,
        viewportHeightDp: Float,
        smallestScreenWidthDp: Int
    ) {
        super.syncLayout(viewportWidthDp, viewportHeightDp, smallestScreenWidthDp)
        layout.syncHostAspectRatioClassesAndStyles()
        syncBootstrapAspectRatioVar()
    }

    private fun syncBootstrapAspectRatioVar() {
        val element = layout.host.nativeImpl?.nativeWidget as? WebDomElement ?: return
        val numericRatio = layout.effectiveAspectRatio
        if (numericRatio > 0f) {
            val scaled = ((1f / numericRatio) * 10000f).roundToInt()
            val percentStr = if (scaled % 100 == 0) {
                "${scaled / 100}%"
            } else {
                "${scaled / 100f}%"
            }
            element.setStyle("--bs-aspect-ratio", percentStr, false)
        }
    }
}
