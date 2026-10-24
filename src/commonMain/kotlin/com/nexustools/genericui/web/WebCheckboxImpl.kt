package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.form.CheckboxNativeImpl

/**
 * Web implementation of [CheckboxNativeImpl] supporting standard checkbox, `.form-switch`, and radio styles.
 */
class WebCheckboxImpl(
    val switchStyle: Boolean = false,
    val radioStyle: Boolean = false
) : WebBaseNativeImpl("div", "form-check"), CheckboxNativeImpl<WebDomElement> {
    private var labelText: String = ""
    private var checkedState: Boolean = false
    private var onCheckedChangedListener: ((Boolean) -> Unit)? = null

    init {
        if (switchStyle) {
            nativeWidget.classList.add("form-switch")
        }
    }

    override fun setLabel(label: String) {
        labelText = label
        nativeWidget.textContent = label
    }

    override fun getLabel(): String = labelText

    override fun setChecked(checked: Boolean) {
        checkedState = checked
        nativeWidget.checked = checked
        onCheckedChangedListener?.invoke(checked)
    }

    override fun isChecked(): Boolean = checkedState

    override fun setOnCheckedChanged(listener: ((Boolean) -> Unit)?) {
        onCheckedChangedListener = listener
    }
}
