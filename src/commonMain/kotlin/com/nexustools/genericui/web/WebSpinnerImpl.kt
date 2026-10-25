package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.feedback.SpinnerNativeImpl

/**
 * Web implementation of [SpinnerNativeImpl] rendered as `<genericui:spinner class="spinner-border">` (or `.spinner-grow`).
 */
class WebSpinnerImpl : WebBaseNativeImpl("genericui:spinner", "spinner-border"), SpinnerNativeImpl<WebDomElement> {
    override fun setGrowStyle(grow: Boolean) {
        if (grow) {
            nativeWidget.classList.remove("spinner-border")
            nativeWidget.classList.add("spinner-grow")
        } else {
            nativeWidget.classList.remove("spinner-grow")
            nativeWidget.classList.add("spinner-border")
        }
    }

    override fun setLabel(label: String?) {
        if (label != null) {
            nativeWidget.setAttribute("aria-label", label)
        } else {
            nativeWidget.removeAttribute("aria-label")
        }
    }
}
