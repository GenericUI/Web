package com.nexustools.genericui.web

import com.nexustools.genericui.component.lazy.TreeNode
import com.nexustools.genericui.enums.ui.Orientation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WebLazyVirtualizationTest {

    @Test
    fun testWebLazyListDynamicScrollVirtualizationAndOverflowXHiddenPreserved() {
        val factory = WebNativeFactory()
        val lazyList = factory.createLazyList() as WebLazyListImpl
        lazyList.setEstimatedItemExtent(50f, overscanItems = 2)
        lazyList.setOrientation(Orientation.VERTICAL)

        assertEquals("auto", lazyList.nativeWidget.getStyle("overflow-y"))
        assertEquals("hidden", lazyList.nativeWidget.getStyle("overflow-x"))

        val items = (0 until 100).map { idx ->
            (factory.createContainer("div", "lazy-item") as WebContainerImpl).also {
                it.nativeWidget.textContent = "Item $idx"
            }
        }
        lazyList.nativeWidget.updateScrollFromBrowser(
            newScrollTop = 0.0,
            newScrollLeft = 0.0,
            clientW = 320,
            clientH = 200
        )
        lazyList.setItemCount(items.size) { index -> items[index] }

        assertEquals(0, lazyList.firstVisibleIndex)
        assertTrue(0 in lazyList.mountedItemIndices)
        assertFalse(50 in lazyList.mountedItemIndices)

        // Scroll down to offset 1000px (around item 20)
        var callbackScrollOffset = -1f
        var callbackViewportExtent = -1f
        lazyList.setOnViewportScrollChanged { scrollOffsetPx, viewportExtentPx ->
            callbackScrollOffset = scrollOffsetPx
            callbackViewportExtent = viewportExtentPx
            lazyList.updateViewportMetrics(
                scrollOffsetPx = scrollOffsetPx,
                viewportExtentPx = viewportExtentPx,
                firstActiveIndex = 18,
                lastActiveIndex = 26,
                leadingSpacerPx = 18 * 50f,
                trailingSpacerPx = (99 - 26) * 50f,
                totalContentExtentPx = 100 * 50f
            )
            lazyList.setItemCount(items.size) { index -> items[index] }
        }

        lazyList.nativeWidget.updateScrollFromBrowser(
            newScrollTop = 1000.0,
            newScrollLeft = 0.0,
            clientW = 320,
            clientH = 200
        )
        lazyList.nativeWidget.dispatchFromBrowser("scroll", null)

        assertEquals(1000f, callbackScrollOffset)
        assertEquals(200f, callbackViewportExtent)
        assertEquals(1000.0, lazyList.nativeWidget.scrollTop)
        assertEquals(18, lazyList.firstVisibleIndex)
        assertEquals(26, lazyList.lastVisibleIndex)
        assertFalse(0 in lazyList.mountedItemIndices)
        assertTrue(20 in lazyList.mountedItemIndices)
        assertEquals("hidden", lazyList.nativeWidget.getStyle("overflow-x"))
    }

    @Test
    fun testWebLazyGridDynamicScrollVirtualizationAndPooledItemPreservation() {
        val factory = WebNativeFactory()
        val lazyGrid = factory.createLazyGrid() as WebLazyGridImpl
        lazyGrid.setColumns(3)
        lazyGrid.setEstimatedRowExtent(100f, overscanRows = 1)

        assertEquals("auto", lazyGrid.nativeWidget.getStyle("overflow-y"))
        assertEquals("hidden", lazyGrid.nativeWidget.getStyle("overflow-x"))

        val items = (0 until 90).map { idx ->
            (factory.createContainer("div", "grid-cell") as WebContainerImpl).also {
                it.nativeWidget.textContent = "Cell $idx"
            }
        }
        lazyGrid.nativeWidget.updateScrollFromBrowser(
            newScrollTop = 0.0,
            newScrollLeft = 0.0,
            clientW = 360,
            clientH = 200
        )
        lazyGrid.setItemCount(items.size) { index -> items[index] }

        assertEquals(0, lazyGrid.firstVisibleIndex)
        assertTrue(0 in lazyGrid.mountedItemIndices)
        assertFalse(45 in lazyGrid.mountedItemIndices)

        var scrollNotified = false
        lazyGrid.setOnViewportScrollChanged { scrollOffsetPx, viewportExtentPx ->
            scrollNotified = true
            lazyGrid.updateViewportMetrics(
                scrollOffsetPx = scrollOffsetPx,
                viewportExtentPx = viewportExtentPx,
                firstActiveIndex = 27,
                lastActiveIndex = 41,
                leadingSpacerPx = 9 * 100f,
                trailingSpacerPx = 16 * 100f,
                totalContentExtentPx = 30 * 100f
            )
            lazyGrid.setItemCount(items.size) { index -> items[index] }
        }

        lazyGrid.nativeWidget.updateScrollFromBrowser(
            newScrollTop = 1000.0,
            newScrollLeft = 0.0,
            clientW = 360,
            clientH = 200
        )
        lazyGrid.nativeWidget.dispatchFromBrowser("scroll", null)

        assertTrue(scrollNotified)
        assertEquals(27, lazyGrid.firstVisibleIndex)
        assertEquals(41, lazyGrid.lastVisibleIndex)
        assertFalse(0 in lazyGrid.mountedItemIndices)
        assertTrue(30 in lazyGrid.mountedItemIndices)
        // Pooled item 0 must remain undisposed so it can be reused when scrolling back up
        assertEquals("Cell 0", items[0].nativeWidget.textContent)
    }

    @Test
    fun testWebTreeViewDynamicScrollVirtualization() {
        val factory = WebNativeFactory()
        val treeView = factory.createTreeView() as WebTreeViewImpl
        treeView.setEstimatedRowHeight(32f, overscanRows = 2)

        assertEquals("auto", treeView.nativeWidget.getStyle("overflow-y"))
        assertEquals("hidden", treeView.nativeWidget.getStyle("overflow-x"))

        val roots = (0 until 80).map { idx ->
            TreeNode(id = "node-$idx", label = "Node $idx", data = idx)
        }
        treeView.nativeWidget.updateScrollFromBrowser(
            newScrollTop = 0.0,
            newScrollLeft = 0.0,
            clientW = 280,
            clientH = 160
        )
        treeView.setRootNodes(roots)

        assertEquals(0, treeView.firstVisibleIndex)
        assertTrue(0 in treeView.mountedRowIndices)
        assertFalse(40 in treeView.mountedRowIndices)

        var scrollNotified = false
        treeView.setOnViewportScrollChanged { scrollOffsetPx, viewportHeightPx ->
            scrollNotified = true
            treeView.updateViewportMetrics(
                scrollOffsetPx = scrollOffsetPx,
                viewportHeightPx = viewportHeightPx,
                firstActiveIndex = 28,
                lastActiveIndex = 37,
                leadingSpacerPx = 28 * 32f,
                trailingSpacerPx = (79 - 37) * 32f,
                totalContentExtentPx = 80 * 32f
            )
        }

        treeView.nativeWidget.updateScrollFromBrowser(
            newScrollTop = 960.0,
            newScrollLeft = 0.0,
            clientW = 280,
            clientH = 160
        )
        treeView.nativeWidget.dispatchFromBrowser("scroll", null)

        assertTrue(scrollNotified)
        assertEquals(28, treeView.firstVisibleIndex)
        assertEquals(37, treeView.lastVisibleIndex)
        assertFalse(0 in treeView.mountedRowIndices)
        assertTrue(30 in treeView.mountedRowIndices)
    }

    @Test
    fun testWebDialogWindowModalFooterButtonBarMountedAfterModalBody() {
        val factory = WebNativeFactory()
        val dialog = factory.createDialogWindow() as WebDialogWindowImpl
        val bodyChild = factory.createContainer("dialog-body-content", "div") as WebContainerImpl
        val footerBar = factory.createContainer("modal-footer d-flex justify-content-end gap-2", "div") as WebContainerImpl

        dialog.addChildNative(bodyChild, -1)
        dialog.addChildNative(footerBar, -1)

        assertTrue(bodyChild.nativeWidget in dialog.modalBody.children)
        assertFalse(footerBar.nativeWidget in dialog.modalBody.children)
        assertTrue(footerBar.nativeWidget in dialog.modalContent.children)
        assertSame(footerBar.nativeWidget, dialog.modalContent.children.last())
        assertEquals("0", footerBar.nativeWidget.getStyle("flex-shrink"))
        assertTrue("py-2" in footerBar.nativeWidget.classList)
        assertTrue("px-3" in footerBar.nativeWidget.classList)

        dialog.removeChildNative(footerBar)
        assertFalse(footerBar.nativeWidget in dialog.modalContent.children)
    }

    @Test
    fun testWebNamespaceSpecLayeredContainerAndTextBubbleRegistration() {
        assertTrue("genericui:layered-container" in WebNamespaceSpec.CUSTOM_COMPONENT_TAGS)
        assertTrue("genericui:text-bubble" in WebNamespaceSpec.CUSTOM_COMPONENT_TAGS)
        assertEquals(
            "genericui:layered-container",
            WebNamespaceSpec.inferTagForClassPrefix("gui-layered-container position-relative", "genericui:layered-container")
        )
        assertEquals(
            "genericui:layered-container",
            WebNamespaceSpec.inferTagForClassPrefix("gui-layered-container")
        )
        assertEquals(
            "genericui:text-bubble",
            WebNamespaceSpec.inferTagForClassPrefix("gui-text-bubble", "genericui:text-bubble")
        )
        assertEquals(
            "genericui:text-bubble",
            WebNamespaceSpec.inferTagForClassPrefix("gui-text-bubble")
        )
        assertTrue(WebNamespaceSpec.DEFAULT_GENERICUI_CSS.contains(".gui-layered-container"))
        assertTrue(WebNamespaceSpec.DEFAULT_GENERICUI_CSS.contains(".gui-text-bubble"))
        assertTrue(WebNamespaceSpec.DEFAULT_GENERICUI_CSS.contains(".lazy-list-viewport"))
        assertTrue(WebNamespaceSpec.DEFAULT_NAMESPACE_XML.contains("tag=\"genericui:layered-container\""))
        assertTrue(WebNamespaceSpec.DEFAULT_NAMESPACE_XML.contains("tag=\"genericui:text-bubble\""))
    }

    @Test
    fun testWebLazyListPreservesExplicitMaxHeightAndScrollsOnWheel() {
        val factory = WebNativeFactory()
        val lazyList = factory.createLazyList() as WebLazyListImpl
        lazyList.setEstimatedItemExtent(56f, overscanItems = 2)

        val style = com.nexustools.genericui.style.css.ComponentStyle().apply {
            setMinHeight(com.nexustools.genericui.style.css.CssLength.px(240))
            setMaxHeight(com.nexustools.genericui.style.css.CssLength.px(420))
            setMinWidth(com.nexustools.genericui.style.css.CssLength.px(340))
        }
        lazyList.applyComponentStyle(style)

        assertEquals("240px", lazyList.nativeWidget.getStyle("min-height"))
        assertEquals("420px", lazyList.nativeWidget.getStyle("max-height"))
        assertEquals("340px", lazyList.nativeWidget.getStyle("min-width"))

        lazyList.nativeWidget.updateScrollFromBrowser(
            newScrollTop = 0.0,
            newScrollLeft = 0.0,
            clientW = 340,
            clientH = 420,
            scrollW = 340,
            scrollH = 50 * 56
        )
        lazyList.setItemCount(50) { idx ->
            (factory.createContainer("div", "lazy-item") as WebContainerImpl).also {
                it.nativeWidget.textContent = "Episode $idx"
            }
        }
        assertEquals(0, lazyList.firstVisibleIndex)
        assertTrue(0 in lazyList.mountedItemIndices)

        lazyList.nativeWidget.dispatchFromBrowser(
            "wheel",
            com.nexustools.genericui.event.WheelEvent(deltaX = 0f, deltaY = 560f, x = 100f, y = 100f)
        )

        assertEquals(560.0, lazyList.nativeWidget.scrollTop)
        assertTrue(lazyList.firstVisibleIndex >= 7)
        assertTrue(10 in lazyList.mountedItemIndices)
        assertFalse(0 in lazyList.mountedItemIndices)
    }
}
