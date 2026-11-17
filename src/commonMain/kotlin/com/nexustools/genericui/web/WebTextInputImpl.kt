package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.form.TextInputNativeImpl

/**
 * Web implementation of [TextInputNativeImpl] rendered as `<input class="form-control">` or `<textarea class="form-control">`.
 */
class WebTextInputImpl(
    multiline: Boolean = false
) : WebBaseNativeImpl(if (multiline) "textarea" else "input", "form-control"), TextInputNativeImpl<WebDomElement> {
    private var placeholderText: String = ""
    private var labelText: String? = null
    private var floatingWrapper: WebDomElement? = null
    private var floatingLabelElement: WebDomElement? = null
    private var onValueChangedListener: ((String) -> Unit)? = null
    private var onSubmitListener: ((String) -> Unit)? = null

    init {
        nativeWidget.addEventListener("input") {
            onValueChangedListener?.invoke(nativeWidget.value)
        }
        nativeWidget.addEventListener("change") {
            onSubmitListener?.invoke(nativeWidget.value)
        }
        nativeWidget.addEventListener("focus") {
            com.nexustools.genericui.event.InputFocusManager.isNativeTextInputFocused = true
        }
        nativeWidget.addEventListener("blur") {
            com.nexustools.genericui.event.InputFocusManager.isNativeTextInputFocused = false
        }
    }

    override fun setAttribute(name: String, value: String?) {
        super<WebBaseNativeImpl>.setAttribute(name, value)
        if (name == "id" && !value.isNullOrBlank()) {
            nativeWidget.id = value
            floatingLabelElement?.setAttribute("for", value)
        }
    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)
        floatingWrapper?.let { wrapper ->
            if (visible) {
                wrapper.setStyle("display", null, false)
            } else {
                wrapper.setStyle("display", "none", true)
            }
        }
    }

    override fun dispose() {
        if (isFocusedState()) {
            com.nexustools.genericui.event.InputFocusManager.isNativeTextInputFocused = false
        }
        floatingWrapper?.remove()
        nativeWidget.domMountWrapper = null
        floatingWrapper = null
        floatingLabelElement = null
        super.dispose()
    }

    override fun setValue(value: String) {
        nativeWidget.value = value
    }

    override fun getValue(): String = nativeWidget.value

    override fun setPlaceholder(placeholder: String) {
        placeholderText = placeholder
        val effectivePlaceholder = if (placeholder.isEmpty() && labelText != null) " " else placeholder
        nativeWidget.setAttribute("placeholder", effectivePlaceholder)
    }

    override fun getPlaceholder(): String = placeholderText

    override fun setLabel(label: String?) {
        val normalized = label?.takeIf { it.isNotBlank() }
        if (labelText == normalized && (normalized == null) == (floatingWrapper == null)) return
        labelText = normalized
        if (normalized != null) {
            if (nativeWidget.id.isBlank()) {
                nativeWidget.id = "gui-floating-input-${nextFloatingId++}"
            }
            if (placeholderText.isEmpty()) {
                nativeWidget.setAttribute("placeholder", " ")
            }
            val existingWrapper = floatingWrapper
            if (existingWrapper == null) {
                val existingParent = nativeWidget.parent
                val existingIndex = existingParent?.children?.indexOf(nativeWidget) ?: -1
                val wrapper = WebDomElement("div").apply {
                    addClass("form-floating")
                    ownerNativeImpl = this@WebTextInputImpl
                }
                if (!isVisible()) {
                    wrapper.setStyle("display", "none", true)
                }
                val lblEl = WebDomElement("label").apply {
                    setAttribute("for", nativeWidget.id)
                    textContent = normalized
                }
                wrapper.appendChild(nativeWidget)
                wrapper.appendChild(lblEl)
                nativeWidget.domMountWrapper = wrapper
                floatingWrapper = wrapper
                floatingLabelElement = lblEl
                if (existingParent != null) {
                    existingParent.insertChildAt(existingIndex, wrapper)
                }
            } else {
                floatingLabelElement?.setAttribute("for", nativeWidget.id)
                floatingLabelElement?.textContent = normalized
            }
        } else {
            val wrapper = floatingWrapper
            if (wrapper != null) {
                val existingParent = wrapper.parent
                val existingIndex = existingParent?.children?.indexOf(wrapper) ?: -1
                wrapper.removeChild(nativeWidget)
                existingParent?.removeChild(wrapper)
                nativeWidget.domMountWrapper = null
                floatingWrapper = null
                floatingLabelElement = null
                if (placeholderText.isEmpty()) {
                    nativeWidget.removeAttribute("placeholder")
                } else {
                    nativeWidget.setAttribute("placeholder", placeholderText)
                }
                if (existingParent != null) {
                    existingParent.insertChildAt(existingIndex, nativeWidget)
                }
            }
        }
    }

    override fun getLabel(): String? = labelText

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

    private companion object {
        private var nextFloatingId: Int = 1
    }
}
