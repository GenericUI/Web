package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.form.RangeSliderNativeImpl

/**
 * Web implementation of [RangeSliderNativeImpl] rendered as `<input type="range" class="form-range">`.
 */
class WebRangeSliderImpl : WebBaseNativeImpl("input", "form-range"), RangeSliderNativeImpl<WebDomElement> {
    private var minVal: Double = 0.0
    private var maxVal: Double = 100.0
    private var stepVal: Double = 1.0
    private var currentVal: Double = 0.0
    private var onValueChangedListener: ((Double) -> Unit)? = null

    init {
        nativeWidget.setAttribute("type", "range")
        nativeWidget.addEventListener("input") {
            val v = nativeWidget.value.toDoubleOrNull() ?: return@addEventListener
            currentVal = v.coerceIn(minVal, maxVal)
            onValueChangedListener?.invoke(currentVal)
        }
    }

    override fun setRange(min: Double, max: Double, step: Double) {
        minVal = min
        maxVal = max
        stepVal = step
        nativeWidget.setAttribute("min", min.toString())
        nativeWidget.setAttribute("max", max.toString())
        nativeWidget.setAttribute("step", step.toString())
    }

    override fun setValue(value: Double) {
        currentVal = value.coerceIn(minVal, maxVal)
        nativeWidget.value = currentVal.toString()
        onValueChangedListener?.invoke(currentVal)
    }

    override fun getValue(): Double = currentVal

    override fun setOnValueChanged(listener: ((Double) -> Unit)?) {
        onValueChangedListener = listener
    }
}
