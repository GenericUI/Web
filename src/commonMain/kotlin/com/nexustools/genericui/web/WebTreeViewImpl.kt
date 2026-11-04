package com.nexustools.genericui.web

import com.nexustools.genericui.component.lazy.TreeNode
import com.nexustools.genericui.nativeimpl.lazy.TreeViewNativeImpl
import com.nexustools.genericui.style.css.ComponentStyle
import com.nexustools.genericui.style.css.ComputedStyle
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Web true lazy virtualized tree view implementation ([TreeViewNativeImpl]).
 *
 * Flattens only currently expanded [TreeNode] branches, computes the visible row window
 * `[firstVisibleIndex .. lastVisibleIndex]` from scroll offset and viewport height, mounts DOM row
 * elements ONLY for visible nodes, and unmounts DOM row elements as soon as nodes scroll out of view.
 * Also supports Android-style boundary overscroll stretch via [WebBrowserDomBridge.attachOverscrollStretch].
 */
class WebTreeViewImpl : WebBaseNativeImpl("genericui:treeview", "tree-view"), TreeViewNativeImpl<WebDomElement> {

    private data class FlattenedTreeRow(
        val node: TreeNode<*>,
        val depth: Int
    )

    private var rootNodes: List<TreeNode<*>> = emptyList()
    private val flattenedVisibleRows = mutableListOf<FlattenedTreeRow>()
    private var selectedNodeId: String? = null

    private var onNodeSelectedListener: ((TreeNode<*>) -> Unit)? = null
    private var onNodeDoubleClickedListener: ((TreeNode<*>) -> Unit)? = null
    private var onNodeContextMenuListener: ((TreeNode<*>, Float, Float) -> Unit)? = null
    private var onViewportScrollChangedListener: ((Float, Float) -> Unit)? = null
    private var explicitViewportWindowProvided: Boolean = false
    private var isHandlingBrowserScroll: Boolean = false
    private var measuredAvgRowHeightPx: Float = 0f
    private var measurementPassScheduled: Boolean = false

    var rowHeightPx: Int = 32
    var overscanRows: Int = 2

    var firstVisibleIndex: Int = 0
        private set
    var lastVisibleIndex: Int = -1
        private set

    private val topSpacer = WebDomElement("div").apply {
        classList.add("tree-view-spacer-top")
        style["flex-shrink"] = "0"
    }
    private val rowsViewport = WebDomElement("div").apply {
        classList.add("tree-view-viewport")
        style["width"] = "100%"
        style["max-width"] = "100%"
        style["overflow-x"] = "hidden"
        style["box-sizing"] = "border-box"
    }
    private val bottomSpacer = WebDomElement("div").apply {
        classList.add("tree-view-spacer-bottom")
        style["flex-shrink"] = "0"
    }

    private val mountedRowElements = linkedMapOf<Int, WebDomElement>()

    val mountedRowIndices: Set<Int>
        get() = mountedRowElements.keys.toSet()

    init {
        ensureScrollContainerStyles()
        nativeWidget.appendChild(topSpacer)
        nativeWidget.appendChild(rowsViewport)
        nativeWidget.appendChild(bottomSpacer)
        WebBrowserDomBridge.attachOverscrollStretch(nativeWidget, rowsViewport)
        nativeWidget.addEventListener("scroll") {
            updateMeasuredAverageRowHeight()
            val rawScrollOffset = nativeWidget.scrollTop.coerceAtLeast(0.0).toFloat()
            val ch = nativeWidget.clientHeight
            val rawViewportHeight = (if (ch > 0) ch else WebBrowserDomBridge.getClientHeight(nativeWidget, rowHeightPx * 6))
                .coerceAtLeast(rowHeightPx)
                .toFloat()
            dispatchViewportScroll(rawScrollOffset, rawViewportHeight)
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
                    val rawHeight = nativeWidget.clientHeight.coerceAtLeast(rowHeightPx).toFloat()
                    dispatchViewportScroll(nextScroll.toFloat(), rawHeight)
                }
            }
        }
        WebBrowserDomBridge.attachLazyViewportObserver(
            scrollElement = nativeWidget,
            viewportElement = rowsViewport,
            isHorizontal = { false }
        ) { scrollOffsetPx, viewportExtentPx, measuredSpanPx, renderedCount ->
            if (renderedCount > 0 && measuredSpanPx > 0) {
                calibrateMeasuredAverageRowHeight(measuredSpanPx, renderedCount)
            } else {
                updateMeasuredAverageRowHeight()
            }
            val rawHeight = viewportExtentPx.coerceAtLeast(rowHeightPx).toFloat()
            dispatchViewportScroll(scrollOffsetPx.coerceAtLeast(0.0).toFloat(), rawHeight)
        }
    }

    private fun dispatchViewportScroll(rawScrollOffset: Float, rawViewportHeight: Float) {
        isHandlingBrowserScroll = true
        try {
            val effectiveRowHeightPx = if (
                WebBrowserDomBridge.isBrowserEnvironment &&
                measuredAvgRowHeightPx > 1f
            ) {
                measuredAvgRowHeightPx
            } else {
                rowHeightPx.coerceAtLeast(1).toFloat()
            }
            val toEstimatedScale = if (effectiveRowHeightPx > 1f && rowHeightPx > 0) {
                rowHeightPx.toFloat() / effectiveRowHeightPx
            } else {
                1f
            }
            val listener = onViewportScrollChangedListener
            if (listener != null) {
                listener(
                    rawScrollOffset.coerceAtLeast(0f) * toEstimatedScale,
                    max(rawViewportHeight, rawViewportHeight * toEstimatedScale)
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
        rowsViewport.setStyle("width", "100%")
        rowsViewport.setStyle("max-width", "100%")
        rowsViewport.setStyle("overflow-x", "hidden")
        rowsViewport.setStyle("box-sizing", "border-box")
    }

    override fun applyComponentStyle(style: ComponentStyle) {
        super<WebBaseNativeImpl>.applyComponentStyle(style)
        ensureScrollContainerStyles()
    }

    override fun applyComputedStyle(style: ComputedStyle) {
        super<WebBaseNativeImpl>.applyComputedStyle(style)
        ensureScrollContainerStyles()
    }

    private fun calibrateMeasuredAverageRowHeight(measuredSpanPx: Int, renderedCount: Int): Boolean {
        if (renderedCount <= 0 || measuredSpanPx <= 0) return false
        val avg = measuredSpanPx.toFloat() / renderedCount.toFloat()
        if (avg < 12f) return false
        val prev = measuredAvgRowHeightPx
        measuredAvgRowHeightPx = if (prev > 1f) {
            (prev * 0.65f) + (avg * 0.35f)
        } else {
            avg
        }
        return kotlin.math.abs(measuredAvgRowHeightPx - prev) > 1.5f
    }

    private fun updateMeasuredAverageRowHeight(): Boolean {
        if (!WebBrowserDomBridge.isBrowserEnvironment) return false
        val childCount = rowsViewport.children.size
        if (childCount <= 0) return false
        val renderedHeight = WebBrowserDomBridge.getClientHeight(rowsViewport, 0)
        return calibrateMeasuredAverageRowHeight(renderedHeight, childCount)
    }

    override fun setEstimatedRowHeight(rowHeightPx: Float, overscanRows: Int) {
        this.rowHeightPx = rowHeightPx.roundToInt().coerceAtLeast(1)
        this.overscanRows = overscanRows.coerceAtLeast(0)
    }

    override fun updateViewportMetrics(
        scrollOffsetPx: Float,
        viewportHeightPx: Float,
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
            rowHeightPx > 0
        ) {
            measuredAvgRowHeightPx / rowHeightPx.toFloat()
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
        if (flattenedVisibleRows.isNotEmpty()) {
            syncMountedRowsToVisibleWindow()
        }
    }

    override fun setOnViewportScrollChanged(listener: ((scrollOffsetPx: Float, viewportHeightPx: Float) -> Unit)?) {
        this.onViewportScrollChangedListener = listener
    }

    override fun setRootNodes(roots: List<TreeNode<*>>) {
        rootNodes = roots
        rebuildFlattenedRows()
        unmountAllRows()
        recomputeVisibleWindow()
    }

    override fun setSelectedNodeId(nodeId: String?) {
        selectedNodeId = nodeId
        for ((idx, dom) in mountedRowElements) {
            val row = flattenedVisibleRows.getOrNull(idx) ?: continue
            if (row.node.id == nodeId) {
                dom.classList.add("active")
            } else {
                dom.classList.remove("active")
            }
        }
        val matched = flattenedVisibleRows.firstOrNull { it.node.id == nodeId }?.node
        if (matched != null) {
            onNodeSelectedListener?.invoke(matched)
        }
    }

    override fun setOnNodeSelected(listener: ((TreeNode<*>) -> Unit)?) {
        onNodeSelectedListener = listener
    }

    override fun setOnNodeDoubleClicked(listener: ((TreeNode<*>) -> Unit)?) {
        onNodeDoubleClickedListener = listener
    }

    override fun setOnNodeContextMenu(listener: ((TreeNode<*>, Float, Float) -> Unit)?) {
        onNodeContextMenuListener = listener
    }

    private fun rebuildFlattenedRows() {
        flattenedVisibleRows.clear()
        fun walk(node: TreeNode<*>, depth: Int) {
            flattenedVisibleRows.add(FlattenedTreeRow(node, depth))
            if (node.isExpanded) {
                node.ensureChildrenLoaded()
                for (child in node.children) {
                    walk(child, depth + 1)
                }
            }
        }
        for (root in rootNodes) {
            walk(root, 0)
        }
    }

    fun recomputeVisibleWindow() {
        val totalRows = flattenedVisibleRows.size
        if (totalRows <= 0) {
            unmountAllRows()
            firstVisibleIndex = 0
            lastVisibleIndex = -1
            topSpacer.style["height"] = "0px"
            bottomSpacer.style["height"] = "0px"
            return
        }

        val wasExplicitWindow = explicitViewportWindowProvided
        if (!wasExplicitWindow) {
            val scrollOffset = nativeWidget.scrollTop.coerceAtLeast(0.0)
            val viewportHeight = nativeWidget.clientHeight.coerceAtLeast(rowHeightPx * 6)
            val extent = max(1, rowHeightPx).toDouble()

            val visibleRows = max(1, ceil(viewportHeight / extent).toInt())
            val maxFirstRow = max(0, totalRows - visibleRows)
            val clampedFirstRow = floor(scrollOffset / extent).toInt().coerceIn(0, maxFirstRow)

            val rawFirst = clampedFirstRow - overscanRows
            val rawLast = clampedFirstRow + visibleRows + overscanRows

            firstVisibleIndex = rawFirst.coerceIn(0, totalRows - 1)
            lastVisibleIndex = min(totalRows - 1, max(firstVisibleIndex, rawLast))

            topSpacer.style["height"] = "${firstVisibleIndex * rowHeightPx}px"
            bottomSpacer.style["height"] = "${max(0, (totalRows - 1 - lastVisibleIndex) * rowHeightPx)}px"
            nativeWidget.scrollHeight = totalRows * rowHeightPx
        }
        explicitViewportWindowProvided = false

        syncMountedRowsToVisibleWindow()
    }

    private fun syncMountedRowsToVisibleWindow() {
        val totalRows = flattenedVisibleRows.size
        if (totalRows <= 0) {
            unmountAllRows()
            return
        }
        val clampedFirst = firstVisibleIndex.coerceIn(0, totalRows - 1)
        val clampedLast = lastVisibleIndex.coerceIn(-1, totalRows - 1)
        val visibleRange = if (clampedLast >= clampedFirst) {
            clampedFirst..clampedLast
        } else {
            IntRange.EMPTY
        }

        // Unmount rows outside visible window
        val outOfView = mountedRowElements.keys.filter { it !in visibleRange }
        for (idx in outOfView) {
            val dom = mountedRowElements.remove(idx) ?: continue
            rowsViewport.removeChild(dom)
        }

        // Mount rows inside visible window
        val desiredDoms = ArrayList<WebDomElement>(if (visibleRange.isEmpty()) 0 else (visibleRange.last - visibleRange.first + 1))
        for (idx in visibleRange) {
            val rowDom = mountedRowElements.getOrPut(idx) {
                val entry = flattenedVisibleRows[idx]
                WebDomElement("div").apply {
                    classList.add("tree-view-row")
                    if (entry.node.id == selectedNodeId) classList.add("active")
                    setAttribute("data-tree-node-id", entry.node.id)
                    setAttribute("data-tree-depth", entry.depth.toString())
                    style["padding-left"] = "${entry.depth * 16}px"
                    textContent = entry.node.label
                    addEventListener("click") {
                        setSelectedNodeId(entry.node.id)
                    }
                    addEventListener("dblclick") {
                        onNodeDoubleClickedListener?.invoke(entry.node)
                    }
                }
            }
            desiredDoms.add(rowDom)
        }

        val desiredSet = desiredDoms.toHashSet()
        val stray = rowsViewport.children.filter { it !in desiredSet }
        for (s in stray) {
            rowsViewport.removeChild(s)
        }

        for ((targetPos, dom) in desiredDoms.withIndex()) {
            val currentAtPos = rowsViewport.children.getOrNull(targetPos)
            if (currentAtPos !== dom) {
                rowsViewport.insertChildAt(targetPos, dom)
            }
        }

        if (WebBrowserDomBridge.isBrowserEnvironment && !measurementPassScheduled && desiredDoms.isNotEmpty()) {
            measurementPassScheduled = true
            WebBrowserDomBridge.schedulePostLayoutPass {
                measurementPassScheduled = false
                if (updateMeasuredAverageRowHeight()) {
                    val scale = if (rowHeightPx > 0) measuredAvgRowHeightPx / rowHeightPx.toFloat() else 1f
                    val leadingPx = (clampedFirst * rowHeightPx * scale).roundToInt().coerceAtLeast(0)
                    val trailingPx = (max(0, totalRows - 1 - clampedLast) * rowHeightPx * scale).roundToInt().coerceAtLeast(0)
                    topSpacer.style["height"] = "${leadingPx}px"
                    bottomSpacer.style["height"] = "${trailingPx}px"
                }
                val liveViewportHeight = WebBrowserDomBridge.getClientHeight(nativeWidget, 0)
                val effectiveRowHeight = if (measuredAvgRowHeightPx > 1f) measuredAvgRowHeightPx.roundToInt() else rowHeightPx
                val currentMountedSpanPx = (clampedLast - clampedFirst + 1).coerceAtLeast(0) * effectiveRowHeight
                if (liveViewportHeight > currentMountedSpanPx && clampedLast < totalRows - 1) {
                    val rawScrollOffset = nativeWidget.scrollTop.coerceAtLeast(0.0).toFloat()
                    dispatchViewportScroll(rawScrollOffset, liveViewportHeight.toFloat())
                }
            }
        }
    }

    private fun unmountAllRows() {
        rowsViewport.clearChildren()
        mountedRowElements.clear()
    }

    override fun dispose() {
        unmountAllRows()
        super.dispose()
    }
}
