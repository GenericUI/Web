package com.nexustools.genericui.web

/**
 * Represents an application shortcut entry inside the W3C / MDN Web App Manifest `shortcuts` array
 * (`https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Manifest/Reference/shortcuts`).
 */
data class WebAppManifestShortcut(
    val name: String,
    val shortName: String = "",
    val description: String = "",
    val url: String = "./",
    val icons: List<WebAppManifestIcon> = emptyList()
)
