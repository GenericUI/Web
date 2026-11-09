package com.nexustools.genericui.web

import com.nexustools.genericui.component.tabs.TabIconPlacement
import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.nativeimpl.tabs.TabNativeImpl
import com.nexustools.genericui.res.Resources

/**
 * Web implementation of [TabNativeImpl], rendering `<genericui:tab class="nav-item" role="presentation">`
 * (or `<genericui:closeabletab>` when [initialClosable] is `true`) containing a `<button class="nav-link" role="tab">`
 * with optional icon, title, optional badge, optional `.btn-close` close button, or an embedded custom [WebDomElement].
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
    private val iconSpan: WebDomElement = WebDomElement("span").apply {
        classList.add("tab-icon")
        style["display"] = "none"
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
    var iconVal: String? = null
        private set
    var iconSizePxVal: Int = 20
        private set
    var iconPlacementVal: TabIconPlacement = TabIconPlacement.START
        private set
    var tintImageToThemeVal: Boolean = true
        private set
    var closableVal: Boolean = initialClosable
        private set
    private var customHeaderImpl: NativeImpl<*>? = null
    private var onCloseClickedListener: (() -> Unit)? = null
    private var onTitleEditedListener: ((String) -> Unit)? = null
    private var tabClickAction: (() -> Unit)? = null

    init {
        nativeWidget.setAttribute("role", "presentation")
        buttonElement.appendChild(iconSpan)
        buttonElement.appendChild(titleSpan)
        buttonElement.appendChild(badgeSpan)
        buttonElement.appendChild(closeBtn)
        nativeWidget.appendChild(buttonElement)
        closeBtn.addEventListener("click") {
            onCloseClickedListener?.invoke()
        }
    }

    override fun setOnClick(listener: (() -> Unit)?) {
        tabClickAction = listener
        super.setOnClick(listener)
    }

    internal fun dispatchClick() {
        tabClickAction?.invoke()
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
        iconVal = icon
        if (icon != null) {
            buttonElement.setAttribute("data-icon", icon)
        } else {
            buttonElement.removeAttribute("data-icon")
        }
        syncIconElement()
    }

    override fun setIconSizePxNative(sizePx: Int) {
        iconSizePxVal = sizePx.coerceAtLeast(1)
        buttonElement.setAttribute("data-icon-size", iconSizePxVal.toString())
        syncIconElement()
    }

    override fun setIconPlacementNative(placement: TabIconPlacement) {
        iconPlacementVal = placement
        buttonElement.setAttribute("data-icon-placement", placement.name.lowercase())
        syncIconElement()
    }

    override fun setTintImageToThemeNative(tintToTheme: Boolean) {
        tintImageToThemeVal = tintToTheme
        buttonElement.setAttribute("data-tint-theme", tintToTheme.toString())
        syncIconElement()
    }

    private fun syncIconElement() {
        val rawIcon = iconVal?.trim().orEmpty()
        if (rawIcon.isEmpty()) {
            iconSpan.innerHTML = ""
            iconSpan.textContent = ""
            iconSpan.style["display"] = "none"
            buttonElement.classList.remove("flex-column")
            buttonElement.classList.remove("align-items-center")
            return
        }
        iconSpan.style["display"] = "inline-flex"
        iconSpan.style["width"] = "${iconSizePxVal}px"
        iconSpan.style["height"] = "${iconSizePxVal}px"
        if (iconPlacementVal == TabIconPlacement.TOP) {
            buttonElement.classList.add("d-flex")
            buttonElement.classList.add("flex-column")
            buttonElement.classList.add("align-items-center")
            iconSpan.style["margin-bottom"] = "4px"
            iconSpan.style.remove("margin-right")
        } else {
            buttonElement.classList.remove("flex-column")
            iconSpan.style["margin-right"] = "6px"
            iconSpan.style.remove("margin-bottom")
        }

        val drawable = Resources.getDrawable(rawIcon)
        val svgSource = drawable?.svgSource?.trim()
        when {
            rawIcon.startsWith("<svg", ignoreCase = true) -> {
                iconSpan.innerHTML = rawIcon
            }
            !svgSource.isNullOrEmpty() && svgSource.startsWith("<svg", ignoreCase = true) -> {
                iconSpan.innerHTML = svgSource
            }
            drawable != null && drawable.svgPathDataList.isNotEmpty() -> {
                val vbW = drawable.viewBoxWidth.toInt().coerceAtLeast(1)
                val vbH = drawable.viewBoxHeight.toInt().coerceAtLeast(1)
                val paths = drawable.svgPathDataList.joinToString("") { d ->
                    "<path fill=\"currentColor\" d=\"$d\"/>"
                }
                iconSpan.innerHTML = "<svg class=\"gui-tab-icon\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 $vbW $vbH\" width=\"$iconSizePxVal\" height=\"$iconSizePxVal\" fill=\"currentColor\">$paths</svg>"
            }
            drawable != null && !drawable.base64Data.isNullOrBlank() -> {
                val mime = when (drawable.format.lowercase()) {
                    "svg" -> "image/svg+xml"
                    "jpg", "jpeg" -> "image/jpeg"
                    "webp" -> "image/webp"
                    "gif" -> "image/gif"
                    else -> "image/png"
                }
                iconSpan.innerHTML = "<img class=\"gui-tab-icon\" src=\"data:$mime;base64,${drawable.base64Data}\" alt=\"\" style=\"width:${iconSizePxVal}px;height:${iconSizePxVal}px;object-fit:contain;\" />"
            }
            rawIcon.startsWith("res://", ignoreCase = true) ||
                rawIcon.startsWith("data:image/", ignoreCase = true) ||
                rawIcon.startsWith("http://", ignoreCase = true) ||
                rawIcon.startsWith("https://", ignoreCase = true) ||
                rawIcon.startsWith("memory://", ignoreCase = true) ||
                rawIcon.startsWith("/") ||
                rawIcon.endsWith(".svg", ignoreCase = true) ||
                rawIcon.endsWith(".png", ignoreCase = true) -> {
                iconSpan.innerHTML = "<img class=\"gui-tab-icon\" src=\"$rawIcon\" alt=\"\" style=\"width:${iconSizePxVal}px;height:${iconSizePxVal}px;object-fit:contain;\" />"
            }
            else -> {
                iconSpan.innerHTML = ""
                iconSpan.textContent = rawIcon
            }
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
