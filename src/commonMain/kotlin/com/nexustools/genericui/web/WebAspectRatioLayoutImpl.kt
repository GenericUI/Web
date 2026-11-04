package com.nexustools.genericui.web

import com.nexustools.genericui.component.layout.AspectRatioLayout
import com.nexustools.genericui.component.layout.DefaultAspectRatioLayoutImpl

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
    }

    override fun syncLayout(
        viewportWidthDp: Float,
        viewportHeightDp: Float,
        smallestScreenWidthDp: Int
    ) {
        super.syncLayout(viewportWidthDp, viewportHeightDp, smallestScreenWidthDp)
        layout.syncHostAspectRatioClassesAndStyles()
    }
}
