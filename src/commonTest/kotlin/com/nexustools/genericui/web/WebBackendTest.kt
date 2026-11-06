package com.nexustools.genericui.web

import com.nexustools.genericui.component.layout.AspectRatioContainer
import com.nexustools.genericui.component.layout.Container
import com.nexustools.genericui.component.layout.HybridColumnContainer
import com.nexustools.genericui.component.layout.HybridFoldMode
import com.nexustools.genericui.component.layout.HybridSplitNavigationContainer
import com.nexustools.genericui.component.layout.LayeredContainer
import com.nexustools.genericui.component.layout.StyledContainer
import com.nexustools.genericui.component.shell.Window
import com.nexustools.genericui.component.tabs.TabContainer
import com.nexustools.genericui.component.tabs.TabPosition
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.bootstrap.BootstrapAspectRatio
import com.nexustools.genericui.enums.ui.Placement
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

        assertEquals("genericui:app", app.nativeApplication.tagName)
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
        assertTrue(html.contains("<link id=\"genericui-css\" rel=\"stylesheet\" href=\"gui/genericui.css?${WebNamespaceSpec.defaultGenericUiCssSha1Hash}\">"))
        assertTrue(html.contains("<genericui:app></genericui:app>"))
        assertFalse(html.contains("<div id=\"genericui-app\""))

        // Verify genericui.css rules for app, scrollviews, fragment.scroll-fragment, and menuseparator
        val css = WebNamespaceSpec.DEFAULT_GENERICUI_CSS
        assertTrue(css.contains("genericui\\:app"))
        assertTrue(css.contains("position: absolute;"))
        assertTrue(css.contains("overflow: hidden;"))
        assertTrue(css.contains("hr.dropdown-divider,\ngenericui\\:menuseparator"))
        assertTrue(css.contains("genericui\\:fragment.scroll-fragment"))
        assertTrue(css.contains("genericui\\:app.scroll-fragment"))

        // Verify loader JS ensures <genericui:app> without id or data-bs-theme
        assertTrue(WebWasmJsLoader.DEFAULT_LOADER_JS.contains("ensureRootContainer"))
        assertTrue(WebWasmJsLoader.DEFAULT_LOADER_JS.contains("getElementsByTagName(\"genericui:app\")"))
        assertTrue(WebWasmJsLoader.DEFAULT_LOADER_JS.contains("removeAttribute(\"id\")"))
        assertTrue(WebWasmJsLoader.DEFAULT_LOADER_JS.contains("removeAttribute(\"data-bs-theme\")"))

        // Verify namespace mappings
        assertEquals("genericui:app", WebNamespaceSpec.inferTagForClassPrefix("app"))
        assertEquals("genericui:app", WebNamespaceSpec.inferTagForClassPrefix("root"))
        assertEquals("genericui:app", WebNamespaceSpec.inferTagForClassPrefix("genericui:app"))
        assertEquals("genericui:window", WebNamespaceSpec.inferTagForClassPrefix("window"))
        assertEquals("genericui:fragment", WebNamespaceSpec.inferTagForClassPrefix("fragment"))
        assertEquals("genericui:fragment-container", WebNamespaceSpec.inferTagForClassPrefix("fragment-container"))
        assertEquals("genericui:fragment", WebNamespaceSpec.inferTagForClassPrefix("scroll-fragment"))
    }

    @Test
    fun testTopMenuBarInsideGenericUiRootAndBootstrapDropdownMenus() {
        val factory = WebNativeFactory()
        val app = factory.createApplication() as WebApplicationImpl
        val window = factory.createWindow() as WebWindowImpl

        assertEquals("genericui:window", window.nativeWidget.tagName)
        assertTrue(window.nativeWidget.enumClasses.contains("genericui-window"))

        window.showWindow()
        assertTrue(app.nativeApplication.children.contains(window.nativeWidget))

        val menuBar = factory.createMenuBar() as WebMenuBarImpl
        assertEquals("genericui:menubar", menuBar.nativeWidget.tagName)
        assertTrue(menuBar.nativeWidget.enumClasses.contains("navbar"))
        assertTrue(menuBar.nativeWidget.enumClasses.contains("navbar-expand"))
        assertTrue(menuBar.nativeWidget.enumClasses.contains("genericui-menubar"))
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
        assertTrue(separator.nativeWidget.enumClasses.contains("dropdown-divider"))

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
        assertTrue(contextMenu.nativeWidget.enumClasses.contains("dropdown-menu"))
        assertTrue(contextMenu.nativeWidget.enumClasses.contains("genericui-context-menu"))

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

    @Test
    fun testTabPositionTopBottomAndAdaptiveHybridLayouts() {
        val factory = WebNativeFactory()
        GenericUI.activeFactory = factory
        factory.createApplication()

        val window = Window("Tab Test")
        val tabContainer = TabContainer()
        tabContainer.addTab("Queue", Container())
        tabContainer.addTab("Shows", Container())
        window.addChild(tabContainer)
        window.show()
        val webTabs = tabContainer.nativeImpl as WebTabContainerImpl

        assertEquals(TabPosition.TOP, tabContainer.tabPosition)
        assertEquals(TabPosition.TOP, webTabs.currentTabPosition)
        assertTrue(webTabs.nativeWidget.classList.contains(TabPosition.TOP.cssClass))
        assertSame(webTabs.navListElement, webTabs.nativeWidget.children.first())
        assertSame(webTabs.tabContentElement, webTabs.nativeWidget.children.last())

        tabContainer.tabPosition = TabPosition.BOTTOM
        assertEquals(TabPosition.BOTTOM, tabContainer.tabPosition)
        assertEquals(TabPosition.BOTTOM, webTabs.currentTabPosition)
        assertTrue(webTabs.nativeWidget.classList.contains(TabPosition.BOTTOM.cssClass))
        assertTrue(webTabs.navListElement.classList.contains("mt-auto"))
        assertSame(webTabs.tabContentElement, webTabs.nativeWidget.children.first())
        assertSame(webTabs.navListElement, webTabs.nativeWidget.children.last())

        // Verify HybridColumnContainer places tabs on bottom on portrait small screens and top on medium/large screens
        val hybridCol = HybridColumnContainer(foldMode = HybridFoldMode.COLLAPSE_ALL_COLUMNS_TO_TABS)
        hybridCol.addColumn(id = "col-a", title = "Col A", component = Container())
        hybridCol.addColumn(id = "col-b", title = "Col B", component = Container())
        hybridCol.addColumn(id = "col-c", title = "Col C", component = Container())

        hybridCol.onResize(390, 844)
        assertFalse(hybridCol.inColumnMode)
        assertEquals(TabPosition.BOTTOM, hybridCol.topTabBar.tabPosition)

        hybridCol.onResize(1280, 800)
        assertTrue(hybridCol.inColumnMode)
        assertEquals(TabPosition.TOP, hybridCol.topTabBar.tabPosition)

        // Verify HybridSplitNavigationContainer places tabs on bottom on portrait small screens and top on medium/large screens
        val splitNav = HybridSplitNavigationContainer(inColumnMode = false)
        splitNav.addSection(id = "movies", title = "Movies", content = Container())
        splitNav.addSection(id = "shows", title = "TV Shows", content = Container())
        splitNav.addSection(id = "music", title = "Music", content = Container())
        splitNav.addSection(id = "radio", title = "Radio", content = Container())
        splitNav.addSection(id = "podcasts", title = "Podcast", content = Container())
        var searchClicked = 0
        splitNav.addButtonSection(id = "search", title = "Search", icon = "bi-search") {
            searchClicked += 1
        }
        assertEquals(TabPosition.BOTTOM, splitNav.topTabLayout.tabPosition)

        window.addChild(splitNav)
        splitNav.inColumnMode = true
        assertEquals(TabPosition.TOP, splitNav.topTabLayout.tabPosition)
        val splitWebTabs = splitNav.topTabLayout.nativeImpl as WebTabContainerImpl
        assertEquals(6, splitWebTabs.navListElement.children.size)
        assertEquals(5, splitWebTabs.tabContentElement.children.size)
    }

    @Test
    fun testAspectRatioContainerAndWebBootstrapComponents() {
        val factory = WebNativeFactory()
        GenericUI.activeFactory = factory
        factory.createApplication()

        val ratio16x9 = AspectRatioContainer(BootstrapAspectRatio.RATIO_16X9)
        assertTrue(ratio16x9.hasClass("ratio"))
        assertTrue(ratio16x9.hasClass("ratio-16x9"))
        assertEquals(16f / 9f, ratio16x9.effectiveAspectRatio, 0.001f)

        val overlayLayered = LayeredContainer()
        val hudLayer = StyledContainer()
        overlayLayered.addFillLayer(hudLayer, zIndex = 10)
        ratio16x9.addChild(overlayLayered)
        overlayLayered.layout.impl.syncLayers()
        val window = Window("Aspect Ratio Test")
        window.addChild(ratio16x9)
        window.show()
        val overlayDom = (overlayLayered.nativeImpl as WebBaseNativeImpl).nativeWidget
        assertFalse(overlayDom.enumClasses.contains("position-relative"))
        assertFalse(overlayDom.classList.contains("position-relative"))
        assertEquals("absolute", overlayLayered.getStyleProperty("position"))
        assertEquals("0", overlayLayered.getStyleProperty("top")?.removeSuffix("px"))
        assertEquals("0", overlayLayered.getStyleProperty("left")?.removeSuffix("px"))
        assertEquals("100%", overlayLayered.getStyleProperty("width"))
        assertEquals("100%", overlayLayered.getStyleProperty("height"))

        val customRatio = AspectRatioContainer(widthRatio = 2f, heightRatio = 1f)
        assertTrue(customRatio.hasClass("ratio"))
        assertEquals(2f, customRatio.effectiveAspectRatio, 0.001f)
        assertEquals("50%", customRatio.getStyleProperty("--bs-aspect-ratio"))

        val dialog = factory.createDialogWindow() as WebDialogWindowImpl
        dialog.setTitle("Settings")
        assertTrue(dialog.nativeWidget.enumClasses.contains("modal"))
        assertTrue(dialog.modalDialog.classList.contains("modal-dialog"))
        assertTrue(dialog.modalContent.classList.contains("modal-content"))
        assertTrue(dialog.modalHeader.classList.contains("modal-header"))
        assertTrue(dialog.modalBody.classList.contains("modal-body"))

        val navbar = factory.createNavbar() as WebNavbarImpl
        navbar.setBrandTitle("Episode Caster")
        assertTrue(navbar.nativeWidget.enumClasses.contains("navbar"))
        assertTrue(navbar.containerFluid.classList.contains("container-fluid"))
        assertTrue(navbar.brandElement.classList.contains("navbar-brand"))
        assertEquals("Episode Caster", navbar.brandElement.textContent)

        val bottomNav = factory.createBottomNavigationBar() as WebBottomNavigationBarImpl
        assertTrue(bottomNav.nativeWidget.enumClasses.contains("navbar"))
        assertTrue(bottomNav.nativeWidget.enumClasses.contains("navbar-bottom"))
        assertTrue(bottomNav.nativeWidget.children.first().classList.contains("nav-pills"))

        val dropdown = factory.createDropdown() as WebDropdownImpl
        dropdown.setLabel("Actions")
        assertTrue(dropdown.nativeWidget.enumClasses.contains("dropdown"))
        assertTrue(dropdown.toggleButton.classList.contains("dropdown-toggle"))
        assertTrue(dropdown.menuList.classList.contains("dropdown-menu"))

        val progress = factory.createProgressBar() as WebProgressBarImpl
        progress.setProgress(0.65f)
        assertTrue(progress.nativeWidget.enumClasses.contains("progress"))
        assertTrue(progress.barElement.classList.contains("progress-bar"))
        assertEquals("65%", progress.barElement.style["width"])

        val popover = factory.createPopover() as WebPopoverImpl
        popover.setTitle("Details")
        popover.setPlacement(Placement.TOP)
        assertTrue(popover.nativeWidget.enumClasses.contains("popover"))
        assertTrue(popover.nativeWidget.classList.contains("bs-popover-top"))
        assertEquals("Details", popover.headerElement.textContent)
    }

    @Test
    fun testDialogBackdropBlurDismissPopupCaptureAndTextInputKeySuppression() {
        val factory = WebNativeFactory()
        GenericUI.activeFactory = factory
        factory.createApplication()
        com.nexustools.genericui.event.PopupMouseCaptureManager.clear()
        com.nexustools.genericui.event.InputFocusManager.reset()

        // 1. DialogWindow modal backdrop blur + click-outside dismiss vs inside click
        val dialog = factory.createDialogWindow() as WebDialogWindowImpl
        dialog.setTitle("Modal Dialog")
        dialog.setModal(true)
        var dialogClosedCount = 0
        dialog.setOnDialogClosed { dialogClosedCount++ }
        dialog.showDialog(null)
        assertTrue(dialog.isVisible())
        assertEquals("blur(6px)", dialog.backdropElement.style["backdrop-filter"])
        assertEquals("blur(6px)", dialog.backdropElement.style["-webkit-backdrop-filter"])
        assertEquals("blur(6px)", dialog.nativeWidget.style["backdrop-filter"])
        assertEquals("blur(6px)", dialog.nativeWidget.style["-webkit-backdrop-filter"])

        // Clicking inside modalDialog should NOT dismiss the dialog
        dialog.nativeWidget.dispatchFromBrowser("click", dialog.modalBody)
        assertTrue(dialog.isVisible())
        assertEquals(0, dialogClosedCount)

        // Clicking on nativeWidget backdrop outside modalDialog SHOULD dismiss the dialog
        dialog.nativeWidget.dispatchFromBrowser("click", dialog.nativeWidget)
        assertFalse(dialog.isVisible(), "Expected dialog.isVisible() to be false after backdrop click")
        assertEquals(1, dialogClosedCount)

        // 2. PopupMouseCaptureManager on component with hookMouseWhenPopupVisible = true
        val rootContainer = Container()
        val popupPanel = StyledContainer().apply {
            isVisible = false
            hookMouseWhenPopupVisible = true
        }
        val insideButton = com.nexustools.genericui.component.action.Button("Inside").apply {
            // attached inside popupPanel
        }
        val outsideButton = com.nexustools.genericui.component.action.Button("Outside")
        var insideClicks = 0
        var outsideClicks = 0
        insideButton.onClick = { insideClicks++ }
        outsideButton.onClick = { outsideClicks++ }
        popupPanel.addChild(insideButton)
        rootContainer.addChild(popupPanel)
        rootContainer.addChild(outsideButton)
        val hostWindow = Window("Popup Capture Host")
        hostWindow.addChild(rootContainer)
        hostWindow.show()

        popupPanel.isVisible = true
        assertTrue(com.nexustools.genericui.event.PopupMouseCaptureManager.hasActivePopup)

        // Clicking inside the popup should trigger insideButton and keep popup visible
        val insideDom = (insideButton.nativeImpl as WebBaseNativeImpl).nativeWidget
        insideDom.dispatchEvent("click")
        assertEquals(1, insideClicks)
        assertTrue(popupPanel.isVisible)
        assertTrue(com.nexustools.genericui.event.PopupMouseCaptureManager.hasActivePopup)

        // Clicking outsideButton while popup is open should dismiss popup and swallow outsideButton's click
        val outsideDom = (outsideButton.nativeImpl as WebBaseNativeImpl).nativeWidget
        outsideDom.dispatchEvent("click")
        assertFalse(
            popupPanel.isVisible,
            "Expected popupPanel.isVisible=false after outsideDom click (activePopupCount=${com.nexustools.genericui.event.PopupMouseCaptureManager.activePopupCount}, popupBounds=${popupPanel.bounds}, contains=${(popupPanel.nativeImpl as? WebBaseNativeImpl)?.nativeWidget?.containsDomElement(outsideDom)})"
        )
        assertFalse(com.nexustools.genericui.event.PopupMouseCaptureManager.hasActivePopup, "Expected hasActivePopup=false")
        assertEquals(0, outsideClicks)

        // Subsequent click on outsideButton now fires normally
        outsideDom.dispatchEvent("click")
        assertEquals(1, outsideClicks)

        // 2b. Direct PopupMouseCaptureManager.registerPopup (without custom hitTester) + nested subtree & same-gesture protection
        val directPopup = StyledContainer().apply {
            isVisible = false
            bounds = com.nexustools.genericui.geometry.Rect(0, 0, 200, 200)
        }
        val nestedRow = Container()
        val deepChildBtn = com.nexustools.genericui.component.action.Button("Deep Option")
        var deepClicks = 0
        deepChildBtn.onClick = { deepClicks++ }
        nestedRow.addChild(deepChildBtn)
        directPopup.addChild(nestedRow)
        rootContainer.addChild(directPopup)

        // Simulate opening directPopup during a pointerdown gesture, followed by click on the trigger button
        outsideDom.dispatchEvent("pointerdown")
        directPopup.isVisible = true
        com.nexustools.genericui.event.PopupMouseCaptureManager.registerPopup(directPopup) {
            directPopup.isVisible = false
        }
        outsideDom.dispatchEvent("pointerup")
        outsideDom.dispatchEvent("click")
        // Same-gesture click on the trigger button must NOT immediately dismiss directPopup
        assertTrue(directPopup.isVisible, "Popup opened during pointerdown must not self-dismiss on same-gesture click")
        assertTrue(com.nexustools.genericui.event.PopupMouseCaptureManager.hasActivePopup)

        // Clicking a deeply nested child inside directPopup must NOT dismiss directPopup
        val deepChildDom = (deepChildBtn.nativeImpl as WebBaseNativeImpl).nativeWidget
        deepChildDom.dispatchEvent("pointerdown")
        deepChildDom.dispatchEvent("pointerup")
        deepChildDom.dispatchEvent("click")
        assertEquals(1, deepClicks)
        assertTrue(directPopup.isVisible, "Clicking nested child inside registered popup must not dismiss it")

        // Clicking outsideButton with NaN coordinates must dismiss directPopup even when directPopup.bounds starts at (0,0)
        outsideDom.dispatchEvent("click")
        assertFalse(directPopup.isVisible, "Clicking outside with NaN coordinates must dismiss directPopup even if bounds starts at (0,0)")
        assertFalse(com.nexustools.genericui.event.PopupMouseCaptureManager.hasActivePopup)

        // 3. TextInput focus suppresses window/component shortcut key events
        val textField = com.nexustools.genericui.component.form.TextField()
        rootContainer.addChild(textField)
        var shortcutTriggered = 0
        rootContainer.onKeyDown = { shortcutTriggered++ }

        val rootDom = (rootContainer.nativeImpl as WebBaseNativeImpl).nativeWidget
        val inputDom = (textField.nativeImpl as WebBaseNativeImpl).nativeWidget

        rootDom.dispatchEvent("keydown", com.nexustools.genericui.event.KeyEvent(key = " ", code = "Space"))
        assertEquals(1, shortcutTriggered)

        // Focus text input -> shortcut key events on non-input components must be ignored
        inputDom.dispatchEvent("focus")
        assertTrue(com.nexustools.genericui.event.InputFocusManager.isTypingInTextInput)
        rootDom.dispatchEvent("keydown", com.nexustools.genericui.event.KeyEvent(key = " ", code = "Space"))
        assertEquals(1, shortcutTriggered)

        // Blur text input -> shortcut key events resume working
        inputDom.dispatchEvent("blur")
        assertFalse(com.nexustools.genericui.event.InputFocusManager.isTypingInTextInput)
        rootDom.dispatchEvent("keydown", com.nexustools.genericui.event.KeyEvent(key = " ", code = "Space"))
        assertEquals(2, shortcutTriggered)
    }
}
