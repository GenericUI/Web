package com.nexustools.genericui.web

import com.nexustools.genericui.component.shell.IApplication
import com.nexustools.genericui.res.Resources
import com.nexustools.genericui.resources.ResourceID
import com.nexustools.genericui.style.css.PrecompiledStyleSheet
import com.nexustools.genericui.style.css.StyleSheetRegistry

/**
 * Manages `<link rel="stylesheet" href="...">` elements in `document.head` (via [WebBrowserDomBridge])
 * and in the `:web` application's root [WebDomElement] for headless inspection and HTML serialization.
 *
 * Automatically normalizes `.scss` and `.sass` stylesheet paths and [ResourceID]s (`R.style.*`) to
 * their compiled `.css` hrefs (for example, `"styles/app.scss"` to `"styles/app.css"`).
 */
object WebStyleSheetManager {

    private val activeLinksByHref = linkedMapOf<String, WebDomElement>()

    /**
     * Normalizes a `.scss`, `.sass`, or `.css` path or logical stylesheet name to its compiled `.css` href.
     */
    fun normalizeCssHref(pathOrName: String): String {
        val clean = pathOrName.trim().replace('\\', '/').removePrefix("/")
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
     * Resolves the compiled `.css` href for a [ResourceID] (`R.style.<name>`).
     */
    fun resolveCssHref(resourceId: ResourceID): String {
        val registeredSheet = StyleSheetRegistry.resolveByResourceId(resourceId)
        if (registeredSheet != null) {
            return normalizeCssHref(registeredSheet.cssResourcePath)
        }
        val registeredFromResources = Resources.getStyleSheet(resourceId)
        if (registeredFromResources != null) {
            return resolveCssHref(registeredFromResources)
        }
        val tableEntry = Resources.activeTable.findResourceId(resourceId.id) ?: resourceId
        if (tableEntry.relativePath.isNotBlank()) {
            return normalizeCssHref(tableEntry.relativePath)
        }
        if (tableEntry.name.isNotBlank()) {
            return normalizeCssHref("styles/${tableEntry.name}.css")
        }
        return normalizeCssHref("styles/style_${resourceId.id}.css")
    }

    /**
     * Resolves the compiled `.css` href for a [PrecompiledStyleSheet].
     */
    fun resolveCssHref(styleSheet: PrecompiledStyleSheet): String {
        val candidate = styleSheet.cssResourcePath.ifBlank { "styles/${styleSheet.name}.css" }
        return normalizeCssHref(candidate)
    }

    /**
     * Returns the deterministic DOM `id` for a stylesheet `<link>` element.
     */
    fun linkIdForHref(href: String): String {
        val normalized = normalizeCssHref(href)
        val slug = normalized.replace(Regex("[^a-zA-Z0-9_-]"), "-")
        return "genericui-stylesheet-$slug"
    }

    /**
     * Adds or updates a `<link rel="stylesheet" href="...">` element for [pathOrHref].
     */
    fun addStyleSheet(
        pathOrHref: String,
        rootElement: WebDomElement? = WebApplicationImpl.activeApplication?.nativeApplication
    ): WebDomElement? {
        val href = normalizeCssHref(pathOrHref)
        if (href.isEmpty()) return null
        val linkId = linkIdForHref(href)
        val linkEl = activeLinksByHref.getOrPut(href) {
            WebDomElement("link").apply {
                id = linkId
                setAttribute("rel", "stylesheet")
                setAttribute("href", href)
            }
        }
        linkEl.setAttribute("rel", "stylesheet")
        linkEl.setAttribute("href", href)
        if (rootElement != null && linkEl.parent !== rootElement) {
            rootElement.insertChildAt(0, linkEl)
        }
        WebBrowserDomBridge.addStyleSheetLink(linkId, href)
        return linkEl
    }

    /**
     * Adds or updates a `<link rel="stylesheet" href="...">` element for [resourceId].
     */
    fun addStyleSheet(
        resourceId: ResourceID,
        rootElement: WebDomElement? = WebApplicationImpl.activeApplication?.nativeApplication
    ): WebDomElement? {
        val href = resolveCssHref(resourceId)
        return addStyleSheet(href, rootElement)
    }

    /**
     * Adds or updates a `<link rel="stylesheet" href="...">` element for [styleSheet].
     */
    fun addStyleSheet(
        styleSheet: PrecompiledStyleSheet,
        rootElement: WebDomElement? = WebApplicationImpl.activeApplication?.nativeApplication
    ): WebDomElement? {
        val href = resolveCssHref(styleSheet)
        return addStyleSheet(href, rootElement)
    }

    /**
     * Removes the `<link rel="stylesheet" href="...">` element corresponding to [pathOrHref].
     */
    fun removeStyleSheet(
        pathOrHref: String,
        rootElement: WebDomElement? = WebApplicationImpl.activeApplication?.nativeApplication
    ): Boolean {
        val href = normalizeCssHref(pathOrHref)
        if (href.isEmpty()) return false
        val removed = activeLinksByHref.remove(href)
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
        return false
    }

    /**
     * Removes the `<link rel="stylesheet" href="...">` element corresponding to [resourceId].
     */
    fun removeStyleSheet(
        resourceId: ResourceID,
        rootElement: WebDomElement? = WebApplicationImpl.activeApplication?.nativeApplication
    ): Boolean {
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
        for (path in app.getStyleSheetPaths()) {
            val href = normalizeCssHref(path)
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
        return href in activeLinksByHref
    }

    fun getActiveStyleSheetHrefs(): List<String> = activeLinksByHref.keys.toList()

    fun getActiveLinkElements(): List<WebDomElement> = activeLinksByHref.values.toList()

    fun clear(rootElement: WebDomElement? = WebApplicationImpl.activeApplication?.nativeApplication) {
        for (href in activeLinksByHref.keys.toList()) {
            removeStyleSheet(href, rootElement)
        }
        activeLinksByHref.clear()
    }
}
