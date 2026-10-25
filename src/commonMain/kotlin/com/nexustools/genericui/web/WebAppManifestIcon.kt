package com.nexustools.genericui.web

/**
 * Represents a single icon entry inside the W3C / MDN Web App Manifest `icons` array
 * (`https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Manifest/Reference/icons`).
 *
 * `.svg` icons (`type = "image/svg+xml"`, `sizes = "any"`) are preferred for GenericUI Web targets,
 * alongside `"192x192"` and `"512x512"` size descriptors for Android Chrome WebApp installability.
 */
data class WebAppManifestIcon(
    val src: String,
    val sizes: String = "any",
    val type: String = "image/svg+xml",
    val purpose: String = "any maskable"
)
