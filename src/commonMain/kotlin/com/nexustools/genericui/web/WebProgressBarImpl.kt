package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.feedback.ProgressBarNativeImpl

/**
 * Web implementation of [ProgressBarNativeImpl] rendered as a Bootstrap 5 `<genericui:progressbar class="progress">`
 * containing an inner `<div class="progress-bar">`.
 */
class WebProgressBarImpl : WebBaseNativeImpl("genericui:progressbar", "progress"), ProgressBarNativeImpl<WebDomElement> {
    private var progressValue: Float = 0f
    private var indeterminateFlag: Boolean = false

    val barElement = WebDomElement("div").apply {
        classList.add("progress-bar")
        style["width"] = "0%"
    }

    init {
        nativeWidget.setAttribute("role", "progressbar")
        nativeWidget.setAttribute("aria-valuemin", "0")
        nativeWidget.setAttribute("aria-valuemax", "100")
        nativeWidget.setAttribute("aria-valuenow", "0")
        nativeWidget.appendChild(barElement)
    }

    override fun setProgress(progress: Float) {
        progressValue = progress.coerceIn(0f, 1f)
        val pct = (progressValue * 100f).toInt()
        nativeWidget.setAttribute("aria-valuenow", pct.toString())
        barElement.style["width"] = if (indeterminateFlag && pct == 0) "100%" else "$pct%"
    }

    override fun getProgress(): Float = progressValue

    override fun setIndeterminate(indeterminate: Boolean) {
        indeterminateFlag = indeterminate
        if (indeterminate) {
            nativeWidget.classList.add("progress-bar-animated")
            nativeWidget.classList.add("progress-bar-striped")
            barElement.classList.add("progress-bar-animated")
            barElement.classList.add("progress-bar-striped")
            if (progressValue == 0f) {
                barElement.style["width"] = "100%"
            }
        } else {
            nativeWidget.classList.remove("progress-bar-animated")
            nativeWidget.classList.remove("progress-bar-striped")
            barElement.classList.remove("progress-bar-animated")
            barElement.classList.remove("progress-bar-striped")
            barElement.style["width"] = "${(progressValue * 100f).toInt()}%"
        }
    }

    override fun isIndeterminate(): Boolean = indeterminateFlag

    override fun setLabel(label: String?) {
        barElement.textContent = label ?: ""
    }
}
