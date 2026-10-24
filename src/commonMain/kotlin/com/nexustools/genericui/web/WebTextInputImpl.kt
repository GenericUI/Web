package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.form.TextInputNativeImpl

/**
 * Web implementation of [TextInputNativeImpl] rendered as `<input class="form-control">` or `<textarea class="form-control">`.
 */
class WebTextInputImpl(
    multiline: Boolean = false
) : WebBaseNativeImpl(if (multiline) "textarea" else "input", "form-control"), TextInputNativeImpl<WebDomElement> {
    private var placeholderText: String = ""
    private var onValueChangedListener: ((String) -> Unit)? = null
    private var onSubmitListener: ((String) -> Unit)? = null

    override fun setValue(value: String) {
        nativeWidget.value = value
    }

    override fun getValue(): String = nativeWidget.value

    override fun setPlaceholder(placeholder: String) {
        placeholderText = placeholder
        nativeWidget.setAttribute("placeholder", placeholder)
    }

    override fun getPlaceholder(): String = placeholderText

    override fun setReadOnly(readOnly: Boolean) {
        nativeWidget.readOnly = readOnly
    }

    override fun setPasswordMode(password: Boolean) {
        nativeWidget.setAttribute("type", if (password) "password" else "text")
    }

    override fun setMultiline(multiline: Boolean) {
        nativeWidget.setAttribute("data-multiline", multiline.toString())
    }

    override fun setOnValueChanged(listener: ((String) -> Unit)?) {
        onValueChangedListener = listener
    }

    override fun setOnSubmit(listener: ((String) -> Unit)?) {
        onSubmitListener = listener
    }
}
