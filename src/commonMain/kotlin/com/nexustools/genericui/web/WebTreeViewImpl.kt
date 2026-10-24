package com.nexustools.genericui.web

import com.nexustools.genericui.component.lazy.TreeNode
import com.nexustools.genericui.nativeimpl.lazy.TreeViewNativeImpl
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Web true lazy virtualized tree view implementation ([TreeViewNativeImpl]).
 *
 * Flattens only currently expanded [TreeNode] branches, computes the visible row window
 * `[firstVisibleIndex .. lastVisibleIndex]` from scroll offset and viewport height, mounts DOM row
 * elements ONLY for visible nodes, and unmounts DOM row elements as soon as nodes scroll out of view.
 */
class WebTreeViewImpl : WebBaseNativeImpl("div", "tree-view"), TreeViewNativeImpl<WebDomElement> {

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

    var rowHeightPx: Int = 32
    var overscanRows: Int = 2

    var firstVisibleIndex: Int = 0
        private set
    var lastVisibleIndex: Int = -1
        private set

    private val topSpacer = WebDomElement("div").apply { classList.add("tree-view-spacer-top") }
    private val rowsViewport = WebDomElement("div").apply { classList.add("tree-view-viewport") }
    private val bottomSpacer = WebDomElement("div").apply { classList.add("tree-view-spacer-bottom") }

    private val mountedRowElements = linkedMapOf<Int, WebDomElement>()

    val mountedRowIndices: Set<Int>
        get() = mountedRowElements.keys.toSet()

    init {
        nativeWidget.style["overflow-y"] = "auto"
        nativeWidget.appendChild(topSpacer)
        nativeWidget.appendChild(rowsViewport)
        nativeWidget.appendChild(bottomSpacer)
        nativeWidget.addEventListener("scroll") {
            recomputeVisibleWindow()
        }
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

        val scrollOffset = nativeWidget.scrollTop.coerceAtLeast(0.0)
        val viewportHeight = nativeWidget.clientHeight.coerceAtLeast(rowHeightPx)
        val extent = max(1, rowHeightPx).toDouble()

        val rawFirst = floor(scrollOffset / extent).toInt() - overscanRows
        val rawLast = ceil((scrollOffset + viewportHeight) / extent).toInt() + overscanRows

        firstVisibleIndex = rawFirst.coerceIn(0, totalRows - 1)
        lastVisibleIndex = min(totalRows - 1, max(firstVisibleIndex, rawLast))
        val visibleRange = firstVisibleIndex..lastVisibleIndex

        topSpacer.style["height"] = "${firstVisibleIndex * rowHeightPx}px"
        bottomSpacer.style["height"] = "${max(0, (totalRows - 1 - lastVisibleIndex) * rowHeightPx)}px"
        nativeWidget.scrollHeight = totalRows * rowHeightPx

        // Unmount rows outside visible window
        val outOfView = mountedRowElements.keys.filter { it !in visibleRange }
        for (idx in outOfView) {
            val dom = mountedRowElements.remove(idx) ?: continue
            rowsViewport.removeChild(dom)
        }

        // Mount rows inside visible window
        for (idx in visibleRange) {
            if (!mountedRowElements.containsKey(idx)) {
                val entry = flattenedVisibleRows[idx]
                val rowDom = WebDomElement("div").apply {
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
                mountedRowElements[idx] = rowDom
                rowsViewport.appendChild(rowDom)
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
