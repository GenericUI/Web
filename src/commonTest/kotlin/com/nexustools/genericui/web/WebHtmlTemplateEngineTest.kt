package com.nexustools.genericui.web

import com.nexustools.genericui.enums.ui.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WebHtmlTemplateEngineTest {

    @Test
    fun testRenderIndexHtmlContainsBootstrapCdnSplashAndHashedLoaderScripts() {
        val engine = WebHtmlTemplateEngine()
        val html = engine.renderIndexHtml(
            title = "My GenericUI Web App",
            themeMode = ThemeMode.DARK,
            wasmJsUrl = "app-wasm.js",
            wasmHash = "1111111111111111111111111111111111111111",
            jsIrUrl = "app-js.js",
            jsHash = "2222222222222222222222222222222222222222",
            webIconUrl = "icon.svg"
        )

        assertTrue(html.contains("<html lang=\"en\" xmlns:genericui=\"https://nexustools.com/ns/genericui\" data-bs-theme=\"dark\">"))
        assertTrue(html.contains("<title>My GenericUI Web App</title>"))
        assertTrue(html.contains("<link id=\"genericui-namespace-spec\" rel=\"namespace\" data-namespace-prefix=\"genericui\" data-namespace-uri=\"https://nexustools.com/ns/genericui\" type=\"application/xml\" href=\"genericui-namespace.xml?${WebNamespaceSpec.defaultNamespaceXmlSha1Hash}\">"))
        assertTrue(html.contains("<style id=\"genericui-namespace-styles\">"))
        assertTrue(html.contains("genericui\\:menubar"))
        assertTrue(html.contains("https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"))
        assertTrue(html.contains("src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js\""))
        assertTrue(html.contains("https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css"))
        assertTrue(html.contains("data-bs-theme=\"dark\""))
        assertTrue(html.contains("<link id=\"genericui-favicon\" rel=\"icon\" type=\"image/svg+xml\" href=\"icon.svg\">"))
        assertTrue(html.contains("id=\"genericui-splash\""))
        assertTrue(html.contains("id=\"genericui-splash-icon\" src=\"icon.svg\" alt=\"\" width=\"64\" height=\"64\""))
        assertTrue(html.contains("id=\"genericui-splash-progress-bar\""))
        assertTrue(html.contains("id=\"genericui-splash-error\""))
        assertTrue(html.contains("id=\"genericui-splash-error-stack\""))
        assertTrue(html.contains("<script type=\"loader-wasm\" src=\"app-wasm.js?1111111111111111111111111111111111111111\"></script>"))
        assertTrue(html.contains("<script type=\"loader-js\" src=\"app-js.js?2222222222222222222222222222222222222222\"></script>"))
        assertTrue(html.contains("<script id=\"genericui-loader-script\" src=\"genericui-loader.js?${WebWasmJsLoader.defaultLoaderSha1Hash}\" defer></script>"))
        assertFalse(html.contains("{{APP_BUNDLE_SCRIPT}}"))
    }

    @Test
    fun testRenderWebAppManifestAndAndroidChromeInstallableHeadTags() {
        val engine = WebHtmlTemplateEngine(
            themeColor = "#2A80FF",
            manifestConfig = WebAppManifestConfig(
                enabled = true,
                allowInstallAsWebApp = true,
                shortName = "GenericWebApp",
                description = "Cross-platform web application showcase",
                display = "standalone",
                orientation = "any",
                backgroundColor = "#0B0E14",
                categories = listOf("productivity", "utilities")
            )
        )

        val manifestJson = engine.renderManifestJson(
            title = "GenericUI Web App",
            webIconUrl = "icon.svg"
        )
        assertTrue(manifestJson.contains("\"name\": \"GenericUI Web App\""))
        assertTrue(manifestJson.contains("\"short_name\": \"GenericWebApp\""))
        assertTrue(manifestJson.contains("\"display\": \"standalone\""))
        assertTrue(manifestJson.contains("\"theme_color\": \"#2A80FF\""))
        assertTrue(manifestJson.contains("\"background_color\": \"#0B0E14\""))
        assertTrue(manifestJson.contains("\"sizes\": \"192x192 512x512\""))

        val html = engine.renderIndexHtml(
            title = "GenericUI Web App",
            webIconUrl = "icon.svg"
        )
        assertTrue(html.contains("<link id=\"genericui-web-manifest\" rel=\"manifest\" href=\"manifest.webmanifest?"))
        assertTrue(html.contains("<meta id=\"genericui-theme-color\" name=\"theme-color\" content=\"#2A80FF\">"))
        assertTrue(html.contains("<meta id=\"genericui-mobile-web-app-capable\" name=\"mobile-web-app-capable\" content=\"yes\">"))
        assertTrue(html.contains("<meta id=\"genericui-apple-mobile-web-app-capable\" name=\"apple-mobile-web-app-capable\" content=\"yes\">"))
        assertTrue(html.contains("<link id=\"genericui-apple-touch-icon\" rel=\"apple-touch-icon\" href=\"icon.svg\">"))
    }

    @Test
    fun testWebApplicationInjectsCdnResourceElements() {
        val factory = WebNativeFactory()
        val app = factory.createApplication() as WebApplicationImpl
        val cssLink = app.nativeApplication.children.firstOrNull { it.id == "genericui-bootstrap-css" }
        val jsScript = app.nativeApplication.children.firstOrNull { it.id == "genericui-bootstrap-js" }

        assertNotNull(cssLink)
        assertNotNull(jsScript)
        assertTrue(cssLink.getAttribute("href")?.contains("bootstrap@5.3.3/dist/css/bootstrap.min.css") == true)
        assertTrue(jsScript.getAttribute("src")?.contains("bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js") == true)
    }

    @Test
    fun testWebBaseNativeImplSpaceSeparatedPrefixAndComputedStyleTypedPrecedence() {
        val impl = WebBaseNativeImpl("div", "menu modal-panel")
        assertTrue(impl.hasClass("menu"))
        assertTrue(impl.hasClass("modal-panel"))

        impl.addClass("border")
        impl.setTextAlignment(com.nexustools.genericui.enums.bootstrap.TextAlignment.CENTER)
        assertTrue(impl.hasClass("border"))
        assertTrue(impl.hasClass("text-center"))

        val staleRawStyle = com.nexustools.genericui.style.css.ComputedStyle(
            transform = "translate(-50%, -50%) scale(1.5, 1.5)",
            left = "50%",
            rawDeclarations = mapOf(
                "transform" to "translate(-50%, -50%)",
                "left" to "0px"
            )
        )
        impl.applyComputedStyle(staleRawStyle)
        assertEquals("translate(-50%, -50%) scale(1.5, 1.5)", impl.nativeWidget.style["transform"])
        assertEquals("50%", impl.nativeWidget.style["left"])
    }

    @Test
    fun testWebNamespaceSpecDefinesAllCustomComponentTagsAndXmlSpec() {
        assertEquals("genericui", WebNamespaceSpec.NAMESPACE_PREFIX)
        assertEquals("https://nexustools.com/ns/genericui", WebNamespaceSpec.NAMESPACE_URI)
        assertEquals("genericui-namespace.xml", WebNamespaceSpec.DEFAULT_NAMESPACE_XML_FILENAME)
        assertTrue(WebNamespaceSpec.defaultNamespaceXmlSha1Hash.length == 40)
        assertTrue(WebNamespaceSpec.DEFAULT_NAMESPACE_XML.contains("<namespaceSpec xmlns=\"https://nexustools.com/ns/genericui/spec\" prefix=\"genericui\" uri=\"https://nexustools.com/ns/genericui\" version=\"1.0\">"))

        for (tag in WebNamespaceSpec.CUSTOM_COMPONENT_TAGS) {
            assertTrue(
                WebNamespaceSpec.DEFAULT_NAMESPACE_XML.contains("tag=\"$tag\""),
                "Expected genericui-namespace.xml to declare custom tag '$tag'"
            )
        }
    }

    @Test
    fun testWebImplClassesGenerateButtonAndCustomGenericUiNamespaceTags() {
        val factory = WebNativeFactory()

        // Button uses <button type="button"> per Bootstrap .btn standard
        val btn = factory.createButton() as WebButtonImpl
        assertEquals("button", btn.nativeWidget.tagName)
        assertEquals("button", btn.nativeWidget.getAttribute("type"))

        // Shell & Dock custom tags
        assertEquals("genericui:menubar", (factory.createMenuBar() as WebMenuBarImpl).nativeWidget.tagName)
        assertEquals("genericui:menu", (factory.createMenu() as WebMenuImpl).nativeWidget.tagName)
        assertEquals("genericui:menuitem", (factory.createMenuItem(checkable = false, radio = false) as WebMenuItemImpl).nativeWidget.tagName)
        assertEquals("genericui:checkmenuitem", (factory.createMenuItem(checkable = true, radio = false) as WebMenuItemImpl).nativeWidget.tagName)
        assertEquals("genericui:radiomenuitem", (factory.createMenuItem(checkable = false, radio = true) as WebMenuItemImpl).nativeWidget.tagName)
        assertEquals("genericui:contextmenu", (factory.createContextMenu() as WebContextMenuImpl).nativeWidget.tagName)
        assertEquals("genericui:splitpane", (factory.createSplitPane() as WebSplitPaneImpl).nativeWidget.tagName)
        assertEquals("genericui:dockworkspace", (factory.createDockWorkspace() as WebDockWorkspaceImpl).nativeWidget.tagName)
        assertEquals("genericui:dockarea", (factory.createDockArea() as WebDockAreaImpl).nativeWidget.tagName)
        assertEquals("genericui:dockgroup", (factory.createDockGroup() as WebDockGroupImpl).nativeWidget.tagName)
        assertEquals("genericui:dockpanel", (factory.createDockPanel() as WebDockPanelImpl).nativeWidget.tagName)
        assertEquals("genericui:panelwindow", (factory.createDockWindow() as WebDockWindowImpl).nativeWidget.tagName)
        assertEquals("genericui:toolpalette", (factory.createToolPalette() as WebToolPaletteImpl).nativeWidget.tagName)

        // Layout, Surface, Tabs, Navigation, Action, Feedback, Overlay, Dialog, Lazy
        assertEquals("genericui:container", (factory.createContainer("container") as WebContainerImpl).nativeWidget.tagName)
        assertEquals("genericui:row", (factory.createContainer("row") as WebContainerImpl).nativeWidget.tagName)
        assertEquals("genericui:col", (factory.createContainer("col-md-6") as WebContainerImpl).nativeWidget.tagName)
        assertEquals("genericui:flexbox", (factory.createContainer("d-flex") as WebContainerImpl).nativeWidget.tagName)
        assertEquals("genericui:card", (factory.createContainer("card") as WebContainerImpl).nativeWidget.tagName)
        assertEquals("genericui:alert", (factory.createContainer("alert") as WebContainerImpl).nativeWidget.tagName)
        assertEquals("genericui:modal", (factory.createContainer("modal") as WebContainerImpl).nativeWidget.tagName)
        assertEquals("genericui:drawer", (factory.createContainer("offcanvas") as WebContainerImpl).nativeWidget.tagName)
        assertEquals("genericui:bottomsheet", (factory.createContainer("offcanvas-bottom") as WebContainerImpl).nativeWidget.tagName)
        assertEquals("genericui:titlebar", (factory.createContainer("titlebar") as WebContainerImpl).nativeWidget.tagName)
        assertEquals("genericui:statusbar", (factory.createContainer("statusbar") as WebContainerImpl).nativeWidget.tagName)
        assertEquals("genericui:mdicontainer", (factory.createContainer("mdi-container") as WebContainerImpl).nativeWidget.tagName)
        assertEquals("genericui:mdidesktop", (factory.createContainer("mdi-desktop-surface") as WebContainerImpl).nativeWidget.tagName)
        assertEquals("genericui:mdiwindow", (factory.createContainer("mdi-window") as WebContainerImpl).nativeWidget.tagName)

        // Standard semantic HTML container tags are preserved when passed explicitly
        assertEquals("div", (factory.createContainer("", "div") as WebContainerImpl).nativeWidget.tagName)
        assertEquals("header", (factory.createContainer("", "header") as WebContainerImpl).nativeWidget.tagName)
        assertEquals("footer", (factory.createContainer("", "footer") as WebContainerImpl).nativeWidget.tagName)
        assertEquals("main", (factory.createContainer("", "main") as WebContainerImpl).nativeWidget.tagName)
        assertEquals("section", (factory.createContainer("", "section") as WebContainerImpl).nativeWidget.tagName)

        assertEquals("genericui:scrollview", (factory.createScrollView() as WebScrollViewImpl).nativeWidget.tagName)
        assertEquals("genericui:markdownview", (factory.createMarkdownView() as WebMarkdownViewImpl).nativeWidget.tagName)
        assertEquals("genericui:dropdown", (factory.createDropdown() as WebDropdownImpl).nativeWidget.tagName)
        assertEquals("genericui:collapsiblepanel", (factory.createCollapse() as WebCollapseImpl).nativeWidget.tagName)
        assertEquals("genericui:tab", (factory.createTab(closable = false) as WebTabImpl).nativeWidget.tagName)
        assertEquals("genericui:closeabletab", (factory.createTab(closable = true) as WebTabImpl).nativeWidget.tagName)
        assertEquals("genericui:tabcontainer", (factory.createTabContainer() as WebTabContainerImpl).nativeWidget.tagName)
        assertEquals("genericui:navbar", (factory.createNavbar() as WebNavbarImpl).nativeWidget.tagName)
        assertEquals("genericui:bottomnavigationbar", (factory.createBottomNavigationBar() as WebBottomNavigationBarImpl).nativeWidget.tagName)
        assertEquals("genericui:progressbar", (factory.createProgressBar() as WebProgressBarImpl).nativeWidget.tagName)
        assertEquals("genericui:spinner", (factory.createSpinner() as WebSpinnerImpl).nativeWidget.tagName)
        assertEquals("genericui:popover", (factory.createPopover() as WebPopoverImpl).nativeWidget.tagName)
        assertEquals("genericui:dialogwindow", (factory.createDialogWindow() as WebDialogWindowImpl).nativeWidget.tagName)
        assertEquals("genericui:lazylist", (factory.createLazyList() as WebLazyListImpl).nativeWidget.tagName)
        assertEquals("genericui:lazygrid", (factory.createLazyGrid() as WebLazyGridImpl).nativeWidget.tagName)
        assertEquals("genericui:treeview", (factory.createTreeView() as WebTreeViewImpl).nativeWidget.tagName)

        // Loader JS completely removes the splash div from the DOM after fade-out
        assertTrue(WebWasmJsLoader.DEFAULT_LOADER_SCRIPT.contains("splash.remove()"))
        assertTrue(WebWasmJsLoader.DEFAULT_LOADER_SCRIPT.contains("splash.parentNode.removeChild(splash)"))
    }
}
