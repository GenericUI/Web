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
    val iconSizePx: Int = 64,
    val statusText: String = STATUS_INITIALIZING,
    val progressBarClass: String = "progress-bar progress-bar-striped progress-bar-animated",
    val fadeDurationMs: Int = 350
) {
    /**
     * Returns [statusText] normalized to one of the 5 allowed short 1–2 word status messages.
     */
    val normalizedStatusText: String
        get() = normalizeStatusText(statusText)

    /**
     * Renders the HTML snippet for the `{{SPLASH_ICON}}` placeholder inside `#genericui-splash`.
     */
    fun renderIconHtml(indent: String = "            "): String {
        val trimmedUrl = iconUrl?.trim().orEmpty()
        if (trimmedUrl.isNotEmpty()) {
            return """$indent<img id="genericui-splash-icon" src="$trimmedUrl" alt="$iconAlt" class="genericui-splash-icon mb-3 rounded" width="$iconSizePx" height="$iconSizePx">"""
        }
        val trimmedClass = iconBootstrapClass?.trim().orEmpty()
        if (trimmedClass.isNotEmpty()) {
            return """$indent<i id="genericui-splash-icon" class="bi $trimmedClass fs-1 text-primary mb-3" aria-hidden="true"></i>"""
        }
        return ""
    }

    companion object {
        const val STATUS_INITIALIZING: String = "Initializing"
        const val STATUS_LOADING_RESOURCES: String = "Loading Resources"
        const val STATUS_LOADING_APPLICATION: String = "Loading Application"
        const val STATUS_LOADING_FALLBACK: String = "Loading Fallback"
        const val STATUS_ERROR: String = "Error"
        const val STATUS_READY: String = "Ready"

        val ALLOWED_STATUS_MESSAGES: Set<String> = setOf(
            STATUS_INITIALIZING,
            STATUS_LOADING_RESOURCES,
            STATUS_LOADING_APPLICATION,
            STATUS_LOADING_FALLBACK,
            STATUS_ERROR,
            STATUS_READY
        )

        /**
         * Normalizes any status text to one of the allowed short 1–2 word status messages:
         * `"Initializing"`, `"Loading Resources"`, `"Loading Application"`, `"Loading Fallback"`, `"Error"`, or `"Ready"`.
         */
        fun normalizeStatusText(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed in ALLOWED_STATUS_MESSAGES) return trimmed
            val lower = trimmed.lowercase()
            return when {
                lower.contains("fallback") -> STATUS_LOADING_FALLBACK
                lower.contains("error") || lower.contains("fail") || lower.contains("exception") -> STATUS_ERROR
                lower.contains("ready") || lower.contains("complete") || lower.contains("finish") -> STATUS_READY
                lower.contains("resource") || lower.contains("preload") || lower.contains("asset") -> STATUS_LOADING_RESOURCES
                lower.contains("application") || lower.contains("wasm") || lower.contains("bundle") || lower.contains("download") -> STATUS_LOADING_APPLICATION
                else -> STATUS_INITIALIZING
            }
        }
    }
}
