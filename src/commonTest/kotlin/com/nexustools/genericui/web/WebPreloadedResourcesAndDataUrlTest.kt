package com.nexustools.genericui.web

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.content.ImageView
import com.nexustools.genericui.component.document.BookContainer
import com.nexustools.genericui.component.media.Image
import com.nexustools.genericui.component.shell.Window
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.document.PrintPageOrientation
import com.nexustools.genericui.enums.document.PrintPageSize
import com.nexustools.genericui.geometry.Size
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.image.ImageLoader
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
import kotlin.test.assertNull
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

    @Test
    fun testWebImageSetImageBufferAndBookContainerIframePreview() {
        val factory = WebNativeFactory()
        GenericUI.activeFactory = factory
        factory.createApplication()

        // 1. Direct WebImageImpl.setImageBuffer(buffer)
        val rawWebImage = factory.createImage() as WebImageImpl
        var loadCallCount = 0
        rawWebImage.setOnLoad { loadCallCount++ }

        val buffer = ImageBuffer(width = 12, height = 8).apply {
            fill(RgbaColor(37, 99, 235, 1f))
        }
        rawWebImage.setImageBuffer(buffer)

        assertEquals(1, loadCallCount)
        assertFalse(rawWebImage.isLoading())
        assertNull(rawWebImage.nativeWidget.getAttribute("data-genericui-loading"))
        assertNull(rawWebImage.nativeWidget.getAttribute("data-genericui-placeholder-active"))
        assertEquals("loaded", rawWebImage.nativeWidget.getAttribute("data-load-state"))
        assertEquals("12", rawWebImage.nativeWidget.getAttribute("data-natural-width"))
        assertEquals("8", rawWebImage.nativeWidget.getAttribute("data-natural-height"))

        val directSrc = rawWebImage.nativeWidget.getAttribute("src").orEmpty()
        assertTrue(directSrc.startsWith("data:image/png;base64,"), "Expected data:image/png;base64,... but got $directSrc")
        val memoryUri = rawWebImage.getSourceUrl().orEmpty()
        assertTrue(memoryUri.startsWith("memory://imagebuffer/12x8@"))
        assertEquals(buffer, ImageLoader.resolveMemoryBuffer(memoryUri))

        // Clearing buffer removes attributes cleanly
        rawWebImage.setImageBuffer(null)
        assertEquals("", rawWebImage.getSourceUrl())
        assertNull(rawWebImage.nativeWidget.getAttribute("src"))
        assertNull(rawWebImage.nativeWidget.getAttribute("data-load-state"))
        assertNull(rawWebImage.nativeWidget.getAttribute("data-natural-width"))
        assertNull(rawWebImage.nativeWidget.getAttribute("data-natural-height"))

        // Setting sourceUrl to a registered memory:// URI resolves directly via setImageBuffer
        ImageLoader.registerMemoryBuffer(memoryUri, buffer)
        rawWebImage.setSourceUrl(memoryUri)
        assertTrue(rawWebImage.nativeWidget.getAttribute("src").orEmpty().startsWith("data:image/png;base64,"))
        assertEquals("loaded", rawWebImage.nativeWidget.getAttribute("data-load-state"))

        // 2. ImageView.setImageBuffer(buffer) + BookContainer + BookPage on :web
        val window = Window("Book Print Preview Test")
        val book = BookContainer(
            pageSize = PrintPageSize.A4,
            orientation = PrintPageOrientation.PORTRAIT,
            marginMm = 10f,
            documentTitle = "Sample Printable Book"
        )
        val pageImageView = ImageView()
        pageImageView.getStyle().setWidth(com.nexustools.genericui.style.css.CssLength.percent(100f))
        pageImageView.getStyle().setHeight(com.nexustools.genericui.style.css.CssLength.AUTO)
        pageImageView.setImageBuffer(buffer)
        val unstyledImageView = ImageView()
        unstyledImageView.setImageBuffer(buffer)
        val page1 = book.addPage("Page 1") {
            addChild(pageImageView)
            addChild(unstyledImageView)
        }
        window.addChild(book)
        window.show(factory)

        val webPageImage = pageImageView.nativeImpl as WebImageImpl
        val pageImgSrc = webPageImage.nativeWidget.getAttribute("src").orEmpty()
        assertTrue(pageImgSrc.startsWith("data:image/png;base64,"), "Expected page ImageView img.src to be data:image/png;base64,... but got $pageImgSrc")
        assertEquals("loaded", webPageImage.nativeWidget.getAttribute("data-load-state"))
        assertEquals("12", webPageImage.nativeWidget.getAttribute("data-natural-width"))
        assertEquals("8", webPageImage.nativeWidget.getAttribute("data-natural-height"))

        // Verify explicit ComponentStyle width/height ("100%" / "auto") is preserved across setImageBuffer / setPreferredSize
        assertEquals("100%", webPageImage.nativeWidget.getStyle("width"))
        assertEquals("auto", webPageImage.nativeWidget.getStyle("height"))
        pageImageView.setPreferredSize(Size(2480, 3508))
        assertEquals("100%", webPageImage.nativeWidget.getStyle("width"))
        assertEquals("auto", webPageImage.nativeWidget.getStyle("height"))
        assertEquals(2480, webPageImage.nativeWidget.clientWidth)
        assertEquals(3508, webPageImage.nativeWidget.clientHeight)
        pageImageView.setPreferredSize(null)
        assertEquals("100%", webPageImage.nativeWidget.getStyle("width"))
        assertEquals("auto", webPageImage.nativeWidget.getStyle("height"))

        // Verify ImageView without explicit ComponentStyle width/height uses preferredSize px and clears cleanly on null
        val webUnstyledImage = unstyledImageView.nativeImpl as WebImageImpl
        assertEquals("12px", webUnstyledImage.nativeWidget.getStyle("width"))
        assertEquals("8px", webUnstyledImage.nativeWidget.getStyle("height"))
        unstyledImageView.setPreferredSize(Size(2480, 3508))
        assertEquals("2480px", webUnstyledImage.nativeWidget.getStyle("width"))
        assertEquals("3508px", webUnstyledImage.nativeWidget.getStyle("height"))
        unstyledImageView.setPreferredSize(null)
        assertNull(webUnstyledImage.nativeWidget.getStyle("width"))
        assertNull(webUnstyledImage.nativeWidget.getStyle("height"))

        val webBook = book.nativeImpl as WebBookContainerImpl
        val iframeCss = webBook.nativeWidget.getAttribute("data-iframe-css").orEmpty()
        assertTrue(
            iframeCss.contains(
                """.book-page img {
    max-width: 100% !important;
    width: 100% !important;
    height: auto !important;
    display: block !important;
}"""
            )
        )
        assertTrue(
            iframeCss.contains(
                """    .book-page img {
        max-height: 100vh !important;
        object-fit: contain !important;
    }"""
            )
        )
        assertTrue(iframeCss.contains("overflow-y: auto !important;"))
        assertTrue(iframeCss.contains("height: auto !important;"))
        assertTrue(iframeCss.contains("width: 210.0mm;") || iframeCss.contains("width: 210mm;"))
        assertTrue(iframeCss.contains("min-height: 297.0mm;") || iframeCss.contains("min-height: 297mm;"))
        assertTrue(iframeCss.contains("@media print"))
        assertTrue(iframeCss.contains("overflow: visible !important;"))
        assertTrue(iframeCss.contains("page-break-after: always !important;"))
        assertTrue(iframeCss.contains("break-after: page !important;"))

        // Verify BookContainer starts with zero stylesheets even when WebStyleSheetManager has host app stylesheets
        WebStyleSheetManager.addStyleSheet("styles/host_shell.css")
        assertTrue(WebStyleSheetManager.hasStyleSheet("styles/host_shell.css"))
        assertTrue(webBook.getBookStyleSheetHrefs().isEmpty())
        assertEquals("", webBook.nativeWidget.getAttribute("data-book-stylesheets").orEmpty())

        // Adding a preloaded ResourceID stylesheet to BookContainer resolves to data:text/css;base64,... on the book iframe
        // WITHOUT mounting the book stylesheet in the host document.head (WebStyleSheetManager.getActiveStyleSheetHrefs())
        val bookStyleResId = ResourceID(
            id = ResourceType.STYLE.buildNumericId(42),
            type = ResourceType.STYLE,
            name = "book_print_theme",
            relativePath = "styles/book_print_theme.scss"
        )
        val bookCssPayload = ".book-page h1 { color: #1e3a8a; font-family: Georgia, serif; }"
        WebBrowserDomBridge.registerPreloadedResource(
            key = bookStyleResId.toResourceUri(),
            mimeType = "text/css",
            bytes = bookCssPayload.encodeToByteArray(),
            text = bookCssPayload
        )
        val hostHrefsBefore = WebStyleSheetManager.getActiveStyleSheetHrefs().toSet()
        book.addStyleSheet(bookStyleResId)

        assertTrue(book.hasStyleSheet(bookStyleResId))
        val bookHrefs = webBook.getBookStyleSheetHrefs()
        assertEquals(1, bookHrefs.size)
        assertTrue(bookHrefs[0].startsWith("data:text/css;base64,"), "Expected data:text/css;base64,... but got ${bookHrefs[0]}")
        assertEquals(bookCssPayload, DataUrlCodec.decodeUtf8Text(bookHrefs[0]))
        assertEquals(bookHrefs[0], webBook.nativeWidget.getAttribute("data-book-stylesheets"))
        assertEquals(hostHrefsBefore, WebStyleSheetManager.getActiveStyleSheetHrefs().toSet())

        // Calling syncIframeDocument without styleSheetHrefs (null) preserves existing data-book-stylesheets
        WebPrintIframeBridge.syncIframeDocument(
            iframeElement = webBook.nativeWidget,
            documentTitle = "Sample Printable Book",
            pageSize = PrintPageSize.A4,
            orientation = PrintPageOrientation.PORTRAIT,
            marginMm = 10f
        )
        assertEquals(bookHrefs[0], webBook.nativeWidget.getAttribute("data-book-stylesheets"))

        // Removing the stylesheet from BookContainer clears it from webBook
        assertTrue(book.removeStyleSheet(bookStyleResId))
        assertFalse(book.hasStyleSheet(bookStyleResId))
        assertTrue(webBook.getBookStyleSheetHrefs().isEmpty())
        assertEquals("", webBook.nativeWidget.getAttribute("data-book-stylesheets").orEmpty())

        assertEquals(1, webBook.nativeWidget.children.size)
        val pageDom = (page1.nativeImpl as WebBaseNativeImpl).nativeWidget
        assertEquals(pageDom, webBook.nativeWidget.children.first())
        assertTrue(pageDom.children.contains(webPageImage.nativeWidget))
    }
}
