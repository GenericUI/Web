package com.nexustools.genericui.web

/**
 * Configuration for the Bootstrap-styled splash screen (`#genericui-splash`), icon,
 * download progress bar (`#genericui-splash-progress-bar`), and error stacktrace alert
 * (`#genericui-splash-error`) rendered in `index.template.html`.
 */
data class WebSplashConfig(
    val enabled: Boolean = true,
    val splashId: String = "genericui-splash",
    val iconUrl: String? = null,
    val iconBootstrapClass: String? = "bi-layers-half",
    val iconAlt: String = "Application Icon",
    val iconSizePx: Int = 256,
    val statusText: String = "Loading application...",
    val progressBarClass: String = "progress-bar progress-bar-striped progress-bar-animated",
    val fadeDurationMs: Int = 350
) {
    /**
     * Renders the HTML snippet for the `{{SPLASH_ICON}}` placeholder inside `#genericui-splash`.
     */
    fun renderIconHtml(indent: String = "            "): String {
        val trimmedUrl = iconUrl?.trim().orEmpty()
        if (trimmedUrl.isNotEmpty()) {
            return """$indent<img id="genericui-splash-icon" src="$trimmedUrl" alt="$iconAlt" class="genericui-splash-icon mb-3 rounded" width="$iconSizePx" height="$iconSizePx" style="width: ${iconSizePx}px; height: ${iconSizePx}px; object-fit: contain;">"""
        }
        val trimmedClass = iconBootstrapClass?.trim().orEmpty()
        if (trimmedClass.isNotEmpty()) {
            return """$indent<i id="genericui-splash-icon" class="bi $trimmedClass text-primary mb-3 d-inline-flex align-items-center justify-content-center" style="width: ${iconSizePx}px; height: ${iconSizePx}px; font-size: ${iconSizePx}px; line-height: 1;" aria-hidden="true"></i>"""
        }
        return ""
    }
}
