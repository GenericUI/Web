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
    private var cdnInjected: Boolean = false

    init {
        activeApplication = this
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
     * Renders the complete HTML5 page for this application using [templateEngine] and its
     * configured Bootstrap CDN CSS & JS links.
     */
    fun renderIndexHtml(
        title: String = "GenericUI Web Application",
        appBundleScriptUrl: String? = null
    ): String = templateEngine.renderIndexHtml(
        title = title,
        themeMode = themeMode,
        rootContainerId = nativeApplication.id.ifEmpty { "genericui-app" },
        appBundleScriptUrl = appBundleScriptUrl
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
