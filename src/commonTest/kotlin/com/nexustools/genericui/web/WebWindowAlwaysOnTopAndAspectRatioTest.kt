package com.nexustools.genericui.web

import com.nexustools.genericui.component.layout.Container
import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.component.shell.Window
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.core.NativeFactory
import com.nexustools.genericui.enums.window.WindowState
import com.nexustools.genericui.geometry.Size
import com.nexustools.genericui.window.WindowAspectRatioHelper
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebWindowAlwaysOnTopAndAspectRatioTest {

    private var savedActiveFactory: NativeFactory? = null
    private var savedActiveWindow: WebWindowImpl? = null
    private var savedActiveApplication: WebApplicationImpl? = null

    @BeforeTest
    fun saveState() {
        savedActiveFactory = GenericUI.activeFactory
        savedActiveWindow = WebWindowImpl.activeWindow
        savedActiveApplication = WebApplicationImpl.activeApplication
        Application.current?.dispose()
    }

    @AfterTest
    fun restoreState() {
        Application.current?.dispose()
        GenericUI.activeFactory = savedActiveFactory
        WebWindowImpl.activeWindow = savedActiveWindow
        WebApplicationImpl.activeApplication = savedActiveApplication
    }

    @Test
    fun webWindowImplElevatesAndRestoresZIndexAndSets16By9AspectRatioCss() {
        val factory = WebNativeFactory()
        val app = factory.createApplication() as WebApplicationImpl
        val win = factory.createWindow() as WebWindowImpl

        assertFalse(win.isAlwaysOnTop())
        assertEquals("false", win.nativeWidget.getAttribute("data-always-on-top"))

        win.bringToFront()
        val baselineZ = win.nativeWidget.style["z-index"]?.toIntOrNull() ?: 0
        assertTrue(baselineZ in 1 until 100000)

        win.setAlwaysOnTop(true)
        assertTrue(win.isAlwaysOnTop())
        assertEquals("true", win.nativeWidget.getAttribute("data-always-on-top"))
        val elevatedZ = win.nativeWidget.style["z-index"]?.toIntOrNull() ?: 0
        assertTrue(elevatedZ >= 100000, "Expected elevated z-index >= 100000, got $elevatedZ")

        win.bringToFront()
        val afterBringToFrontZ = win.nativeWidget.style["z-index"]?.toIntOrNull() ?: 0
        assertTrue(afterBringToFrontZ >= 100000, "bringToFront must preserve elevated z-index when alwaysOnTop=true")

        win.setAlwaysOnTop(false)
        assertFalse(win.isAlwaysOnTop())
        assertEquals("false", win.nativeWidget.getAttribute("data-always-on-top"))
        assertEquals(baselineZ.toString(), win.nativeWidget.style["z-index"])

        // 16:9 aspect ratio in WindowState.NORMAL
        win.setWindowSize(Size(1280, 800))
        val ratio16By9 = 16.0 / 9.0
        win.setAspectRatio(ratio16By9)
        assertEquals(ratio16By9, win.getAspectRatio() ?: 0.0, 1e-6)
        assertEquals(ratio16By9.toString(), win.nativeWidget.style["aspect-ratio"])
        assertEquals(Size(1280, 720), win.getWindowSize())

        win.setMinWindowSize(Size(640, 360))
        win.setWindowSize(Size(960, 900))
        assertEquals(Size(960, 540), win.getWindowSize())

        // Maximized clears style["aspect-ratio"] and allows unconstrained size
        win.setWindowState(WindowState.MAXIMIZED)
        assertNull(win.nativeWidget.style["aspect-ratio"])
        win.setWindowSize(Size(1920, 1200))
        assertEquals(Size(1920, 1200), win.getWindowSize())

        // Restoring NORMAL re-applies style["aspect-ratio"] and clamps
        win.setWindowState(WindowState.NORMAL)
        assertEquals(ratio16By9.toString(), win.nativeWidget.style["aspect-ratio"])
        assertTrue(WindowAspectRatioHelper.matchesAspectRatio(win.getWindowSize(), ratio16By9))

        // Fullscreen clears style["aspect-ratio"] and allows unconstrained size
        win.setFullscreen(true)
        assertTrue(win.isFullscreen())
        assertNull(win.nativeWidget.style["aspect-ratio"])
        win.setWindowSize(Size(2560, 1600))
        assertEquals(Size(2560, 1600), win.getWindowSize())

        // Exiting fullscreen restores style["aspect-ratio"] and clamps
        win.setFullscreen(false)
        assertFalse(win.isFullscreen())
        assertEquals(WindowState.NORMAL, win.getWindowState())
        assertEquals(ratio16By9.toString(), win.nativeWidget.style["aspect-ratio"])
        assertTrue(WindowAspectRatioHelper.matchesAspectRatio(win.getWindowSize(), ratio16By9))

        win.setAspectRatio(null)
        assertNull(win.getAspectRatio())
        assertNull(win.nativeWidget.style["aspect-ratio"])

        win.dispose()
        app.dispose()
    }

    @Test
    fun commonWindowIntegratesAlwaysOnTopAnd16By9WithWebWindowImpl() {
        val factory = WebNativeFactory()
        GenericUI.initialize(factory)
        val appImpl = factory.createApplication() as WebApplicationImpl

        val window = Window(title = "Web Ratio", initialSize = Size(1280, 800))
        window.addChild(Container())
        window.show(factory)

        val webImpl = window.nativeImpl as WebWindowImpl
        window.setAlwaysOnTop(true)
        assertTrue(window.isAlwaysOnTop)
        assertTrue(webImpl.isAlwaysOnTop())
        assertEquals("true", webImpl.nativeWidget.getAttribute("data-always-on-top"))
        assertTrue((webImpl.nativeWidget.style["z-index"]?.toIntOrNull() ?: 0) >= 100000)

        window.setAlwaysOnTop(false)
        assertFalse(window.isAlwaysOnTop)
        assertEquals("false", webImpl.nativeWidget.getAttribute("data-always-on-top"))
        assertTrue((webImpl.nativeWidget.style["z-index"]?.toIntOrNull() ?: 0) < 100000)

        val ratio16By9 = 16.0 / 9.0
        window.setAspectRatio(16, 9)
        assertEquals(Size(1280, 720), window.getWindowSize())
        assertEquals(ratio16By9.toString(), webImpl.nativeWidget.style["aspect-ratio"])

        window.setMinWindowSize(Size(640, 360))
        window.setWindowSize(Size(960, 900))
        assertEquals(Size(960, 540), window.getWindowSize())

        window.setWindowState(WindowState.MAXIMIZED)
        assertNull(webImpl.nativeWidget.style["aspect-ratio"])
        window.setWindowSize(Size(1920, 1200))
        assertEquals(Size(1920, 1200), window.getWindowSize())

        window.restore()
        assertEquals(WindowState.NORMAL, window.getWindowState())
        assertEquals(Size(960, 540), window.getWindowSize())
        assertEquals(ratio16By9.toString(), webImpl.nativeWidget.style["aspect-ratio"])

        window.setFullscreen(true)
        assertTrue(window.isFullscreen())
        assertNull(webImpl.nativeWidget.style["aspect-ratio"])
        window.setWindowSize(Size(2560, 1600))
        assertEquals(Size(2560, 1600), window.getWindowSize())

        window.setFullscreen(false)
        assertFalse(window.isFullscreen())
        assertEquals(WindowState.NORMAL, window.getWindowState())
        assertEquals(Size(960, 540), window.getWindowSize())
        assertEquals(ratio16By9.toString(), webImpl.nativeWidget.style["aspect-ratio"])

        window.dispose()
        appImpl.dispose()
    }
}
