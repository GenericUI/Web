package com.nexustools.genericui.web

import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.enums.ui.Orientation
import com.nexustools.genericui.nativeimpl.lazy.LazyListNativeImpl
import com.nexustools.genericui.style.css.ComponentStyle
import com.nexustools.genericui.style.css.ComputedStyle
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Web true lazy virtualized list implementation ([LazyListNativeImpl]).
 *
 * Computes the visible item index window `[firstVisibleIndex .. lastVisibleIndex]` from the scroll offset
 * and viewport size, mounts/loads DOM nodes ONLY for items in the visible window (using top and bottom
 * spacer elements to preserve total scroll height), and unmounts/unloads DOM nodes immediately when items
 * scroll out of view.
 */
class WebLazyListImpl : WebBaseContainerNativeImpl("genericui:lazylist", "lazy-list"), LazyListNativeImpl<WebDomElement> {

    private var orientation: Orientation = Orientation.VERTICAL
    private var totalItemCount: Int = 0
    private var itemFactory: ((Int) -> NativeImpl<*>)? = null
    private var onViewportScrollChangedListener: ((Float, Float) -> Unit)? = null
    private var explicitViewportWindowProvided: Boolean = false
    private var isHandlingBrowserScroll: Boolean = false
    private var measuredAvgExtentPx: Float = 0f
    private var measurementPassScheduled: Boolean = false

    var estimatedItemExtentPx: Int = 56
    var overscanItems: Int = 3

    var firstVisibleIndex: Int = 0
        private set
    var lastVisibleIndex: Int = -1
        private set

    private val topSpacer = WebDomElement("div").apply {
        classList.add("lazy-list-spacer-start")
        style["flex-shrink"] = "0"
    }
    private val viewportHost = WebDomElement("div").apply {
        classList.add("lazy-list-viewport")
        style["width"] = "100%"
        style["max-width"] = "100%"
        style["overflow-x"] = "hidden"
        style["box-sizing"] = "border-box"
    }
    private val bottomSpacer = WebDomElement("div").apply {
        classList.add("lazy-list-spacer-end")
        style["flex-shrink"] = "0"
    }

    private val mountedItems = linkedMapOf<Int, NativeImpl<*>>()

    val mountedItemIndices: Set<Int>
        get() = mountedItems.keys.toSet()

    init {
        ensureScrollContainerStyles()
        nativeWidget.appendChild(topSpacer)
        nativeWidget.appendChild(viewportHost)
        nativeWidget.appendChild(bottomSpacer)
        WebBrowserDomBridge.attachOverscrollStretch(nativeWidget, viewportHost)
        nativeWidget.addEventListener("scroll") {
            updateMeasuredAverageExtent()
            val rawScrollOffset = if (orientation == Orientation.HORIZONTAL) {
                nativeWidget.scrollLeft
            } else {
                nativeWidget.scrollTop
            }.coerceAtLeast(0.0).toFloat()
            val rawViewportExtent = if (orientation == Orientation.HORIZONTAL) {
                val cw = nativeWidget.clientWidth
                if (cw > 0) cw else WebBrowserDomBridge.getClientWidth(nativeWidget, estimatedItemExtentPx * 10)
            } else {
                val ch = nativeWidget.clientHeight
                if (ch > 0) ch else WebBrowserDomBridge.getClientHeight(nativeWidget, estimatedItemExtentPx * 10)
            }.coerceAtLeast(estimatedItemExtentPx).toFloat()
            dispatchViewportScroll(rawScrollOffset, rawViewportExtent)
        }
        nativeWidget.addEventListener("wheel") { ev ->
            val liveDomExtent = if (orientation == Orientation.HORIZONTAL) {
                WebBrowserDomBridge.getClientWidth(nativeWidget, 0)
            } else {
                WebBrowserDomBridge.getClientHeight(nativeWidget, 0)
            }
            if (!WebBrowserDomBridge.isBrowserEnvironment || liveDomExtent <= 0) {
                val we = ev as? com.nexustools.genericui.event.WheelEvent ?: return@addEventListener
                if (orientation == Orientation.HORIZONTAL) {
                    val delta = if (we.deltaX != 0f) we.deltaX else we.deltaY
                    if (delta != 0f) {
                        val maxScroll = max(0, nativeWidget.scrollWidth - nativeWidget.clientWidth).toDouble()
                        val nextScroll = (nativeWidget.scrollLeft + delta.toDouble()).coerceIn(0.0, maxScroll)
                        nativeWidget.updateScrollFromBrowser(
                            newScrollTop = nativeWidget.scrollTop,
                            newScrollLeft = nextScroll,
                            clientW = nativeWidget.clientWidth,
                            clientH = nativeWidget.clientHeight,
                            scrollW = nativeWidget.scrollWidth,
                            scrollH = nativeWidget.scrollHeight
                        )
                        val rawExtent = nativeWidget.clientWidth.coerceAtLeast(estimatedItemExtentPx).toFloat()
                        dispatchViewportScroll(nextScroll.toFloat(), rawExtent)
                    }
                } else if (we.deltaY != 0f) {
                    val maxScroll = max(0, nativeWidget.scrollHeight - nativeWidget.clientHeight).toDouble()
                    val nextScroll = (nativeWidget.scrollTop + we.deltaY.toDouble()).coerceIn(0.0, maxScroll)
                    nativeWidget.updateScrollFromBrowser(
                        newScrollTop = nextScroll,
                        newScrollLeft = nativeWidget.scrollLeft,
                        clientW = nativeWidget.clientWidth,
                        clientH = nativeWidget.clientHeight,
                        scrollW = nativeWidget.scrollWidth,
                        scrollH = nativeWidget.scrollHeight
                    )
                    val rawExtent = nativeWidget.clientHeight.coerceAtLeast(estimatedItemExtentPx).toFloat()
                    dispatchViewportScroll(nextScroll.toFloat(), rawExtent)
                }
            }
        }
        WebBrowserDomBridge.attachLazyViewportObserver(
            scrollElement = nativeWidget,
            viewportElement = viewportHost,
            isHorizontal = { orientation == Orientation.HORIZONTAL }
        ) { scrollOffsetPx, viewportExtentPx, measuredSpanPx, renderedCount ->
            if (renderedCount > 0 && measuredSpanPx > 0) {
                calibrateMeasuredAverageExtent(measuredSpanPx, renderedCount)
            } else {
                updateMeasuredAverageExtent()
            }
            val rawExtent = viewportExtentPx.coerceAtLeast(estimatedItemExtentPx).toFloat()
            dispatchViewportScroll(scrollOffsetPx.coerceAtLeast(0.0).toFloat(), rawExtent)
        }
    }

    private fun dispatchViewportScroll(rawScrollOffset: Float, rawViewportExtent: Float) {
        isHandlingBrowserScroll = true
        try {
            val effectiveExtentPx = if (
                WebBrowserDomBridge.isBrowserEnvironment &&
                measuredAvgExtentPx > 1f
            ) {
                measuredAvgExtentPx
            } else {
                estimatedItemExtentPx.coerceAtLeast(1).toFloat()
            }
            val toEstimatedScale = if (effectiveExtentPx > 1f && estimatedItemExtentPx > 0) {
                estimatedItemExtentPx.toFloat() / effectiveExtentPx
            } else {
                1f
            }
            val listener = onViewportScrollChangedListener
            if (listener != null) {
                listener(
                    rawScrollOffset.coerceAtLeast(0f) * toEstimatedScale,
                    max(rawViewportExtent, rawViewportExtent * toEstimatedScale)
                )
            } else {
                explicitViewportWindowProvided = false
                recomputeVisibleWindow()
            }
        } finally {
            isHandlingBrowserScroll = false
        }
    }

    private fun ensureScrollContainerStyles() {
        if (orientation == Orientation.HORIZONTAL) {
            nativeWidget.setStyle("overflow-x", "auto")
            nativeWidget.setStyle("overflow-y", "hidden")
        } else {
            nativeWidget.setStyle("overflow-y", "auto")
            nativeWidget.setStyle("overflow-x", "hidden")
        }
        if (!hasExplicitStyleProperty("min-height")) {
            nativeWidget.setStyle("min-height", "0")
        }
        if (!hasExplicitStyleProperty("max-height") && !hasExplicitStyleProperty("height")) {
            nativeWidget.setStyle("max-height", "100%")
        }
        if (!hasExplicitStyleProperty("width")) {
            nativeWidget.setStyle("width", "100%")
        }
        nativeWidget.setStyle("box-sizing", "border-box")
        viewportHost.setStyle("width", "100%")
        viewportHost.setStyle("max-width", "100%")
        viewportHost.setStyle("overflow-x", "hidden")
        viewportHost.setStyle("box-sizing", "border-box")
    }

    override fun applyComponentStyle(style: ComponentStyle) {
        super<WebBaseContainerNativeImpl>.applyComponentStyle(style)
        ensureScrollContainerStyles()
    }

    override fun applyComputedStyle(style: ComputedStyle) {
        super<WebBaseContainerNativeImpl>.applyComputedStyle(style)
        ensureScrollContainerStyles()
    }

    private fun calibrateMeasuredAverageExtent(measuredSpanPx: Int, renderedCount: Int): Boolean {
        if (renderedCount <= 0 || measuredSpanPx <= 0) return false
        val avg = measuredSpanPx.toFloat() / renderedCount.toFloat()
        if (avg < 12f) return false
        val prev = measuredAvgExtentPx
        measuredAvgExtentPx = if (prev > 1f) {
            (prev * 0.65f) + (avg * 0.35f)
        } else {
            avg
        }
        return kotlin.math.abs(measuredAvgExtentPx - prev) > 1.5f
    }

    private fun updateMeasuredAverageExtent(): Boolean {
        if (!WebBrowserDomBridge.isBrowserEnvironment) return false
        val childCount = viewportHost.children.size
        if (childCount <= 0) return false
        val renderedExtent = if (orientation == Orientation.HORIZONTAL) {
            WebBrowserDomBridge.getClientWidth(viewportHost, 0)
        } else {
            WebBrowserDomBridge.getClientHeight(viewportHost, 0)
        }
        return calibrateMeasuredAverageExtent(renderedExtent, childCount)
    }

    override fun addChildNative(childImpl: NativeImpl<*>, index: Int) {
        // Items managed by LazyList are mounted inside viewportHost via recomputeVisibleWindow()
        val dom = childImpl.nativeWidget as? WebDomElement ?: return
        if (dom === topSpacer || dom === viewportHost || dom === bottomSpacer) return
        if (dom !in viewportHost.children) {
            viewportHost.appendChild(dom)
        }
    }

    override fun removeChildNative(childImpl: NativeImpl<*>) {
        val dom = childImpl.nativeWidget as? WebDomElement ?: return
        if (dom === topSpacer || dom === viewportHost || dom === bottomSpacer) return
        val keysToRemove = mountedItems.entries.filter { it.value === childImpl }.map { it.key }
        for (k in keysToRemove) {
            mountedItems.remove(k)
        }
        viewportHost.removeChild(dom)
        nativeWidget.removeChild(dom)
    }

    override fun clearChildrenNative() {
        unmountAllItems()
    }

    override fun setEstimatedItemExtent(extentPx: Float, overscanItems: Int) {
        this.estimatedItemExtentPx = extentPx.roundToInt().coerceAtLeast(1)
        this.overscanItems = overscanItems.coerceAtLeast(0)
    }

    override fun updateViewportMetrics(
        scrollOffsetPx: Float,
        viewportExtentPx: Float,
        firstActiveIndex: Int,
        lastActiveIndex: Int,
        leadingSpacerPx: Float,
        trailingSpacerPx: Float,
        totalContentExtentPx: Float
    ) {
        explicitViewportWindowProvided = true
        firstVisibleIndex = firstActiveIndex
        lastVisibleIndex = lastActiveIndex
        val fromEstimatedScale = if (
            WebBrowserDomBridge.isBrowserEnvironment &&
            measuredAvgExtentPx > 1f &&
            estimatedItemExtentPx > 0
        ) {
            measuredAvgExtentPx / estimatedItemExtentPx.toFloat()
        } else {
            1f
        }
        val scaledLeading = (leadingSpacerPx * fromEstimatedScale).roundToInt().coerceAtLeast(0)
        val scaledTrailing = (trailingSpacerPx * fromEstimatedScale).roundToInt().coerceAtLeast(0)
        val scaledTotal = (totalContentExtentPx * fromEstimatedScale).roundToInt().coerceAtLeast(0)
        if (orientation == Orientation.HORIZONTAL) {
            if (!isHandlingBrowserScroll) {
                nativeWidget.scrollLeft = (scrollOffsetPx * fromEstimatedScale).toDouble()
            }
            topSpacer.style["width"] = "${scaledLeading}px"
            bottomSpacer.style["width"] = "${scaledTrailing}px"
            nativeWidget.scrollWidth = scaledTotal
        } else {
            if (!isHandlingBrowserScroll) {
                nativeWidget.scrollTop = (scrollOffsetPx * fromEstimatedScale).toDouble()
            }
            topSpacer.style["height"] = "${scaledLeading}px"
            bottomSpacer.style["height"] = "${scaledTrailing}px"
            nativeWidget.scrollHeight = scaledTotal
        }
    }

    override fun setOnViewportScrollChanged(listener: ((scrollOffsetPx: Float, viewportExtentPx: Float) -> Unit)?) {
        this.onViewportScrollChangedListener = listener
    }

    override fun setOrientation(orientation: Orientation) {
        val changed = this.orientation != orientation
        this.orientation = orientation
        ensureScrollContainerStyles()
        if (changed) {
            recomputeVisibleWindow()
        }
    }

    override fun setItemCount(count: Int, itemFactory: (index: Int) -> NativeImpl<*>) {
        if (!explicitViewportWindowProvided) {
            unmountAllItems()
        }
        this.totalItemCount = count.coerceAtLeast(0)
        this.itemFactory = itemFactory
        recomputeVisibleWindow()
    }

    override fun notifyItemRangeChanged(startIndex: Int, count: Int) {
        val endIndex = startIndex + count - 1
        for (idx in startIndex..endIndex) {
            val old = mountedItems.remove(idx) ?: continue
            (old.nativeWidget as? WebDomElement)?.let { viewportHost.removeChild(it) }
        }
        recomputeVisibleWindow()
    }

    override fun scrollToIndex(index: Int) {
        val clamped = if (totalItemCount > 0) index.coerceIn(0, totalItemCount - 1) else index.coerceAtLeast(0)
        val contextIdx = (clamped - 1).coerceAtLeast(0)
        val fromEstimatedScale = if (
            WebBrowserDomBridge.isBrowserEnvironment &&
            measuredAvgExtentPx > 1f &&
            estimatedItemExtentPx > 0
        ) {
            measuredAvgExtentPx / estimatedItemExtentPx.toFloat()
        } else {
            1f
        }
        val targetOffset = (contextIdx * estimatedItemExtentPx * fromEstimatedScale).toDouble()
        if (orientation == Orientation.HORIZONTAL) {
            nativeWidget.scrollLeft = targetOffset
        } else {
            nativeWidget.scrollTop = targetOffset
        }
        if (totalItemCount > 0) {
            explicitViewportWindowProvided = false
            recomputeVisibleWindow()
        }
    }

    fun recomputeVisibleWindow() {
        val factory = itemFactory
        if (factory == null || totalItemCount <= 0) {
            unmountAllItems()
            firstVisibleIndex = 0
            lastVisibleIndex = -1
            topSpacer.style["height"] = "0px"
            bottomSpacer.style["height"] = "0px"
            return
        }

        val wasExplicitWindow = explicitViewportWindowProvided
        if (!wasExplicitWindow) {
            val scrollOffset = if (orientation == Orientation.HORIZONTAL) {
                nativeWidget.scrollLeft
            } else {
                nativeWidget.scrollTop
            }.coerceAtLeast(0.0)

            val viewportExtent = if (orientation == Orientation.HORIZONTAL) {
                nativeWidget.clientWidth
            } else {
                nativeWidget.clientHeight
            }.coerceAtLeast(estimatedItemExtentPx)

            val extent = max(1, estimatedItemExtentPx).toDouble()
            val rawFirst = floor(scrollOffset / extent).toInt() - overscanItems
            val rawLast = ceil((scrollOffset + viewportExtent) / extent).toInt() + overscanItems

            firstVisibleIndex = rawFirst.coerceIn(0, totalItemCount - 1)
            lastVisibleIndex = min(totalItemCount - 1, max(firstVisibleIndex, rawLast))

            val leadingPx = firstVisibleIndex * estimatedItemExtentPx
            val trailingPx = max(0, (totalItemCount - 1 - lastVisibleIndex) * estimatedItemExtentPx)

            if (orientation == Orientation.HORIZONTAL) {
                topSpacer.style["width"] = "${leadingPx}px"
                bottomSpacer.style["width"] = "${trailingPx}px"
                nativeWidget.scrollWidth = totalItemCount * estimatedItemExtentPx
            } else {
                topSpacer.style["height"] = "${leadingPx}px"
                bottomSpacer.style["height"] = "${trailingPx}px"
                nativeWidget.scrollHeight = totalItemCount * estimatedItemExtentPx
            }
        }
        explicitViewportWindowProvided = false

        val visibleRange = if (lastVisibleIndex >= firstVisibleIndex) {
            firstVisibleIndex..lastVisibleIndex
        } else {
            IntRange.EMPTY
        }

        // Unmount DOM nodes that scrolled out of the visible window
        val outOfView = mountedItems.keys.filter { it !in visibleRange }
        for (idx in outOfView) {
            val impl = mountedItems.remove(idx) ?: continue
            (impl.nativeWidget as? WebDomElement)?.let { viewportHost.removeChild(it) }
        }

        // Mount DOM nodes for visible items in ascending index order while preserving already-mounted nodes
        val desiredDoms = ArrayList<WebDomElement>(if (visibleRange.isEmpty()) 0 else (visibleRange.last - visibleRange.first + 1))
        for (idx in visibleRange) {
            val impl = if (wasExplicitWindow) {
                factory(idx).also { mountedItems[idx] = it }
            } else {
                mountedItems.getOrPut(idx) { factory(idx) }
            }
            val dom = impl.nativeWidget as? WebDomElement ?: continue
            dom.setAttribute("data-lazy-index", idx.toString())
            desiredDoms.add(dom)
        }

        // Remove any stray children not in desiredDoms
        val desiredSet = desiredDoms.toHashSet()
        val stray = viewportHost.children.filter { it !in desiredSet }
        for (s in stray) {
            viewportHost.removeChild(s)
        }

        // Ensure exact ascending order in viewportHost without clearing already-ordered children
        for ((targetPos, dom) in desiredDoms.withIndex()) {
            val currentAtPos = viewportHost.children.getOrNull(targetPos)
            if (currentAtPos !== dom) {
                viewportHost.insertChildAt(targetPos, dom)
            }
        }

        if (WebBrowserDomBridge.isBrowserEnvironment && !measurementPassScheduled && desiredDoms.isNotEmpty()) {
            measurementPassScheduled = true
            WebBrowserDomBridge.schedulePostLayoutPass {
                measurementPassScheduled = false
                if (updateMeasuredAverageExtent()) {
                    val scale = if (estimatedItemExtentPx > 0) measuredAvgExtentPx / estimatedItemExtentPx.toFloat() else 1f
                    val leadingPx = (firstVisibleIndex * estimatedItemExtentPx * scale).roundToInt().coerceAtLeast(0)
                    val trailingPx = (max(0, totalItemCount - 1 - lastVisibleIndex) * estimatedItemExtentPx * scale).roundToInt().coerceAtLeast(0)
                    if (orientation == Orientation.HORIZONTAL) {
                        topSpacer.style["width"] = "${leadingPx}px"
                        bottomSpacer.style["width"] = "${trailingPx}px"
                    } else {
                        topSpacer.style["height"] = "${leadingPx}px"
                        bottomSpacer.style["height"] = "${trailingPx}px"
                    }
                }
                val liveViewportExtent = if (orientation == Orientation.HORIZONTAL) {
                    WebBrowserDomBridge.getClientWidth(nativeWidget, 0)
                } else {
                    WebBrowserDomBridge.getClientHeight(nativeWidget, 0)
                }
                val effectiveExtent = if (measuredAvgExtentPx > 1f) measuredAvgExtentPx.roundToInt() else estimatedItemExtentPx
                val currentMountedSpanPx = (lastVisibleIndex - firstVisibleIndex + 1).coerceAtLeast(0) * effectiveExtent
                if (liveViewportExtent > currentMountedSpanPx && lastVisibleIndex < totalItemCount - 1) {
                    val rawScrollOffset = if (orientation == Orientation.HORIZONTAL) {
                        nativeWidget.scrollLeft
                    } else {
                        nativeWidget.scrollTop
                    }.coerceAtLeast(0.0).toFloat()
                    dispatchViewportScroll(rawScrollOffset, liveViewportExtent.toFloat())
                }
            }
        }
    }

    private fun unmountAllItems() {
        viewportHost.clearChildren()
        mountedItems.clear()
    }

    override fun dispose() {
        unmountAllItems()
        super.dispose()
    }
}
