package com.nexustools.genericui.web

import com.nexustools.genericui.enums.ui.ThemeMode

/**
 * Template engine and runtime CDN resource injector for the `:web` backend.
 *
 * Uses `templates/index.template.html` and [WebBootstrapCdnConfig] to render HTML5 entry pages
 * and construct `<link rel="stylesheet">` and `<script defer>` DOM elements pointing to the
 * Bootstrap 5 CSS & JS CDN resources.
 */
class WebHtmlTemplateEngine(
    var cdnConfig: WebBootstrapCdnConfig = WebBootstrapCdnConfig()
) {
    /**
     * Default HTML5 template matching `src/commonMain/resources/templates/index.template.html`.
     */
    val defaultHtmlTemplate: String = DEFAULT_INDEX_TEMPLATE

    /**
     * Renders a complete `index.html` document with CDN links for Bootstrap CSS and JS.
     */
    fun renderIndexHtml(
        title: String = "GenericUI Web Application",
        lang: String = "en",
        themeMode: ThemeMode = ThemeMode.DARK,
        rootContainerId: String = "genericui-app",
        appBundleScriptUrl: String? = null,
        customTemplate: String = defaultHtmlTemplate
    ): String {
        val bsTheme = when (themeMode) {
            ThemeMode.LIGHT -> "light"
            ThemeMode.DARK, ThemeMode.SYSTEM -> "dark"
        }

        val iconsLink = if (cdnConfig.includeBootstrapIcons && cdnConfig.iconsCssCdnUrl.isNotBlank()) {
            """    <link id="genericui-bootstrap-icons-css" rel="stylesheet" href="${cdnConfig.iconsCssCdnUrl}" crossorigin="${cdnConfig.crossOrigin}">"""
        } else {
            ""
        }

        val extraStylesheets = cdnConfig.extraCssCdnUrls.joinToString("\n") { url ->
            """    <link rel="stylesheet" href="$url" crossorigin="${cdnConfig.crossOrigin}">"""
        }

        val extraScripts = cdnConfig.extraJsCdnUrls.joinToString("\n") { url ->
            """    <script src="$url" crossorigin="${cdnConfig.crossOrigin}" defer></script>"""
        }

        val appScriptTag = if (!appBundleScriptUrl.isNullOrBlank()) {
            """    <script src="$appBundleScriptUrl"></script>"""
        } else {
            ""
        }

        return customTemplate
            .replace("{{LANG}}", lang)
            .replace("{{TITLE}}", title)
            .replace("{{THEME_MODE}}", bsTheme)
            .replace("{{ROOT_CONTAINER_ID}}", rootContainerId)
            .replace("{{BOOTSTRAP_CSS_URL}}", cdnConfig.cssCdnUrl)
            .replace("{{BOOTSTRAP_CSS_INTEGRITY}}", cdnConfig.cssIntegrity)
            .replace("{{BOOTSTRAP_JS_URL}}", cdnConfig.jsBundleCdnUrl)
            .replace("{{BOOTSTRAP_JS_INTEGRITY}}", cdnConfig.jsBundleIntegrity)
            .replace("{{CROSSORIGIN}}", cdnConfig.crossOrigin)
            .replace("{{BOOTSTRAP_ICONS_LINK}}", iconsLink)
            .replace("{{EXTRA_STYLESHEETS}}", extraStylesheets)
            .replace("{{EXTRA_SCRIPTS}}", extraScripts)
            .replace("{{APP_BUNDLE_SCRIPT}}", appScriptTag)
            .lineSequence()
            .filter { line -> line.isNotBlank() || !line.contains("{{") }
            .joinToString("\n")
    }

    /**
     * Builds the `<link>` and `<script>` [WebDomElement] nodes for the configured Bootstrap CDN
     * CSS and JS bundle so [WebApplicationImpl] can ensure they are mounted at runtime.
     */
    fun createCdnResourceElements(): List<WebDomElement> {
        val elements = mutableListOf<WebDomElement>()

        elements.add(
            WebDomElement("link").apply {
                id = "genericui-bootstrap-css"
                setAttribute("rel", "stylesheet")
                setAttribute("href", cdnConfig.cssCdnUrl)
                if (cdnConfig.cssIntegrity.isNotBlank()) {
                    setAttribute("integrity", cdnConfig.cssIntegrity)
                }
                if (cdnConfig.crossOrigin.isNotBlank()) {
                    setAttribute("crossorigin", cdnConfig.crossOrigin)
                }
            }
        )

        if (cdnConfig.includeBootstrapIcons && cdnConfig.iconsCssCdnUrl.isNotBlank()) {
            elements.add(
                WebDomElement("link").apply {
                    id = "genericui-bootstrap-icons-css"
                    setAttribute("rel", "stylesheet")
                    setAttribute("href", cdnConfig.iconsCssCdnUrl)
                    if (cdnConfig.crossOrigin.isNotBlank()) {
                        setAttribute("crossorigin", cdnConfig.crossOrigin)
                    }
                }
            )
        }

        for ((index, cssUrl) in cdnConfig.extraCssCdnUrls.withIndex()) {
            elements.add(
                WebDomElement("link").apply {
                    id = "genericui-extra-css-$index"
                    setAttribute("rel", "stylesheet")
                    setAttribute("href", cssUrl)
                }
            )
        }

        elements.add(
            WebDomElement("script").apply {
                id = "genericui-bootstrap-js"
                setAttribute("src", cdnConfig.jsBundleCdnUrl)
                if (cdnConfig.jsBundleIntegrity.isNotBlank()) {
                    setAttribute("integrity", cdnConfig.jsBundleIntegrity)
                }
                if (cdnConfig.crossOrigin.isNotBlank()) {
                    setAttribute("crossorigin", cdnConfig.crossOrigin)
                }
                setAttribute("defer", "true")
            }
        )

        for ((index, jsUrl) in cdnConfig.extraJsCdnUrls.withIndex()) {
            elements.add(
                WebDomElement("script").apply {
                    id = "genericui-extra-js-$index"
                    setAttribute("src", jsUrl)
                    setAttribute("defer", "true")
                }
            )
        }

        return elements
    }

    /**
     * Ensures the Bootstrap CDN `<link>` and `<script>` nodes are attached to the given
     * application container or document head.
     */
    fun injectCdnResourcesInto(targetContainer: WebDomElement) {
        val existingIds = targetContainer.children.map { it.id }.toSet()
        for (resourceElement in createCdnResourceElements()) {
            if (resourceElement.id.isEmpty() || resourceElement.id !in existingIds) {
                targetContainer.appendChild(resourceElement)
            }
        }
    }

    companion object {
        const val DEFAULT_INDEX_TEMPLATE: String = """<!DOCTYPE html>
<html lang="{{LANG}}" data-bs-theme="{{THEME_MODE}}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{TITLE}}</title>
    <link id="genericui-bootstrap-css" rel="stylesheet" href="{{BOOTSTRAP_CSS_URL}}" integrity="{{BOOTSTRAP_CSS_INTEGRITY}}" crossorigin="{{CROSSORIGIN}}">
{{BOOTSTRAP_ICONS_LINK}}
{{EXTRA_STYLESHEETS}}
</head>
<body class="bg-body text-body m-0 p-0">
    <div id="{{ROOT_CONTAINER_ID}}" data-bs-theme="{{THEME_MODE}}"></div>
    <script id="genericui-bootstrap-js" src="{{BOOTSTRAP_JS_URL}}" integrity="{{BOOTSTRAP_JS_INTEGRITY}}" crossorigin="{{CROSSORIGIN}}" defer></script>
{{EXTRA_SCRIPTS}}
{{APP_BUNDLE_SCRIPT}}
</body>
</html>"""
    }
}
