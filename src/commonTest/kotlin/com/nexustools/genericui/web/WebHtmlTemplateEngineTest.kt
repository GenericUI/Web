package com.nexustools.genericui.web

import com.nexustools.genericui.enums.ui.ThemeMode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WebHtmlTemplateEngineTest {

    @Test
    fun testRenderIndexHtmlContainsBootstrapCdnSplashAndHashedLoaderScripts() {
        val engine = WebHtmlTemplateEngine()
        val html = engine.renderIndexHtml(
            title = "My GenericUI Web App",
            themeMode = ThemeMode.DARK,
            wasmJsUrl = "app-wasm.js",
            wasmHash = "1111111111111111111111111111111111111111",
            jsIrUrl = "app-js.js",
            jsHash = "2222222222222222222222222222222222222222",
            webIconUrl = "icon.svg"
        )

        assertTrue(html.contains("<title>My GenericUI Web App</title>"))
        assertTrue(html.contains("https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"))
        assertTrue(html.contains("src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js\""))
        assertTrue(html.contains("https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css"))
        assertTrue(html.contains("data-bs-theme=\"dark\""))
        assertTrue(html.contains("<link id=\"genericui-favicon\" rel=\"icon\" type=\"image/svg+xml\" href=\"icon.svg\">"))
        assertTrue(html.contains("id=\"genericui-splash\""))
        assertTrue(html.contains("id=\"genericui-splash-icon\" src=\"icon.svg\""))
        assertTrue(html.contains("id=\"genericui-splash-progress-bar\""))
        assertTrue(html.contains("id=\"genericui-splash-error\""))
        assertTrue(html.contains("id=\"genericui-splash-error-stack\""))
        assertTrue(html.contains("<script type=\"loader-wasm\" src=\"app-wasm.js?1111111111111111111111111111111111111111\"></script>"))
        assertTrue(html.contains("<script type=\"loader-js\" src=\"app-js.js?2222222222222222222222222222222222222222\"></script>"))
        assertTrue(html.contains("<script id=\"genericui-loader-script\" src=\"genericui-loader.js?${WebWasmJsLoader.defaultLoaderSha1Hash}\" defer></script>"))
        assertFalse(html.contains("{{APP_BUNDLE_SCRIPT}}"))
    }

    @Test
    fun testWebApplicationInjectsCdnResourceElements() {
        val factory = WebNativeFactory()
        val app = factory.createApplication() as WebApplicationImpl
        val cssLink = app.nativeApplication.children.firstOrNull { it.id == "genericui-bootstrap-css" }
        val jsScript = app.nativeApplication.children.firstOrNull { it.id == "genericui-bootstrap-js" }

        assertNotNull(cssLink)
        assertNotNull(jsScript)
        assertTrue(cssLink.getAttribute("href")?.contains("bootstrap@5.3.3/dist/css/bootstrap.min.css") == true)
        assertTrue(jsScript.getAttribute("src")?.contains("bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js") == true)
    }
}
