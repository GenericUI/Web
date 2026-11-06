package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.layout.StyledContainerNativeImpl
import com.nexustools.genericui.style.css.ComponentStyle
import com.nexustools.genericui.style.css.ComputedStyle
import com.nexustools.genericui.style.css.StyledVisualSpec

/**
 * Web implementation of [StyledContainerNativeImpl] rendering `<genericui:styled-container>`
 * with cross-platform background color (including RGBA alpha transparency), CSS gradients
 * (`linear-gradient` / `radial-gradient`), borders, corner radii, padding, and opacity.
 */
class WebStyledContainerImpl(
    classPrefix: String = "styled-container",
    tagName: String = "genericui:styled-container"
) : WebContainerImpl(classPrefix, tagName), StyledContainerNativeImpl<WebDomElement> {

    private var visualsState: StyledVisualSpec = StyledVisualSpec.EMPTY

    init {
        nativeWidget.setAttribute("data-styled-container", "true")
    }

    override fun setStyledVisuals(visuals: StyledVisualSpec) {
        visualsState = visuals
        applyStyledVisualsToDom(visuals)
    }

    override fun getStyledVisuals(): StyledVisualSpec = visualsState

    override fun applyComponentStyle(style: ComponentStyle) {
        super<WebContainerImpl>.applyComponentStyle(style)
        applyStyledVisualsToDom(visualsState)
    }

    override fun applyComputedStyle(style: ComputedStyle) {
        super<WebContainerImpl>.applyComputedStyle(style)
        applyStyledVisualsToDom(visualsState)
    }

    private fun applyStyledVisualsToDom(visuals: StyledVisualSpec) {
        visuals.backgroundColor?.let {
            nativeWidget.setStyle("background-color", it.toCssRgbaString())
        }
        visuals.backgroundGradient?.let {
            nativeWidget.setStyle("background-image", it.toCssString())
        }
        visuals.textColor?.let {
            nativeWidget.setStyle("color", it.toCssRgbaString())
        }
        if (visuals.hasBorder) {
            nativeWidget.setStyle("border-width", "${visuals.borderWidthPx ?: 1f}px")
            nativeWidget.setStyle("border-style", (visuals.borderStyle?.cssValue ?: "solid"))
            visuals.borderColor?.let {
                nativeWidget.setStyle("border-color", it.toCssRgbaString())
            }
        } else if (visuals.borderWidthPx != null && visuals.borderWidthPx <= 0f) {
            nativeWidget.setStyle("border", "none")
            nativeWidget.setStyle("border-width", "0px")
        }
        visuals.borderRadiusPx?.let {
            nativeWidget.setStyle("border-radius", "${it}px")
        }
        visuals.opacity?.let {
            nativeWidget.setStyle("opacity", it.toString())
        }
        visuals.padding?.let { pad ->
            nativeWidget.setStyle("padding", "${pad.top}px ${pad.right}px ${pad.bottom}px ${pad.left}px")
        }
        if (visuals.boxShadowColor != null && (visuals.boxShadowBlurPx ?: 0f) > 0f) {
            nativeWidget.setStyle(
                "box-shadow",
                "0px 2px ${visuals.boxShadowBlurPx}px ${visuals.boxShadowColor.toCssRgbaString()}"
            )
        }
        val backdropCss = visuals.backdropFilter?.toCssString()
            ?: visuals.backdropBlurPx?.takeIf { it > 0f }?.let { "blur(${it}px)" }
        if (backdropCss != null) {
            nativeWidget.setStyle("backdrop-filter", backdropCss)
            nativeWidget.setStyle("-webkit-backdrop-filter", backdropCss)
        }
        visuals.filter?.let {
            nativeWidget.setStyle("filter", it.toCssString())
        }
    }
}
