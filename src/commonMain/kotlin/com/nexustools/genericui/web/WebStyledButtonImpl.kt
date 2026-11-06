package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.action.StyledButtonNativeImpl
import com.nexustools.genericui.style.css.ComponentStyle
import com.nexustools.genericui.style.css.ComputedStyle
import com.nexustools.genericui.style.css.StyledVisualSpec

/**
 * Web implementation of [StyledButtonNativeImpl] extending [WebButtonImpl] with full support
 * for solid/transparent/gradient backgrounds, borders, corner radii, SVG/image icon tinting,
 * and interactive `:hover`, `:focus`, `:active`, and `:disabled` visual state overrides.
 */
class WebStyledButtonImpl(
    classPrefix: String = "styled-btn"
) : WebButtonImpl(classPrefix), StyledButtonNativeImpl<WebDomElement> {

    private var baseVisualsState: StyledVisualSpec = StyledVisualSpec.EMPTY
    private var hoverVisualsState: StyledVisualSpec? = null
    private var focusVisualsState: StyledVisualSpec? = null
    private var activeVisualsState: StyledVisualSpec? = null
    private var disabledVisualsState: StyledVisualSpec? = null

    private var hoveredFlag: Boolean = false
    private var focusedFlag: Boolean = false
    private var activePressedFlag: Boolean = false

    init {
        nativeWidget.setAttribute("data-styled-button", "true")
        nativeWidget.addEventListener("mouseenter") {
            if (isEnabled()) {
                setHoveredState(true)
            }
        }
        nativeWidget.addEventListener("mouseleave") {
            setHoveredState(false)
            if (activePressedFlag) {
                setActivePressedState(false)
            }
        }
        nativeWidget.addEventListener("focus") {
            if (isEnabled()) {
                setFocusedState(true)
            }
        }
        nativeWidget.addEventListener("blur") {
            setFocusedState(false)
        }
        nativeWidget.addEventListener("pointerdown") {
            if (isEnabled()) {
                setActivePressedState(true)
            }
        }
        nativeWidget.addEventListener("pointerup") {
            setActivePressedState(false)
        }
    }

    override fun setHoveredState(hovered: Boolean) {
        val coerced = if (!isEnabled()) false else hovered
        if (hoveredFlag != coerced) {
            hoveredFlag = coerced
            nativeWidget.setAttribute("data-hovered", coerced.toString())
            syncActiveVisualsToDom()
        }
    }

    override fun isHoveredState(): Boolean = if (!isEnabled()) false else hoveredFlag

    override fun setFocusedState(focused: Boolean) {
        val coerced = if (!isEnabled()) false else focused
        if (focusedFlag != coerced) {
            focusedFlag = coerced
            nativeWidget.setAttribute("data-focused", coerced.toString())
            syncActiveVisualsToDom()
        }
    }

    override fun isFocusedState(): Boolean = if (!isEnabled()) false else focusedFlag

    override fun setActivePressedState(active: Boolean) {
        val coerced = if (!isEnabled()) false else active
        if (activePressedFlag != coerced) {
            activePressedFlag = coerced
            nativeWidget.setAttribute("data-active-pressed", coerced.toString())
            syncActiveVisualsToDom()
        }
    }

    override fun isActivePressedState(): Boolean = if (!isEnabled()) false else activePressedFlag

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (!enabled) {
            hoveredFlag = false
            focusedFlag = false
            activePressedFlag = false
            nativeWidget.setAttribute("data-hovered", "false")
            nativeWidget.setAttribute("data-focused", "false")
            nativeWidget.setAttribute("data-active-pressed", "false")
        }
        syncActiveVisualsToDom()
    }

    override fun setBaseVisuals(visuals: StyledVisualSpec) {
        baseVisualsState = visuals
        syncActiveVisualsToDom()
    }

    override fun getBaseVisuals(): StyledVisualSpec = baseVisualsState

    override fun setHoverVisuals(visuals: StyledVisualSpec?) {
        hoverVisualsState = visuals
        syncActiveVisualsToDom()
    }

    override fun getHoverVisuals(): StyledVisualSpec? = hoverVisualsState

    override fun setFocusVisuals(visuals: StyledVisualSpec?) {
        focusVisualsState = visuals
        syncActiveVisualsToDom()
    }

    override fun getFocusVisuals(): StyledVisualSpec? = focusVisualsState

    override fun setActiveVisuals(visuals: StyledVisualSpec?) {
        activeVisualsState = visuals
        syncActiveVisualsToDom()
    }

    override fun getActiveVisuals(): StyledVisualSpec? = activeVisualsState

    override fun setDisabledVisuals(visuals: StyledVisualSpec?) {
        disabledVisualsState = visuals
        syncActiveVisualsToDom()
    }

    override fun getDisabledVisuals(): StyledVisualSpec? = disabledVisualsState

    override fun applyComponentStyle(style: ComponentStyle) {
        super<WebButtonImpl>.applyComponentStyle(style)
        syncActiveVisualsToDom()
    }

    override fun applyComputedStyle(style: ComputedStyle) {
        super<WebButtonImpl>.applyComputedStyle(style)
        syncActiveVisualsToDom()
    }

    private fun syncActiveVisualsToDom() {
        val effective = resolveCurrentVisuals()
        effective.backgroundColor?.let {
            nativeWidget.setStyle("background-color", it.toCssRgbaString())
        }
        if (effective.backgroundGradient != null) {
            nativeWidget.setStyle("background-image", effective.backgroundGradient.toCssString())
        } else if (baseVisualsState.backgroundGradient != null && effective.backgroundColor != null) {
            nativeWidget.setStyle("background-image", "none")
        }
        effective.textColor?.let {
            nativeWidget.setStyle("color", it.toCssRgbaString())
        }
        if (effective.hasBorder) {
            nativeWidget.setStyle("border-width", "${effective.borderWidthPx ?: 1f}px")
            nativeWidget.setStyle("border-style", (effective.borderStyle?.cssValue ?: "solid"))
            effective.borderColor?.let {
                nativeWidget.setStyle("border-color", it.toCssRgbaString())
            }
        } else if (effective.borderWidthPx != null && effective.borderWidthPx <= 0f) {
            nativeWidget.setStyle("border", "none", true)
            nativeWidget.setStyle("border-width", "0px", true)
        }
        effective.borderRadiusPx?.let {
            nativeWidget.setStyle("border-radius", "${it}px")
        }
        effective.opacity?.let {
            nativeWidget.setStyle("opacity", it.toString())
        }
        effective.padding?.let { pad ->
            nativeWidget.setStyle("padding", "${pad.top}px ${pad.right}px ${pad.bottom}px ${pad.left}px")
        }
        if (focusedFlag && effective.hasFocusRing) {
            val ringW = effective.focusRingWidthPx ?: 2f
            val ringColor = effective.focusRingColor!!.toCssRgbaString()
            nativeWidget.setStyle("outline", "${ringW}px solid $ringColor")
            nativeWidget.setStyle("outline-offset", "1px")
        } else if (!focusedFlag && (focusVisualsState?.hasFocusRing == true || baseVisualsState.hasFocusRing)) {
            nativeWidget.setStyle("outline", "none")
        }
        if (effective.boxShadowColor != null && (effective.boxShadowBlurPx ?: 0f) > 0f) {
            nativeWidget.setStyle(
                "box-shadow",
                "0px 2px ${effective.boxShadowBlurPx}px ${effective.boxShadowColor.toCssRgbaString()}"
            )
        }
        val backdropCss = effective.backdropFilter?.toCssString()
            ?: effective.backdropBlurPx?.takeIf { it > 0f }?.let { "blur(${it}px)" }
        if (backdropCss != null) {
            nativeWidget.setStyle("backdrop-filter", backdropCss)
            nativeWidget.setStyle("-webkit-backdrop-filter", backdropCss)
        }
        effective.filter?.let {
            nativeWidget.setStyle("filter", it.toCssString())
        }
    }
}
