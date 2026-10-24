package com.nexustools.genericui.web

import com.nexustools.genericui.component.dock.ToolPaletteItem
import com.nexustools.genericui.nativeimpl.dock.ToolPaletteNativeImpl
import com.nexustools.genericui.style.color.RgbaColor

/**
 * Web implementation of [ToolPaletteNativeImpl].
 */
class WebToolPaletteImpl : WebBaseContainerNativeImpl("div", "tool-palette"), ToolPaletteNativeImpl<WebDomElement> {
    private var columnsCount: Int = 2
    private var toolsList: List<ToolPaletteItem> = emptyList()
    private var activeId: String? = null
    private var fgColor: RgbaColor = RgbaColor.BLACK
    private var bgColor: RgbaColor = RgbaColor.WHITE
    private var onToolSelectedListener: ((String) -> Unit)? = null
    private var onColorsSwappedListener: ((RgbaColor, RgbaColor) -> Unit)? = null

    override fun setColumns(columns: Int) {
        columnsCount = columns.coerceAtLeast(1)
        nativeWidget.style["grid-template-columns"] = "repeat($columnsCount, 1fr)"
    }

    override fun setTools(tools: List<ToolPaletteItem>, activeToolId: String?) {
        toolsList = tools
        activeId = activeToolId
    }

    override fun setActiveToolId(toolId: String?) {
        activeId = toolId
        if (toolId != null) {
            onToolSelectedListener?.invoke(toolId)
        }
    }

    override fun setForegroundColor(color: RgbaColor) {
        fgColor = color
    }

    override fun setBackgroundColor(color: RgbaColor) {
        bgColor = color
    }

    override fun setOnToolSelected(listener: ((String) -> Unit)?) {
        onToolSelectedListener = listener
    }

    override fun setOnColorsSwapped(listener: ((RgbaColor, RgbaColor) -> Unit)?) {
        onColorsSwappedListener = listener
    }
}
