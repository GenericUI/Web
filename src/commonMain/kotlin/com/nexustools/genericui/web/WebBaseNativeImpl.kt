package com.nexustools.genericui.web

import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.enums.bootstrap.*
import com.nexustools.genericui.event.KeyEvent
import com.nexustools.genericui.event.PointerEvent
import com.nexustools.genericui.event.WheelEvent
import com.nexustools.genericui.geometry.EdgeInsets
import com.nexustools.genericui.geometry.Rect
import com.nexustools.genericui.geometry.Size
import com.nexustools.genericui.style.css.ComputedStyle
import com.nexustools.genericui.style.mapper.BootstrapClassMapper
import com.nexustools.genericui.style.mapper.BootstrapStyleSnapshot
import com.nexustools.genericui.style.transition.CssTransitionSpec

/**
 * Base [NativeImpl] for the `:web` backend.
 *
 * Per the dual-mode styling architecture:
 * - Applies CSS classes directly to [nativeWidget]'s `classList` (`setClass`, `getClass`, `addClass`, `removeClass`, `hasClass`).
 * - Simulates the Bootstrap Enum getters (`getType`, `getSize`, etc.) and setters (`setType`, `setSize`, etc.)
 *   by parsing/updating the DOM element's CSS class list via [BootstrapClassMapper].
 */
open class WebBaseNativeImpl(
    tagName: String = "div",
    override val componentClassPrefix: String = ""
) : NativeImpl<WebDomElement> {

    override val nativeWidget: WebDomElement = WebDomElement(tagName).also {
        if (componentClassPrefix.isNotEmpty()) {
            it.classList.add(componentClassPrefix)
        }
    }

    private var visibleFlag: Boolean = true
    private var enabledFlag: Boolean = true
    private var opacityValue: Float = 1.0f
    private var tooltipValue: String? = null
    private var preferredSizeValue: Size? = null

    private var onClickCallback: (() -> Unit)? = null
    private var onDoubleClickCallback: (() -> Unit)? = null
    private var onContextMenuCallback: ((PointerEvent) -> Unit)? = null
    private var onPointerDownCallback: ((PointerEvent) -> Unit)? = null
    private var onPointerMoveCallback: ((PointerEvent) -> Unit)? = null
    private var onPointerUpCallback: ((PointerEvent) -> Unit)? = null
    private var onWheelCallback: ((WheelEvent) -> Unit)? = null
    private var onKeyDownCallback: ((KeyEvent) -> Unit)? = null
    private var onKeyUpCallback: ((KeyEvent) -> Unit)? = null

    init {
        nativeWidget.addEventListener("click") { onClickCallback?.invoke() }
        nativeWidget.addEventListener("dblclick") { onDoubleClickCallback?.invoke() }
        nativeWidget.addEventListener("contextmenu") { ev ->
            val pe = (ev as? PointerEvent) ?: PointerEvent(0f, 0f, button = 2)
            onContextMenuCallback?.invoke(pe)
        }
        nativeWidget.addEventListener("pointerdown") { ev ->
            val pe = (ev as? PointerEvent) ?: PointerEvent(0f, 0f)
            onPointerDownCallback?.invoke(pe)
        }
        nativeWidget.addEventListener("pointermove") { ev ->
            val pe = (ev as? PointerEvent) ?: PointerEvent(0f, 0f)
            onPointerMoveCallback?.invoke(pe)
        }
        nativeWidget.addEventListener("pointerup") { ev ->
            val pe = (ev as? PointerEvent) ?: PointerEvent(0f, 0f)
            onPointerUpCallback?.invoke(pe)
        }
        nativeWidget.addEventListener("wheel") { ev ->
            val we = (ev as? WheelEvent) ?: WheelEvent(0f, 0f, 0f, 0f)
            onWheelCallback?.invoke(we)
        }
        nativeWidget.addEventListener("keydown") { ev ->
            val ke = (ev as? KeyEvent) ?: KeyEvent("")
            onKeyDownCallback?.invoke(ke)
        }
        nativeWidget.addEventListener("keyup") { ev ->
            val ke = (ev as? KeyEvent) ?: KeyEvent("")
            onKeyUpCallback?.invoke(ke)
        }
    }

    protected fun currentSnapshot(): BootstrapStyleSnapshot =
        BootstrapClassMapper.fromClassString(nativeWidget.className, componentClassPrefix)

    protected fun applySnapshot(snapshot: BootstrapStyleSnapshot) {
        nativeWidget.className = BootstrapClassMapper.toClassString(snapshot)
    }

    override fun setClass(className: String) {
        nativeWidget.className = className
    }

    override fun getClass(): String = nativeWidget.className

    override fun addClass(className: String) {
        className.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.forEach {
            nativeWidget.classList.add(it)
        }
    }

    override fun removeClass(className: String) {
        className.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.forEach {
            nativeWidget.classList.remove(it)
        }
    }

    override fun hasClass(className: String): Boolean =
        nativeWidget.classList.contains(className.trim())

    override fun setType(type: BootstrapType?) = applySnapshot(currentSnapshot().copy(type = type))
    override fun getType(): BootstrapType? = currentSnapshot().type

    override fun setOutline(outline: Boolean) = applySnapshot(currentSnapshot().copy(outline = outline))
    override fun isOutline(): Boolean = currentSnapshot().outline

    override fun setSize(size: BootstrapSize?) = applySnapshot(currentSnapshot().copy(size = size))
    override fun getSize(): BootstrapSize? = currentSnapshot().size

    override fun setBackgroundType(type: BootstrapType?) = applySnapshot(currentSnapshot().copy(backgroundType = type))
    override fun getBackgroundType(): BootstrapType? = currentSnapshot().backgroundType

    override fun setTextColorType(type: BootstrapType?) = applySnapshot(currentSnapshot().copy(textColorType = type))
    override fun getTextColorType(): BootstrapType? = currentSnapshot().textColorType

    override fun setBorderType(type: BootstrapType?) = applySnapshot(currentSnapshot().copy(borderType = type))
    override fun getBorderType(): BootstrapType? = currentSnapshot().borderType

    override fun setTextAlignment(alignment: TextAlignment?) = applySnapshot(currentSnapshot().copy(textAlignment = alignment))
    override fun getTextAlignment(): TextAlignment? = currentSnapshot().textAlignment

    override fun setFontWeight(weight: FontWeight?) = applySnapshot(currentSnapshot().copy(fontWeight = weight))
    override fun getFontWeight(): FontWeight? = currentSnapshot().fontWeight

    override fun setTextTransform(transform: TextTransform?) = applySnapshot(currentSnapshot().copy(textTransform = transform))
    override fun getTextTransform(): TextTransform? = currentSnapshot().textTransform

    override fun setTextOverflow(overflow: TextOverflow?) = applySnapshot(currentSnapshot().copy(textOverflow = overflow))
    override fun getTextOverflow(): TextOverflow? = currentSnapshot().textOverflow

    override fun setBorderRadius(radius: BorderRadiusStyle?) = applySnapshot(currentSnapshot().copy(borderRadius = radius))
    override fun getBorderRadius(): BorderRadiusStyle? = currentSnapshot().borderRadius

    override fun setShadow(shadow: ShadowElevation?) = applySnapshot(currentSnapshot().copy(shadow = shadow))
    override fun getShadow(): ShadowElevation? = currentSnapshot().shadow

    override fun setDisplay(display: DisplayMode?) = applySnapshot(currentSnapshot().copy(display = display))
    override fun getDisplay(): DisplayMode? = currentSnapshot().display

    override fun setFlexDirection(direction: FlexDirection?) = applySnapshot(currentSnapshot().copy(flexDirection = direction))
    override fun getFlexDirection(): FlexDirection? = currentSnapshot().flexDirection

    override fun setFlexWrap(wrap: FlexWrap?) = applySnapshot(currentSnapshot().copy(flexWrap = wrap))
    override fun getFlexWrap(): FlexWrap? = currentSnapshot().flexWrap

    override fun setJustifyContent(justify: JustifyContent?) = applySnapshot(currentSnapshot().copy(justifyContent = justify))
    override fun getJustifyContent(): JustifyContent? = currentSnapshot().justifyContent

    override fun setAlignItems(align: AlignItems?) = applySnapshot(currentSnapshot().copy(alignItems = align))
    override fun getAlignItems(): AlignItems? = currentSnapshot().alignItems

    override fun setAlignSelf(align: AlignSelf?) = applySnapshot(currentSnapshot().copy(alignSelf = align))
    override fun getAlignSelf(): AlignSelf? = currentSnapshot().alignSelf

    override fun setOverflow(overflow: OverflowBehavior?) = applySnapshot(currentSnapshot().copy(overflow = overflow))
    override fun getOverflow(): OverflowBehavior? = currentSnapshot().overflow

    override fun setPosition(position: PositionMode?) = applySnapshot(currentSnapshot().copy(position = position))
    override fun getPosition(): PositionMode? = currentSnapshot().position

    override fun setMarginAll(scale: SpacingScale?) = applySnapshot(currentSnapshot().copy(marginAll = scale))
    override fun getMarginAll(): SpacingScale? = currentSnapshot().marginAll

    override fun setPaddingAll(scale: SpacingScale?) = applySnapshot(currentSnapshot().copy(paddingAll = scale))
    override fun getPaddingAll(): SpacingScale? = currentSnapshot().paddingAll

    override fun setVisible(visible: Boolean) {
        visibleFlag = visible
        if (visible) {
            nativeWidget.style.remove("display")
        } else {
            nativeWidget.style["display"] = "none"
        }
    }

    override fun isVisible(): Boolean = visibleFlag

    override fun setEnabled(enabled: Boolean) {
        enabledFlag = enabled
        nativeWidget.disabled = !enabled
    }

    override fun isEnabled(): Boolean = enabledFlag

    override fun setOpacity(opacity: Float) {
        opacityValue = opacity.coerceIn(0f, 1f)
        nativeWidget.style["opacity"] = opacityValue.toString()
    }

    override fun getOpacity(): Float = opacityValue

    override fun setTooltipText(tooltip: String?) {
        tooltipValue = tooltip
        if (tooltip != null) {
            nativeWidget.setAttribute("title", tooltip)
        } else {
            nativeWidget.removeAttribute("title")
        }
    }

    override fun getTooltipText(): String? = tooltipValue

    override fun setPreferredSize(size: Size?) {
        preferredSizeValue = size
        if (size != null) {
            nativeWidget.style["width"] = "${size.width}px"
            nativeWidget.style["height"] = "${size.height}px"
            nativeWidget.clientWidth = size.width
            nativeWidget.clientHeight = size.height
        } else {
            nativeWidget.style.remove("width")
            nativeWidget.style.remove("height")
        }
    }

    override fun getBounds(): Rect =
        Rect(0, 0, preferredSizeValue?.width ?: nativeWidget.clientWidth, preferredSizeValue?.height ?: nativeWidget.clientHeight)

    override fun setPadding(padding: EdgeInsets) {
        nativeWidget.style["padding"] = "${padding.top}px ${padding.right}px ${padding.bottom}px ${padding.left}px"
    }

    override fun setMargin(margin: EdgeInsets) {
        nativeWidget.style["margin"] = "${margin.top}px ${margin.right}px ${margin.bottom}px ${margin.left}px"
    }

    override fun applyComputedStyle(style: ComputedStyle) {
        style.backgroundColor?.let { nativeWidget.style["background-color"] = it.toCssRgba() }
        style.textColor?.let { nativeWidget.style["color"] = it.toCssRgba() }
        style.borderColor?.let { nativeWidget.style["border-color"] = it.toCssRgba() }
        style.borderWidthPx?.let { nativeWidget.style["border-width"] = "${it}px" }
        style.borderRadiusPx?.let { nativeWidget.style["border-radius"] = "${it}px" }
        style.fontSizePx?.let { nativeWidget.style["font-size"] = "${it}px" }
        style.fontFamily?.let { nativeWidget.style["font-family"] = it }
        style.fontWeightNumeric?.let { nativeWidget.style["font-weight"] = it.toString() }
        style.backgroundImageUrl?.let { nativeWidget.style["background-image"] = "url('$it')" }
        style.textShadow?.let { nativeWidget.style["text-shadow"] = it }
        style.boxShadow?.let { nativeWidget.style["box-shadow"] = it }
        style.transform?.let { nativeWidget.style["transform"] = it }
        style.transition?.let { nativeWidget.style["transition"] = it }
        style.zIndex?.let { nativeWidget.style["z-index"] = it.toString() }
        style.left?.let { nativeWidget.style["left"] = it }
        style.top?.let { nativeWidget.style["top"] = it }
        style.cursor?.let { nativeWidget.style["cursor"] = it }
        style.pointerEventsEnabled?.let { nativeWidget.style["pointer-events"] = if (it) "auto" else "none" }
        style.rawDeclarations.forEach { (k, v) -> nativeWidget.style[k] = v }
    }

    override fun supportsNativeTransitions(): Boolean = true

    override fun configureNativeTransitions(specs: List<CssTransitionSpec>) {
        if (specs.isEmpty()) {
            nativeWidget.style.remove("transition")
        } else {
            nativeWidget.style["transition"] = specs.joinToString(", ") { spec ->
                val tf = if (spec.timingFunction.isLinear) {
                    "linear"
                } else {
                    "cubic-bezier(${spec.timingFunction.x1}, ${spec.timingFunction.y1}, ${spec.timingFunction.x2}, ${spec.timingFunction.y2})"
                }
                "${spec.property} ${spec.durationMs}ms $tf ${spec.delayMs}ms"
            }
        }
    }

    override fun runNativeTransition(
        property: String,
        fromValue: String?,
        toValue: String,
        spec: CssTransitionSpec,
        onComplete: () -> Unit
    ): Boolean {
        nativeWidget.style[property] = toValue
        return true
    }

    override fun setOnClick(listener: (() -> Unit)?) { onClickCallback = listener }
    override fun setOnDoubleClick(listener: (() -> Unit)?) { onDoubleClickCallback = listener }
    override fun setOnContextMenu(listener: ((PointerEvent) -> Unit)?) { onContextMenuCallback = listener }
    override fun setOnPointerDown(listener: ((PointerEvent) -> Unit)?) { onPointerDownCallback = listener }
    override fun setOnPointerMove(listener: ((PointerEvent) -> Unit)?) { onPointerMoveCallback = listener }
    override fun setOnPointerUp(listener: ((PointerEvent) -> Unit)?) { onPointerUpCallback = listener }
    override fun setOnWheel(listener: ((WheelEvent) -> Unit)?) { onWheelCallback = listener }
    override fun setOnKeyDown(listener: ((KeyEvent) -> Unit)?) { onKeyDownCallback = listener }
    override fun setOnKeyUp(listener: ((KeyEvent) -> Unit)?) { onKeyUpCallback = listener }

    override fun dispose() {
        nativeWidget.remove()
    }
}
