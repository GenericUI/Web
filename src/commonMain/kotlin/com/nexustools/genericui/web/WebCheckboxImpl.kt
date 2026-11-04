package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.form.CheckboxNativeImpl

/**
 * Web implementation of [CheckboxNativeImpl] supporting standard checkbox, `.form-switch`, and radio styles.
 *
 * Renders the canonical Bootstrap `<div class="form-check [form-switch]">` containing:
 * - `<input class="form-check-input" type="checkbox|radio" role="switch">`
 * - `<label class="form-check-label">`
 */
class WebCheckboxImpl(
    val switchStyle: Boolean = false,
    val radioStyle: Boolean = false
) : WebBaseNativeImpl("div", "form-check"), CheckboxNativeImpl<WebDomElement> {
    private var labelText: String = ""
    private var checkedState: Boolean = false
    private var onCheckedChangedListener: ((Boolean) -> Unit)? = null

    val inputElement: WebDomElement = WebDomElement("input").apply {
        classList.add("form-check-input")
        setAttribute("type", if (radioStyle) "radio" else "checkbox")
        if (switchStyle) {
            setAttribute("role", "switch")
        }
        style["cursor"] = "pointer"
    }

    val labelElement: WebDomElement = WebDomElement("label").apply {
        classList.add("form-check-label")
        style["cursor"] = "pointer"
    }

    init {
        if (switchStyle) {
            nativeWidget.classList.add("form-switch")
        }
        nativeWidget.style["display"] = "flex"
        nativeWidget.style["align-items"] = "center"
        nativeWidget.style["gap"] = "0.5rem"
        nativeWidget.appendChild(inputElement)
        nativeWidget.appendChild(labelElement)

        nativeWidget.addEventListener("click") {
            setChecked(!checkedState)
        }
    }

    override fun setLabel(label: String) {
        labelText = label
        labelElement.textContent = label
        labelElement.style["display"] = if (label.isEmpty()) "none" else "inline-block"
    }

    override fun getLabel(): String = labelText

    override fun setChecked(checked: Boolean) {
        checkedState = checked
        nativeWidget.checked = checked
        inputElement.checked = checked
        if (checked) {
            inputElement.setAttribute("checked", "checked")
            nativeWidget.setAttribute("aria-checked", "true")
        } else {
            inputElement.removeAttribute("checked")
            nativeWidget.setAttribute("aria-checked", "false")
        }
        onCheckedChangedListener?.invoke(checked)
    }

    override fun isChecked(): Boolean = checkedState

    override fun setOnCheckedChanged(listener: ((Boolean) -> Unit)?) {
        onCheckedChangedListener = listener
    }
}
