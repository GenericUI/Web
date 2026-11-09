package com.nexustools.genericui.web

import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.enums.bootstrap.*
import com.nexustools.genericui.event.KeyEvent
import com.nexustools.genericui.event.PointerEvent
import com.nexustools.genericui.event.WheelEvent
import com.nexustools.genericui.geometry.EdgeInsets
import com.nexustools.genericui.geometry.Rect
import com.nexustools.genericui.geometry.Size
import com.nexustools.genericui.style.bootstrap.ComponentBootstrap
import com.nexustools.genericui.style.css.ComponentStyle
import com.nexustools.genericui.style.css.ComputedStyle
import com.nexustools.genericui.style.css.CssCursorValue
import com.nexustools.genericui.style.gradient.Gradient.Gradient
import com.nexustools.genericui.style.mapper.BootstrapClassMapper
import com.nexustools.genericui.style.mapper.BootstrapStyleSnapshot
import com.nexustools.genericui.style.transition.CssTransitionSpec
import com.nexustools.genericui.web.style.WebCssStyleTranslator

/**
 * Base [NativeImpl] for the `:web` backend.
 *
 * Per the dual-mode styling architecture:
 * - Applies user-added CSS classes strictly to [nativeWidget]'s `classList` (`setClass`, `getClass`, `getClasses`, `addClass`, `removeClass`, `hasClass`).
 * - Applies Bootstrap Enum classes (`ComponentBootstrap`, `setType`, `setSize`, etc.) to [nativeWidget]'s
 *   `enumClasses` (`setEnumClasses`) via [BootstrapClassMapper], keeping them 100% invisible to `getClasses()` / `hasClass()`.
 */
open class WebBaseNativeImpl(
    tagName: String = "div",
    override val componentClassPrefix: String = ""
) : NativeImpl<WebDomElement> {

    override val nativeWidget: WebDomElement = WebDomElement(tagName)

    protected var styleSnapshot: BootstrapStyleSnapshot =
        BootstrapStyleSnapshot(componentPrefix = componentClassPrefix)
    private var bootstrapState: ComponentBootstrap? = null

    private var visibleFlag: Boolean = true
    private var enabledFlag: Boolean = true
    private var hoveredStateFlag: Boolean = false
    private var focusedStateFlag: Boolean = false
    private var activePressedStateFlag: Boolean = false
    private var opacityValue: Float = 1.0f
    private var tooltipValue: String? = null
    private var preferredSizeValue: Size? = null
    private var backgroundGradientValue: Gradient? = null

    private var onClickCallback: (() -> Unit)? = null
    private var onDoubleClickCallback: (() -> Unit)? = null
    private var onContextMenuCallback: ((PointerEvent) -> Unit)? = null
    private var onPointerDownCallback: ((PointerEvent) -> Unit)? = null
    private var onPointerMoveCallback: ((PointerEvent) -> Unit)? = null
    private var onPointerUpCallback: ((PointerEvent) -> Unit)? = null
    private var onPointerEnterCallback: ((PointerEvent) -> Unit)? = null
    private var onPointerLeaveCallback: ((PointerEvent) -> Unit)? = null
    private var onWheelCallback: ((WheelEvent) -> Unit)? = null
    private var onKeyDownCallback: ((KeyEvent) -> Unit)? = null
    private var onKeyUpCallback: ((KeyEvent) -> Unit)? = null

    private val contextMenuDomListener: (Any?) -> Unit = { ev ->
        val pe = when (ev) {
            is PointerEvent -> ev
            is String -> {
                val parts = ev.split(",")
                val px = parts.getOrNull(0)?.trim()?.toFloatOrNull() ?: 0f
                val py = parts.getOrNull(1)?.trim()?.toFloatOrNull() ?: 0f
                PointerEvent(x = px, y = py, button = 2)
            }
            else -> PointerEvent(0f, 0f, button = 2)
        }
        onContextMenuCallback?.invoke(pe)
    }

    init {
        nativeWidget.ownerNativeImpl = this
        syncBootstrapClasses()
        nativeWidget.addEventListener("click") { onClickCallback?.invoke() }
        nativeWidget.addEventListener("dblclick") { onDoubleClickCallback?.invoke() }
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
        nativeWidget.addEventListener("mouseenter") { ev ->
            val pe = (ev as? PointerEvent) ?: PointerEvent(0f, 0f)
            onPointerEnterCallback?.invoke(pe)
        }
        nativeWidget.addEventListener("mouseleave") { ev ->
            val pe = (ev as? PointerEvent) ?: PointerEvent(0f, 0f)
            onPointerLeaveCallback?.invoke(pe)
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

    protected fun syncBootstrapClasses() {
        val enumClasses = linkedSetOf<String>()
        val prefix = componentClassPrefix.trim()
        if (prefix.isNotEmpty() && prefix != "btn") {
            prefix.split(Regex("\\s+")).filter { it.isNotEmpty() }.forEach { enumClasses.add(it) }
        }
        val normalizedSnapshot = styleSnapshot.copy(type = styleSnapshot.type?.takeIf { it != BootstrapType.NONE })
        enumClasses.addAll(BootstrapClassMapper.toEnumClassSet(normalizedSnapshot))
        enumClasses.removeAll { it.endsWith("-") }
        if (!visibleFlag) {
            enumClasses.removeAll(
                setOf(
                    "d-flex",
                    "d-inline-flex",
                    "d-block",
                    "d-inline-block",
                    "d-grid",
                    "d-inline",
                    "d-table",
                    "d-table-row",
                    "d-table-cell"
                )
            )
            enumClasses.add("d-none")
        }
        if (!enabledFlag) {
            enumClasses.add("disabled")
        }
        nativeWidget.setEnumClasses(enumClasses)
    }

    protected fun currentSnapshot(): BootstrapStyleSnapshot =
        styleSnapshot.copy(customClasses = nativeWidget.classList.toSet())

    protected fun applySnapshot(snapshot: BootstrapStyleSnapshot) {
        styleSnapshot = snapshot.copy(componentPrefix = componentClassPrefix)
        syncBootstrapClasses()
    }

    override fun applyComponentBootstrap(bootstrap: ComponentBootstrap) {
        bootstrapState = bootstrap
        styleSnapshot = bootstrap.applyToSnapshot(styleSnapshot)
        syncBootstrapClasses()
    }

    override fun setClass(className: String) {
        nativeWidget.setClassName(className)
    }

    override fun getClass(): String = nativeWidget.className

    override fun getClasses(): Set<String> = nativeWidget.classList.toSet()

    override fun addClass(className: String) {
        nativeWidget.addClass(className)
    }

    override fun removeClass(className: String) {
        nativeWidget.removeClass(className)
    }

    override fun hasClass(className: String): Boolean =
        nativeWidget.hasClass(className)

    private val appliedComputedStyleKeys = mutableSetOf<String>()
    private val appliedComponentStyleKeys = mutableSetOf<String>()
    private val appliedComponentStyleProps = linkedMapOf<String, Pair<String, Boolean>>()
    private var savedDisplayWhenVisible: Pair<String, Boolean>? = null

    protected fun hasExplicitStyleProperty(property: String): Boolean {
        val key = property.trim().lowercase()
        return key in appliedComponentStyleKeys || key in appliedComputedStyleKeys
    }

    private fun setComputedStyleProp(key: String, value: String, important: Boolean = false) {
        appliedComputedStyleKeys.add(key)
        if (key == "display" && value != "none") {
            savedDisplayWhenVisible = value to important
        }
        nativeWidget.setStyle(key, value, important)
    }

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
            val savedDisplay = savedDisplayWhenVisible
            if (savedDisplay != null && ("display" in appliedComponentStyleKeys || "display" in appliedComputedStyleKeys)) {
                nativeWidget.setStyle("display", savedDisplay.first, savedDisplay.second)
            } else if ("display" !in appliedComponentStyleKeys && "display" !in appliedComputedStyleKeys) {
                nativeWidget.setStyle("display", null, false)
            }
        } else {
            nativeWidget.setStyle("display", "none", true)
        }
        syncBootstrapClasses()
    }

    override fun isVisible(): Boolean = visibleFlag

    override fun setEnabled(enabled: Boolean) {
        enabledFlag = enabled
        nativeWidget.disabled = !enabled
        if (!enabled) {
            hoveredStateFlag = false
            focusedStateFlag = false
            activePressedStateFlag = false
            nativeWidget.setAttribute("aria-disabled", "true")
        } else {
            nativeWidget.removeAttribute("aria-disabled")
        }
        syncBootstrapClasses()
    }

    override fun isEnabled(): Boolean = enabledFlag

    override fun setHoveredState(hovered: Boolean) {
        hoveredStateFlag = if (!enabledFlag) false else hovered
    }

    override fun isHoveredState(): Boolean = if (!enabledFlag) false else hoveredStateFlag

    override fun setFocusedState(focused: Boolean) {
        focusedStateFlag = if (!enabledFlag) false else focused
    }

    override fun isFocusedState(): Boolean = if (!enabledFlag) false else focusedStateFlag

    override fun setActivePressedState(active: Boolean) {
        activePressedStateFlag = if (!enabledFlag) false else active
    }

    override fun isActivePressedState(): Boolean = if (!enabledFlag) false else activePressedStateFlag

    override fun setOpacity(opacity: Float) {
        opacityValue = opacity.coerceIn(0f, 1f)
        if (opacityValue >= 1.0f && "opacity" !in appliedComponentStyleKeys) {
            nativeWidget.setStyle("opacity", null, false)
        } else {
            nativeWidget.setStyle("opacity", opacityValue.toString(), false)
        }
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

    override fun setAttribute(name: String, value: String?) {
        if (value != null) {
            nativeWidget.setAttribute(name, value)
        } else {
            nativeWidget.removeAttribute(name)
        }
    }

    override fun setPreferredSize(size: Size?) {
        preferredSizeValue = size
        if (size != null) {
            if (size.width > 0) {
                nativeWidget.clientWidth = size.width
                val explicitWidth = appliedComponentStyleProps["width"]
                if (explicitWidth != null) {
                    nativeWidget.setStyle("width", explicitWidth.first, explicitWidth.second)
                } else if ("width" !in appliedComponentStyleKeys) {
                    nativeWidget.setStyle("width", "${size.width}px", false)
                }
            } else {
                val explicitWidth = appliedComponentStyleProps["width"]
                if (explicitWidth != null) {
                    nativeWidget.setStyle("width", explicitWidth.first, explicitWidth.second)
                } else if ("width" !in appliedComputedStyleKeys && "width" !in appliedComponentStyleKeys) {
                    nativeWidget.setStyle("width", null, false)
                }
            }
            if (size.height > 0) {
                nativeWidget.clientHeight = size.height
                val explicitHeight = appliedComponentStyleProps["height"]
                if (explicitHeight != null) {
                    nativeWidget.setStyle("height", explicitHeight.first, explicitHeight.second)
                } else if ("height" !in appliedComponentStyleKeys) {
                    nativeWidget.setStyle("height", "${size.height}px", false)
                }
            } else {
                val explicitHeight = appliedComponentStyleProps["height"]
                if (explicitHeight != null) {
                    nativeWidget.setStyle("height", explicitHeight.first, explicitHeight.second)
                } else if ("height" !in appliedComputedStyleKeys && "height" !in appliedComponentStyleKeys) {
                    nativeWidget.setStyle("height", null, false)
                }
            }
        } else {
            val explicitWidth = appliedComponentStyleProps["width"]
            if (explicitWidth != null) {
                nativeWidget.setStyle("width", explicitWidth.first, explicitWidth.second)
            } else if ("width" !in appliedComputedStyleKeys && "width" !in appliedComponentStyleKeys) {
                nativeWidget.setStyle("width", null, false)
            }
            val explicitHeight = appliedComponentStyleProps["height"]
            if (explicitHeight != null) {
                nativeWidget.setStyle("height", explicitHeight.first, explicitHeight.second)
            } else if ("height" !in appliedComputedStyleKeys && "height" !in appliedComponentStyleKeys) {
                nativeWidget.setStyle("height", null, false)
            }
        }
    }

    override fun getBounds(): Rect {
        val hitTestDom = WebDomElement.activePopupHitTestDom
        if (hitTestDom != null) {
            return if (nativeWidget.containsDomElement(hitTestDom)) {
                Rect(WebDomElement.activePopupHitTestX, WebDomElement.activePopupHitTestY, 1, 1)
            } else {
                Rect(WebDomElement.activePopupHitTestX + 100000, WebDomElement.activePopupHitTestY + 100000, 0, 0)
            }
        }
        return Rect(
            nativeWidget.offsetLeft,
            nativeWidget.offsetTop,
            preferredSizeValue?.width ?: nativeWidget.clientWidth,
            preferredSizeValue?.height ?: nativeWidget.clientHeight
        )
    }

    override fun setPadding(padding: EdgeInsets) {
        if (padding == EdgeInsets.ZERO) {
            if ("padding" !in appliedComponentStyleKeys) {
                nativeWidget.setStyle("padding", null, false)
            }
        } else {
            nativeWidget.setStyle(
                "padding",
                "${padding.top}px ${padding.right}px ${padding.bottom}px ${padding.left}px",
                false
            )
        }
    }

    override fun setMargin(margin: EdgeInsets) {
        if (margin == EdgeInsets.ZERO) {
            if ("margin" !in appliedComponentStyleKeys) {
                nativeWidget.setStyle("margin", null, false)
            }
        } else {
            nativeWidget.setStyle(
                "margin",
                "${margin.top}px ${margin.right}px ${margin.bottom}px ${margin.left}px",
                false
            )
        }
    }

    override fun setBackgroundGradient(gradient: Gradient?) {
        backgroundGradientValue = gradient
        if (gradient != null) {
            nativeWidget.setStyle("background-image", gradient.toCssString(), nativeWidget.isStyleImportant("background-image"))
        } else if ("background-image" !in appliedComponentStyleKeys && "background-image" !in appliedComputedStyleKeys) {
            nativeWidget.setStyle("background-image", null, false)
        }
    }

    override fun getBackgroundGradient(): Gradient? = backgroundGradientValue

    override fun setCustomCursor(cursor: CssCursorValue?) {
        if (cursor != null) {
            val isImp = nativeWidget.isStyleImportant("cursor")
            nativeWidget.setStyle("cursor", cursor.toCssString(), isImp)
        } else if ("cursor" !in appliedComponentStyleKeys && "cursor" !in appliedComputedStyleKeys) {
            nativeWidget.setStyle("cursor", null, false)
        }
    }

    override fun applyComponentStyle(style: ComponentStyle) {
        val newProps = WebCssStyleTranslator.toInlineCssDeclarations(ComputedStyle.EMPTY, style)
        val removedKeys = appliedComponentStyleKeys.filter { it !in newProps }
        for (oldKey in removedKeys) {
            appliedComponentStyleKeys.remove(oldKey)
            appliedComponentStyleProps.remove(oldKey)
            appliedComputedStyleKeys.remove(oldKey)
            if (oldKey == "display") {
                savedDisplayWhenVisible = null
            }
            nativeWidget.setStyle(oldKey, null, false)
        }
        for ((prop, pair) in newProps) {
            val (value, isImp) = pair
            appliedComponentStyleKeys.add(prop)
            appliedComponentStyleProps[prop] = value to isImp
            if (prop == "display" && value != "none") {
                savedDisplayWhenVisible = value to isImp
            }
            nativeWidget.setStyle(prop, value, isImp)
        }
        val aspect = style.getAspectRatio()?.numericRatio
        if (aspect != null && aspect > 0f && (nativeWidget.tagName == "genericui:aspectratiocontainer" || nativeWidget.hasClass("ratio") || "ratio" in nativeWidget.enumClasses)) {
            val scaled = ((1f / aspect) * 10000f).let { kotlin.math.round(it).toInt() }
            val percentStr = if (scaled % 100 == 0) "${scaled / 100}%" else "${scaled / 100f}%"
            nativeWidget.setStyle("--bs-aspect-ratio", percentStr, false)
        }
        preferredSizeValue?.let { size ->
            if (size.width > 0 && "width" !in newProps) nativeWidget.setStyle("width", "${size.width}px", false)
            if (size.height > 0 && "height" !in newProps) nativeWidget.setStyle("height", "${size.height}px", false)
        }
        if (!visibleFlag) {
            nativeWidget.setStyle("display", "none", true)
        }
    }

    protected var lastComputedStyle: ComputedStyle = ComputedStyle()

    override fun applyComputedStyle(style: ComputedStyle) {
        lastComputedStyle = style
        // On :web, all stylesheets (genericui.css, bootstrap.min.css, application .css) are mounted
        // in <head> by WebStyleSheetManager and evaluated natively by the browser (including @media
        // queries, :hover/:focus pseudo-classes, and [data-bs-theme]). Do not stamp ComputedStyle
        // declarations as inline style="..." attributes on DOM elements.
        if (appliedComputedStyleKeys.isNotEmpty()) {
            val removedKeys = appliedComputedStyleKeys.filter { it !in appliedComponentStyleKeys }
            for (oldKey in removedKeys) {
                appliedComputedStyleKeys.remove(oldKey)
                if (oldKey == "display") {
                    savedDisplayWhenVisible = null
                }
                nativeWidget.setStyle(oldKey, null, false)
            }
            appliedComputedStyleKeys.clear()
        }

        preferredSizeValue?.let { size ->
            if (size.width > 0 && "width" !in appliedComponentStyleKeys) nativeWidget.setStyle("width", "${size.width}px", false)
            if (size.height > 0 && "height" !in appliedComponentStyleKeys) nativeWidget.setStyle("height", "${size.height}px", false)
        }
        if (!visibleFlag) {
            nativeWidget.setStyle("display", "none", true)
        }
    }

    override fun supportsNativeTransitions(): Boolean = true

    override fun configureNativeTransitions(specs: List<CssTransitionSpec>) {
        if (specs.isEmpty()) {
            nativeWidget.setStyle("transition", null, false)
        } else {
            val css = specs.joinToString(", ") { spec ->
                val tf = if (spec.timingFunction.isLinear) {
                    "linear"
                } else {
                    "cubic-bezier(${spec.timingFunction.x1}, ${spec.timingFunction.y1}, ${spec.timingFunction.x2}, ${spec.timingFunction.y2})"
                }
                "${spec.property} ${spec.durationMs}ms $tf ${spec.delayMs}ms"
            }
            nativeWidget.setStyle("transition", css, false)
        }
    }

    override fun runNativeTransition(
        property: String,
        fromValue: String?,
        toValue: String,
        spec: CssTransitionSpec,
        onComplete: () -> Unit
    ): Boolean {
        nativeWidget.setStyle(property, toValue, false)
        return true
    }

    override fun scheduleDelayed(delayMs: Int, action: () -> Unit): Boolean =
        WebBrowserDomBridge.scheduleTimeout(delayMs, action)

    override fun setOnClick(listener: (() -> Unit)?) { onClickCallback = listener }
    internal fun hasClickListener(): Boolean = onClickCallback != null
    override fun setOnDoubleClick(listener: (() -> Unit)?) { onDoubleClickCallback = listener }
    override fun setOnContextMenu(listener: ((PointerEvent) -> Unit)?) {
        onContextMenuCallback = listener
        nativeWidget.removeEventListener("contextmenu", contextMenuDomListener)
        if (listener != null) {
            nativeWidget.setAttribute("data-has-context-menu", "true")
            nativeWidget.addEventListener("contextmenu", contextMenuDomListener)
        } else {
            nativeWidget.removeAttribute("data-has-context-menu")
        }
    }
    override fun setOnPointerDown(listener: ((PointerEvent) -> Unit)?) { onPointerDownCallback = listener }
    override fun setOnPointerMove(listener: ((PointerEvent) -> Unit)?) { onPointerMoveCallback = listener }
    override fun setOnPointerUp(listener: ((PointerEvent) -> Unit)?) { onPointerUpCallback = listener }
    override fun setOnPointerEnter(listener: ((PointerEvent) -> Unit)?) { onPointerEnterCallback = listener }
    override fun setOnPointerLeave(listener: ((PointerEvent) -> Unit)?) { onPointerLeaveCallback = listener }
    override fun setOnWheel(listener: ((WheelEvent) -> Unit)?) { onWheelCallback = listener }
    override fun setOnKeyDown(listener: ((KeyEvent) -> Unit)?) { onKeyDownCallback = listener }
    internal fun hasKeyDownListener(): Boolean = onKeyDownCallback != null
    override fun setOnKeyUp(listener: ((KeyEvent) -> Unit)?) { onKeyUpCallback = listener }
    internal fun hasKeyUpListener(): Boolean = onKeyUpCallback != null

    override fun dispose() {
        nativeWidget.remove()
    }
}
