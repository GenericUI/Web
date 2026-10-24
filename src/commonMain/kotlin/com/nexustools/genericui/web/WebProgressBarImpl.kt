package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.feedback.ProgressBarNativeImpl

/**
 * Web implementation of [ProgressBarNativeImpl] rendered as `<div class="progress">`.
 */
class WebProgressBarImpl : WebBaseNativeImpl("div", "progress"), ProgressBarNativeImpl<WebDomElement> {
    private var progressValue: Float = 0f
    private var indeterminateFlag: Boolean = false

    override fun setProgress(progress: Float) {
        progressValue = progress.coerceIn(0f, 1f)
        nativeWidget.setAttribute("aria-valuenow", (progressValue * 100f).toInt().toString())
    }

    override fun getProgress(): Float = progressValue

    override fun setIndeterminate(indeterminate: Boolean) {
        indeterminateFlag = indeterminate
        if (indeterminate) {
            nativeWidget.classList.add("progress-bar-animated")
            nativeWidget.classList.add("progress-bar-striped")
        } else {
            nativeWidget.classList.remove("progress-bar-animated")
            nativeWidget.classList.remove("progress-bar-striped")
        }
    }

    override fun isIndeterminate(): Boolean = indeterminateFlag

    override fun setLabel(label: String?) {
        nativeWidget.textContent = label ?: ""
    }
}
