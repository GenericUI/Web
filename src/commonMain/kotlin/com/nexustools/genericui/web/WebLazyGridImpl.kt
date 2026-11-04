package com.nexustools.genericui.web

import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.nativeimpl.lazy.LazyGridNativeImpl
import com.nexustools.genericui.style.css.ComponentStyle
import com.nexustools.genericui.style.css.ComputedStyle
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Web true lazy virtualized grid implementation ([LazyGridNativeImpl]).
 *
 * Computes the visible row and item index window `[firstVisibleIndex .. lastVisibleIndex]` from scroll offset
 * and viewport height, mounts/loads DOM nodes ONLY for items in the visible window (using top and bottom
 * spacer padding for total scroll height), and unmounts/unloads DOM nodes as soon as items scroll out of view.
 * Also supports Android-style boundary overscroll stretch via [WebBrowserDomBridge.attachOverscrollStretch].
 */
class WebLazyGridImpl : WebBaseContainerNativeImpl("genericui:lazygrid", "lazy-grid"), LazyGridNativeImpl<WebDomElement> {

    private var columnsCount: Int = 3
    private var totalItemCount: Int = 0
    private var itemFactory: ((Int) -> NativeImpl<*>)? = null
    private var onViewportScrollChangedListener: ((Float, Float) -> Unit)? = null
    private var explicitViewportWindowProvided: Boolean = false
    private var isHandlingBrowserScroll: Boolean = false
    private var measuredAvgRowHeightPx: Float = 0f
    private var measurementPassScheduled: Boolean = false

    var estimatedRowHeightPx: Int = 120
    var overscanRows: Int = 2

    var firstVisibleIndex: Int = 0
        private set
    var lastVisibleIndex: Int = -1
        private set

    private val topSpacer = WebDomElement("div").apply {
        classList.add("lazy-grid-spacer-top")
        style["flex-shrink"] = "0"
    }
    private val gridViewport = WebDomElement("div").apply {
        classList.add("lazy-grid-viewport")
        style["display"] = "grid"
        style["width"] = "100%"
        style["max-width"] = "100%"
        style["overflow-x"] = "hidden"
        style["box-sizing"] = "border-box"
    }
    private val bottomSpacer = WebDomElement("div").apply {
        classList.add("lazy-grid-spacer-bottom")
        style["flex-shrink"] = "0"
    }

    private val mountedItems = linkedMapOf<Int, NativeImpl<*>>()

    val mountedItemIndices: Set<Int>
        get() = mountedItems.keys.toSet()

    init {
        ensureScrollContainerStyles()
        nativeWidget.appendChild(topSpacer)
        nativeWidget.appendChild(gridViewport)
        nativeWidget.appendChild(bottomSpacer)
        setColumns(columnsCount)
        WebBrowserDomBridge.attachOverscrollStretch(nativeWidget, gridViewport)
        nativeWidget.addEventListener("scroll") {
            updateMeasuredAverageRowExtent()
            val rawScrollOffset = nativeWidget.scrollTop.coerceAtLeast(0.0).toFloat()
            val ch = nativeWidget.clientHeight
            val rawViewportExtent = (if (ch > 0) ch else WebBrowserDomBridge.getClientHeight(nativeWidget, estimatedRowHeightPx * 4))
                .coerceAtLeast(estimatedRowHeightPx)
                .toFloat()
            dispatchViewportScroll(rawScrollOffset, rawViewportExtent)
        }
        nativeWidget.addEventListener("wheel") { ev ->
            if (!WebBrowserDomBridge.isBrowserEnvironment || WebBrowserDomBridge.getClientHeight(nativeWidget, 0) <= 0) {
                val we = ev as? com.nexustools.genericui.event.WheelEvent ?: return@addEventListener
                if (we.deltaY != 0f) {
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
                    val rawExtent = nativeWidget.clientHeight.coerceAtLeast(estimatedRowHeightPx).toFloat()
                    dispatchViewportScroll(nextScroll.toFloat(), rawExtent)
                }
            }
        }
        WebBrowserDomBridge.attachLazyViewportObserver(
            scrollElement = nativeWidget,
            viewportElement = gridViewport,
            isHorizontal = { false }
        ) { scrollOffsetPx, viewportExtentPx, measuredSpanPx, renderedCount ->
            if (renderedCount > 0 && measuredSpanPx > 0) {
                val renderedRows = ceil(renderedCount.toDouble() / columnsCount.coerceAtLeast(1).toDouble()).toInt().coerceAtLeast(1)
                calibrateMeasuredAverageRowExtent(measuredSpanPx, renderedRows)
            } else {
                updateMeasuredAverageRowExtent()
            }
            val rawExtent = viewportExtentPx.coerceAtLeast(estimatedRowHeightPx).toFloat()
            dispatchViewportScroll(scrollOffsetPx.coerceAtLeast(0.0).toFloat(), rawExtent)
        }
    }

    private fun dispatchViewportScroll(rawScrollOffset: Float, rawViewportExtent: Float) {
        isHandlingBrowserScroll = true
        try {
            val effectiveRowHeightPx = if (
                WebBrowserDomBridge.isBrowserEnvironment &&
                measuredAvgRowHeightPx > 1f
            ) {
                measuredAvgRowHeightPx
            } else {
                estimatedRowHeightPx.coerceAtLeast(1).toFloat()
            }
            val toEstimatedScale = if (effectiveRowHeightPx > 1f && estimatedRowHeightPx > 0) {
                estimatedRowHeightPx.toFloat() / effectiveRowHeightPx
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
        nativeWidget.setStyle("overflow-y", "auto")
        nativeWidget.setStyle("overflow-x", "hidden")
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
        gridViewport.setStyle("display", "grid")
        gridViewport.setStyle("width", "100%")
        gridViewport.setStyle("max-width", "100%")
        gridViewport.setStyle("overflow-x", "hidden")
        gridViewport.setStyle("box-sizing", "border-box")
    }

    override fun applyComponentStyle(style: ComponentStyle) {
        super<WebBaseContainerNativeImpl>.applyComponentStyle(style)
        ensureScrollContainerStyles()
    }

    override fun applyComputedStyle(style: ComputedStyle) {
        super<WebBaseContainerNativeImpl>.applyComputedStyle(style)
        ensureScrollContainerStyles()
    }

    private fun calibrateMeasuredAverageRowExtent(measuredSpanPx: Int, renderedRows: Int): Boolean {
        if (renderedRows <= 0 || measuredSpanPx <= 0) return false
        val avg = measuredSpanPx.toFloat() / renderedRows.toFloat()
        if (avg < 12f) return false
        val prev = measuredAvgRowHeightPx
        measuredAvgRowHeightPx = if (prev > 1f) {
            (prev * 0.65f) + (avg * 0.35f)
        } else {
            avg
        }
        return kotlin.math.abs(measuredAvgRowHeightPx - prev) > 1.5f
    }

    private fun updateMeasuredAverageRowExtent(): Boolean {
        if (!WebBrowserDomBridge.isBrowserEnvironment) return false
        val childCount = gridViewport.children.size
        if (childCount <= 0) return false
        val renderedRows = ceil(childCount.toDouble() / columnsCount.coerceAtLeast(1).toDouble()).toInt().coerceAtLeast(1)
        val renderedHeight = WebBrowserDomBridge.getClientHeight(gridViewport, 0)
        return calibrateMeasuredAverageRowExtent(renderedHeight, renderedRows)
    }

    override fun addChildNative(childImpl: NativeImpl<*>, index: Int) {
        val dom = childImpl.nativeWidget as? WebDomElement ?: return
        if (dom === topSpacer || dom === gridViewport || dom === bottomSpacer) return
        if (dom !in gridViewport.children) {
            gridViewport.appendChild(dom)
        }
    }

    override fun removeChildNative(childImpl: NativeImpl<*>) {
        val dom = childImpl.nativeWidget as? WebDomElement ?: return
        if (dom === topSpacer || dom === gridViewport || dom === bottomSpacer) return
        val keysToRemove = mountedItems.entries.filter { it.value === childImpl }.map { it.key }
        for (k in keysToRemove) {
            mountedItems.remove(k)
        }
        gridViewport.removeChild(dom)
        nativeWidget.removeChild(dom)
    }

    override fun clearChildrenNative() {
        unmountAllItems()
    }

    override fun setEstimatedRowExtent(rowHeightPx: Float, overscanRows: Int) {
        this.estimatedRowHeightPx = rowHeightPx.roundToInt().coerceAtLeast(1)
        this.overscanRows = overscanRows.coerceAtLeast(0)
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
            measuredAvgRowHeightPx > 1f &&
            estimatedRowHeightPx > 0
        ) {
            measuredAvgRowHeightPx / estimatedRowHeightPx.toFloat()
        } else {
            1f
        }
        val scaledLeading = (leadingSpacerPx * fromEstimatedScale).roundToInt().coerceAtLeast(0)
        val scaledTrailing = (trailingSpacerPx * fromEstimatedScale).roundToInt().coerceAtLeast(0)
        val scaledTotal = (totalContentExtentPx * fromEstimatedScale).roundToInt().coerceAtLeast(0)
        if (!isHandlingBrowserScroll) {
            nativeWidget.scrollTop = (scrollOffsetPx * fromEstimatedScale).toDouble()
        }
        topSpacer.style["height"] = "${scaledLeading}px"
        bottomSpacer.style["height"] = "${scaledTrailing}px"
        nativeWidget.scrollHeight = scaledTotal
    }

    override fun setOnViewportScrollChanged(listener: ((scrollOffsetPx: Float, viewportExtentPx: Float) -> Unit)?) {
        this.onViewportScrollChangedListener = listener
    }

    override fun setColumns(columns: Int) {
        val clamped = columns.coerceAtLeast(1)
        val changed = columnsCount != clamped
        columnsCount = clamped
        gridViewport.style["grid-template-columns"] = "repeat($columnsCount, 1fr)"
        if (changed) {
            recomputeVisibleWindow()
        }
    }

    override fun setItemCount(count: Int, itemFactory: (index: Int) -> NativeImpl<*>) {
        if (!explicitViewportWindowProvided) {
            unmountAllItems()
        }
        totalItemCount = count.coerceAtLeast(0)
        this.itemFactory = itemFactory
        recomputeVisibleWindow()
    }

    override fun scrollToIndex(index: Int) {
        if (totalItemCount <= 0) return
        val clamped = index.coerceIn(0, totalItemCount - 1)
        val targetRow = clamped / columnsCount
        nativeWidget.scrollTop = (targetRow * estimatedRowHeightPx).toDouble()
        explicitViewportWindowProvided = false
        recomputeVisibleWindow()
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
            val totalRows = ceil(totalItemCount.toDouble() / columnsCount.toDouble()).toInt()
            val scrollOffset = nativeWidget.scrollTop.coerceAtLeast(0.0)
            val viewportHeight = nativeWidget.clientHeight.coerceAtLeast(estimatedRowHeightPx * 4)
            val rowHeight = max(1, estimatedRowHeightPx).toDouble()

            val visibleRows = max(1, ceil(viewportHeight / rowHeight).toInt())
            val maxFirstRow = max(0, totalRows - visibleRows)
            val rawFirstRow = floor(scrollOffset / rowHeight).toInt().coerceIn(0, maxFirstRow)
            val firstRow = (rawFirstRow - overscanRows).coerceIn(0, max(0, totalRows - 1))
            val lastRow = (rawFirstRow + visibleRows + overscanRows).coerceIn(firstRow, max(0, totalRows - 1))

            firstVisibleIndex = (firstRow * columnsCount).coerceIn(0, totalItemCount - 1)
            lastVisibleIndex = min(totalItemCount - 1, ((lastRow + 1) * columnsCount) - 1)

            topSpacer.style["height"] = "${firstRow * estimatedRowHeightPx}px"
            bottomSpacer.style["height"] = "${max(0, (totalRows - 1 - lastRow) * estimatedRowHeightPx)}px"
            nativeWidget.scrollHeight = totalRows * estimatedRowHeightPx
        }
        explicitViewportWindowProvided = false

        val visibleRange = if (lastVisibleIndex >= firstVisibleIndex) {
            firstVisibleIndex..lastVisibleIndex
        } else {
            IntRange.EMPTY
        }

        // Unmount DOM nodes outside the visible window without disposing pooled ViewHolders
        val outOfView = mountedItems.keys.filter { it !in visibleRange }
        for (idx in outOfView) {
            val impl = mountedItems.remove(idx) ?: continue
            (impl.nativeWidget as? WebDomElement)?.let { gridViewport.removeChild(it) }
        }

        // Mount DOM nodes inside the visible window in index order
        val desiredDoms = ArrayList<WebDomElement>(if (visibleRange.isEmpty()) 0 else (visibleRange.last - visibleRange.first + 1))
        for (idx in visibleRange) {
            val impl = if (wasExplicitWindow) {
                factory(idx).also { mountedItems[idx] = it }
            } else {
                mountedItems.getOrPut(idx) { factory(idx) }
            }
            val dom = impl.nativeWidget as? WebDomElement ?: continue
            dom.setAttribute("data-lazy-grid-index", idx.toString())
            desiredDoms.add(dom)
        }

        val desiredSet = desiredDoms.toHashSet()
        val stray = gridViewport.children.filter { it !in desiredSet }
        for (s in stray) {
            gridViewport.removeChild(s)
        }

        for ((targetPos, dom) in desiredDoms.withIndex()) {
            val currentAtPos = gridViewport.children.getOrNull(targetPos)
            if (currentAtPos !== dom) {
                gridViewport.insertChildAt(targetPos, dom)
            }
        }

        if (WebBrowserDomBridge.isBrowserEnvironment && !measurementPassScheduled && desiredDoms.isNotEmpty()) {
            measurementPassScheduled = true
            WebBrowserDomBridge.schedulePostLayoutPass {
                measurementPassScheduled = false
                val totalRows = ceil(totalItemCount.toDouble() / columnsCount.toDouble()).toInt()
                if (updateMeasuredAverageRowExtent()) {
                    val scale = if (estimatedRowHeightPx > 0) measuredAvgRowHeightPx / estimatedRowHeightPx.toFloat() else 1f
                    val firstRow = firstVisibleIndex / columnsCount.coerceAtLeast(1)
                    val lastRow = lastVisibleIndex / columnsCount.coerceAtLeast(1)
                    val leadingPx = (firstRow * estimatedRowHeightPx * scale).roundToInt().coerceAtLeast(0)
                    val trailingPx = (max(0, totalRows - 1 - lastRow) * estimatedRowHeightPx * scale).roundToInt().coerceAtLeast(0)
                    topSpacer.style["height"] = "${leadingPx}px"
                    bottomSpacer.style["height"] = "${trailingPx}px"
                }
                val liveViewportHeight = WebBrowserDomBridge.getClientHeight(nativeWidget, 0)
                val effectiveRowHeight = if (measuredAvgRowHeightPx > 1f) measuredAvgRowHeightPx.roundToInt() else estimatedRowHeightPx
                val renderedRows = ceil((lastVisibleIndex - firstVisibleIndex + 1).coerceAtLeast(0).toDouble() / columnsCount.toDouble()).toInt()
                val currentMountedSpanPx = renderedRows * effectiveRowHeight
                if (liveViewportHeight > currentMountedSpanPx && lastVisibleIndex < totalItemCount - 1) {
                    val rawScrollOffset = nativeWidget.scrollTop.coerceAtLeast(0.0).toFloat()
                    dispatchViewportScroll(rawScrollOffset, liveViewportHeight.toFloat())
                }
            }
        }
    }

    private fun unmountAllItems() {
        gridViewport.clearChildren()
        mountedItems.clear()
    }

    override fun dispose() {
        unmountAllItems()
        super.dispose()
    }
}
