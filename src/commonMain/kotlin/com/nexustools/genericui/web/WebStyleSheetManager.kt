package com.nexustools.genericui.web

import com.nexustools.genericui.component.shell.IApplication
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.res.DataUrlCodec
import com.nexustools.genericui.res.Resources
import com.nexustools.genericui.resources.ResourceID
import com.nexustools.genericui.style.css.ComputedStyle
import com.nexustools.genericui.style.css.CssStyleSheet
import com.nexustools.genericui.style.css.PrecompiledStyleSheet
import com.nexustools.genericui.style.css.StyleSheetRegistry

/**
 * Manages `<link rel="stylesheet" href="...">` elements in `document.head` (via [WebBrowserDomBridge])
 * and in the `:web` application's root [WebDomElement] for headless inspection and HTML serialization.
 *
 * Resolves preloaded [ResourceID] stylesheets (`R.style.*`) from [WebBrowserDomBridge] /
 * [WebResourceResolverImpl] into `data:text/css;base64,...` Data-URLs and parses their CSS rules
 * into [CssStyleSheet] instances for [ComputedStyle] resolution.
 */
object WebStyleSheetManager {

    private val activeLinksByHref = linkedMapOf<String, WebDomElement>()
    private val activeHrefsByResourceId = linkedMapOf<Int, String>()
    private val parsedStyleSheetsByResourceId = linkedMapOf<Int, CssStyleSheet>()
    private val parsedStyleSheetsByHref = linkedMapOf<String, CssStyleSheet>()

    private fun isBuiltInStyleSheetResourceId(resourceId: ResourceID): Boolean =
        resourceId.id == Resources.GENERICUI.id ||
            resourceId.id == Resources.BOOTSTRAP.id ||
            resourceId.id == Resources.BOOTSTRAP_ICONS.id

    private fun isBuiltInStyleSheetFileName(pathOrHref: String): Boolean {
        val clean = pathOrHref.trim().substringBefore('?').substringBefore('#').replace('\\', '/').substringAfterLast('/').lowercase()
        return clean == "genericui.css" ||
            clean == "bootstrap.min.css" ||
            clean == "bootstrap-icons.min.css"
    }

    private fun canonicalBuiltInHref(pathOrHref: String): String? {
        val clean = pathOrHref.trim().substringBefore('?').substringBefore('#').replace('\\', '/').substringAfterLast('/').lowercase()
        return when (clean) {
            "genericui.css" -> WebNamespaceSpec.DEFAULT_GENERICUI_CSS_FILENAME
            "bootstrap.min.css" -> WebNamespaceSpec.DEFAULT_BOOTSTRAP_CSS_URL
            "bootstrap-icons.min.css" -> WebNamespaceSpec.DEFAULT_BOOTSTRAP_ICONS_CSS_URL
            else -> null
        }
    }

    /**
     * Normalizes a `.scss`, `.sass`, or `.css` path, logical stylesheet name, or `data:` URL
     * to its compiled `.css` href or Data-URL.
     */
    fun normalizeCssHref(pathOrName: String): String {
        val trimmed = pathOrName.trim()
        if (trimmed.isEmpty()) return ""
        if (DataUrlCodec.isDataUrl(trimmed)) return trimmed

        canonicalBuiltInHref(trimmed)?.let { return it }

        val clean = trimmed.replace('\\', '/').removePrefix("/")
        if (clean.isEmpty()) return ""
        return when {
            clean.endsWith(".scss", ignoreCase = true) -> clean.dropLast(5) + ".css"
            clean.endsWith(".sass", ignoreCase = true) -> clean.dropLast(5) + ".css"
            clean.endsWith(".css", ignoreCase = true) -> clean
            clean.contains('/') -> "$clean.css"
            else -> "styles/$clean.css"
        }
    }

    /**
     * Resolves the `data:text/css;base64,...` Data-URL (when preloaded in [WebBrowserDomBridge] /
     * [WebResourceResolverImpl] / [Resources.activeTable]) or fallback `.css` href for a [ResourceID] (`R.style.<name>`).
     */
    fun resolveCssHref(resourceId: ResourceID): String {
        if (isBuiltInStyleSheetResourceId(resourceId)) {
            return when (resourceId.id) {
                Resources.GENERICUI.id -> WebNamespaceSpec.DEFAULT_GENERICUI_CSS_FILENAME
                Resources.BOOTSTRAP.id -> WebNamespaceSpec.DEFAULT_BOOTSTRAP_CSS_URL
                Resources.BOOTSTRAP_ICONS.id -> WebNamespaceSpec.DEFAULT_BOOTSTRAP_ICONS_CSS_URL
                else -> normalizeCssHref(resourceId.relativePath)
            }
        }

        val resolvedFromResolver = WebResourceResolverImpl.resolveResourceUrl(resourceId)
        if (DataUrlCodec.isDataUrl(resolvedFromResolver)) {
            return ensureCssDataUrl(resolvedFromResolver)
        }

        val registeredSheet = StyleSheetRegistry.resolveByResourceId(resourceId)
            ?: Resources.getStyleSheet(resourceId)
        if (registeredSheet != null) {
            val cssText = registeredSheet.rawCss.ifBlank { registeredSheet.styleSheet.sourceCssText }
            if (cssText.isNotBlank()) {
                return DataUrlCodec.encodeUtf8Text(cssText, "text/css")
            }
            val candidateHref = normalizeCssHref(registeredSheet.cssResourcePath)
            if (isBuiltInStyleSheetFileName(candidateHref)) {
                return candidateHref
            }
            resolvePreloadedCssDataUrlByPath(candidateHref)?.let { return it }
            return candidateHref
        }

        val tableEntry = Resources.activeTable.findResourceId(resourceId.id) ?: resourceId
        if (tableEntry.relativePath.isNotBlank()) {
            val candidateHref = normalizeCssHref(tableEntry.relativePath)
            if (isBuiltInStyleSheetFileName(candidateHref)) {
                return candidateHref
            }
            resolvePreloadedCssDataUrlByPath(candidateHref)?.let { return it }
            return candidateHref
        }
        if (tableEntry.name.isNotBlank()) {
            val candidateHref = normalizeCssHref("styles/${tableEntry.name}.css")
            resolvePreloadedCssDataUrlByPath(candidateHref)?.let { return it }
            return candidateHref
        }
        return normalizeCssHref("styles/style_${resourceId.id}.css")
    }

    /**
     * Resolves the compiled `.css` href (when [PrecompiledStyleSheet.cssResourcePath] is present so `<head>` `<link>`
     * deduplication reuses the pre-packaged `<link id="genericui-compiled-sass-css">`) or `data:text/css;base64,...` Data-URL
     * for a [PrecompiledStyleSheet].
     */
    fun resolveCssHref(styleSheet: PrecompiledStyleSheet): String {
        canonicalBuiltInHref(styleSheet.cssResourcePath)?.let { return it }
        if (styleSheet.cssResourcePath.isNotBlank()) {
            return normalizeCssHref(styleSheet.cssResourcePath)
        }
        val inlineCss = styleSheet.rawCss.ifBlank { styleSheet.styleSheet.sourceCssText }
        if (inlineCss.isNotBlank()) {
            return DataUrlCodec.encodeUtf8Text(inlineCss, "text/css")
        }
        for (candidate in listOf(styleSheet.sourcePath, styleSheet.cssResourcePath, styleSheet.name)) {
            if (candidate.isNotBlank()) {
                resolvePreloadedCssDataUrlByPath(candidate)?.let { return it }
            }
        }
        val candidate = styleSheet.cssResourcePath.ifBlank { "styles/${styleSheet.name}.css" }
        return normalizeCssHref(candidate)
    }

    private fun resolvePreloadedCssDataUrlByPath(pathOrKey: String): String? {
        val keys = listOf(pathOrKey, normalizeCssHref(pathOrKey))
        for (key in keys) {
            if (key.isBlank()) continue
            WebBrowserDomBridge.getPreloadedResourceDataUrl(key)?.let {
                if (DataUrlCodec.isDataUrl(it)) return ensureCssDataUrl(it)
            }
            WebBrowserDomBridge.getPreloadedResourceText(key)?.let {
                return DataUrlCodec.encodeUtf8Text(it, "text/css")
            }
            WebBrowserDomBridge.getPreloadedResourceBytes(key)?.let {
                return DataUrlCodec.encodeBytes(it, "text/css")
            }
        }
        return null
    }

    private fun ensureCssDataUrl(dataUrl: String): String {
        if (dataUrl.startsWith("data:text/css", ignoreCase = true)) {
            return dataUrl
        }
        val bytes = DataUrlCodec.decodeBytes(dataUrl) ?: return dataUrl
        return DataUrlCodec.encodeBytes(bytes, "text/css")
    }

    /**
     * Returns the deterministic DOM `id` for a stylesheet `<link>` element in `<head>`.
     */
    fun linkIdForHref(href: String): String {
        canonicalBuiltInHref(href)?.let { builtIn ->
            return when (builtIn) {
                WebNamespaceSpec.DEFAULT_GENERICUI_CSS_FILENAME -> "genericui-css"
                WebNamespaceSpec.DEFAULT_BOOTSTRAP_CSS_URL -> "genericui-bootstrap-css"
                WebNamespaceSpec.DEFAULT_BOOTSTRAP_ICONS_CSS_URL -> "genericui-bootstrap-icons-css"
                else -> "genericui-stylesheet-$builtIn"
            }
        }
        val normalized = normalizeCssHref(href)
        if (DataUrlCodec.isDataUrl(normalized)) {
            val hash = WebSha1Hasher.sha1Hex(normalized).take(16)
            return "genericui-stylesheet-data-$hash"
        }
        val slug = normalized.replace(Regex("[^a-zA-Z0-9_-]"), "-")
        return "genericui-stylesheet-$slug"
    }

    /**
     * Loads, parses, and attaches a stylesheet referenced by [resourceId] from preloaded memory
     * ([WebBrowserDomBridge] / [WebResourceResolverImpl] / [Resources.activeTable]), mounting a
     * `<link rel="stylesheet" href="...">` element in `<head>` and registering its parsed
     * [CssStyleSheet] rules for [ComputedStyle] resolution.
     */
    fun loadStyleSheet(
        resourceId: ResourceID,
        rootElement: WebDomElement? = WebApplicationImpl.activeApplication?.nativeApplication
    ): PrecompiledStyleSheet {
        val isBuiltIn = isBuiltInStyleSheetResourceId(resourceId)
        val resolvedText = WebResourceResolverImpl.resolveText(resourceId)
            ?: resolvePreloadedCssDataUrlByPath(resourceId.toResourceUri())?.let { DataUrlCodec.decodeUtf8Text(it) }
            ?: resolvePreloadedCssDataUrlByPath(resourceId.name)?.let { DataUrlCodec.decodeUtf8Text(it) }

        val existingSheet = StyleSheetRegistry.resolveByResourceId(resourceId)
            ?: Resources.getStyleSheet(resourceId)

        val cssText = resolvedText
            ?: existingSheet?.rawCss?.takeIf { it.isNotBlank() }
            ?: existingSheet?.styleSheet?.sourceCssText?.takeIf { it.isNotBlank() }
            ?: ""

        val parsedCss: CssStyleSheet = when {
            cssText.isNotBlank() -> CssStyleSheet.parse(cssText)
            existingSheet != null && existingSheet.styleSheet.rules.isNotEmpty() -> existingSheet.styleSheet
            else -> CssStyleSheet.empty()
        }

        val href = if (!isBuiltIn && cssText.isNotBlank()) {
            DataUrlCodec.encodeUtf8Text(cssText, "text/css")
        } else {
            resolveCssHref(resourceId)
        }

        val logicalName = resourceId.name.ifBlank { existingSheet?.name ?: "style_${resourceId.id}" }
        val cssPath = resourceId.relativePath.ifBlank { existingSheet?.cssResourcePath ?: "styles/$logicalName.css" }

        val precompiled = object : PrecompiledStyleSheet {
            override val name: String = logicalName
            override val styleSheet: CssStyleSheet = parsedCss
            override val rawCss: String = if (isBuiltIn) "" else cssText
            override val cssResourcePath: String = cssPath
            override val sourcePath: String = resourceId.toResourceUri()
        }

        Resources.activeTable.registerStyleSheet(resourceId, precompiled)
        StyleSheetRegistry.register(logicalName, cssPath, resourceId, precompiled)
        parsedStyleSheetsByResourceId[resourceId.id] = parsedCss
        parsedStyleSheetsByHref[href] = parsedCss
        activeHrefsByResourceId[resourceId.id] = href

        mountStyleSheetLink(href, rootElement)
        return precompiled
    }

    /**
     * Adds or updates a `<link rel="stylesheet" href="...">` element in `<head>` for [pathOrHref].
     */
    fun addStyleSheet(
        pathOrHref: String,
        rootElement: WebDomElement? = WebApplicationImpl.activeApplication?.nativeApplication
    ): WebDomElement? {
        val builtInHref = canonicalBuiltInHref(pathOrHref)
        val preloadedDataUrl = if (builtInHref != null) {
            null
        } else if (DataUrlCodec.isDataUrl(pathOrHref.trim())) {
            ensureCssDataUrl(pathOrHref.trim())
        } else {
            resolvePreloadedCssDataUrlByPath(pathOrHref)
        }
        val href = builtInHref ?: preloadedDataUrl ?: normalizeCssHref(pathOrHref)
        if (href.isEmpty()) return null

        if (DataUrlCodec.isDataUrl(href)) {
            val cssText = DataUrlCodec.decodeUtf8Text(href)
            if (!cssText.isNullOrBlank()) {
                val parsed = CssStyleSheet.parse(cssText)
                parsedStyleSheetsByHref[href] = parsed
                val normalizedPath = if (DataUrlCodec.isDataUrl(pathOrHref.trim())) "styles/inline.css" else normalizeCssHref(pathOrHref)
                val stem = normalizedPath.substringAfterLast('/').substringBeforeLast('.').ifBlank { "inline" }
                val sheet = object : PrecompiledStyleSheet {
                    override val name: String = stem
                    override val styleSheet: CssStyleSheet = parsed
                    override val rawCss: String = cssText
                    override val cssResourcePath: String = normalizedPath
                }
                StyleSheetRegistry.register(stem, normalizedPath, null, sheet)
            }
        }

        return mountStyleSheetLink(href, rootElement)
    }

    /**
     * Adds or updates a `<link rel="stylesheet" href="data:text/css;base64,...">` element for [resourceId]
     * and parses its CSS rules for [ComputedStyle] resolution.
     */
    fun addStyleSheet(
        resourceId: ResourceID,
        rootElement: WebDomElement? = WebApplicationImpl.activeApplication?.nativeApplication
    ): WebDomElement? {
        loadStyleSheet(resourceId, rootElement)
        val href = activeHrefsByResourceId[resourceId.id] ?: resolveCssHref(resourceId)
        return activeLinksByHref[href]
    }

    /**
     * Adds or updates a `<link rel="stylesheet" href="...">` element for [styleSheet].
     */
    fun addStyleSheet(
        styleSheet: PrecompiledStyleSheet,
        rootElement: WebDomElement? = WebApplicationImpl.activeApplication?.nativeApplication
    ): WebDomElement? {
        val href = resolveCssHref(styleSheet)
        if (href.isEmpty()) return null
        if (DataUrlCodec.isDataUrl(href)) {
            val cssText = styleSheet.rawCss.ifBlank { DataUrlCodec.decodeUtf8Text(href).orEmpty() }
            if (cssText.isNotBlank()) {
                val parsed = if (styleSheet.styleSheet.rules.isNotEmpty()) {
                    styleSheet.styleSheet
                } else {
                    CssStyleSheet.parse(cssText)
                }
                parsedStyleSheetsByHref[href] = parsed
            }
        } else if (styleSheet.styleSheet.rules.isNotEmpty()) {
            parsedStyleSheetsByHref[href] = styleSheet.styleSheet
        }
        return mountStyleSheetLink(href, rootElement)
    }

    internal fun mountStyleSheetLink(
        href: String,
        @Suppress("UNUSED_PARAMETER") rootElement: WebDomElement? = WebApplicationImpl.activeApplication?.nativeApplication
    ): WebDomElement? {
        if (href.isEmpty()) return null
        val linkId = linkIdForHref(href)
        val linkEl = activeLinksByHref.getOrPut(href) {
            WebDomElement("link").apply {
                id = linkId
                setAttribute("rel", "stylesheet")
                setAttribute("href", href)
            }
        }
        linkEl.id = linkId
        linkEl.setAttribute("rel", "stylesheet")
        linkEl.setAttribute("href", href)
        WebBrowserDomBridge.addStyleSheetLink(linkId, href)
        return linkEl
    }

    /**
     * Returns the parsed [CssStyleSheet] for [resourceId], if loaded.
     */
    fun getParsedStyleSheet(resourceId: ResourceID): CssStyleSheet? =
        parsedStyleSheetsByResourceId[resourceId.id]
            ?: StyleSheetRegistry.resolveByResourceId(resourceId)?.styleSheet
            ?: Resources.getStyleSheet(resourceId)?.styleSheet

    /**
     * Resolves a [ComputedStyle] for [classes] using the stylesheet identified by [resourceId].
     */
    fun resolveComputedStyle(
        resourceId: ResourceID,
        classes: Set<String>,
        tagName: String = "div",
        id: String? = null,
        viewportWidthPx: Int = 1280
    ): ComputedStyle {
        val sheet = getParsedStyleSheet(resourceId) ?: loadStyleSheet(resourceId).styleSheet
        return sheet.resolveStyleForClasses(
            classes = classes,
            tagName = tagName,
            id = id,
            viewportWidthPx = viewportWidthPx
        )
    }

    /**
     * Resolves a [ComputedStyle] for [component] using the stylesheet identified by [resourceId].
     */
    fun resolveComputedStyle(
        resourceId: ResourceID,
        component: AbstractComponent,
        viewportWidthPx: Int = 1280
    ): ComputedStyle {
        val sheet = getParsedStyleSheet(resourceId) ?: loadStyleSheet(resourceId).styleSheet
        return sheet.resolveStyleForComponent(component, viewportWidthPx = viewportWidthPx)
    }

    /**
     * Removes the `<link rel="stylesheet" href="...">` element corresponding to [pathOrHref].
     */
    fun removeStyleSheet(
        pathOrHref: String,
        rootElement: WebDomElement? = WebApplicationImpl.activeApplication?.nativeApplication
    ): Boolean {
        val candidates = linkedSetOf<String>()
        val normalized = normalizeCssHref(pathOrHref)
        if (normalized.isNotEmpty()) candidates.add(normalized)
        resolvePreloadedCssDataUrlByPath(pathOrHref)?.let { candidates.add(it) }
        if (candidates.isEmpty()) return false

        for (href in candidates) {
            val removed = activeLinksByHref.remove(href)
            parsedStyleSheetsByHref.remove(href)
            val linkId = linkIdForHref(href)
            if (removed != null) {
                removed.remove()
                rootElement?.removeChild(removed)
                WebBrowserDomBridge.removeStyleSheetLink(linkId)
                return true
            }
            val existingChild = rootElement?.children?.firstOrNull {
                it.tagName == "link" && (it.getAttribute("href") == href || it.id == linkId)
            }
            if (existingChild != null) {
                rootElement.removeChild(existingChild)
                WebBrowserDomBridge.removeStyleSheetLink(linkId)
                return true
            }
        }
        return false
    }

    /**
     * Removes the `<link rel="stylesheet" href="...">` element corresponding to [resourceId].
     */
    fun removeStyleSheet(
        resourceId: ResourceID,
        rootElement: WebDomElement? = WebApplicationImpl.activeApplication?.nativeApplication
    ): Boolean {
        parsedStyleSheetsByResourceId.remove(resourceId.id)
        val trackedHref = activeHrefsByResourceId.remove(resourceId.id)
        if (trackedHref != null && removeStyleSheet(trackedHref, rootElement)) {
            return true
        }
        val href = resolveCssHref(resourceId)
        return removeStyleSheet(href, rootElement)
    }

    /**
     * Removes the `<link rel="stylesheet" href="...">` element corresponding to [styleSheet].
     */
    fun removeStyleSheet(
        styleSheet: PrecompiledStyleSheet,
        rootElement: WebDomElement? = WebApplicationImpl.activeApplication?.nativeApplication
    ): Boolean {
        val href = resolveCssHref(styleSheet)
        return removeStyleSheet(href, rootElement)
    }

    /**
     * Synchronizes active `<link rel="stylesheet">` elements to match [app]'s registered stylesheets.
     */
    fun syncApplicationStyleSheets(
        app: IApplication,
        rootElement: WebDomElement? = WebApplicationImpl.activeApplication?.nativeApplication
    ) {
        val desiredHrefs = linkedSetOf<String>()
        for (builtInId in listOf(Resources.GENERICUI, Resources.BOOTSTRAP, Resources.BOOTSTRAP_ICONS)) {
            if (app.hasStyleSheet(builtInId)) {
                val href = resolveCssHref(builtInId)
                if (href.isNotEmpty()) {
                    desiredHrefs.add(href)
                }
            }
        }
        for (path in app.getStyleSheetPaths()) {
            val href = canonicalBuiltInHref(path) ?: resolvePreloadedCssDataUrlByPath(path) ?: normalizeCssHref(path)
            if (href.isNotEmpty()) {
                desiredHrefs.add(href)
            }
        }
        for (sheet in app.getStyleSheets()) {
            val href = resolveCssHref(sheet)
            if (href.isNotEmpty()) {
                desiredHrefs.add(href)
            }
        }

        val toRemove = activeLinksByHref.keys.filter { it !in desiredHrefs }
        for (href in toRemove) {
            removeStyleSheet(href, rootElement)
        }
        for (href in desiredHrefs) {
            addStyleSheet(href, rootElement)
        }
    }

    fun hasStyleSheet(pathOrHref: String): Boolean {
        val href = normalizeCssHref(pathOrHref)
        if (href in activeLinksByHref) return true
        val preloaded = resolvePreloadedCssDataUrlByPath(pathOrHref)
        return preloaded != null && preloaded in activeLinksByHref
    }

    fun hasStyleSheet(resourceId: ResourceID): Boolean {
        val tracked = activeHrefsByResourceId[resourceId.id]
        if (tracked != null && tracked in activeLinksByHref) return true
        val href = resolveCssHref(resourceId)
        return href in activeLinksByHref
    }

    fun getActiveStyleSheetHrefs(): List<String> = activeLinksByHref.keys.toList()

    fun getActiveLinkElements(): List<WebDomElement> = activeLinksByHref.values.toList()

    fun getHeadStyleSheetLinks(): List<WebDomElement> = activeLinksByHref.values.toList()

    fun getHeadStyleSheetLink(hrefOrId: String): WebDomElement? {
        val trimmed = hrefOrId.trim()
        if (trimmed.isEmpty()) return null
        activeLinksByHref[trimmed]?.let { return it }
        val normalized = normalizeCssHref(trimmed)
        activeLinksByHref[normalized]?.let { return it }
        val targetId = linkIdForHref(trimmed)
        return activeLinksByHref.values.firstOrNull {
            it.id == trimmed || it.id == targetId || it.getAttribute("href") == trimmed || it.getAttribute("href") == normalized
        }
    }

    fun clear(rootElement: WebDomElement? = WebApplicationImpl.activeApplication?.nativeApplication) {
        for (href in activeLinksByHref.keys.toList()) {
            removeStyleSheet(href, rootElement)
        }
        activeLinksByHref.clear()
        activeHrefsByResourceId.clear()
        parsedStyleSheetsByResourceId.clear()
        parsedStyleSheetsByHref.clear()
    }
}
