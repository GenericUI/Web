package com.nexustools.genericui.web

import com.nexustools.genericui.component.shell.Application
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
        assertTrue(html.contains("<link id=\"genericui-resources-manifest\" rel=\"resources\" type=\"application/json\" href=\"resources.json?${WebSha1Hasher.sha1Hex("[]\n")}\">"))
        assertTrue(html.contains("<link id=\"genericui-css\" rel=\"stylesheet\" href=\"gui/genericui.css?${WebNamespaceSpec.defaultGenericUiCssSha1Hash}\">"))
        assertTrue(html.contains("<genericui:app></genericui:app>"))
        assertFalse(html.contains("<genericui:app id="))
        assertFalse(html.contains("<genericui:app data-bs-theme="))
        assertTrue(html.contains("<link id=\"genericui-bootstrap-css\" rel=\"stylesheet\" href=\"gui/bootstrap.min.css\">"))
        assertTrue(html.contains("<script id=\"genericui-bootstrap-js\" src=\"gui/bootstrap.bundle.min.js\" defer></script>"))
        assertTrue(html.contains("<link id=\"genericui-bootstrap-icons-css\" rel=\"stylesheet\" href=\"gui/bootstrap-icons.min.css\">"))
        assertFalse(html.contains("cdn.jsdelivr.net"))
        assertTrue(html.contains("data-bs-theme=\"dark\""))
        assertTrue(html.contains("<link id=\"genericui-favicon\" rel=\"icon\" type=\"image/svg+xml\" href=\"icon.svg\">"))
        assertTrue(html.contains("id=\"genericui-splash\""))
        assertTrue(html.contains("<span id=\"genericui-splash-status-icon\" class=\"genericui-splash-status-icon spinner-border spinner-border-sm\" aria-hidden=\"true\"></span><span id=\"genericui-splash-status-text\" class=\"genericui-splash-status-text\">Initializing</span>"))
        assertTrue(WebWasmJsLoader.DEFAULT_LOADER_SCRIPT.contains("bi bi-check-circle-fill text-success"))
        assertTrue(WebWasmJsLoader.DEFAULT_LOADER_SCRIPT.contains("\"Error\": \"genericui-splash-status-icon bi bi-dash-circle-fill text-danger\""))
        assertTrue(WebNamespaceSpec.DEFAULT_GENERICUI_CSS.contains(".genericui-splash-status-icon {"))
        assertTrue(html.contains("id=\"genericui-splash-icon\" src=\"icon.svg\" alt=\"Application Icon\" class=\"genericui-splash-icon mb-3 rounded\" width=\"64\" height=\"64\""))
        assertTrue(html.contains("id=\"genericui-splash-progress-bar\""))
        assertTrue(html.contains("id=\"genericui-splash-error\""))
        assertTrue(html.contains("id=\"genericui-splash-error-stack\""))
        assertTrue(html.contains("<script type=\"loader-wasm\" src=\"app-wasm.js?1111111111111111111111111111111111111111\"></script>"))
        assertTrue(html.contains("<script type=\"loader-js\" src=\"app-js.js?2222222222222222222222222222222222222222\"></script>"))
        assertTrue(html.contains("<script id=\"genericui-loader-script\" src=\"loader.js?${WebWasmJsLoader.defaultLoaderSha1Hash}\" defer></script>"))
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

        // <genericui:app> must NEVER have <link>, <style>, or <script> child elements injected inside it
        assertEquals("genericui:app", app.nativeApplication.tagName)
        assertTrue(app.nativeApplication.children.none { it.tagName in setOf("link", "style", "script") })

        // Built-in stylesheets are mounted in <head> via WebStyleSheetManager
        val genericUiLink = WebStyleSheetManager.getHeadStyleSheetLink("genericui-css")
        val bootstrapLink = WebStyleSheetManager.getHeadStyleSheetLink("genericui-bootstrap-css")
        val iconsLink = WebStyleSheetManager.getHeadStyleSheetLink("genericui-bootstrap-icons-css")

        assertNotNull(genericUiLink)
        assertNotNull(bootstrapLink)
        assertNotNull(iconsLink)
        assertEquals("gui/genericui.css", genericUiLink.getAttribute("href"))
        assertEquals("gui/bootstrap.min.css", bootstrapLink.getAttribute("href"))
        assertEquals("gui/bootstrap-icons.min.css", iconsLink.getAttribute("href"))
    }

    @Test
    fun testWebBaseNativeImplSpaceSeparatedPrefixAndComputedStyleTypedPrecedence() {
        val impl = WebBaseNativeImpl("div", "menu modal-panel")
        assertTrue(impl.nativeWidget.enumClasses.contains("menu"))
        assertTrue(impl.nativeWidget.enumClasses.contains("modal-panel"))
        assertFalse(impl.hasClass("menu"))

        impl.addClass("border")
        impl.setTextAlignment(com.nexustools.genericui.enums.bootstrap.TextAlignment.CENTER)
        assertTrue(impl.hasClass("border"))
        assertFalse(impl.hasClass("text-center"))
        assertTrue(impl.nativeWidget.enumClasses.contains("text-center"))

        val compStyle = com.nexustools.genericui.style.css.ComponentStyle().apply {
            setTransform(com.nexustools.genericui.style.transition.CssTransform2D.parse("translate(-50%, -50%) scale(1.5, 1.5)"))
            setLeft(com.nexustools.genericui.style.css.CssLength.percent(50f))
        }
        impl.applyComponentStyle(compStyle)
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
        assertEquals("genericui:splitpane", (factory.createContainer("split-pane", "genericui:splitpane") as WebContainerImpl).nativeWidget.tagName)
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
        assertEquals("genericui:collapsiblepanel", (factory.createContainer("collapse", "genericui:collapsiblepanel") as WebContainerImpl).nativeWidget.tagName)
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
        assertTrue(WebWasmJsLoader.DEFAULT_LOADER_SCRIPT.contains("0x01, 0x03, 0x01, 0x5f, 0x00"))
        assertFalse(WebWasmJsLoader.DEFAULT_LOADER_SCRIPT.contains("0x01, 0x04, 0x01, 0x5f, 0x00, 0x0b"))
        assertTrue(WebWasmJsLoader.DEFAULT_LOADER_SCRIPT.contains("warnWasmUnsupported"))
        assertTrue(WebWasmJsLoader.DEFAULT_LOADER_SCRIPT.contains("[GenericUI Loader] WebAssembly detected as not supported:"))
    }

    @Test
    fun testLoaderPreloadsResourcesFallsBackToJsAndErrorStackIsSelectable() {
        val loader = WebWasmJsLoader.DEFAULT_LOADER_SCRIPT
        // Resources are always preloaded into the window global before Wasm/JS, regardless of manifest.webmanifest
        assertTrue(loader.contains("__GENERICUI_PRELOADED_RESOURCES__"))
        assertTrue(loader.contains("link[rel=\"resources\"]"))
        assertTrue(loader.contains("buildHashedResourceUrl"))
        assertTrue(loader.contains("./resources.json"))
        assertTrue(loader.indexOf("return preloadAllResources(config)") < loader.indexOf("return loadWasmPrimaryWithFallback(config);"))
        // WebAssembly failures log silently to the console and show "Loading Fallback" while loading the JS bundle
        assertTrue(loader.contains("\"Loading Fallback\""))
        assertTrue(loader.contains("logWasmFallbackErrorSilently(wasmErr);"))
        assertTrue(loader.contains("return loadJsIrFallback(config, STATUS_LOADING_FALLBACK, wasmErr);"))
        // Verbose diagnostics: cause chains with stacktraces, HTTP status/URL context, WebAssembly call context, loader log
        assertTrue(loader.contains("\"Caused by: \""))
        assertTrue(loader.contains("httpStatus: response.status"))
        assertTrue(loader.contains("genericUiWasmContext"))
        assertTrue(loader.contains("Loader log (most recent last):"))
        assertTrue(loader.contains("Both the WebAssembly bundle and the JS fallback bundle failed to load"))
        assertTrue(WebSplashConfig.STATUS_LOADING_FALLBACK in WebSplashConfig.ALLOWED_STATUS_MESSAGES)
        assertEquals(WebSplashConfig.STATUS_LOADING_FALLBACK, WebSplashConfig.normalizeStatusText("loading fallback bundle"))
        // Splash error stacktrace <pre> is text-selectable
        val html = WebHtmlTemplateEngine().renderIndexHtml(title = "Selectable")
        assertTrue(WebNamespaceSpec.DEFAULT_GENERICUI_CSS.contains("user-select: text !important"))
        assertTrue(WebNamespaceSpec.DEFAULT_GENERICUI_CSS.contains("#genericui-splash-error-stack"))
        assertTrue(WebNamespaceSpec.DEFAULT_GENERICUI_CSS.contains(".genericui-splash.genericui-splash-hidden"))
        // Splash markup carries no inline style="" attributes; all static styling lives in genericui.css
        val splashMarkup = html.substring(html.indexOf("id=\"genericui-splash\""), html.indexOf("<genericui:app>"))
        assertFalse(splashMarkup.contains("style=\""))
        assertTrue(html.contains("<link id=\"genericui-css\" rel=\"stylesheet\" href=\"gui/genericui.css?"))
    }

    @Test
    fun testAllThreeThemeModesInIndexHtmlAndAutoThemeDetectionInLoader() {
        val engine = WebHtmlTemplateEngine()

        val autoHtml = engine.renderIndexHtml(title = "Auto App", themeMode = ThemeMode.AUTO)
        assertTrue(autoHtml.contains("<html lang=\"en\" xmlns:genericui=\"https://nexustools.com/ns/genericui\" data-bs-theme=\"auto\">"))

        val darkHtml = engine.renderIndexHtml(title = "Dark App", themeMode = ThemeMode.DARK)
        assertTrue(darkHtml.contains("<html lang=\"en\" xmlns:genericui=\"https://nexustools.com/ns/genericui\" data-bs-theme=\"dark\">"))

        val lightHtml = engine.renderIndexHtml(title = "Light App", themeMode = ThemeMode.LIGHT)
        assertTrue(lightHtml.contains("<html lang=\"en\" xmlns:genericui=\"https://nexustools.com/ns/genericui\" data-bs-theme=\"light\">"))

        val loader = WebWasmJsLoader.DEFAULT_LOADER_SCRIPT
        assertTrue(loader.contains("document.documentElement.setAttribute('data-bs-theme', theme);"))
        assertTrue(loader.contains("currentTheme === \"auto\""))
        assertTrue(loader.contains("matchMedia('(prefers-color-scheme: dark)').matches"))
        assertTrue(loader.contains("isDarkMode ? 'Dark mode' : 'Light mode'"))
        assertTrue(loader.contains("setTheme(isDarkMode ? 'dark' : 'light');"))
        assertTrue(
            loader.indexOf("applyInitialBrowserTheme();") < loader.indexOf("var STATUS_INITIALIZING"),
            "Auto theme detection and conversion to dark/light must run at the top of the loader before anything else"
        )
    }

    @Test
    fun testApplicationDrivenWebSplashLifecycleAndCustomStatusCallback() {
        val loader = WebWasmJsLoader.DEFAULT_LOADER_SCRIPT
        assertTrue(loader.contains("function setStatus(statusText, bootstrapType)"))
        assertTrue(loader.contains("function setProgress(percent, bootstrapType)"))
        assertTrue(loader.contains("function setError(message, stackTrace, title)"))
        assertTrue(loader.contains("setStatus: setStatus"))
        assertTrue(loader.contains("setProgress: setProgress"))
        assertTrue(loader.contains("setError: setError"))
        assertTrue(loader.contains("function setApplicationStatus(statusText, percent)"))
        assertTrue(loader.contains("setApplicationStatus: setApplicationStatus"))
        assertTrue(loader.contains("if (!config.wasmJsUrl && !config.jsIrUrl)"))
        assertTrue(
            loader.indexOf("state.wasmSupported = detectWasmSupport();") < loader.indexOf("return preloadAllResources(config)"),
            "Loader must detect WebAssembly support upfront before preloading resources so totalBytes includes the active bundle size"
        )
        assertTrue(loader.contains("state.totalBytes = state.resourceBytes + appBundleBytes;"))

        // 1. Default Application automatically finishes the splash screen once mainWindow is shown
        WebBrowserDomBridge.resetHeadlessWebSplashState()
        val webFactory = WebNativeFactory()
        assertTrue(webFactory.isWebSplashSupported)
        val defaultApp = Application(com.nexustools.genericui.component.layout.Container(), "DefaultWebApp")
        defaultApp.run(webFactory)
        assertTrue(defaultApp.isWebSplashSupported)
        assertTrue(WebBrowserDomBridge.isWebSplashFinished)
        assertEquals(WebSplashConfig.STATUS_READY, WebBrowserDomBridge.lastWebSplashStatus)
        assertEquals(100, WebBrowserDomBridge.lastWebSplashProgress)

        // 2. Application overriding finishWebSplash() keeps the splash screen visible with custom status/progress until super.finishWebSplash() is invoked
        WebBrowserDomBridge.resetHeadlessWebSplashState()
        var completeDeferredSplash: (() -> Unit)? = null
        val deferredApp = object : Application(com.nexustools.genericui.component.layout.Container(), "DeferredWebApp") {
            override fun finishWebSplash() {
                setWebSplashStatus("Initializing")
                setWebSplashProgress(99, com.nexustools.genericui.enums.bootstrap.BootstrapType.INFO)
                completeDeferredSplash = {
                    super.finishWebSplash()
                }
            }
        }
        deferredApp.run(WebNativeFactory())
        assertFalse(WebBrowserDomBridge.isWebSplashFinished)
        assertEquals("Initializing", WebBrowserDomBridge.lastWebSplashStatus)
        assertEquals(com.nexustools.genericui.enums.bootstrap.BootstrapType.NONE, WebBrowserDomBridge.lastWebSplashStatusType)
        assertEquals(99, WebBrowserDomBridge.lastWebSplashProgress)
        assertEquals(com.nexustools.genericui.enums.bootstrap.BootstrapType.INFO, WebBrowserDomBridge.lastWebSplashProgressType)

        // 3. Verify BootstrapType.DANGER status and setWebSplashError state before finishing
        deferredApp.setWebSplashStatus("Connection Failed", com.nexustools.genericui.enums.bootstrap.BootstrapType.DANGER)
        assertEquals("Connection Failed", WebBrowserDomBridge.lastWebSplashStatus)
        assertEquals(com.nexustools.genericui.enums.bootstrap.BootstrapType.DANGER, WebBrowserDomBridge.lastWebSplashStatusType)

        deferredApp.setWebSplashError("Server unreachable", "at App.connect(App.kt:42)", "Connection Error")
        assertEquals("Server unreachable", WebBrowserDomBridge.lastWebSplashErrorMessage)
        assertEquals("at App.connect(App.kt:42)", WebBrowserDomBridge.lastWebSplashErrorStack)
        assertEquals("Connection Error", WebBrowserDomBridge.lastWebSplashErrorTitle)

        val finishCallback = completeDeferredSplash
        assertNotNull(finishCallback)
        finishCallback.invoke()
        assertTrue(WebBrowserDomBridge.isWebSplashFinished)
        assertEquals(WebSplashConfig.STATUS_READY, WebBrowserDomBridge.lastWebSplashStatus)
        assertEquals(100, WebBrowserDomBridge.lastWebSplashProgress)
    }
}
