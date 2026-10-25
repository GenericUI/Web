package com.nexustools.genericui.web

/**
 * Configuration model for generating a W3C / MDN Progressive Web App (PWA) Web App Manifest
 * (`manifest.webmanifest`, `https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Manifest`)
 * and injecting HTML `<head>` tags that allow the application to be installed as a standalone
 * WebApp on Android (via Chrome), iOS, and Desktop browsers.
 */
data class WebAppManifestConfig(
    val enabled: Boolean = false,
    val allowInstallAsWebApp: Boolean = false,
    val manifestFileName: String = "manifest.webmanifest",
    val id: String = "",
    val name: String = "",
    val shortName: String = "",
    val description: String = "",
    val startUrl: String = "./",
    val scope: String = "./",
    val display: String = "standalone",
    val displayOverride: List<String> = emptyList(),
    val orientation: String = "any",
    val themeColor: String = "#0d6efd",
    val backgroundColor: String = "#121519",
    val lang: String = "en",
    val dir: String = "auto",
    val categories: List<String> = emptyList(),
    val preferRelatedApplications: Boolean = false,
    val appleStatusBarStyle: String = "black-translucent",
    val icons: List<WebAppManifestIcon> = emptyList(),
    val shortcuts: List<WebAppManifestShortcut> = emptyList()
) {
    /**
     * Returns `true` when either [enabled] or [allowInstallAsWebApp] is `true`.
     * Enabling [allowInstallAsWebApp] automatically requires a Web App Manifest for Chrome on Android
     * to trigger WebAPK / standalone WebApp installation.
     */
    val isActive: Boolean
        get() = enabled || allowInstallAsWebApp

    /**
     * Resolves the effective manifest icon list. If [icons] is non-empty, returns [icons];
     * otherwise, if [defaultIconUrl] is provided, builds `.svg`-preferred icon entries covering
     * `"any"`, `"192x192"`, and `"512x512"` so Chrome on Android and desktop PWA installers
     * accept the manifest for standalone WebApp installation.
     */
    fun resolveIcons(
        defaultIconUrl: String?,
        inferMimeType: (String) -> String = ::defaultInferMimeType
    ): List<WebAppManifestIcon> {
        if (icons.isNotEmpty()) {
            return icons
        }
        val cleanIcon = defaultIconUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return emptyList()
        val mime = inferMimeType(cleanIcon)
        return if (mime == "image/svg+xml") {
            listOf(
                WebAppManifestIcon(
                    src = cleanIcon,
                    sizes = "any",
                    type = mime,
                    purpose = "any maskable"
                ),
                WebAppManifestIcon(
                    src = cleanIcon,
                    sizes = "192x192 512x512",
                    type = mime,
                    purpose = "any maskable"
                )
            )
        } else {
            listOf(
                WebAppManifestIcon(
                    src = cleanIcon,
                    sizes = "192x192",
                    type = mime,
                    purpose = "any maskable"
                ),
                WebAppManifestIcon(
                    src = cleanIcon,
                    sizes = "512x512",
                    type = mime,
                    purpose = "any maskable"
                )
            )
        }
    }

    /**
     * Serializes this configuration into a standard W3C / MDN `manifest.webmanifest` JSON document.
     */
    fun toManifestJson(
        defaultAppName: String = "GenericUI Web Application",
        defaultIconUrl: String? = null,
        inferMimeType: (String) -> String = ::defaultInferMimeType
    ): String {
        val resolvedName = name.trim().ifEmpty { defaultAppName.trim().ifEmpty { "GenericUI Web Application" } }
        val resolvedShortName = shortName.trim().ifEmpty { resolvedName }
        val resolvedIcons = resolveIcons(defaultIconUrl, inferMimeType)

        val lines = mutableListOf<String>()
        if (id.isNotBlank()) {
            lines.add("""  "id": "${escapeJson(id.trim())}"""")
        }
        lines.add("""  "name": "${escapeJson(resolvedName)}"""")
        lines.add("""  "short_name": "${escapeJson(resolvedShortName)}"""")
        if (description.isNotBlank()) {
            lines.add("""  "description": "${escapeJson(description.trim())}"""")
        }
        lines.add("""  "lang": "${escapeJson(lang.trim().ifEmpty { "en" })}"""")
        lines.add("""  "dir": "${escapeJson(dir.trim().ifEmpty { "auto" })}"""")
        lines.add("""  "start_url": "${escapeJson(startUrl.trim().ifEmpty { "./" })}"""")
        lines.add("""  "scope": "${escapeJson(scope.trim().ifEmpty { "./" })}"""")
        lines.add("""  "display": "${escapeJson(display.trim().ifEmpty { "standalone" })}"""")
        if (displayOverride.isNotEmpty()) {
            val overridesJson = displayOverride.joinToString(", ") { """"${escapeJson(it.trim())}"""" }
            lines.add("""  "display_override": [$overridesJson]""")
        }
        lines.add("""  "orientation": "${escapeJson(orientation.trim().ifEmpty { "any" })}"""")
        lines.add("""  "theme_color": "${escapeJson(themeColor.trim().ifEmpty { "#0d6efd" })}"""")
        lines.add("""  "background_color": "${escapeJson(backgroundColor.trim().ifEmpty { "#121519" })}"""")
        lines.add("""  "prefer_related_applications": $preferRelatedApplications""")

        if (categories.isNotEmpty()) {
            val catsJson = categories.joinToString(", ") { """"${escapeJson(it.trim())}"""" }
            lines.add("""  "categories": [$catsJson]""")
        }

        if (resolvedIcons.isNotEmpty()) {
            val iconEntries = resolvedIcons.joinToString(",\n") { icon ->
                buildString {
                    append("    {\n")
                    append("""      "src": "${escapeJson(icon.src)}",""").append("\n")
                    append("""      "sizes": "${escapeJson(icon.sizes.ifBlank { "any" })}",""").append("\n")
                    append("""      "type": "${escapeJson(icon.type.ifBlank { inferMimeType(icon.src) })}",""").append("\n")
                    append("""      "purpose": "${escapeJson(icon.purpose.ifBlank { "any maskable" })}"""").append("\n")
                    append("    }")
                }
            }
            lines.add("  \"icons\": [\n$iconEntries\n  ]")
        } else {
            lines.add("  \"icons\": []")
        }

        if (shortcuts.isNotEmpty()) {
            val shortcutEntries = shortcuts.joinToString(",\n") { sc ->
                buildString {
                    append("    {\n")
                    append("""      "name": "${escapeJson(sc.name)}",""").append("\n")
                    if (sc.shortName.isNotBlank()) {
                        append("""      "short_name": "${escapeJson(sc.shortName)}",""").append("\n")
                    }
                    if (sc.description.isNotBlank()) {
                        append("""      "description": "${escapeJson(sc.description)}",""").append("\n")
                    }
                    append("""      "url": "${escapeJson(sc.url.ifBlank { "./" })}"""").append("\n")
                    append("    }")
                }
            }
            lines.add("  \"shortcuts\": [\n$shortcutEntries\n  ]")
        }

        return "{\n" + lines.joinToString(",\n") + "\n}\n"
    }

    /**
     * Renders the `<link rel="manifest">` and installable mobile/desktop WebApp `<meta>` / `<link>`
     * tags for injection into `index.template.html`.
     */
    fun renderHeadTagsHtml(
        defaultAppName: String = "GenericUI Web Application",
        defaultIconUrl: String? = null,
        manifestHash: String? = null,
        indent: String = "    "
    ): String {
        if (!isActive) {
            return ""
        }
        val resolvedName = name.trim().ifEmpty { defaultAppName.trim().ifEmpty { "GenericUI Web Application" } }
        val resolvedShortName = shortName.trim().ifEmpty { resolvedName }
        val resolvedFile = manifestFileName.trim().ifEmpty { "manifest.webmanifest" }
        val manifestHref = if (!manifestHash.isNullOrBlank()) {
            WebSha1Hasher.appendHashQuery(resolvedFile, manifestHash)
        } else {
            resolvedFile
        }
        val resolvedThemeColor = themeColor.trim().ifEmpty { "#0d6efd" }
        val primaryIcon = icons.firstOrNull()?.src?.trim()?.takeIf { it.isNotEmpty() }
            ?: defaultIconUrl?.trim()?.takeIf { it.isNotEmpty() }

        val tags = mutableListOf<String>()
        tags.add("""$indent<link id="genericui-web-manifest" rel="manifest" href="${escapeHtmlAttr(manifestHref)}" crossorigin="use-credentials">""")
        tags.add("""$indent<meta id="genericui-theme-color" name="theme-color" content="${escapeHtmlAttr(resolvedThemeColor)}">""")

        if (allowInstallAsWebApp) {
            tags.add("""$indent<meta id="genericui-mobile-web-app-capable" name="mobile-web-app-capable" content="yes">""")
            tags.add("""$indent<meta id="genericui-apple-mobile-web-app-capable" name="apple-mobile-web-app-capable" content="yes">""")
            tags.add("""$indent<meta id="genericui-apple-mobile-web-app-status-bar-style" name="apple-mobile-web-app-status-bar-style" content="${escapeHtmlAttr(appleStatusBarStyle.trim().ifEmpty { "black-translucent" })}">""")
            tags.add("""$indent<meta id="genericui-application-name" name="application-name" content="${escapeHtmlAttr(resolvedShortName)}">""")
            tags.add("""$indent<meta id="genericui-apple-mobile-web-app-title" name="apple-mobile-web-app-title" content="${escapeHtmlAttr(resolvedShortName)}">""")
            if (!primaryIcon.isNullOrBlank()) {
                tags.add("""$indent<link id="genericui-apple-touch-icon" rel="apple-touch-icon" href="${escapeHtmlAttr(primaryIcon)}">""")
            }
        }
        return tags.joinToString("\n")
    }

    companion object {
        private fun defaultInferMimeType(url: String): String {
            val clean = url.substringBefore('?').substringBefore('#').lowercase()
            return when {
                clean.endsWith(".svg") -> "image/svg+xml"
                clean.endsWith(".png") -> "image/png"
                clean.endsWith(".webp") -> "image/webp"
                clean.endsWith(".ico") -> "image/x-icon"
                clean.endsWith(".gif") -> "image/gif"
                else -> "image/svg+xml"
            }
        }

        private fun escapeJson(value: String): String =
            value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")

        private fun escapeHtmlAttr(value: String): String =
            value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
    }
}
