package com.nexustools.genericui.web

/**
 * Configuration for CDN-delivered Bootstrap 5 CSS and JavaScript bundle resources
 * used by the `:web` module's template engine and runtime DOM injector.
 */
data class WebBootstrapCdnConfig(
    val version: String = DEFAULT_BOOTSTRAP_VERSION,
    val cssCdnUrl: String = "https://cdn.jsdelivr.net/npm/bootstrap@$version/dist/css/bootstrap.min.css",
    val cssIntegrity: String = DEFAULT_CSS_INTEGRITY,
    val jsBundleCdnUrl: String = "https://cdn.jsdelivr.net/npm/bootstrap@$version/dist/js/bootstrap.bundle.min.js",
    val jsBundleIntegrity: String = DEFAULT_JS_BUNDLE_INTEGRITY,
    val includeBootstrapIcons: Boolean = true,
    val iconsCssCdnUrl: String = "https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css",
    val crossOrigin: String = "anonymous",
    val extraCssCdnUrls: List<String> = emptyList(),
    val extraJsCdnUrls: List<String> = emptyList()
) {
    companion object {
        const val DEFAULT_BOOTSTRAP_VERSION: String = "5.3.3"
        const val DEFAULT_CSS_INTEGRITY: String =
            "sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH"
        const val DEFAULT_JS_BUNDLE_INTEGRITY: String =
            "sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz"
    }
}
