package com.nexustools.genericui.web

import com.nexustools.genericui.enums.ui.ThemeMode
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WebHtmlTemplateEngineTest {

    @Test
    fun testRenderIndexHtmlContainsBootstrapCdnCssAndJs() {
        val engine = WebHtmlTemplateEngine()
        val html = engine.renderIndexHtml(
            title = "My GenericUI Web App",
            themeMode = ThemeMode.DARK,
            appBundleScriptUrl = "app.js"
        )

        assertTrue(html.contains("<title>My GenericUI Web App</title>"))
        assertTrue(html.contains("https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"))
        assertTrue(html.contains("https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"))
        assertTrue(html.contains("https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css"))
        assertTrue(html.contains("data-bs-theme=\"dark\""))
        assertTrue(html.contains("<script src=\"app.js\"></script>"))
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
