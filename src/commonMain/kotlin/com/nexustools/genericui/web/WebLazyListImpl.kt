package com.nexustools.genericui.web

import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.enums.ui.Orientation
import com.nexustools.genericui.nativeimpl.lazy.LazyListNativeImpl
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

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

    var estimatedItemExtentPx: Int = 44
    var overscanItems: Int = 2

    var firstVisibleIndex: Int = 0
        private set
    var lastVisibleIndex: Int = -1
        private set

    private val topSpacer = WebDomElement("div").apply { classList.add("lazy-list-spacer-start") }
    private val viewportHost = WebDomElement("div").apply { classList.add("lazy-list-viewport") }
    private val bottomSpacer = WebDomElement("div").apply { classList.add("lazy-list-spacer-end") }

    private val mountedItems = linkedMapOf<Int, NativeImpl<*>>()

    val mountedItemIndices: Set<Int>
        get() = mountedItems.keys.toSet()

    init {
        nativeWidget.style["overflow-y"] = "auto"
        nativeWidget.appendChild(topSpacer)
        nativeWidget.appendChild(viewportHost)
        nativeWidget.appendChild(bottomSpacer)
        nativeWidget.addEventListener("scroll") {
            recomputeVisibleWindow()
        }
    }

    override fun setOrientation(orientation: Orientation) {
        this.orientation = orientation
        if (orientation == Orientation.HORIZONTAL) {
            nativeWidget.style["overflow-x"] = "auto"
            nativeWidget.style.remove("overflow-y")
        } else {
            nativeWidget.style["overflow-y"] = "auto"
            nativeWidget.style.remove("overflow-x")
        }
        recomputeVisibleWindow()
    }

    override fun setItemCount(count: Int, itemFactory: (index: Int) -> NativeImpl<*>) {
        this.totalItemCount = count.coerceAtLeast(0)
        this.itemFactory = itemFactory
        unmountAllItems()
        recomputeVisibleWindow()
    }

    override fun notifyItemRangeChanged(startIndex: Int, count: Int) {
        val endIndex = startIndex + count - 1
        for (idx in startIndex..endIndex) {
            val old = mountedItems.remove(idx) ?: continue
            (old.nativeWidget as? WebDomElement)?.let { viewportHost.removeChild(it) }
            old.dispose()
        }
        recomputeVisibleWindow()
    }

    override fun scrollToIndex(index: Int) {
        if (totalItemCount <= 0) return
        val clamped = index.coerceIn(0, totalItemCount - 1)
        val targetOffset = (clamped * estimatedItemExtentPx).toDouble()
        if (orientation == Orientation.HORIZONTAL) {
            nativeWidget.scrollLeft = targetOffset
        } else {
            nativeWidget.scrollTop = targetOffset
        }
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
        val visibleRange = firstVisibleIndex..lastVisibleIndex

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

        // Unmount DOM nodes that scrolled out of the visible window
        val outOfView = mountedItems.keys.filter { it !in visibleRange }
        for (idx in outOfView) {
            val impl = mountedItems.remove(idx) ?: continue
            (impl.nativeWidget as? WebDomElement)?.let { viewportHost.removeChild(it) }
            impl.dispose()
        }

        // Mount DOM nodes for newly visible items in index order
        for (idx in visibleRange) {
            if (!mountedItems.containsKey(idx)) {
                val created = factory(idx)
                mountedItems[idx] = created
                (created.nativeWidget as? WebDomElement)?.let { dom ->
                    dom.setAttribute("data-lazy-index", idx.toString())
                    viewportHost.appendChild(dom)
                }
            }
        }
    }

    private fun unmountAllItems() {
        for ((_, impl) in mountedItems) {
            (impl.nativeWidget as? WebDomElement)?.let { viewportHost.removeChild(it) }
            impl.dispose()
        }
        mountedItems.clear()
    }

    override fun dispose() {
        unmountAllItems()
        super.dispose()
    }
}
