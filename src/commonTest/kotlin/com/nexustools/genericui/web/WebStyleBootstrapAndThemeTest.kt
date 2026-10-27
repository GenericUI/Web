package com.nexustools.genericui.web

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.shell.Window
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.bootstrap.BootstrapHeight
import com.nexustools.genericui.enums.bootstrap.BootstrapSize
import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.enums.bootstrap.BootstrapWidth
import com.nexustools.genericui.enums.bootstrap.SpacingScale
import com.nexustools.genericui.enums.css.CssCursor
import com.nexustools.genericui.enums.ui.ThemeMode
import com.nexustools.genericui.style.RgbaColor
import com.nexustools.genericui.style.css.CssCustomCursor
import com.nexustools.genericui.style.css.CssLength
import com.nexustools.genericui.style.css.CssProperty
import com.nexustools.genericui.style.css.CssStyleSheet
import com.nexustools.genericui.style.css.PrecompiledStyleSheet
import com.nexustools.genericui.style.theme.SystemThemeDetector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebStyleBootstrapAndThemeTest {

    @Test
    fun testUserClassesStrictlyIsolatedFromBootstrapEnumClassesOnWebDomElement() {
        val factory = WebNativeFactory()
        GenericUI.activeFactory = factory
        val window = Window("Test Window")
        val btn = Button("Submit")
        window.addChild(btn)
        window.show()

        val webImpl = btn.nativeImpl as WebButtonImpl
        val dom = webImpl.nativeWidget

        // Initially no user classes and no Bootstrap type
        assertTrue(btn.getClasses().isEmpty())
        assertTrue(webImpl.getClasses().isEmpty())
        assertEquals("", btn.getClass())

        // Configure Bootstrap properties via component.getBootstrap()
        val bs = btn.getBootstrap()
        bs.type = BootstrapType.DANGER
        bs.size = BootstrapSize.LG
        bs.width = BootstrapWidth.W_100
        bs.height = BootstrapHeight.H_50
        bs.marginTop = SpacingScale.S3
        bs.paddingX = SpacingScale.S4

        // Bootstrap enum classes are NEVER visible in getClasses() / hasClass() / getClass()
        assertTrue(btn.getClasses().isEmpty())
        assertFalse(btn.hasClass("btn"))
        assertFalse(btn.hasClass("btn-danger"))
        assertFalse(btn.hasClass("btn-lg"))
        assertFalse(btn.hasClass("w-100"))
        assertFalse(webImpl.hasClass("btn-danger"))
        assertEquals("", webImpl.getClass())

        // Yet on WebDomElement, the enum classes are present in enumClassList and effective DOM className
        assertTrue("btn" in dom.enumClassList)
        assertTrue("btn-danger" in dom.enumClassList)
        assertTrue("btn-lg" in dom.enumClassList)
        assertTrue("w-100" in dom.enumClassList)
        assertTrue("h-50" in dom.enumClassList)
        assertTrue("mt-3" in dom.enumClassList)
        assertTrue("px-4" in dom.enumClassList)
        val effectiveClass = dom.getEffectiveDomClassName()
        assertTrue(effectiveClass.contains("btn-danger"))
        assertTrue(effectiveClass.contains("w-100"))

        // Adding a custom user class makes ONLY the user class visible in getClasses() / hasClass()
        btn.addClass("my-custom-btn")
        assertEquals(setOf("my-custom-btn"), btn.getClasses())
        assertEquals(setOf("my-custom-btn"), webImpl.getClasses())
        assertTrue(btn.hasClass("my-custom-btn"))
        assertFalse(btn.hasClass("btn-danger"))
        assertTrue(dom.getEffectiveDomClassName().contains("my-custom-btn"))
        assertTrue(dom.getEffectiveDomClassName().contains("btn-danger"))

        // Null-clearing or setting BootstrapType.NONE removes semantic enum classes while preserving user classes
        bs.type = BootstrapType.NONE
        assertFalse("btn-danger" in dom.enumClassList)
        assertFalse("btn-" in dom.enumClassList)
        bs.type = null
        bs.size = null
        bs.width = null
        assertFalse("btn-danger" in dom.enumClassList)
        assertFalse("btn-lg" in dom.enumClassList)
        assertFalse("w-100" in dom.enumClassList)
        assertEquals(setOf("my-custom-btn"), btn.getClasses())
    }

    @Test
    fun testComponentStyleInlineCssImportantNullClearingAndCustomCursorInWebDom() {
        val factory = WebNativeFactory()
        GenericUI.activeFactory = factory
        val window = Window("Style Window")
        val btn = Button("Styled")
        window.addChild(btn)
        window.show()

        val webImpl = btn.nativeImpl as WebButtonImpl
        val dom = webImpl.nativeWidget

        val style = btn.getStyle()
        style.backgroundColor = RgbaColor.fromHex("#112233")
        style.setImportant(CssProperty.BACKGROUND_COLOR, true)
        style.width = CssLength.percent(75f)
        style.cursor = CssCustomCursor(
            urlOrPath = "/assets/cursors/brush.png",
            hotspotX = 4,
            hotspotY = 12,
            fallback = CssCursor.CROSSHAIR
        )

        assertEquals("rgba(17, 34, 51, 1.0)", dom.getStyle("background-color"))
        assertTrue(dom.isStyleImportant("background-color"))
        assertEquals("75%", dom.getStyle("width"))
        assertEquals("url(\"/assets/cursors/brush.png\") 4 12, crosshair", dom.getStyle("cursor"))

        val outerHtml = dom.toOuterHtml()
        assertTrue(outerHtml.contains("background-color: rgba(17, 34, 51, 1.0) !important"))
        assertTrue(outerHtml.contains("4 12, crosshair"))

        // Null-clearing a style property removes it from WebDomElement.style and clears !important
        style.backgroundColor = null
        assertNull(dom.getStyle("background-color"))
        assertFalse(dom.isStyleImportant("background-color"))
    }

    @Test
    fun testThemeModeAutoDefaultAndStyleSheetLinkInjection() {
        val factory = WebNativeFactory()
        val appImpl = factory.createApplication() as WebApplicationImpl
        WebStyleSheetManager.clear(appImpl.nativeApplication)

        // Default themeMode is ThemeMode.AUTO
        assertEquals(ThemeMode.AUTO, appImpl.getThemeMode())

        SystemThemeDetector.fallbackSystemTheme = ThemeMode.LIGHT
        appImpl.setThemeMode(ThemeMode.AUTO)
        assertEquals("light", appImpl.appliedBootstrapTheme)

        SystemThemeDetector.fallbackSystemTheme = ThemeMode.DARK
        appImpl.setThemeMode(ThemeMode.AUTO)
        assertEquals("dark", appImpl.appliedBootstrapTheme)

        appImpl.setThemeMode(ThemeMode.LIGHT)
        assertEquals("light", appImpl.appliedBootstrapTheme)

        // Adding a stylesheet with .scss path normalizes to .css and mounts <link rel="stylesheet" href="...">
        val sheet = object : PrecompiledStyleSheet {
            override val name: String = "studio-theme"
            override val styleSheet: CssStyleSheet = CssStyleSheet.empty()
            override val rawCss: String = ""
            override val cssResourcePath: String = "/styles/studio-theme.scss"
        }
        appImpl.addStyleSheet(sheet)

        assertTrue(WebStyleSheetManager.getActiveStyleSheetHrefs().contains("styles/studio-theme.css"))
        val linkNode = appImpl.nativeApplication.children.firstOrNull {
            it.tagName == "link" && it.getAttribute("href") == "styles/studio-theme.css"
        }
        assertTrue(linkNode != null, "Expected <link rel=\"stylesheet\" href=\"styles/studio-theme.css\"> in root")
        assertEquals("stylesheet", linkNode.getAttribute("rel"))

        // Removing the stylesheet removes the <link> node
        appImpl.removeStyleSheet(sheet)
        assertFalse(WebStyleSheetManager.getActiveStyleSheetHrefs().contains("styles/studio-theme.css"))
        val removedLink = appImpl.nativeApplication.children.firstOrNull {
            it.tagName == "link" && it.getAttribute("href") == "styles/studio-theme.css"
        }
        assertNull(removedLink)
    }
}
