package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.action.ButtonNativeImpl
import com.nexustools.genericui.res.Resources

/**
 * Web implementation of [ButtonNativeImpl] rendered as `<button type="button" class="btn ...">` per the Bootstrap `.btn` standard.
 *
 * Supports theme-tinted SVG icons (`currentColor`) resolved from `res://drawable/<name>` ([Resources.getDrawable]),
 * inline `<svg...>` markup, or image URLs, alongside optional button text.
 */
open class WebButtonImpl(
    classPrefix: String = "btn"
) : WebBaseNativeImpl("button", classPrefix), ButtonNativeImpl<WebDomElement> {
    protected var textValue: String = ""
    protected var iconName: String? = null

    init {
        nativeWidget.setAttribute("type", "button")
    }

    override fun setText(text: String) {
        textValue = text
        syncButtonContent()
    }

    override fun getText(): String = textValue

    override fun setIcon(icon: String?) {
        iconName = icon
        if (icon != null) {
            nativeWidget.setAttribute("data-icon", icon)
        } else {
            nativeWidget.removeAttribute("data-icon")
        }
        syncButtonContent()
    }

    override fun getIcon(): String? = iconName

    protected open fun syncButtonContent() {
        val rawIcon = iconName?.trim().orEmpty()
        if (rawIcon.isEmpty()) {
            nativeWidget.innerHTML = ""
            nativeWidget.textContent = textValue
            return
        }

        // Keep textContent in sync for headless unit tests before setting innerHTML for browser rendering
        nativeWidget.textContent = textValue

        val svgMarkup = resolveIconSvgMarkup(rawIcon)
        val escapedText = escapeHtml(textValue)
        if (svgMarkup != null) {
            nativeWidget.innerHTML = if (escapedText.isNotEmpty()) {
                "$svgMarkup<span class=\"genericui-btn-label\" style=\"margin-left:0.35rem;\">$escapedText</span>"
            } else {
                svgMarkup
            }
        } else if (
            rawIcon.startsWith("data:image/", ignoreCase = true) ||
            rawIcon.startsWith("http://", ignoreCase = true) ||
            rawIcon.startsWith("https://", ignoreCase = true) ||
            rawIcon.startsWith("/") ||
            rawIcon.endsWith(".svg", ignoreCase = true) ||
            rawIcon.endsWith(".png", ignoreCase = true)
        ) {
            val imgHtml = "<img class=\"genericui-btn-icon-img\" src=\"${escapeHtml(rawIcon)}\" alt=\"\" style=\"width:1.15em;height:1.15em;vertical-align:middle;object-fit:contain;pointer-events:none;\" />"
            nativeWidget.innerHTML = if (escapedText.isNotEmpty()) {
                "$imgHtml<span class=\"genericui-btn-label\" style=\"margin-left:0.35rem;\">$escapedText</span>"
            } else {
                imgHtml
            }
        } else {
            nativeWidget.textContent = if (textValue.isNotEmpty()) textValue else rawIcon
        }
    }

    private fun resolveIconSvgMarkup(rawIcon: String): String? {
        if (rawIcon.startsWith("<svg", ignoreCase = true)) {
            return normalizeThemeTintedSvg(rawIcon)
        }
        val drawable = Resources.getDrawable(rawIcon)
        if (drawable != null) {
            val source = drawable.svgSource?.trim()
            if (!source.isNullOrEmpty() && source.startsWith("<svg", ignoreCase = true)) {
                return normalizeThemeTintedSvg(source)
            }
            if (drawable.svgPathDataList.isNotEmpty()) {
                val vbW = drawable.viewBoxWidth.toInt().coerceAtLeast(1)
                val vbH = drawable.viewBoxHeight.toInt().coerceAtLeast(1)
                val paths = drawable.svgPathDataList.joinToString("") { d ->
                    "<path fill=\"currentColor\" d=\"${escapeHtml(d)}\"/>"
                }
                return "<svg class=\"genericui-btn-svg-icon\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 $vbW $vbH\" width=\"18\" height=\"18\" fill=\"currentColor\" style=\"width:1.15em;height:1.15em;vertical-align:middle;pointer-events:none;flex-shrink:0;\">$paths</svg>"
            }
        }
        return null
    }

    private fun normalizeThemeTintedSvg(rawSvg: String): String {
        var svg = rawSvg.trim()
        svg = svg.replace(Regex("fill\\s*=\\s*\"#(?:fff|ffffff|ffffffff|000|000000)\"", RegexOption.IGNORE_CASE), "fill=\"currentColor\"")
        svg = svg.replace(Regex("stroke\\s*=\\s*\"#(?:fff|ffffff|ffffffff|000|000000)\"", RegexOption.IGNORE_CASE), "stroke=\"currentColor\"")
        if (!svg.contains("class=", ignoreCase = true)) {
            svg = svg.replaceFirst("<svg", "<svg class=\"genericui-btn-svg-icon\" style=\"width:1.15em;height:1.15em;vertical-align:middle;pointer-events:none;flex-shrink:0;\"")
        }
        if (!svg.contains("fill=", ignoreCase = true)) {
            svg = svg.replaceFirst("<svg", "<svg fill=\"currentColor\"")
        }
        return svg
    }

    private fun escapeHtml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
