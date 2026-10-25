package com.nexustools.genericui.web

import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.nativeimpl.tabs.TabNativeImpl

/**
 * Web implementation of [TabNativeImpl], rendering `<genericui:tab class="nav-item" role="presentation">`
 * (or `<genericui:closeabletab>` when [initialClosable] is `true`) containing a `<button class="nav-link" role="tab">`
 * with title, optional badge, optional `.btn-close` close button, or an embedded custom [WebDomElement].
 */
class WebTabImpl(
    initialClosable: Boolean = false
) : WebBaseContainerNativeImpl(
    if (initialClosable) "genericui:closeabletab" else "genericui:tab",
    "nav-item"
), TabNativeImpl<WebDomElement> {

    val buttonElement: WebDomElement = WebDomElement("button").apply {
        classList.add("nav-link")
        setAttribute("type", "button")
        setAttribute("role", "tab")
    }
    private val titleSpan: WebDomElement = WebDomElement("span").apply {
        classList.add("tab-title")
    }
    private val badgeSpan: WebDomElement = WebDomElement("span").apply {
        classList.add("badge")
        classList.add("ms-2")
        style["display"] = "none"
    }
    private val closeBtn: WebDomElement = WebDomElement("button").apply {
        classList.add("btn-close")
        classList.add("ms-2")
        setAttribute("type", "button")
        setAttribute("aria-label", "Close")
        style["display"] = if (initialClosable) "inline-block" else "none"
    }

    var tabIdVal: String = ""
        private set
    var titleVal: String = ""
        private set
    var closableVal: Boolean = initialClosable
        private set
    private var customHeaderImpl: NativeImpl<*>? = null
    private var onCloseClickedListener: (() -> Unit)? = null
    private var onTitleEditedListener: ((String) -> Unit)? = null

    init {
        nativeWidget.setAttribute("role", "presentation")
        buttonElement.appendChild(titleSpan)
        buttonElement.appendChild(badgeSpan)
        buttonElement.appendChild(closeBtn)
        nativeWidget.appendChild(buttonElement)
        closeBtn.addEventListener("click") {
            onCloseClickedListener?.invoke()
        }
    }

    override fun setTabIdNative(id: String) {
        tabIdVal = id
        nativeWidget.setAttribute("data-tab-id", id)
        buttonElement.id = "tab-btn-$id"
    }

    override fun setTitleNative(title: String) {
        titleVal = title
        titleSpan.textContent = title
    }

    override fun setIconNative(icon: String?) {
        if (icon != null) {
            buttonElement.setAttribute("data-icon", icon)
        } else {
            buttonElement.removeAttribute("data-icon")
        }
    }

    override fun setTooltipNative(tooltip: String?) {
        if (tooltip != null) {
            buttonElement.setAttribute("title", tooltip)
        } else {
            buttonElement.removeAttribute("title")
        }
    }

    override fun setClosableNative(closable: Boolean) {
        closableVal = closable
        closeBtn.style["display"] = if (closable) "inline-block" else "none"
    }

    override fun setDirtyNative(dirty: Boolean) {
        if (dirty) {
            buttonElement.classList.add("tab-dirty")
        } else {
            buttonElement.classList.remove("tab-dirty")
        }
    }

    override fun setPinnedNative(pinned: Boolean) {
        if (pinned) {
            buttonElement.classList.add("tab-pinned")
        } else {
            buttonElement.classList.remove("tab-pinned")
        }
    }

    override fun setEditableNative(editable: Boolean) {
        buttonElement.setAttribute("data-editable", editable.toString())
    }

    override fun setBadgeNative(badgeText: String?, variantCss: String?) {
        if (!badgeText.isNullOrEmpty()) {
            badgeSpan.textContent = badgeText
            badgeSpan.classList.add("text-bg-${variantCss ?: "primary"}")
            badgeSpan.style["display"] = "inline-block"
        } else {
            badgeSpan.textContent = ""
            badgeSpan.style["display"] = "none"
        }
    }

    override fun setSelectedNative(selected: Boolean) {
        if (selected) {
            buttonElement.classList.add("active")
            buttonElement.setAttribute("aria-selected", "true")
        } else {
            buttonElement.classList.remove("active")
            buttonElement.setAttribute("aria-selected", "false")
        }
    }

    override fun setCustomHeaderComponentNative(customImpl: NativeImpl<*>?) {
        (customHeaderImpl?.nativeWidget as? WebDomElement)?.let { buttonElement.removeChild(it) }
        customHeaderImpl = customImpl
        val customDom = customImpl?.nativeWidget as? WebDomElement
        if (customDom != null) {
            buttonElement.insertChildAt(0, customDom)
        }
    }

    override fun setOnCloseClicked(listener: (() -> Unit)?) {
        onCloseClickedListener = listener
    }

    override fun setOnTitleEdited(listener: ((String) -> Unit)?) {
        onTitleEditedListener = listener
    }
}
