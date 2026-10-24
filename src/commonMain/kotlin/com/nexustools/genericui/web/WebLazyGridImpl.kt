package com.nexustools.genericui.web

import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.nativeimpl.lazy.LazyGridNativeImpl
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Web true lazy virtualized grid implementation ([LazyGridNativeImpl]).
 *
 * Computes the visible row and item index window `[firstVisibleIndex .. lastVisibleIndex]` from scroll offset
 * and viewport height, mounts/loads DOM nodes ONLY for items in the visible window (using top and bottom
 * spacer padding for total scroll height), and unmounts/unloads DOM nodes as soon as items scroll out of view.
 */
class WebLazyGridImpl : WebBaseContainerNativeImpl("div", "lazy-grid"), LazyGridNativeImpl<WebDomElement> {

    private var columnsCount: Int = 3
    private var totalItemCount: Int = 0
    private var itemFactory: ((Int) -> NativeImpl<*>)? = null

    var estimatedRowHeightPx: Int = 120
    var overscanRows: Int = 1

    var firstVisibleIndex: Int = 0
        private set
    var lastVisibleIndex: Int = -1
        private set

    private val topSpacer = WebDomElement("div").apply { classList.add("lazy-grid-spacer-top") }
    private val gridViewport = WebDomElement("div").apply {
        classList.add("lazy-grid-viewport")
        style["display"] = "grid"
    }
    private val bottomSpacer = WebDomElement("div").apply { classList.add("lazy-grid-spacer-bottom") }

    private val mountedItems = linkedMapOf<Int, NativeImpl<*>>()

    val mountedItemIndices: Set<Int>
        get() = mountedItems.keys.toSet()

    init {
        nativeWidget.style["overflow-y"] = "auto"
        nativeWidget.appendChild(topSpacer)
        nativeWidget.appendChild(gridViewport)
        nativeWidget.appendChild(bottomSpacer)
        setColumns(columnsCount)
        nativeWidget.addEventListener("scroll") {
            recomputeVisibleWindow()
        }
    }

    override fun setColumns(columns: Int) {
        columnsCount = columns.coerceAtLeast(1)
        gridViewport.style["grid-template-columns"] = "repeat($columnsCount, 1fr)"
        recomputeVisibleWindow()
    }

    override fun setItemCount(count: Int, itemFactory: (index: Int) -> NativeImpl<*>) {
        totalItemCount = count.coerceAtLeast(0)
        this.itemFactory = itemFactory
        unmountAllItems()
        recomputeVisibleWindow()
    }

    override fun scrollToIndex(index: Int) {
        if (totalItemCount <= 0) return
        val clamped = index.coerceIn(0, totalItemCount - 1)
        val targetRow = clamped / columnsCount
        nativeWidget.scrollTop = (targetRow * estimatedRowHeightPx).toDouble()
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

        val totalRows = ceil(totalItemCount.toDouble() / columnsCount.toDouble()).toInt()
        val scrollOffset = nativeWidget.scrollTop.coerceAtLeast(0.0)
        val viewportHeight = nativeWidget.clientHeight.coerceAtLeast(estimatedRowHeightPx)
        val rowHeight = max(1, estimatedRowHeightPx).toDouble()

        val firstRow = (floor(scrollOffset / rowHeight).toInt() - overscanRows).coerceIn(0, max(0, totalRows - 1))
        val lastRow = (ceil((scrollOffset + viewportHeight) / rowHeight).toInt() + overscanRows).coerceIn(firstRow, max(0, totalRows - 1))

        firstVisibleIndex = (firstRow * columnsCount).coerceIn(0, totalItemCount - 1)
        lastVisibleIndex = min(totalItemCount - 1, ((lastRow + 1) * columnsCount) - 1)
        val visibleRange = firstVisibleIndex..lastVisibleIndex

        topSpacer.style["height"] = "${firstRow * estimatedRowHeightPx}px"
        bottomSpacer.style["height"] = "${max(0, (totalRows - 1 - lastRow) * estimatedRowHeightPx)}px"
        nativeWidget.scrollHeight = totalRows * estimatedRowHeightPx

        // Unmount DOM nodes outside the visible window
        val outOfView = mountedItems.keys.filter { it !in visibleRange }
        for (idx in outOfView) {
            val impl = mountedItems.remove(idx) ?: continue
            (impl.nativeWidget as? WebDomElement)?.let { gridViewport.removeChild(it) }
            impl.dispose()
        }

        // Mount DOM nodes inside the visible window
        for (idx in visibleRange) {
            if (!mountedItems.containsKey(idx)) {
                val created = factory(idx)
                mountedItems[idx] = created
                (created.nativeWidget as? WebDomElement)?.let { dom ->
                    dom.setAttribute("data-lazy-grid-index", idx.toString())
                    gridViewport.appendChild(dom)
                }
            }
        }
    }

    private fun unmountAllItems() {
        for ((_, impl) in mountedItems) {
            (impl.nativeWidget as? WebDomElement)?.let { gridViewport.removeChild(it) }
            impl.dispose()
        }
        mountedItems.clear()
    }

    override fun dispose() {
        unmountAllItems()
        super.dispose()
    }
}
