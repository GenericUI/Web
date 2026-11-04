package com.nexustools.genericui.web

import com.nexustools.genericui.component.layout.ColumnLayout
import com.nexustools.genericui.component.layout.ResponsiveColumnLayoutImpl

/**
 * Web (`js` + `wasmJs`) implementation of [ResponsiveColumnLayoutImpl] that maps [ColumnLayout.host]
 * directly to Bootstrap 5's `.row` (plus `.g-*`, `.gx-*`, `.gy-*`, `.row-cols-*`, `.row-cols-{sm,md,lg,xl,xxl}-*`)
 * and each [com.nexustools.genericui.component.layout.Column] in [ColumnLayout.columns] directly to Bootstrap 5's
 * `.col`, `.col-auto`, `.col-1`..`.col-12`, `.col-{sm,md,lg,xl,xxl}-{1..12|auto}`, `.offset-*`, and `.order-*` classes.
 */
class WebColumnLayoutImpl(
    layout: ColumnLayout
) : ResponsiveColumnLayoutImpl(layout) {

    override fun syncLayout(viewportWidthDp: Float, viewportHeightDp: Float) {
        super.syncLayout(viewportWidthDp, viewportHeightDp)
        layout.syncHostGridClasses()
        for (col in layout.columns) {
            col.syncColumnClasses()
        }
    }
}
