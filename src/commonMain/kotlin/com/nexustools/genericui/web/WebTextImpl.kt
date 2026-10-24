package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.typography.TextNativeImpl

/**
 * Web implementation of [TextNativeImpl].
 */
class WebTextImpl(
    tagName: String = "span",
    classPrefix: String = ""
) : WebBaseNativeImpl(tagName, classPrefix), TextNativeImpl<WebDomElement> {
    private var maxLinesValue: Int? = null

    override fun setText(text: String) {
        nativeWidget.textContent = text
    }

    override fun getText(): String = nativeWidget.textContent

    override fun setMaxLines(maxLines: Int?) {
        maxLinesValue = maxLines
        if (maxLines != null) {
            nativeWidget.style["display"] = "-webkit-box"
            nativeWidget.style["-webkit-line-clamp"] = maxLines.toString()
            nativeWidget.style["-webkit-box-orient"] = "vertical"
            nativeWidget.style["overflow"] = "hidden"
        } else {
            nativeWidget.style.remove("-webkit-line-clamp")
            nativeWidget.style.remove("-webkit-box-orient")
        }
    }

    override fun getMaxLines(): Int? = maxLinesValue
}
