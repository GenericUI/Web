package com.nexustools.genericui.web

import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.enums.ui.ThemeMode
import com.nexustools.genericui.nativeimpl.shell.ApplicationNativeImpl
import com.nexustools.genericui.res.Resources
import com.nexustools.genericui.style.css.PrecompiledStyleSheet
import com.nexustools.genericui.style.theme.SystemThemeDetector
import com.nexustools.genericui.style.theme.ThemeConfig

/**
 * Web implementation of [ApplicationNativeImpl] managing the root `<genericui:app>` DOM container,
 * document `data-bs-theme` attribute, optional top-level `<genericui:menubar>` / `<genericui:navbar>` chrome,
 * and `<head>` `<link rel="stylesheet">` synchronization via [WebStyleSheetManager].
 */
class WebApplicationImpl(
    val templateEngine: WebHtmlTemplateEngine = WebHtmlTemplateEngine()
) : ApplicationNativeImpl<WebDomElement> {
    override val nativeApplication: WebDomElement = WebDomElement(WebNamespaceSpec.APP_TAG)

    internal var topMenuBarElement: WebDomElement? = null
    private var themeMode: ThemeMode = ThemeMode.AUTO
    private var themeColor: String? = templateEngine.themeColor
    private var themeConfig: ThemeConfig = ThemeConfig.DEFAULT.let { base ->
        val tc = templateEngine.themeColor?.trim()?.takeIf { it.isNotEmpty() }
        if (tc != null) base.withPrimaryAccentColor(tc) else base
    }

    /**
     * Most recently applied Bootstrap `data-bs-theme` value (`"dark"` or `"light"`).
     */
    var appliedBootstrapTheme: String = "dark"
        private set

    init {
        activeApplication = this
        installSystemThemeHooks()
        SystemThemeDetector.backendDetector = { detectSystemThemeMode() }
        applyThemeNative(themeMode)
        if (!themeColor.isNullOrBlank()) {
            nativeApplication.setStyle("--genericui-theme-color", themeColor)
            nativeApplication.setAttribute("data-genericui-theme-color", themeColor!!)
        }
        if (WebBrowserDomBridge.isBrowserEnvironment) {
            WebBrowserDomBridge.bindApplicationRoot(nativeApplication)
        }
        mountDefaultHeadStyleSheets()
        if (themeConfig.hasColorOverrides) {
            WebStyleSheetManager.applyThemeConfigOverrides(
                nativeApplication,
                themeConfig,
                SystemThemeDetector.resolveTheme(themeMode)
            )
        }
    }

    private fun mountDefaultHeadStyleSheets() {
        Resources.getStyleSheet(Resources.GENERICUI)?.let { WebStyleSheetManager.addStyleSheet(it, nativeApplication) }
        Resources.getStyleSheet(Resources.BOOTSTRAP)?.let { WebStyleSheetManager.addStyleSheet(it, nativeApplication) }
        if (templateEngine.cdnConfig.includeBootstrapIcons) {
            Resources.getStyleSheet(Resources.BOOTSTRAP_ICONS)?.let { WebStyleSheetManager.addStyleSheet(it, nativeApplication) }
        }
    }

    internal fun attachTopMenuBar(menuBarDom: WebDomElement?) {
        val prev = topMenuBarElement
        if (prev != null && prev !== menuBarDom) {
            nativeApplication.removeChild(prev)
        }
        topMenuBarElement = menuBarDom
        if (menuBarDom != null) {
            menuBarDom.parent?.removeChild(menuBarDom)
            menuBarDom.parent = nativeApplication
            nativeApplication.children.add(0, menuBarDom)
            WebBrowserDomBridge.insertChildAt(nativeApplication, 0, menuBarDom)
            WebBrowserDomBridge.syncElementTree(nativeApplication)
        }
    }

    internal fun applyThemeNative(mode: ThemeMode) {
        val resolved = SystemThemeDetector.resolveTheme(mode)
        val bsTheme = if (resolved == ThemeMode.LIGHT) "light" else "dark"
        appliedBootstrapTheme = bsTheme
        nativeApplication.removeAttribute("id")
        nativeApplication.removeAttribute("data-bs-theme")
        WebBrowserDomBridge.syncBootstrapTheme(bsTheme)
        WebBrowserDomBridge.setDocumentTheme(bsTheme)
        if (themeConfig.hasColorOverrides) {
            WebStyleSheetManager.applyThemeConfigOverrides(nativeApplication, themeConfig, resolved)
        }
    }

    /**
     * Ensures the built-in stylesheets are registered in `<head>` via [WebStyleSheetManager].
     * Never injects `<link>`, `<style>`, or `<script>` elements into `<genericui:app>`.
     */
    fun ensureBootstrapCdnInjected() {
        mountDefaultHeadStyleSheets()
    }

    /**
     * Renders the complete HTML5 page for this application using [templateEngine], its
     * configured Bootstrap CDN CSS & JS links, splash icon/progress bar, favicon,
     * optional Web App Manifest / PWA links, and Wasm-first / JS-IR-fallback loader script.
     */
    fun renderIndexHtml(
        title: String = "GenericUI Web Application",
        appBundleScriptUrl: String? = null,
        wasmJsUrl: String? = null,
        wasmBinaryUrl: String? = null,
        jsIrUrl: String? = null,
        webIconUrl: String? = null,
        themeColor: String? = this.themeColor ?: templateEngine.themeColor,
        splash: WebSplashConfig = templateEngine.splashConfig,
        manifest: WebAppManifestConfig = templateEngine.manifestConfig
    ): String = templateEngine.renderIndexHtml(
        title = title,
        themeMode = themeMode,
        themeColor = themeColor,
        rootContainerId = nativeApplication.id.ifEmpty { "genericui-app" },
        appBundleScriptUrl = appBundleScriptUrl,
        wasmJsUrl = wasmJsUrl,
        wasmBinaryUrl = wasmBinaryUrl,
        jsIrUrl = jsIrUrl,
        webIconUrl = webIconUrl,
        splash = splash,
        manifest = manifest
    )

    override fun setThemeMode(mode: ThemeMode) {
        themeMode = mode
        themeConfig = themeConfig.withMode(mode)
        applyThemeNative(mode)
    }

    override fun getThemeMode(): ThemeMode = themeMode

    override fun detectSystemThemeMode(): ThemeMode {
        SystemThemeDetector.overrideDetectedTheme?.let {
            if (!it.isAuto) return it
        }
        if (WebBrowserDomBridge.isBrowserEnvironment) {
            installSystemThemeHooks()
            val isDark = WebBrowserDomBridge.isBrowserDarkMode(null)
            return if (isDark) ThemeMode.DARK else ThemeMode.LIGHT
        }
        return if (SystemThemeDetector.fallbackSystemTheme == ThemeMode.LIGHT) ThemeMode.LIGHT else ThemeMode.DARK
    }

    override fun setThemeColor(colorHex: String?) {
        val clean = colorHex?.trim()?.takeIf { it.isNotEmpty() }
        themeColor = clean
        templateEngine.themeColor = clean
        nativeApplication.setStyle("--genericui-theme-color", clean)
        if (clean != null) {
            nativeApplication.setAttribute("data-genericui-theme-color", clean)
            themeConfig = themeConfig.withPrimaryAccentColor(clean)
        } else {
            nativeApplication.removeAttribute("data-genericui-theme-color")
            themeConfig = themeConfig.copy(
                colors = themeConfig.colors.copy(primary = null, accent = null)
            )
        }
        val resolved = SystemThemeDetector.resolveTheme(themeMode)
        WebStyleSheetManager.applyThemeConfigOverrides(nativeApplication, themeConfig, resolved)
    }

    override fun getThemeColor(): String? = themeColor

    override fun setThemeConfig(config: ThemeConfig) {
        themeConfig = config
        themeMode = config.mode
        val primaryColor = config.colors.primary ?: config.colors.accent
        if (primaryColor != null) {
            val hex = primaryColor.toHex(includeAlpha = false)
            themeColor = hex
            templateEngine.themeColor = hex
            nativeApplication.setStyle("--genericui-theme-color", hex)
            nativeApplication.setAttribute("data-genericui-theme-color", hex)
        }
        if (!config.styleSheet.isNullOrBlank()) {
            WebStyleSheetManager.addStyleSheet(config.styleSheet!!, nativeApplication)
        }
        applyThemeNative(themeMode)
        val resolved = SystemThemeDetector.resolveTheme(themeMode)
        WebStyleSheetManager.applyThemeConfigOverrides(nativeApplication, themeConfig, resolved)
    }

    override fun getThemeConfig(): ThemeConfig = themeConfig

    override fun addStyleSheet(styleSheet: PrecompiledStyleSheet) {
        WebStyleSheetManager.addStyleSheet(styleSheet, nativeApplication)
    }

    override fun removeStyleSheet(styleSheet: PrecompiledStyleSheet) {
        WebStyleSheetManager.removeStyleSheet(styleSheet, nativeApplication)
    }

    override fun runEventLoop(onStarted: () -> Unit) {
        activeApplication = this
        if (WebBrowserDomBridge.isBrowserEnvironment) {
            WebBrowserDomBridge.bindApplicationRoot(nativeApplication)
        }
        WebBrowserDomBridge.setWebSplashStatus(WebSplashConfig.STATUS_LOADING_APPLICATION, BootstrapType.NONE)
        onStarted()
    }

    override fun exitApplication(exitCode: Int) {
        nativeApplication.clearChildren()
    }

    override fun dispose() {
        nativeApplication.remove()
        if (activeApplication === this) {
            activeApplication = null
        }
    }

    companion object {
        var activeApplication: WebApplicationImpl? = null
            internal set
        private var browserThemeListenerInstalled: Boolean = false

        internal fun installSystemThemeHooks() {
            if (WebBrowserDomBridge.isBrowserEnvironment && !browserThemeListenerInstalled) {
                browserThemeListenerInstalled = true
                val isDark = WebBrowserDomBridge.isBrowserDarkMode { darkNow ->
                    val nextTheme = if (darkNow) ThemeMode.DARK else ThemeMode.LIGHT
                    SystemThemeDetector.fallbackSystemTheme = nextTheme
                    activeApplication?.let { app ->
                        if (app.themeMode.isAuto) {
                            app.applyThemeNative(ThemeMode.AUTO)
                        }
                    }
                    SystemThemeDetector.notifySystemThemeChanged(nextTheme)
                }
                SystemThemeDetector.fallbackSystemTheme = if (isDark) ThemeMode.DARK else ThemeMode.LIGHT
            }
            if (SystemThemeDetector.backendDetector == null) {
                SystemThemeDetector.backendDetector = {
                    activeApplication?.detectSystemThemeMode()
                        ?: if (WebBrowserDomBridge.isBrowserEnvironment) {
                            if (WebBrowserDomBridge.isBrowserDarkMode()) ThemeMode.DARK else ThemeMode.LIGHT
                        } else {
                            SystemThemeDetector.fallbackSystemTheme
                        }
                }
            }
            SystemThemeDetector.onThemeAppliedHook = { configured, resolved ->
                val bsTheme = if (resolved == ThemeMode.LIGHT) "light" else "dark"
                activeApplication?.let { app ->
                    app.themeMode = configured
                    app.themeConfig = app.themeConfig.withMode(configured)
                    app.appliedBootstrapTheme = bsTheme
                    if (app.themeConfig.hasColorOverrides) {
                        WebStyleSheetManager.applyThemeConfigOverrides(app.nativeApplication, app.themeConfig, resolved)
                    }
                }
                WebBrowserDomBridge.syncBootstrapTheme(bsTheme)
                WebBrowserDomBridge.setDocumentTheme(bsTheme)
            }
            SystemThemeDetector.onThemeConfigChangedHook = { _, config, resolved ->
                val bsTheme = if (resolved == ThemeMode.LIGHT) "light" else "dark"
                activeApplication?.let { app ->
                    app.themeConfig = config
                    app.themeMode = config.mode
                    app.appliedBootstrapTheme = bsTheme
                }
                WebBrowserDomBridge.syncBootstrapTheme(bsTheme)
                WebBrowserDomBridge.setDocumentTheme(bsTheme)
                WebStyleSheetManager.applyThemeConfigOverrides(
                    activeApplication?.nativeApplication,
                    config,
                    resolved
                )
            }
            SystemThemeDetector.onStyleSheetChangedHook = { app ->
                val root = activeApplication?.nativeApplication
                WebStyleSheetManager.syncApplicationStyleSheets(app, root)
            }
        }
    }
}
