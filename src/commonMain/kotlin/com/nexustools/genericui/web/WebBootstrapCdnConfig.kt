package com.nexustools.genericui.web

/**
 * Configuration for packaged local (or custom URL) Bootstrap 5 CSS and JavaScript bundle resources
 * used by the `:web` module's template engine and `<head>` stylesheet manager.
 */
data class WebBootstrapCdnConfig(
    val version: String = DEFAULT_BOOTSTRAP_VERSION,
    val cssCdnUrl: String = WebNamespaceSpec.DEFAULT_BOOTSTRAP_CSS_URL,
    val cssIntegrity: String = DEFAULT_CSS_INTEGRITY,
    val jsBundleCdnUrl: String = WebNamespaceSpec.DEFAULT_BOOTSTRAP_JS_URL,
    val jsBundleIntegrity: String = DEFAULT_JS_BUNDLE_INTEGRITY,
    val includeBootstrapIcons: Boolean = true,
    val iconsCssCdnUrl: String = WebNamespaceSpec.DEFAULT_BOOTSTRAP_ICONS_CSS_URL,
    val crossOrigin: String = "",
    val extraCssCdnUrls: List<String> = emptyList(),
    val extraJsCdnUrls: List<String> = emptyList()
) {
    companion object {
        const val DEFAULT_BOOTSTRAP_VERSION: String = "5.3.3"
        const val DEFAULT_CSS_INTEGRITY: String = ""
        const val DEFAULT_JS_BUNDLE_INTEGRITY: String = ""
    }
}
