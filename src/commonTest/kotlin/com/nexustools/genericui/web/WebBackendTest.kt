package com.nexustools.genericui.web

import com.nexustools.genericui.enums.ui.ThemeMode
import com.nexustools.genericui.event.KeyShortcut
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WebBackendTest {

    @Test
    fun testGenericUiRootAndHashedGenericUiCssInRenderedHtmlAndLoader() {
        val factory = WebNativeFactory()
        val app = factory.createApplication() as WebApplicationImpl

        assertEquals("genericui:root", app.nativeApplication.tagName)
        assertEquals("", app.nativeApplication.id)
        assertFalse(app.nativeApplication.hasAttribute("id"))
        assertFalse(app.nativeApplication.hasAttribute("data-bs-theme"))

        app.setThemeMode(ThemeMode.LIGHT)
        assertFalse(app.nativeApplication.hasAttribute("data-bs-theme"))
        assertEquals("light", app.appliedBootstrapTheme)
        app.setThemeMode(ThemeMode.DARK)
        assertFalse(app.nativeApplication.hasAttribute("data-bs-theme"))
        assertEquals("dark", app.appliedBootstrapTheme)

        val html = app.renderIndexHtml(
            title = "Studio Web App",
            wasmJsUrl = "studio-wasm.js",
            jsIrUrl = "studio-js.js"
        )
        assertTrue(html.contains("<html lang=\"en\" xmlns:genericui=\"https://nexustools.com/ns/genericui\" data-bs-theme=\"dark\">"))
        assertTrue(html.contains("<link id=\"genericui-css\" rel=\"stylesheet\" href=\"genericui.css?${WebNamespaceSpec.defaultGenericUiCssSha1Hash}\">"))
        assertTrue(html.contains("<genericui:root></genericui:root>"))
        assertFalse(html.contains("<div id=\"genericui-app\""))

        // Verify genericui.css rules for root, scrollviews, scrollfragment, and menuseparator
        val css = WebNamespaceSpec.DEFAULT_GENERICUI_CSS
        assertTrue(css.contains("genericui\\:root"))
        assertTrue(css.contains("position: absolute;"))
        assertTrue(css.contains("overflow: hidden;"))
        assertTrue(css.contains("hr.dropdown-divider,\ngenericui\\:menuseparator"))
        assertTrue(css.contains("genericui\\:scrollfragment"))
        assertTrue(css.contains("genericui\\:root.scroll-fragment"))

        // Verify loader JS ensures <genericui:root> without id or data-bs-theme
        assertTrue(WebWasmJsLoader.DEFAULT_LOADER_JS.contains("ensureRootContainer"))
        assertTrue(WebWasmJsLoader.DEFAULT_LOADER_JS.contains("getElementsByTagName(\"genericui:root\")"))
        assertTrue(WebWasmJsLoader.DEFAULT_LOADER_JS.contains("removeAttribute(\"id\")"))
        assertTrue(WebWasmJsLoader.DEFAULT_LOADER_JS.contains("removeAttribute(\"data-bs-theme\")"))

        // Verify namespace mappings
        assertEquals("genericui:root", WebNamespaceSpec.inferTagForClassPrefix("root"))
        assertEquals("genericui:root", WebNamespaceSpec.inferTagForClassPrefix("genericui:root"))
        assertEquals("genericui:window", WebNamespaceSpec.inferTagForClassPrefix("window"))
        assertEquals("genericui:fragment", WebNamespaceSpec.inferTagForClassPrefix("fragment"))
        assertEquals("genericui:fragment-container", WebNamespaceSpec.inferTagForClassPrefix("fragment-container"))
        assertEquals("genericui:scrollfragment", WebNamespaceSpec.inferTagForClassPrefix("scroll-fragment"))
    }

    @Test
    fun testTopMenuBarInsideGenericUiRootAndBootstrapDropdownMenus() {
        val factory = WebNativeFactory()
        val app = factory.createApplication() as WebApplicationImpl
        val window = factory.createWindow() as WebWindowImpl

        assertEquals("genericui:window", window.nativeWidget.tagName)
        assertTrue(window.nativeWidget.enumClassList.contains("genericui-window"))

        window.showWindow()
        assertTrue(app.nativeApplication.children.contains(window.nativeWidget))

        val menuBar = factory.createMenuBar() as WebMenuBarImpl
        assertEquals("genericui:menubar", menuBar.nativeWidget.tagName)
        assertTrue(menuBar.nativeWidget.enumClassList.contains("navbar"))
        assertTrue(menuBar.nativeWidget.enumClassList.contains("navbar-expand"))
        assertTrue(menuBar.nativeWidget.enumClassList.contains("genericui-menubar"))
        assertEquals("menubar", menuBar.nativeWidget.getAttribute("role"))

        val fileMenu = factory.createMenu() as WebMenuImpl
        fileMenu.setLabel("File")
        val editMenu = factory.createMenu() as WebMenuImpl
        editMenu.setLabel("Edit")

        val openItem = factory.createMenuItem(checkable = false, radio = false) as WebMenuItemImpl
        openItem.setLabel("Open...")
        openItem.setShortcut(KeyShortcut.parse("Ctrl+O"))
        var openClicked = 0
        openItem.onActionListener = { openClicked++ }

        val separator = factory.createDivider() as WebDividerImpl
        assertEquals("hr", separator.nativeWidget.tagName)
        assertTrue(separator.nativeWidget.enumClassList.contains("dropdown-divider"))

        val checkItem = factory.createMenuItem(checkable = true, radio = false) as WebMenuItemImpl
        checkItem.setLabel("Show Grid")

        val radioA = factory.createMenuItem(checkable = true, radio = true) as WebMenuItemImpl
        radioA.setLabel("Mode A")
        val radioB = factory.createMenuItem(checkable = true, radio = true) as WebMenuItemImpl
        radioB.setLabel("Mode B")

        val recentSubmenu = factory.createMenu() as WebMenuImpl
        recentSubmenu.setLabel("Recent Files")

        fileMenu.addChildNative(openItem, -1)
        fileMenu.addChildNative(separator, -1)
        fileMenu.addChildNative(checkItem, -1)
        fileMenu.addChildNative(radioA, -1)
        fileMenu.addChildNative(radioB, -1)
        fileMenu.addChildNative(recentSubmenu, -1)

        assertTrue(recentSubmenu.nativeWidget.classList.contains("dropend"))
        assertTrue(recentSubmenu.nativeWidget.classList.contains("genericui-submenu"))

        menuBar.addChildNative(fileMenu, -1)
        menuBar.addChildNative(editMenu, -1)

        // Attach MenuBar to Window -> mounts at index 0 of <genericui:root>
        window.setMenuBarNative(menuBar)
        assertSame(menuBar.nativeWidget, app.topMenuBarElement)
        assertSame(menuBar.nativeWidget, app.nativeApplication.children.first())

        // Verify Bootstrap dropdown toggle & hover switching
        assertFalse(fileMenu.isOpen)
        assertEquals("none", fileMenu.dropdownMenu.style["display"])
        fileMenu.toggleButton.click()
        assertTrue(fileMenu.isOpen)
        assertTrue(fileMenu.nativeWidget.classList.contains("show"))
        assertEquals("block", fileMenu.dropdownMenu.style["display"])
        assertEquals("true", fileMenu.toggleButton.getAttribute("aria-expanded"))

        // Hovering sibling menu while File menu is open switches active dropdown to Edit
        editMenu.nativeWidget.dispatchEvent("mouseenter")
        assertFalse(fileMenu.isOpen)
        assertTrue(editMenu.isOpen)

        // Clicking a check menu item toggles its state and closes the menu hierarchy
        fileMenu.open()
        assertFalse(checkItem.isChecked())
        checkItem.nativeWidget.click()
        assertTrue(checkItem.isChecked())
        assertEquals("true", checkItem.nativeWidget.getAttribute("aria-checked"))
        assertFalse(fileMenu.isOpen)

        // Clicking radio menu items selects the clicked radio and unchecks siblings
        fileMenu.open()
        radioA.nativeWidget.click()
        assertTrue(radioA.isChecked())
        assertFalse(radioB.isChecked())
        fileMenu.open()
        radioB.nativeWidget.click()
        assertFalse(radioA.isChecked())
        assertTrue(radioB.isChecked())

        // Clicking leaf action item triggers callback and closes menu
        fileMenu.open()
        openItem.nativeWidget.click()
        assertEquals(1, openClicked)
        assertFalse(fileMenu.isOpen)
    }

    @Test
    fun testWebContextMenuShowAtAndCloseOnItemClick() {
        val factory = WebNativeFactory()
        val app = factory.createApplication() as WebApplicationImpl

        val contextMenu = factory.createContextMenu() as WebContextMenuImpl
        assertEquals("genericui:contextmenu", contextMenu.nativeWidget.tagName)
        assertTrue(contextMenu.nativeWidget.enumClassList.contains("dropdown-menu"))
        assertTrue(contextMenu.nativeWidget.enumClassList.contains("genericui-context-menu"))

        val copyItem = factory.createMenuItem(checkable = false, radio = false) as WebMenuItemImpl
        copyItem.setLabel("Copy")
        var copied = false
        copyItem.onActionListener = { copied = true }

        val sep = factory.createDivider() as WebDividerImpl
        val subMenu = factory.createMenu() as WebMenuImpl
        subMenu.setLabel("More")

        contextMenu.addChildNative(copyItem, -1)
        contextMenu.addChildNative(sep, -1)
        contextMenu.addChildNative(subMenu, -1)
        assertTrue(subMenu.nativeWidget.classList.contains("dropend"))
        assertTrue(subMenu.nativeWidget.classList.contains("genericui-submenu"))

        contextMenu.showAt(null, 240, 180)
        assertSame(app.nativeApplication, contextMenu.nativeWidget.parent)
        assertTrue(contextMenu.nativeWidget.classList.contains("show"))
        assertEquals("fixed", contextMenu.nativeWidget.style["position"])
        assertEquals("240px", contextMenu.nativeWidget.style["left"])
        assertEquals("180px", contextMenu.nativeWidget.style["top"])
        assertEquals("block", contextMenu.nativeWidget.style["display"])
        assertEquals("2050", contextMenu.nativeWidget.style["z-index"])

        copyItem.nativeWidget.click()
        assertTrue(copied)
        assertFalse(contextMenu.nativeWidget.classList.contains("show"))
        assertEquals("none", contextMenu.nativeWidget.style["display"])
    }

    @Test
    fun testContextMenuRightClickAndMobileLongPressActivation() {
        val factory = WebNativeFactory()
        val app = factory.createApplication() as WebApplicationImpl
        val window = factory.createWindow() as WebWindowImpl
        window.showWindow()

        val targetButton = factory.createButton() as WebButtonImpl
        window.addChildNative(targetButton, -1)

        val contextMenu = factory.createContextMenu() as WebContextMenuImpl
        val cutItem = factory.createMenuItem(checkable = false, radio = false) as WebMenuItemImpl
        cutItem.setLabel("Cut")
        var cutTriggered = 0
        cutItem.onActionListener = { cutTriggered++ }
        contextMenu.addChildNative(cutItem, -1)

        var lastEventX = -1
        var lastEventY = -1
        targetButton.setOnContextMenu { ev ->
            lastEventX = ev.x.toInt()
            lastEventY = ev.y.toInt()
            contextMenu.showAt(targetButton, lastEventX, lastEventY)
        }
        assertEquals("true", targetButton.nativeWidget.getAttribute("data-has-context-menu"))
        assertTrue("contextmenu" in targetButton.nativeWidget.eventListeners)

        // 1. Right-click activation where a mouse is available
        targetButton.nativeWidget.simulateRightClick(320, 210)
        assertEquals(320, lastEventX)
        assertEquals(210, lastEventY)
        assertTrue(contextMenu.nativeWidget.classList.contains("show"))
        assertEquals("320px", contextMenu.nativeWidget.style["left"])
        assertEquals("210px", contextMenu.nativeWidget.style["top"])

        // Click item to execute and close
        cutItem.nativeWidget.click()
        assertEquals(1, cutTriggered)
        assertFalse(contextMenu.nativeWidget.classList.contains("show"))

        // 2. Short touch (< 500ms) on mobile does NOT activate context menu
        val shortPressFired = targetButton.nativeWidget.simulateLongPress(140, 160, durationMs = 250)
        assertFalse(shortPressFired)
        assertFalse(contextMenu.nativeWidget.classList.contains("show"))

        // 3. Touch drag (> 10px movement) on mobile cancels long press
        val dragPressFired = targetButton.nativeWidget.simulateLongPress(
            x = 140,
            y = 160,
            durationMs = 600,
            moveDeltaX = 18,
            moveDeltaY = 12
        )
        assertFalse(dragPressFired)
        assertFalse(contextMenu.nativeWidget.classList.contains("show"))

        // 4. Touch long press (>= 500ms, stationary within 10px) on mobile activates context menu
        val longPressFired = targetButton.nativeWidget.simulateLongPress(
            x = 155,
            y = 275,
            durationMs = 520,
            moveDeltaX = 3,
            moveDeltaY = 4
        )
        assertTrue(longPressFired)
        assertEquals(155, lastEventX)
        assertEquals(275, lastEventY)
        assertTrue(contextMenu.nativeWidget.classList.contains("show"))
        assertEquals("155px", contextMenu.nativeWidget.style["left"])
        assertEquals("275px", contextMenu.nativeWidget.style["top"])
        assertSame(app.nativeApplication, contextMenu.nativeWidget.parent)
    }

    @Test
    fun testToolModalPanelWindowAndDockWindowStayOnTopOfOwnerWindow() {
        val factory = WebNativeFactory()
        factory.createApplication()
        val mainWindow = factory.createWindow() as WebWindowImpl
        mainWindow.nativeWidget.id = "studio-main-window"
        mainWindow.showWindow()

        val panelWindow = factory.createWindow() as WebWindowImpl
        panelWindow.configureAsToolModalWindow(
            ownerWindow = mainWindow,
            showInTaskbar = false,
            minimizable = false,
            alwaysOnTopOfOwner = true
        )
        assertEquals("true", panelWindow.nativeWidget.getAttribute("data-tool-modal"))
        assertEquals("false", panelWindow.nativeWidget.getAttribute("data-taskbar-entry"))
        assertEquals("false", panelWindow.nativeWidget.getAttribute("data-minimizable"))
        assertEquals("true", panelWindow.nativeWidget.getAttribute("data-always-on-top-of-owner"))
        assertEquals("studio-main-window", panelWindow.nativeWidget.getAttribute("data-owner-window"))

        val dockWindow = factory.createDockWindow() as WebDockWindowImpl
        assertEquals("true", dockWindow.nativeWidget.getAttribute("data-tool-modal"))
        assertEquals("false", dockWindow.nativeWidget.getAttribute("data-taskbar-entry"))
        assertEquals("false", dockWindow.nativeWidget.getAttribute("data-minimizable"))
        assertEquals("true", dockWindow.nativeWidget.getAttribute("data-always-on-top-of-owner"))
        dockWindow.attachToOwnerWindow(mainWindow)
        assertEquals("studio-main-window", dockWindow.nativeWidget.getAttribute("data-owner-window"))

        // Focusing / bringing mainWindow to the front brings all attached panel/dock windows above it
        mainWindow.bringToFront()
        val mainZ = mainWindow.nativeWidget.style["z-index"]?.toIntOrNull() ?: 0
        val panelZ = panelWindow.nativeWidget.style["z-index"]?.toIntOrNull() ?: 0
        val dockZ = dockWindow.nativeWidget.style["z-index"]?.toIntOrNull() ?: 0
        assertTrue(mainZ > 0)
        assertTrue(panelZ > mainZ, "Expected tool modal panelWindow z-index ($panelZ) > owner mainWindow z-index ($mainZ)")
        assertTrue(dockZ > mainZ, "Expected tool modal dockWindow z-index ($dockZ) > owner mainWindow z-index ($mainZ)")
    }
}
