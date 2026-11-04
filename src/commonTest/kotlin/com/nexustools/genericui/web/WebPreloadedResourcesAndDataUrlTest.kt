package com.nexustools.genericui.web

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.media.Image
import com.nexustools.genericui.component.shell.Window
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.res.DataUrlCodec
import com.nexustools.genericui.resources.ResourceType
import com.nexustools.genericui.res.Resources
import com.nexustools.genericui.resources.ResourceID
import com.nexustools.genericui.resources.ResourceResolver
import com.nexustools.genericui.style.color.RgbaColor
import com.nexustools.genericui.style.css.StyleSheetRegistry
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WebPreloadedResourcesAndDataUrlTest {

    @Test
    fun testDataUrlCodecRoundTripsBinaryAndUtf8CssAndSvgPayloads() {
        val pngBytes = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        )
        val encodedPng = DataUrlCodec.encodeBytes(pngBytes, "image/png")
        assertTrue(DataUrlCodec.isDataUrl(encodedPng))
        assertTrue(encodedPng.startsWith("data:image/png;base64,"))
        assertEquals("image/png", DataUrlCodec.extractMimeType(encodedPng))
        val decodedPng = DataUrlCodec.decodeBytes(encodedPng)
        assertNotNull(decodedPng)
        assertContentEquals(pngBytes, decodedPng)

        val cssText = ".studio-card { background-color: #1e293b; color: #f8fafc; border-radius: 12px; }"
        val encodedCss = DataUrlCodec.encodeUtf8Text(cssText, "text/css")
        assertTrue(DataUrlCodec.isDataUrl(encodedCss))
        assertTrue(encodedCss.startsWith("data:text/css;base64,"))
        assertEquals("text/css", DataUrlCodec.extractMimeType(encodedCss))
        assertEquals(cssText, DataUrlCodec.decodeUtf8Text(encodedCss))

        val svgText = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/></svg>"""
        val encodedSvg = DataUrlCodec.encodeUtf8Text(svgText, "image/svg+xml")
        assertTrue(encodedSvg.startsWith("data:image/svg+xml;base64,"))
        assertEquals("image/svg+xml", DataUrlCodec.extractMimeType(encodedSvg))
        assertEquals(svgText, DataUrlCodec.decodeUtf8Text(encodedSvg))

        assertEquals("text/css", DataUrlCodec.guessMimeType("styles/app.css"))
        assertEquals("text/css", DataUrlCodec.guessMimeType("styles/theme.scss"))
        assertEquals("image/png", DataUrlCodec.guessMimeType("assets/drawable/logo.png"))
        assertEquals("image/jpeg", DataUrlCodec.guessMimeType("photo.jpg"))
        assertEquals("image/jpeg", DataUrlCodec.guessMimeType("photo.jpeg"))
        assertEquals("image/gif", DataUrlCodec.guessMimeType("anim.gif"))
        assertEquals("image/webp", DataUrlCodec.guessMimeType("hero.webp"))
        assertEquals("image/svg+xml", DataUrlCodec.guessMimeType("icon.svg"))
        assertEquals("image/x-icon", DataUrlCodec.guessMimeType("favicon.ico"))
        assertEquals("font/woff2", DataUrlCodec.guessMimeType("inter.woff2"))
        assertEquals("font/woff", DataUrlCodec.guessMimeType("inter.woff"))
        assertEquals("font/ttf", DataUrlCodec.guessMimeType("inter.ttf"))
        assertEquals("font/otf", DataUrlCodec.guessMimeType("inter.otf"))
        assertEquals("application/json", DataUrlCodec.guessMimeType("config.json"))
        assertEquals("application/xml", DataUrlCodec.guessMimeType("layout.xml"))
        assertEquals("text/plain", DataUrlCodec.guessMimeType("readme.txt"))
        assertEquals("audio/mpeg", DataUrlCodec.guessMimeType("click.mp3"))
        assertEquals("audio/wav", DataUrlCodec.guessMimeType("tone.wav"))
        assertEquals("audio/ogg", DataUrlCodec.guessMimeType("music.ogg"))
        assertEquals("video/mp4", DataUrlCodec.guessMimeType("intro.mp4"))
        assertEquals("video/webm", DataUrlCodec.guessMimeType("clip.webm"))

        assertEquals("text/css", DataUrlCodec.guessMimeType("app_theme", "style"))
        assertEquals("image/png", DataUrlCodec.guessMimeType("ic_brush", "drawable"))
        assertEquals("application/octet-stream", DataUrlCodec.guessMimeType("blob_data", "raw"))
    }

    @Test
    fun testLoaderShortStatusMessagesAndResourcePreloadPipeline() {
        assertEquals(
            listOf("Initializing", "Loading Resources", "Loading Application", "Loading Fallback", "Error", "Ready"),
            WebWasmJsLoader.ALLOWED_STATUS_MESSAGES
        )
        assertEquals("Initializing", WebSplashConfig().statusText)
        assertEquals("Initializing", WebSplashConfig.normalizeStatusText("Detecting WebAssembly support..."))
        assertEquals("Loading Resources", WebSplashConfig.normalizeStatusText("Preloading application resources..."))
        assertEquals("Loading Application", WebSplashConfig.normalizeStatusText("Downloading WebAssembly binary"))
        assertEquals("Error", WebSplashConfig.normalizeStatusText("Failed to load application"))
        assertEquals("Ready", WebSplashConfig.normalizeStatusText("Complete"))

        val script = WebWasmJsLoader.DEFAULT_LOADER_SCRIPT
        assertTrue(script.contains("\"Initializing\""))
        assertTrue(script.contains("\"Loading Resources\""))
        assertTrue(script.contains("\"Loading Application\""))
        assertTrue(script.contains("\"Error\""))
        assertTrue(script.contains("\"Ready\""))
        assertTrue(script.contains("__GENERICUI_RESOURCE_MANIFEST__"))
        assertTrue(script.contains("__GENERICUI_PRELOADED_RESOURCES__"))
        assertTrue(script.contains("./resources.json"))
        assertTrue(script.contains("preloadAllResources"))
        assertFalse(script.contains("Downloading JS application bundle"))
        assertFalse(script.contains("Detecting WebAssembly support..."))
    }

    @Test
    fun testPreloadedResourceIdStylesheetAndDrawableImageDataUrlResolution() {
        val factory = WebNativeFactory()
        GenericUI.activeFactory = factory
        val app = factory.createApplication() as WebApplicationImpl
        WebStyleSheetManager.clear(app.nativeApplication)
        WebBrowserDomBridge.clearPreloadedResources()

        val styleResId = ResourceID(
            id = ResourceType.STYLE.buildNumericId(1),
            type = ResourceType.STYLE,
            name = "studio_dark_theme",
            relativePath = "styles/studio_dark_theme.scss"
        )
        val cssPayload = ".accent-btn { background-color: #2563eb; color: #ffffff; border-radius: 8px; }"
        WebBrowserDomBridge.registerPreloadedResource(
            key = styleResId.toResourceUri(),
            mimeType = "text/css",
            bytes = cssPayload.encodeToByteArray(),
            text = cssPayload
        )

        val resolver = WebResourceResolverImpl()
        val resolvedStyleUrl = resolver.resolveUrl(styleResId)
        assertTrue(resolvedStyleUrl.startsWith("data:text/css;base64,"), "Expected data:text/css;base64,... but got $resolvedStyleUrl")
        assertEquals(cssPayload, DataUrlCodec.decodeUtf8Text(resolvedStyleUrl))
        assertEquals(cssPayload, resolver.getText(styleResId))

        // Add stylesheet via ResourceID in WebStyleSheetManager -> attaches <link rel="stylesheet" href="data:text/css;base64,...">
        val linkEl = WebStyleSheetManager.addStyleSheet(styleResId, app.nativeApplication)
        assertNotNull(linkEl)
        assertEquals("stylesheet", linkEl.getAttribute("rel"))
        val href = linkEl.getAttribute("href").orEmpty()
        assertTrue(href.startsWith("data:text/css;base64,"))
        assertEquals(cssPayload, DataUrlCodec.decodeUtf8Text(href))

        // Verify CSS rules were parsed for ComputedStyle resolution
        val window = Window("Styled Window")
        val btn = Button("Accent")
        btn.addClass("accent-btn")
        window.addChild(btn)
        val computed = WebStyleSheetManager.resolveComputedStyle(styleResId, btn)
        assertEquals(RgbaColor.fromHex("#2563eb"), computed.backgroundColor)
        assertEquals(RgbaColor.fromHex("#ffffff"), computed.textColor)
        assertEquals(8f, computed.borderRadiusPx)
        assertNotNull(StyleSheetRegistry.resolveByResourceId(styleResId))
        assertNotNull(Resources.getStyleSheet(styleResId))

        // Register a preloaded PNG drawable resource and verify Image(ResourceID) sets img.src to data:image/png;base64,...
        val logoResId = ResourceID(
            id = ResourceType.DRAWABLE.buildNumericId(2),
            type = ResourceType.DRAWABLE,
            name = "ic_studio_logo",
            relativePath = "drawable/ic_studio_logo.png"
        )
        val fakePngBytes = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x01, 0x02, 0x03, 0x04
        )
        WebBrowserDomBridge.registerPreloadedResource(
            key = logoResId.toResourceUri(),
            mimeType = "image/png",
            bytes = fakePngBytes
        )

        val resolvedLogoUrl = ResourceResolver.getUrl(logoResId)
        assertTrue(resolvedLogoUrl.startsWith("data:image/png;base64,"))
        assertContentEquals(fakePngBytes, DataUrlCodec.decodeBytes(resolvedLogoUrl))
        assertContentEquals(fakePngBytes, resolver.getRawBytes(logoResId))
        val drawable = resolver.getDrawable(logoResId)
        assertNotNull(drawable)
        assertEquals("png", drawable.format)

        val imageComponent = Image(logoResId)
        window.addChild(imageComponent)
        window.show(factory)

        val webImage = imageComponent.nativeImpl as WebImageImpl
        val imgSrc = webImage.nativeWidget.getAttribute("src").orEmpty()
        assertTrue(imgSrc.startsWith("data:image/png;base64,"), "Expected img.src to be data:image/png;base64,... but got $imgSrc")
        assertContentEquals(fakePngBytes, DataUrlCodec.decodeBytes(imgSrc))
    }
}
