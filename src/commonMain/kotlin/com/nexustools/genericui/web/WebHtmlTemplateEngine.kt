package com.nexustools.genericui.web

import com.nexustools.genericui.enums.ui.ThemeMode

/**
 * Template engine and runtime CDN resource injector for the `:web` backend.
 *
 * Uses `templates/index.template.html`, [WebBootstrapCdnConfig], [WebSplashConfig], and
 * [WebWasmJsLoader] (`templates/genericui-loader.js`) to render HTML5 entry pages containing:
 * - A `<link id="genericui-favicon" rel="icon" ...>` favicon tag (preferring `.svg` icons)
 * - A Bootstrap-styled `#genericui-splash` screen with the project's icon, download progress bar
 *   (`#genericui-splash-progress-bar`), and runtime error stacktrace alert (`#genericui-splash-error`)
 * - `<script type="loader-wasm" src="{{WASM_URL}}?{{WASM_HASH}}">` and
 *   `<script type="loader-js" src="{{JS_URL}}?{{JS_HASH}}">` descriptor tags with SHA-1 hashes
 * - `<script id="genericui-loader-script" src="{{LOADER_JS_URL}}?{{LOADER_JS_HASH}}" defer></script>`
 *   with the loader script's own SHA-1 hash appended
 * - Bootstrap 5 CSS & JS CDN resources (`{{BOOTSTRAP_JS_URL}}` without hash since the CDN URL version is sufficient)
 */
class WebHtmlTemplateEngine(
    var cdnConfig: WebBootstrapCdnConfig = WebBootstrapCdnConfig(),
    var splashConfig: WebSplashConfig = WebSplashConfig(),
    var manifestConfig: WebAppManifestConfig = WebAppManifestConfig(),
    var themeColor: String? = null
) {
    /**
     * Default HTML5 template matching `src/commonMain/resources/templates/index.template.html`.
     */
    val defaultHtmlTemplate: String = DEFAULT_INDEX_TEMPLATE

    /**
     * Renders a complete `index.html` document from `index.template.html` with Bootstrap CDN links,
     * favicon (`webIconUrl`), optional W3C Web App Manifest (`manifest.webmanifest`) and Android/iOS
     * installable WebApp `<meta>`/`<link>` tags (`{{WEB_MANIFEST_AND_PWA_LINKS}}`), `<meta name="theme-color">`,
     * Bootstrap splash screen (icon + progress bar + error stacktrace box),
     * `<script type="loader-wasm">` and `<script type="loader-js">` descriptor tags (with SHA-1 hashes),
     * and the Wasm-first / JS-IR-fallback `genericui-loader.js` script (with its own SHA-1 hash).
     */
    fun renderIndexHtml(
        title: String = "GenericUI Web Application",
        lang: String = "en",
        themeMode: ThemeMode = ThemeMode.DARK,
        themeColor: String? = this.themeColor,
        rootContainerId: String = "genericui-app",
        appBundleScriptUrl: String? = null,
        wasmJsUrl: String? = null,
        wasmHash: String? = null,
        wasmBinaryUrl: String? = null,
        wasmBinaryHash: String? = null,
        jsIrUrl: String? = null,
        jsHash: String? = null,
        loaderScriptUrl: String = WebWasmJsLoader.DEFAULT_LOADER_FILENAME,
        loaderHash: String? = null,
        namespaceXmlUrl: String = WebNamespaceSpec.DEFAULT_NAMESPACE_XML_FILENAME,
        namespaceXmlHash: String? = null,
        genericUiCssUrl: String = WebNamespaceSpec.DEFAULT_GENERICUI_CSS_FILENAME,
        genericUiCssContent: String = WebNamespaceSpec.DEFAULT_GENERICUI_CSS,
        genericUiCssHash: String? = null,
        webIconUrl: String? = null,
        splash: WebSplashConfig = splashConfig,
        manifest: WebAppManifestConfig = manifestConfig,
        manifestHash: String? = null,
        customTemplate: String = defaultHtmlTemplate
    ): String {
        val bsTheme = when (themeMode) {
            ThemeMode.LIGHT -> "light"
            ThemeMode.DARK, ThemeMode.SYSTEM -> "dark"
        }

        val effectiveIconUrl = webIconUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?: splash.iconUrl?.trim()?.takeIf { it.isNotEmpty() }

        val effectiveSplash = if (effectiveIconUrl != null && effectiveIconUrl != splash.iconUrl) {
            splash.copy(iconUrl = effectiveIconUrl)
        } else {
            splash
        }

        val faviconLink = if (!effectiveIconUrl.isNullOrBlank()) {
            val mimeType = inferIconMimeType(effectiveIconUrl)
            """    <link id="genericui-favicon" rel="icon" type="$mimeType" href="$effectiveIconUrl">"""
        } else {
            ""
        }

        val cleanThemeColor = themeColor?.trim()?.takeIf { it.isNotEmpty() }
        val effectiveManifest = if (cleanThemeColor != null && (manifest.themeColor.isBlank() || manifest.themeColor == "#0d6efd")) {
            manifest.copy(themeColor = cleanThemeColor)
        } else {
            manifest
        }

        val webManifestAndPwaLinks = if (effectiveManifest.isActive) {
            val resolvedManifestHash = manifestHash?.trim()?.takeIf { it.isNotEmpty() }
                ?: WebSha1Hasher.sha1Hex(
                    effectiveManifest.toManifestJson(
                        defaultAppName = title,
                        defaultIconUrl = effectiveIconUrl,
                        inferMimeType = ::inferIconMimeType
                    )
                )
            effectiveManifest.renderHeadTagsHtml(
                defaultAppName = title,
                defaultIconUrl = effectiveIconUrl,
                manifestHash = resolvedManifestHash
            )
        } else if (cleanThemeColor != null) {
            """    <meta id="genericui-theme-color" name="theme-color" content="$cleanThemeColor">"""
        } else {
            ""
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

        val resolvedWasmUrl = wasmJsUrl?.trim()?.takeIf { it.isNotEmpty() } ?: "app-wasm.js"
        val resolvedWasmHash = wasmHash?.trim()?.takeIf { it.isNotEmpty() }
            ?: WebSha1Hasher.sha1Hex(resolvedWasmUrl)

        val resolvedWasmBinaryAttr = if (!wasmBinaryUrl.isNullOrBlank()) {
            val binUrl = wasmBinaryUrl.trim()
            val binHash = wasmBinaryHash?.trim()?.takeIf { it.isNotEmpty() }
                ?: WebSha1Hasher.sha1Hex(binUrl)
            """ data-wasm-binary="${WebSha1Hasher.appendHashQuery(binUrl, binHash)}""""
        } else {
            ""
        }

        val resolvedJsUrl = jsIrUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?: appBundleScriptUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?: "app-js.js"
        val resolvedJsHash = jsHash?.trim()?.takeIf { it.isNotEmpty() }
            ?: WebSha1Hasher.sha1Hex(resolvedJsUrl)

        val resolvedLoaderUrl = loaderScriptUrl.trim().ifEmpty { WebWasmJsLoader.DEFAULT_LOADER_FILENAME }
        val resolvedLoaderHash = loaderHash?.trim()?.takeIf { it.isNotEmpty() }
            ?: WebWasmJsLoader.defaultLoaderSha1Hash

        val resolvedNamespaceXmlUrl = namespaceXmlUrl.trim().ifEmpty { WebNamespaceSpec.DEFAULT_NAMESPACE_XML_FILENAME }
        val resolvedNamespaceXmlHash = namespaceXmlHash?.trim()?.takeIf { it.isNotBlank() }
            ?: WebNamespaceSpec.defaultNamespaceXmlSha1Hash

        val resolvedGenericUiCssUrl = genericUiCssUrl.trim().ifEmpty { WebNamespaceSpec.DEFAULT_GENERICUI_CSS_FILENAME }
        val resolvedGenericUiCssHash = genericUiCssHash?.trim()?.takeIf { it.isNotBlank() }
            ?: WebSha1Hasher.sha1Hex(genericUiCssContent)

        return customTemplate
            .replace("{{LANG}}", lang)
            .replace("{{TITLE}}", title)
            .replace("{{THEME_MODE}}", bsTheme)
            .replace("{{NAMESPACE_XML_URL}}", resolvedNamespaceXmlUrl)
            .replace("{{NAMESPACE_XML_HASH}}", resolvedNamespaceXmlHash)
            .replace("{{GENERICUI_CSS_URL}}", resolvedGenericUiCssUrl)
            .replace("{{GENERICUI_CSS_HASH}}", resolvedGenericUiCssHash)
            .replace("{{NAMESPACE_CSS}}", WebNamespaceSpec.DEFAULT_NAMESPACE_CSS)
            .replace("{{FAVICON_LINK}}", faviconLink)
            .replace("{{WEB_MANIFEST_AND_PWA_LINKS}}", webManifestAndPwaLinks)
            .replace("{{SPLASH_ICON}}", effectiveSplash.renderIconHtml())
            .replace("{{SPLASH_STATUS}}", effectiveSplash.statusText)
            .replace("{{SPLASH_PROGRESS_BAR_CLASS}}", effectiveSplash.progressBarClass)
            .replace("{{ROOT_CONTAINER_ID}}", rootContainerId)
            .replace("{{BOOTSTRAP_CSS_URL}}", cdnConfig.cssCdnUrl)
            .replace("{{BOOTSTRAP_CSS_INTEGRITY}}", cdnConfig.cssIntegrity)
            .replace("{{BOOTSTRAP_JS_URL}}", cdnConfig.jsBundleCdnUrl)
            .replace("{{BOOTSTRAP_JS_INTEGRITY}}", cdnConfig.jsBundleIntegrity)
            .replace("{{CROSSORIGIN}}", cdnConfig.crossOrigin)
            .replace("{{BOOTSTRAP_ICONS_LINK}}", iconsLink)
            .replace("{{EXTRA_STYLESHEETS}}", extraStylesheets)
            .replace("{{EXTRA_SCRIPTS}}", extraScripts)
            .replace("{{WASM_URL}}", resolvedWasmUrl)
            .replace("{{WASM_HASH}}", resolvedWasmHash)
            .replace("{{WASM_BINARY_ATTR}}", resolvedWasmBinaryAttr)
            .replace("{{JS_URL}}", resolvedJsUrl)
            .replace("{{JS_HASH}}", resolvedJsHash)
            .replace("{{LOADER_JS_URL}}", resolvedLoaderUrl)
            .replace("{{LOADER_JS_HASH}}", resolvedLoaderHash)
            .lineSequence()
            .filter { line -> line.isNotBlank() || !line.contains("{{") }
            .joinToString("\n")
    }

    /**
     * Renders a W3C / MDN Progressive Web App `manifest.webmanifest` JSON string from [manifest].
     */
    fun renderManifestJson(
        title: String = "GenericUI Web Application",
        webIconUrl: String? = null,
        themeColor: String? = this.themeColor,
        manifest: WebAppManifestConfig = manifestConfig
    ): String {
        val effectiveIconUrl = webIconUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?: splashConfig.iconUrl?.trim()?.takeIf { it.isNotEmpty() }
        val cleanThemeColor = themeColor?.trim()?.takeIf { it.isNotEmpty() }
        val effectiveManifest = if (cleanThemeColor != null && (manifest.themeColor.isBlank() || manifest.themeColor == "#0d6efd")) {
            manifest.copy(themeColor = cleanThemeColor)
        } else {
            manifest
        }
        return effectiveManifest.toManifestJson(
            defaultAppName = title,
            defaultIconUrl = effectiveIconUrl,
            inferMimeType = ::inferIconMimeType
        )
    }

    /**
     * Infers the HTML `<link rel="icon" type="...">` MIME type for the given icon URL/path.
     * SVG (`image/svg+xml`) is the preferred icon format for the Web backend.
     */
    fun inferIconMimeType(iconUrl: String): String {
        val clean = iconUrl.substringBefore('?').substringBefore('#').lowercase()
        return when {
            clean.endsWith(".svg") -> "image/svg+xml"
            clean.endsWith(".png") -> "image/png"
            clean.endsWith(".ico") -> "image/x-icon"
            clean.endsWith(".webp") -> "image/webp"
            clean.endsWith(".gif") -> "image/gif"
            else -> "image/svg+xml"
        }
    }

    /**
     * Builds the `<link>` and `<script>` [WebDomElement] nodes for the configured Bootstrap CDN
     * CSS and JS bundle so [WebApplicationImpl] can ensure they are mounted at runtime.
     */
    fun createCdnResourceElements(): List<WebDomElement> {
        val elements = mutableListOf<WebDomElement>()

        elements.add(
            WebDomElement("link").apply {
                id = "genericui-namespace-spec"
                setAttribute("rel", "namespace")
                setAttribute("data-namespace-prefix", WebNamespaceSpec.NAMESPACE_PREFIX)
                setAttribute("data-namespace-uri", WebNamespaceSpec.NAMESPACE_URI)
                setAttribute("type", "application/xml")
                setAttribute(
                    "href",
                    "${WebNamespaceSpec.DEFAULT_NAMESPACE_XML_FILENAME}?${WebNamespaceSpec.defaultNamespaceXmlSha1Hash}"
                )
            }
        )

        elements.add(
            WebDomElement("link").apply {
                id = "genericui-css"
                setAttribute("rel", "stylesheet")
                setAttribute(
                    "href",
                    "${WebNamespaceSpec.DEFAULT_GENERICUI_CSS_FILENAME}?${WebNamespaceSpec.defaultGenericUiCssSha1Hash}"
                )
            }
        )

        elements.add(
            WebDomElement("style").apply {
                id = "genericui-namespace-styles"
                textContent = WebNamespaceSpec.DEFAULT_NAMESPACE_CSS
            }
        )

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
<html lang="{{LANG}}" xmlns:genericui="https://nexustools.com/ns/genericui" data-bs-theme="{{THEME_MODE}}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{TITLE}}</title>
    <link id="genericui-namespace-spec" rel="namespace" data-namespace-prefix="genericui" data-namespace-uri="https://nexustools.com/ns/genericui" type="application/xml" href="{{NAMESPACE_XML_URL}}?{{NAMESPACE_XML_HASH}}">
    <link id="genericui-css" rel="stylesheet" href="{{GENERICUI_CSS_URL}}?{{GENERICUI_CSS_HASH}}">
{{FAVICON_LINK}}
{{WEB_MANIFEST_AND_PWA_LINKS}}
    <link id="genericui-bootstrap-css" rel="stylesheet" href="{{BOOTSTRAP_CSS_URL}}" integrity="{{BOOTSTRAP_CSS_INTEGRITY}}" crossorigin="{{CROSSORIGIN}}">
{{BOOTSTRAP_ICONS_LINK}}
{{EXTRA_STYLESHEETS}}
</head>
<body class="bg-body text-body m-0 p-0">
    <div id="genericui-splash" class="genericui-splash position-fixed top-0 start-0 w-100 h-100 d-flex flex-column align-items-center justify-content-center bg-body text-body p-4" style="z-index: 2000; transition: opacity 0.35s ease, visibility 0.35s ease;">
        <div class="d-flex flex-column align-items-center justify-content-center w-100" style="max-width: 420px;">
{{SPLASH_ICON}}
            <div id="genericui-splash-title" class="fw-semibold fs-5 mb-2 text-center">{{TITLE}}</div>
            <div id="genericui-splash-status" class="genericui-splash-status text-body-secondary small mb-3 text-center">{{SPLASH_STATUS}}</div>
            <div id="genericui-splash-progress" class="progress w-100 mb-2" style="height: 8px;" role="progressbar" aria-label="Application loading progress" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0">
                <div id="genericui-splash-progress-bar" class="{{SPLASH_PROGRESS_BAR_CLASS}}" style="width: 0%" aria-valuenow="0"></div>
            </div>
            <div id="genericui-splash-error" class="genericui-splash-error alert alert-danger d-none text-start w-100 mt-3 mb-0 shadow-sm" role="alert">
                <div class="d-flex align-items-center mb-2">
                    <i class="bi bi-exclamation-triangle-fill me-2" aria-hidden="true"></i>
                    <strong id="genericui-splash-error-title">Application Error</strong>
                </div>
                <div id="genericui-splash-error-message" class="genericui-splash-error-message small fw-semibold mb-2"></div>
                <pre id="genericui-splash-error-stack" class="genericui-splash-error-stack small mb-0 p-2 bg-dark text-light rounded overflow-auto" style="max-height: 240px; white-space: pre-wrap; word-break: break-word;"></pre>
            </div>
        </div>
    </div>
    <genericui:root></genericui:root>
    <script id="genericui-bootstrap-js" src="{{BOOTSTRAP_JS_URL}}" integrity="{{BOOTSTRAP_JS_INTEGRITY}}" crossorigin="{{CROSSORIGIN}}" defer></script>
{{EXTRA_SCRIPTS}}
    <script type="loader-wasm" src="{{WASM_URL}}?{{WASM_HASH}}"{{WASM_BINARY_ATTR}}></script>
    <script type="loader-js" src="{{JS_URL}}?{{JS_HASH}}"></script>
    <script id="genericui-loader-script" src="{{LOADER_JS_URL}}?{{LOADER_JS_HASH}}" defer></script>
</body>
</html>"""

        const val DEFAULT_HTML_TEMPLATE: String = DEFAULT_INDEX_TEMPLATE
    }
}
