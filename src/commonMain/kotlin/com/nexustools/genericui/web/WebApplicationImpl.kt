package com.nexustools.genericui.web

import com.nexustools.genericui.enums.ui.ThemeMode
import com.nexustools.genericui.nativeimpl.shell.ApplicationNativeImpl

/**
 * Web implementation of [ApplicationNativeImpl] managing the root `#genericui-app` DOM body container,
 * Bootstrap `data-bs-theme` attribute, and [WebHtmlTemplateEngine] CDN resource injection.
 */
class WebApplicationImpl(
    val templateEngine: WebHtmlTemplateEngine = WebHtmlTemplateEngine()
) : ApplicationNativeImpl<WebDomElement> {
    override val nativeApplication: WebDomElement = WebDomElement("div").apply {
        id = "genericui-app"
        setAttribute("data-bs-theme", "dark")
    }

    private var themeMode: ThemeMode = ThemeMode.DARK
    private var themeColor: String? = templateEngine.themeColor
    private var cdnInjected: Boolean = false

    init {
        activeApplication = this
        if (!themeColor.isNullOrBlank()) {
            nativeApplication.setStyle("--genericui-theme-color", themeColor)
            nativeApplication.setAttribute("data-genericui-theme-color", themeColor!!)
        }
        if (WebBrowserDomBridge.isBrowserEnvironment) {
            WebBrowserDomBridge.bindApplicationRoot(nativeApplication)
        } else {
            ensureBootstrapCdnInjected()
        }
    }

    /**
     * Ensures the Bootstrap 5 CSS and JS bundle CDN elements from [templateEngine]
     * are mounted in the application root.
     */
    fun ensureBootstrapCdnInjected() {
        if (!cdnInjected) {
            templateEngine.injectCdnResourcesInto(nativeApplication)
            cdnInjected = true
        }
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
        val bsTheme = when (mode) {
            ThemeMode.LIGHT -> "light"
            ThemeMode.DARK, ThemeMode.SYSTEM -> "dark"
        }
        nativeApplication.setAttribute("data-bs-theme", bsTheme)
        WebBrowserDomBridge.setDocumentTheme(bsTheme)
    }

    override fun getThemeMode(): ThemeMode = themeMode

    override fun setThemeColor(colorHex: String?) {
        val clean = colorHex?.trim()?.takeIf { it.isNotEmpty() }
        themeColor = clean
        templateEngine.themeColor = clean
        nativeApplication.setStyle("--genericui-theme-color", clean)
        if (clean != null) {
            nativeApplication.setAttribute("data-genericui-theme-color", clean)
        } else {
            nativeApplication.removeAttribute("data-genericui-theme-color")
        }
    }

    override fun getThemeColor(): String? = themeColor

    override fun runEventLoop(onStarted: () -> Unit) {
        activeApplication = this
        if (WebBrowserDomBridge.isBrowserEnvironment) {
            WebBrowserDomBridge.bindApplicationRoot(nativeApplication)
        } else {
            ensureBootstrapCdnInjected()
        }
        onStarted()
    }

    override fun exitApplication(exitCode: Int) {
        nativeApplication.clearChildren()
        cdnInjected = false
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
    }
}
