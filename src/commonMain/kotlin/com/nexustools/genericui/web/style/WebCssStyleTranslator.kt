package com.nexustools.genericui.web.style

import com.nexustools.genericui.enums.bootstrap.AlignItems
import com.nexustools.genericui.enums.bootstrap.AlignSelf
import com.nexustools.genericui.enums.bootstrap.FlexDirection
import com.nexustools.genericui.enums.bootstrap.FlexWrap
import com.nexustools.genericui.enums.bootstrap.JustifyContent
import com.nexustools.genericui.enums.bootstrap.OverflowBehavior
import com.nexustools.genericui.enums.bootstrap.TextAlignment
import com.nexustools.genericui.enums.bootstrap.TextOverflow
import com.nexustools.genericui.enums.bootstrap.TextTransform
import com.nexustools.genericui.enums.ui.ObjectFit
import com.nexustools.genericui.style.bootstrap.ComponentBootstrap
import com.nexustools.genericui.style.css.ComponentStyle
import com.nexustools.genericui.style.css.ComputedStyle
import com.nexustools.genericui.style.css.CssLength
import com.nexustools.genericui.style.css.CssProperty

/**
 * Translates strongly-typed [ComponentStyle], [ComputedStyle], and [ComponentBootstrap] models
 * into standard W3C CSS inline declarations (`style="..."`) and Bootstrap 5 CSS class sets for `:web`.
 */
object WebCssStyleTranslator {

    /**
     * Translates all strongly-typed properties on [componentStyle] (and fallback properties on [style])
     * into a map of `cssPropertyName -> (cssValueString to isImportant)`.
     */
    fun toInlineCssDeclarations(
        style: ComputedStyle,
        componentStyle: ComponentStyle
    ): Map<String, Pair<String, Boolean>> {
        val out = linkedMapOf<String, Pair<String, Boolean>>()

        if (!componentStyle.isEmpty()) {
            for (prop in CssProperty.entries) {
                val cssValue = getComponentStylePropertyCssString(componentStyle, style, prop)
                if (cssValue != null) {
                    val isImp = componentStyle.isImportant(prop) || style.isImportant(prop)
                    out[prop.cssName] = cssValue to isImp
                }
            }
        }

        if (style != ComputedStyle.EMPTY) {
            appendComputedStyleFallbacks(style, componentStyle, out)
        }

        return out
    }

    /**
     * Translates all strongly-typed properties on [componentStyle] into a map of
     * `cssPropertyName -> (cssValueString to isImportant)`.
     */
    fun toInlineCssDeclarations(componentStyle: ComponentStyle): Map<String, Pair<String, Boolean>> =
        toInlineCssDeclarations(ComputedStyle.EMPTY, componentStyle)

    /**
     * Formats [style] and [componentStyle] as a standard inline CSS string (`prop: value [!important]; ...`).
     */
    fun toInlineCssString(style: ComputedStyle, componentStyle: ComponentStyle): String =
        toInlineCssDeclarations(style, componentStyle).entries.joinToString("; ") { (prop, pair) ->
            val (value, isImp) = pair
            if (isImp) "$prop: $value !important" else "$prop: $value"
        }

    /**
     * Formats [componentStyle] as a standard inline CSS string (`prop: value [!important]; ...`).
     */
    fun toInlineCssString(componentStyle: ComponentStyle): String =
        toInlineCssString(ComputedStyle.EMPTY, componentStyle)

    /**
     * Translates [bootstrap] into its canonical set of Bootstrap 5 CSS class tokens.
     */
    fun toBootstrapClassSet(bootstrap: ComponentBootstrap): Set<String> =
        bootstrap.toEnumClassSet()

    /**
     * Translates [bootstrap] into its canonical set of Bootstrap 5 CSS class tokens using [componentPrefix].
     */
    fun toBootstrapClassSet(bootstrap: ComponentBootstrap, componentPrefix: String): Set<String> =
        bootstrap.toEnumClassSet(componentPrefix)

    private fun getComponentStylePropertyCssString(
        componentStyle: ComponentStyle,
        style: ComputedStyle,
        prop: CssProperty
    ): String? = when (prop) {
        CssProperty.BACKGROUND -> componentStyle.getBackgroundImage()?.toCssString()
            ?: componentStyle.getBackgroundColor()?.toCssRgbaString()
        CssProperty.BACKGROUND_COLOR -> componentStyle.getBackgroundColor()?.toCssRgbaString()
        CssProperty.BACKGROUND_IMAGE -> componentStyle.getBackgroundImage()?.toCssString()
        CssProperty.BACKGROUND_SIZE -> componentStyle.getBackgroundSize()?.toCssString()
        CssProperty.BACKGROUND_POSITION -> componentStyle.getBackgroundPosition()?.toCssString()
        CssProperty.BACKGROUND_REPEAT -> componentStyle.getBackgroundRepeat()?.toCssString()
        CssProperty.BACKGROUND_ATTACHMENT -> componentStyle.getBackgroundAttachment()?.toCssString()
        CssProperty.BACKGROUND_CLIP -> componentStyle.getBackgroundClip()?.toCssString()
        CssProperty.BACKGROUND_ORIGIN -> componentStyle.getBackgroundOrigin()?.toCssString()
        CssProperty.COLOR -> componentStyle.getTextColor()?.toCssRgbaString()
        CssProperty.ACCENT_COLOR -> componentStyle.getAccentColor()?.toCssRgbaString()
        CssProperty.OPACITY -> componentStyle.getOpacity()?.toString()
        CssProperty.DISPLAY -> componentStyle.getDisplay()?.toCssString()
        CssProperty.VISIBILITY -> componentStyle.getVisibility()?.toCssString()
        CssProperty.POSITION -> componentStyle.getPosition()?.toCssString()
        CssProperty.TOP -> componentStyle.getTop()?.toCssString()
        CssProperty.RIGHT -> componentStyle.getRight()?.toCssString()
        CssProperty.BOTTOM -> componentStyle.getBottom()?.toCssString()
        CssProperty.LEFT -> componentStyle.getLeft()?.toCssString()
        CssProperty.WIDTH -> componentStyle.getWidth()?.toCssString()
        CssProperty.HEIGHT -> componentStyle.getHeight()?.toCssString()
        CssProperty.MIN_WIDTH -> componentStyle.getMinWidth()?.toCssString()
        CssProperty.MIN_HEIGHT -> componentStyle.getMinHeight()?.toCssString()
        CssProperty.MAX_WIDTH -> componentStyle.getMaxWidth()?.toCssString()
        CssProperty.MAX_HEIGHT -> componentStyle.getMaxHeight()?.toCssString()
        CssProperty.MARGIN -> componentStyle.getMargin()?.toCssString()
        CssProperty.MARGIN_TOP -> componentStyle.getMarginTop()?.toCssString()
        CssProperty.MARGIN_RIGHT -> componentStyle.getMarginRight()?.toCssString()
        CssProperty.MARGIN_BOTTOM -> componentStyle.getMarginBottom()?.toCssString()
        CssProperty.MARGIN_LEFT -> componentStyle.getMarginLeft()?.toCssString()
        CssProperty.PADDING -> componentStyle.getPadding()?.toCssString()
        CssProperty.PADDING_TOP -> componentStyle.getPaddingTop()?.toCssString()
        CssProperty.PADDING_RIGHT -> componentStyle.getPaddingRight()?.toCssString()
        CssProperty.PADDING_BOTTOM -> componentStyle.getPaddingBottom()?.toCssString()
        CssProperty.PADDING_LEFT -> componentStyle.getPaddingLeft()?.toCssString()
        CssProperty.BOX_SIZING -> componentStyle.getBoxSizing()?.toCssString()
        CssProperty.FLOAT -> componentStyle.getFloatSide()?.toCssString()
        CssProperty.CLEAR -> componentStyle.getClearSide()?.toCssString()
        CssProperty.BORDER -> {
            val w = componentStyle.getBorderWidth()?.toCssString()
            val s = componentStyle.getBorderStyle()?.toCssString()
            val c = componentStyle.getBorderColor()?.toCssRgbaString()
            if (w != null || s != null || c != null) listOfNotNull(w, s, c).joinToString(" ") else null
        }
        CssProperty.BORDER_WIDTH -> componentStyle.getBorderWidth()?.toCssString()
        CssProperty.BORDER_STYLE -> componentStyle.getBorderStyle()?.toCssString()
        CssProperty.BORDER_COLOR -> componentStyle.getBorderColor()?.toCssRgbaString()
        CssProperty.BORDER_RADIUS -> componentStyle.getBorderRadius()?.toCssString()
        CssProperty.OUTLINE -> {
            val w = style.outlineWidthPx?.let { CssLength.px(it).toCssString() }
            val s = style.outlineStyle
            val c = style.outlineColor?.toCssRgbaString()
            if (w != null || s != null || c != null) listOfNotNull(w, s, c).joinToString(" ") else null
        }
        CssProperty.OUTLINE_WIDTH -> style.outlineWidthPx?.let { CssLength.px(it).toCssString() }
        CssProperty.OUTLINE_STYLE -> style.outlineStyle
        CssProperty.OUTLINE_COLOR -> style.outlineColor?.toCssRgbaString()
        CssProperty.BOX_SHADOW -> componentStyle.getBoxShadow()?.toCssString()
        CssProperty.TEXT_SHADOW -> componentStyle.getTextShadow()?.toCssString()
        CssProperty.FONT_FAMILY -> componentStyle.getFontFamily()?.toCssString()
        CssProperty.FONT_SIZE -> componentStyle.getFontSize()?.toCssString()
        CssProperty.FONT_WEIGHT -> (componentStyle.fontWeightNumeric ?: componentStyle.getFontWeight()?.numericWeight)?.toString()
        CssProperty.FONT_STYLE -> componentStyle.getFontStyle()?.toCssString()
        CssProperty.LINE_HEIGHT -> componentStyle.getLineHeight()?.toCssString()
        CssProperty.LETTER_SPACING -> componentStyle.getLetterSpacing()?.toCssString()
        CssProperty.TEXT_ALIGN -> formatTextAlign(componentStyle.getTextAlign())
        CssProperty.TEXT_DECORATION -> componentStyle.getTextDecoration()?.toCssString()
        CssProperty.TEXT_TRANSFORM -> formatTextTransform(componentStyle.getTextTransform())
        CssProperty.TEXT_OVERFLOW -> formatTextOverflow(componentStyle.getTextOverflow())
        CssProperty.WHITE_SPACE -> componentStyle.getWhiteSpace()?.toCssString()
        CssProperty.WORD_BREAK -> componentStyle.getWordBreak()?.toCssString()
        CssProperty.VERTICAL_ALIGN -> componentStyle.getVerticalAlign()?.toCssString()
        CssProperty.FLEX_DIRECTION -> formatFlexDirection(componentStyle.getFlexDirection())
        CssProperty.FLEX_WRAP -> formatFlexWrap(componentStyle.getFlexWrap())
        CssProperty.JUSTIFY_CONTENT -> formatJustifyContent(componentStyle.getJustifyContent())
        CssProperty.ALIGN_ITEMS -> formatAlignItems(componentStyle.getAlignItems())
        CssProperty.ALIGN_SELF -> formatAlignSelf(componentStyle.getAlignSelf())
        CssProperty.ALIGN_CONTENT -> componentStyle.getAlignContent()?.toCssString()
        CssProperty.FLEX_GROW -> componentStyle.getFlexGrow()?.toString()
        CssProperty.FLEX_SHRINK -> componentStyle.getFlexShrink()?.toString()
        CssProperty.FLEX_BASIS -> componentStyle.getFlexBasis()?.toCssString()
        CssProperty.ORDER -> style.order?.toString()
        CssProperty.GAP -> componentStyle.getGap()?.toCssString()
        CssProperty.ROW_GAP -> componentStyle.getRowGap()?.toCssString()
        CssProperty.COLUMN_GAP -> componentStyle.getColumnGap()?.toCssString()
        CssProperty.OVERFLOW -> formatOverflow(componentStyle.getOverflow())
        CssProperty.OVERFLOW_X -> formatOverflow(componentStyle.getOverflowX())
        CssProperty.OVERFLOW_Y -> formatOverflow(componentStyle.getOverflowY())
        CssProperty.CURSOR -> componentStyle.getCursor()?.toCssString()
        CssProperty.POINTER_EVENTS -> componentStyle.getPointerEvents()?.toCssString()
        CssProperty.USER_SELECT -> componentStyle.getUserSelect()?.toCssString()
        CssProperty.OBJECT_FIT -> formatObjectFit(componentStyle.getObjectFit())
        CssProperty.ASPECT_RATIO -> componentStyle.getAspectRatio()?.toCssString()
        CssProperty.Z_INDEX -> componentStyle.getZIndex()?.toString()
        CssProperty.TRANSFORM -> componentStyle.getTransform()?.toCssString()
        CssProperty.TRANSFORM_ORIGIN -> componentStyle.getTransformOrigin()?.toCssString()
        CssProperty.TRANSITION -> componentStyle.getTransitions()?.joinToString(", ") { it.toCssString() }
        CssProperty.FILTER -> componentStyle.getFilter()?.toCssString()
        CssProperty.BACKDROP_FILTER -> componentStyle.getBackdropFilter()?.toCssString()
    }

    private fun appendComputedStyleFallbacks(
        style: ComputedStyle,
        componentStyle: ComponentStyle,
        out: MutableMap<String, Pair<String, Boolean>>
    ) {
        fun putIfAbsent(prop: CssProperty, value: String?) {
            if (value != null && prop.cssName !in out) {
                val isImp = componentStyle.isImportant(prop) || style.isImportant(prop)
                out[prop.cssName] = value to isImp
            }
        }

        putIfAbsent(CssProperty.BACKGROUND_COLOR, style.backgroundColor?.toCssRgbaString())
        val bgImage = when {
            style.backgroundGradient != null -> style.backgroundGradient?.toCssString()
            style.backgroundImageUrl != null -> {
                val raw = style.backgroundImageUrl!!.trim()
                if (raw.startsWith("url(", ignoreCase = true)) raw else "url(\"$raw\")"
            }
            else -> null
        }
        putIfAbsent(CssProperty.BACKGROUND_IMAGE, bgImage)
        putIfAbsent(CssProperty.BACKGROUND_REPEAT, style.backgroundRepeat)
        putIfAbsent(CssProperty.BACKGROUND_ATTACHMENT, style.backgroundAttachment)
        putIfAbsent(CssProperty.BACKGROUND_CLIP, style.backgroundClip?.toCssString())
        putIfAbsent(CssProperty.BACKGROUND_ORIGIN, style.backgroundOrigin?.toCssString())
        putIfAbsent(CssProperty.COLOR, style.textColor?.toCssRgbaString())
        putIfAbsent(CssProperty.ACCENT_COLOR, style.accentColor?.toCssRgbaString())
        if (style.opacity != 1.0f || style.hasProperty(CssProperty.OPACITY)) {
            putIfAbsent(CssProperty.OPACITY, style.opacity.toString())
        }
        putIfAbsent(CssProperty.DISPLAY, style.display)
        putIfAbsent(CssProperty.VISIBILITY, style.visibility?.toCssString())
        putIfAbsent(CssProperty.POSITION, style.position)
        putIfAbsent(CssProperty.TOP, style.top)
        putIfAbsent(CssProperty.RIGHT, style.right)
        putIfAbsent(CssProperty.BOTTOM, style.bottom)
        putIfAbsent(CssProperty.LEFT, style.left)
        putIfAbsent(CssProperty.WIDTH, style.width)
        putIfAbsent(CssProperty.HEIGHT, style.height)
        putIfAbsent(CssProperty.MIN_WIDTH, style.minWidth)
        putIfAbsent(CssProperty.MIN_HEIGHT, style.minHeight)
        putIfAbsent(CssProperty.MAX_WIDTH, style.maxWidth)
        putIfAbsent(CssProperty.MAX_HEIGHT, style.maxHeight)
        putIfAbsent(CssProperty.MARGIN, style.margin?.toCssString())
        putIfAbsent(CssProperty.MARGIN_TOP, style.marginTop?.toCssString())
        putIfAbsent(CssProperty.MARGIN_RIGHT, style.marginRight?.toCssString())
        putIfAbsent(CssProperty.MARGIN_BOTTOM, style.marginBottom?.toCssString())
        putIfAbsent(CssProperty.MARGIN_LEFT, style.marginLeft?.toCssString())
        putIfAbsent(CssProperty.PADDING, style.padding?.toCssString())
        putIfAbsent(CssProperty.PADDING_TOP, style.paddingTop?.toCssString())
        putIfAbsent(CssProperty.PADDING_RIGHT, style.paddingRight?.toCssString())
        putIfAbsent(CssProperty.PADDING_BOTTOM, style.paddingBottom?.toCssString())
        putIfAbsent(CssProperty.PADDING_LEFT, style.paddingLeft?.toCssString())
        putIfAbsent(CssProperty.BOX_SIZING, style.boxSizing?.toCssString())
        putIfAbsent(CssProperty.FLOAT, style.floatSide)
        putIfAbsent(CssProperty.CLEAR, style.clearSide)
        putIfAbsent(CssProperty.BORDER_WIDTH, style.borderWidthPx?.let { CssLength.px(it).toCssString() })
        putIfAbsent(CssProperty.BORDER_STYLE, style.borderStyle)
        putIfAbsent(CssProperty.BORDER_COLOR, style.borderColor?.toCssRgbaString())
        val radiusStr = style.borderRadius?.toCssString()
            ?: style.borderRadiusPx?.let { CssLength.px(it).toCssString() }
        putIfAbsent(CssProperty.BORDER_RADIUS, radiusStr)
        putIfAbsent(CssProperty.BOX_SHADOW, style.boxShadowList?.toCssString() ?: style.boxShadow)
        putIfAbsent(CssProperty.TEXT_SHADOW, style.textShadowList?.toCssString() ?: style.textShadow)
        putIfAbsent(CssProperty.FONT_FAMILY, style.fontFamily)
        putIfAbsent(CssProperty.FONT_SIZE, style.fontSizePx?.let { CssLength.px(it).toCssString() })
        putIfAbsent(CssProperty.FONT_WEIGHT, style.fontWeightNumeric?.toString())
        putIfAbsent(CssProperty.FONT_STYLE, style.fontStyle?.toCssString())
        putIfAbsent(CssProperty.LINE_HEIGHT, style.lineHeight?.toCssString())
        putIfAbsent(CssProperty.LETTER_SPACING, style.letterSpacing?.toCssString())
        putIfAbsent(CssProperty.TEXT_ALIGN, style.textAlign)
        putIfAbsent(CssProperty.TEXT_DECORATION, style.textDecoration?.toCssString())
        putIfAbsent(CssProperty.TEXT_TRANSFORM, formatTextTransform(style.textTransform))
        putIfAbsent(CssProperty.TEXT_OVERFLOW, formatTextOverflow(style.textOverflow))
        putIfAbsent(CssProperty.WHITE_SPACE, style.whiteSpace)
        putIfAbsent(CssProperty.WORD_BREAK, style.wordBreak?.toCssString())
        putIfAbsent(CssProperty.VERTICAL_ALIGN, style.verticalAlign)
        putIfAbsent(CssProperty.FLEX_DIRECTION, formatFlexDirection(style.flexDirection))
        putIfAbsent(CssProperty.FLEX_WRAP, formatFlexWrap(style.flexWrap))
        putIfAbsent(CssProperty.JUSTIFY_CONTENT, formatJustifyContent(style.justifyContent))
        putIfAbsent(CssProperty.ALIGN_ITEMS, formatAlignItems(style.alignItems))
        putIfAbsent(CssProperty.ALIGN_SELF, formatAlignSelf(style.alignSelf))
        putIfAbsent(CssProperty.ALIGN_CONTENT, style.alignContent?.toCssString())
        putIfAbsent(CssProperty.FLEX_GROW, style.flexGrow?.toString())
        putIfAbsent(CssProperty.FLEX_SHRINK, style.flexShrink?.toString())
        putIfAbsent(CssProperty.FLEX_BASIS, style.flexBasis?.toCssString())
        putIfAbsent(CssProperty.ORDER, style.order?.toString())
        putIfAbsent(CssProperty.GAP, style.gap?.toCssString())
        putIfAbsent(CssProperty.ROW_GAP, style.rowGap?.toCssString())
        putIfAbsent(CssProperty.COLUMN_GAP, style.columnGap?.toCssString())
        putIfAbsent(CssProperty.OVERFLOW, style.overflow)
        putIfAbsent(CssProperty.OVERFLOW_X, formatOverflow(style.overflowX))
        putIfAbsent(CssProperty.OVERFLOW_Y, formatOverflow(style.overflowY))
        putIfAbsent(CssProperty.CURSOR, style.customCursor?.toCssString() ?: style.cursor)
        if (!style.pointerEventsEnabled || style.hasProperty(CssProperty.POINTER_EVENTS)) {
            putIfAbsent(CssProperty.POINTER_EVENTS, if (style.pointerEventsEnabled) "auto" else "none")
        }
        putIfAbsent(CssProperty.USER_SELECT, style.userSelect?.toCssString())
        putIfAbsent(CssProperty.OBJECT_FIT, formatObjectFit(style.objectFit))
        putIfAbsent(CssProperty.ASPECT_RATIO, style.aspectRatio?.toString())
        putIfAbsent(CssProperty.Z_INDEX, style.zIndex?.toString())
        putIfAbsent(CssProperty.TRANSFORM, style.transform2D?.toCssString() ?: style.transform)
        putIfAbsent(CssProperty.TRANSFORM_ORIGIN, style.transformOrigin?.toCssString())
        val transitionStr = style.transitions?.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.toCssString() }
            ?: style.transition
        putIfAbsent(CssProperty.TRANSITION, transitionStr)
        putIfAbsent(CssProperty.FILTER, style.filter?.toCssString())
        putIfAbsent(CssProperty.BACKDROP_FILTER, style.backdropFilter?.toCssString())
    }

    private fun formatTextAlign(align: TextAlignment?): String? = when (align) {
        TextAlignment.START -> "start"
        TextAlignment.CENTER -> "center"
        TextAlignment.END -> "end"
        TextAlignment.JUSTIFY -> "justify"
        null -> null
    }

    private fun formatTextTransform(transform: TextTransform?): String? = when (transform) {
        TextTransform.NONE -> "none"
        TextTransform.LOWERCASE -> "lowercase"
        TextTransform.UPPERCASE -> "uppercase"
        TextTransform.CAPITALIZE -> "capitalize"
        null -> null
    }

    private fun formatTextOverflow(overflow: TextOverflow?): String? = when (overflow) {
        TextOverflow.ELLIPSIS -> "ellipsis"
        TextOverflow.NOWRAP -> "clip"
        TextOverflow.WRAP -> "clip"
        null -> null
    }

    private fun formatFlexDirection(direction: FlexDirection?): String? = when (direction) {
        FlexDirection.ROW -> "row"
        FlexDirection.ROW_REVERSE -> "row-reverse"
        FlexDirection.COLUMN -> "column"
        FlexDirection.COLUMN_REVERSE -> "column-reverse"
        null -> null
    }

    private fun formatFlexWrap(wrap: FlexWrap?): String? = when (wrap) {
        FlexWrap.NOWRAP -> "nowrap"
        FlexWrap.WRAP -> "wrap"
        FlexWrap.WRAP_REVERSE -> "wrap-reverse"
        null -> null
    }

    private fun formatJustifyContent(justify: JustifyContent?): String? = when (justify) {
        JustifyContent.START -> "flex-start"
        JustifyContent.END -> "flex-end"
        JustifyContent.CENTER -> "center"
        JustifyContent.BETWEEN -> "space-between"
        JustifyContent.AROUND -> "space-around"
        JustifyContent.EVENLY -> "space-evenly"
        null -> null
    }

    private fun formatAlignItems(align: AlignItems?): String? = when (align) {
        AlignItems.START -> "flex-start"
        AlignItems.END -> "flex-end"
        AlignItems.CENTER -> "center"
        AlignItems.BASELINE -> "baseline"
        AlignItems.STRETCH -> "stretch"
        null -> null
    }

    private fun formatAlignSelf(align: AlignSelf?): String? = when (align) {
        AlignSelf.AUTO -> "auto"
        AlignSelf.START -> "flex-start"
        AlignSelf.END -> "flex-end"
        AlignSelf.CENTER -> "center"
        AlignSelf.BASELINE -> "baseline"
        AlignSelf.STRETCH -> "stretch"
        null -> null
    }

    private fun formatOverflow(overflow: OverflowBehavior?): String? = when (overflow) {
        OverflowBehavior.AUTO -> "auto"
        OverflowBehavior.HIDDEN -> "hidden"
        OverflowBehavior.VISIBLE -> "visible"
        OverflowBehavior.SCROLL -> "scroll"
        null -> null
    }

    private fun formatObjectFit(fit: ObjectFit?): String? = when (fit) {
        ObjectFit.CONTAIN -> "contain"
        ObjectFit.COVER -> "cover"
        ObjectFit.FILL -> "fill"
        ObjectFit.SCALE_DOWN -> "scale-down"
        ObjectFit.NONE -> "none"
        null -> null
    }
}
