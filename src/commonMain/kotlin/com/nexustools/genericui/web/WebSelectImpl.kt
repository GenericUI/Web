package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.form.SelectNativeImpl

/**
 * Web implementation of [SelectNativeImpl] rendered as `<select class="form-select">`.
 */
class WebSelectImpl : WebBaseNativeImpl("select", "form-select"), SelectNativeImpl<WebDomElement> {
    private var optionsList: List<Pair<String, String>> = emptyList()
    private var selectedValue: String? = null
    private var onSelectionChangedListener: ((String) -> Unit)? = null

    override fun setOptions(options: List<Pair<String, String>>) {
        optionsList = options
        nativeWidget.clearChildren()
        for ((value, label) in options) {
            val opt = WebDomElement("option").apply {
                this.value = value
                this.textContent = label
            }
            nativeWidget.appendChild(opt)
        }
        if (selectedValue == null && options.isNotEmpty()) {
            selectedValue = options.first().first
        }
    }

    override fun setSelectedValue(value: String?) {
        selectedValue = value
        nativeWidget.value = value ?: ""
        if (value != null) {
            onSelectionChangedListener?.invoke(value)
        }
    }

    override fun getSelectedValue(): String? = selectedValue

    override fun setOnSelectionChanged(listener: ((String) -> Unit)?) {
        onSelectionChangedListener = listener
    }
}
