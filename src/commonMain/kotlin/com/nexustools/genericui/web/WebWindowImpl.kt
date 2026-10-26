package com.nexustools.genericui.web

import com.nexustools.genericui.core.AbstractContainer
import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.enums.window.DisplayCutoutMode
import com.nexustools.genericui.enums.window.WindowState
import com.nexustools.genericui.geometry.Size
import com.nexustools.genericui.nativeimpl.shell.MenuBarNativeImpl
import com.nexustools.genericui.nativeimpl.shell.WindowNativeImpl
import com.nexustools.genericui.window.PipConfig
import com.nexustools.genericui.window.TitleBar
import com.nexustools.genericui.window.TitleCapabilities
import com.nexustools.genericui.window.WindowConfiguration

/**
 * Web implementation of [WindowNativeImpl].
 *
 * Implements the 4-state Web chrome rules:
 * - Neither `TitleBar` nor `MenuBar` (`titleBar == null && menuBar == null`): renders no navbar/titlebar chrome at all.
 * - `MenuBar` only (`titleBar == null && menuBar != null`): renders only the menubar (no titlebar navbar).
 * - `TitleBar` only (`titleBar != null && menuBar == null`): renders a Bootstrap navbar with the titlebar's icon,
 *   title, and leading/center/trailing components (detached from any body parent so they appear only once).
 * - `TitleBar` + `MenuBar` (`titleBar != null && menuBar != null`): renders a Bootstrap navbar with the title, icon,
 *   and leading/center components on the left and the `MenuBar` + trailing components on the right.
 */
class WebWindowImpl : WebBaseContainerNativeImpl("genericui:window", "genericui-window"), WindowNativeImpl<WebDomElement> {
    override val supportsCustomTitleBar: Boolean = true
    override val supportsEmbeddedMenuBarInTitleBar: Boolean = false
    override val supportsMultiWindow: Boolean = false
    override val supportsNavbarMenuBar: Boolean = true

    override fun getTitleCapabilities(): TitleCapabilities = TitleCapabilities(
        isSupported = true,
        isEmulated = true,
        supportsCustomTitleBar = true,
        supportsCustomComponents = true,
        supportsLeadingComponents = true,
        supportsCenterComponents = true,
        supportsTrailingComponents = true,
        supportsEmbeddedMenuBar = true,
        supportsNavbarMenuBar = true,
        supportsWindowIcon = true,
        supportsWindowTitle = true,
        supportsMinimizeButton = false,
        supportsMaximizeButton = false,
        supportsCloseButton = false,
        supportsCollapseToggle = false,
        supportsControlsPlacement = false,
        supportsCustomHeight = true,
        supportsSystemAeroOrBackdrop = false,
        isToolWindowTitleBar = false,
        isTitleBarRequired = false,
        titleBarRendersAsNavbar = true,
        menuBarRendersAsNavbar = false,
        menuBarMergesIntoTitleNavbar = true
    )

    private var titleText: String = "GenericUI"
    private var windowSize: Size = Size(
        WebBrowserDomBridge.getViewportWidth(1280),
        WebBrowserDomBridge.getViewportHeight(800)
    )
    private var windowState: WindowState = WindowState.NORMAL
    private var fullscreenFlag: Boolean = false
    private var pipActive: Boolean = false

    private var titleBarInstructions: TitleBar? = null
    private var menuBarImpl: MenuBarNativeImpl<*>? = null
    private var explicitNavBarImpl: NativeImpl<*>? = null

    var synthesizedTitleNavbar: WebNavbarImpl? = null
        private set

    val activeNavbar: WebNavbarImpl?
        get() = (explicitNavBarImpl as? WebNavbarImpl) ?: synthesizedTitleNavbar

    private val menuSlot = WebDomElement("header").apply { classList.add("genericui-window-menubar-slot") }
    private val navSlot = WebDomElement("nav").apply { classList.add("genericui-window-navbar-slot") }
    private val contentSlot = WebDomElement("main").apply {
        classList.add("genericui-window-content-slot")
        style["width"] = "100%"
        style["height"] = "100%"
        style["position"] = "relative"
    }
    private val bottomNavSlot = WebDomElement("footer").apply { classList.add("genericui-window-bottomnav-slot") }

    private var onResizeListener: ((WindowConfiguration) -> Unit)? = null
    private var onFocusListener: ((Boolean) -> Unit)? = null
    private var onCloseRequestedListener: (() -> Boolean)? = null

    private var ownerWindowRef: WebWindowImpl? = null
    private var alwaysOnTopOfOwnerFlag: Boolean = false
    internal val attachedToolWindows = mutableSetOf<WebWindowImpl>()
    internal val attachedDockWindows = mutableSetOf<WebDockWindowImpl>()

    init {
        if (activeWindow == null) {
            activeWindow = this
        }
        nativeWidget.style["width"] = "100%"
        nativeWidget.style["height"] = "100%"
        nativeWidget.style["position"] = "relative"
        nativeWidget.appendChild(contentSlot)
        nativeWidget.addEventListener("pointerdown") {
            bringToFront()
        }
        WebBrowserDomBridge.registerWindowResize { w, h ->
            windowSize = Size(w, h)
            nativeWidget.clientWidth = w
            nativeWidget.clientHeight = h
            onResizeListener?.invoke(WindowConfiguration(widthPx = w, heightPx = h))
        }
    }

    internal fun notifyPostLayoutResize() {
        val vw = WebBrowserDomBridge.getViewportWidth(windowSize.width)
        val vh = WebBrowserDomBridge.getViewportHeight(windowSize.height)
        windowSize = Size(vw, vh)
        nativeWidget.clientWidth = vw
        nativeWidget.clientHeight = vh
        onResizeListener?.invoke(WindowConfiguration(widthPx = vw, heightPx = vh))
    }

    companion object {
        internal var activeWindow: WebWindowImpl? = null
        private var layoutRefreshPending: Boolean = false
        private var nextWindowZIndex: Int = 1000

        internal fun allocateNextZIndex(): Int {
            nextWindowZIndex += 1
            return nextWindowZIndex
        }

        internal fun scheduleLayoutRefresh() {
            if (!WebBrowserDomBridge.isBrowserEnvironment || layoutRefreshPending) return
            layoutRefreshPending = true
            WebBrowserDomBridge.schedulePostLayoutPass {
                layoutRefreshPending = false
                activeWindow?.notifyPostLayoutResize()
            }
        }
    }

    override fun addChildNative(childImpl: NativeImpl<*>, index: Int) {
        val dom = childImpl.nativeWidget as? WebDomElement ?: return
        contentSlot.insertChildAt(index, dom)
        scheduleLayoutRefresh()
    }

    override fun removeChildNative(childImpl: NativeImpl<*>) {
        val dom = childImpl.nativeWidget as? WebDomElement ?: return
        contentSlot.removeChild(dom)
    }

    override fun clearChildrenNative() {
        contentSlot.clearChildren()
    }

    override fun setTitle(title: String) {
        titleText = title
        nativeWidget.setAttribute("data-window-title", title)
        WebBrowserDomBridge.setDocumentTitle(title)
        if (titleBarInstructions != null) {
            rebuildWebWindowChrome()
        }
    }

    override fun getTitle(): String = titleText

    override fun setWindowSize(size: Size) {
        windowSize = size
        nativeWidget.clientWidth = size.width
        nativeWidget.clientHeight = size.height
        onResizeListener?.invoke(WindowConfiguration(widthPx = size.width, heightPx = size.height))
    }

    override fun getWindowSize(): Size = windowSize

    override fun setWindowState(state: WindowState) {
        windowState = state
    }

    override fun getWindowState(): WindowState = windowState

    override fun setFullscreen(fullscreen: Boolean, cutoutMode: DisplayCutoutMode) {
        fullscreenFlag = fullscreen
        if (fullscreen) {
            nativeWidget.classList.add("fullscreen")
        } else {
            nativeWidget.classList.remove("fullscreen")
        }
        rebuildWebWindowChrome()
    }

    override fun isFullscreen(): Boolean = fullscreenFlag

    override fun applyTitleBarInstructions(titleBar: TitleBar?) {
        if (this.titleBarInstructions != titleBar) {
            this.titleBarInstructions?.setHostEmbedsComponents(false)
        }
        titleBarInstructions = titleBar
        titleBar?.setHostEmbedsComponents(true)
        rebuildWebWindowChrome()
    }

    fun getTitleBarInstructions(): TitleBar? = titleBarInstructions

    override fun setMenuBarNative(menuBarImpl: MenuBarNativeImpl<*>?) {
        this.menuBarImpl = menuBarImpl
        rebuildWebWindowChrome()
    }

    override fun setNavBarNative(navBarImpl: NativeImpl<*>?) {
        explicitNavBarImpl = navBarImpl
        navSlot.clearChildren()
        val dom = navBarImpl?.nativeWidget as? WebDomElement
        if (dom != null) {
            if (navSlot.parent == null) {
                val contentIdx = nativeWidget.children.indexOf(contentSlot).coerceAtLeast(0)
                nativeWidget.insertChildAt(contentIdx, navSlot)
            }
            navSlot.appendChild(dom)
        } else if (titleBarInstructions == null) {
            navSlot.remove()
        }
    }

    override fun setBottomNavigationBarNative(bottomNavImpl: NativeImpl<*>?) {
        bottomNavSlot.clearChildren()
        val dom = bottomNavImpl?.nativeWidget as? WebDomElement
        if (dom != null) {
            if (bottomNavSlot.parent == null) {
                nativeWidget.appendChild(bottomNavSlot)
            }
            bottomNavSlot.appendChild(dom)
        } else {
            bottomNavSlot.remove()
        }
    }

    fun rebuildWebWindowChrome() {
        val tb = titleBarInstructions
        val mb = menuBarImpl
        val activeApp = WebApplicationImpl.activeApplication

        if (fullscreenFlag || (tb == null && mb == null)) {
            synthesizedTitleNavbar = null
            menuSlot.clearChildren()
            menuSlot.remove()
            if (explicitNavBarImpl == null) {
                navSlot.clearChildren()
                navSlot.remove()
            }
            activeApp?.attachTopMenuBar(null)
            return
        }

        if (tb == null && mb != null) {
            // MenuBar only -> render only the menubar (no titlebar navbar)
            synthesizedTitleNavbar = null
            menuSlot.clearChildren()
            if (explicitNavBarImpl == null) {
                navSlot.clearChildren()
                navSlot.remove()
            }
            val mbDom = mb.nativeWidget as? WebDomElement
            if (activeApp != null) {
                menuSlot.remove()
                activeApp.attachTopMenuBar(mbDom)
            } else if (mbDom != null) {
                if (menuSlot.parent == null) {
                    nativeWidget.insertChildAt(0, menuSlot)
                }
                menuSlot.appendChild(mbDom)
            }
            return
        }

        // TitleBar only (tb != null && mb == null) OR TitleBar + MenuBar (tb != null && mb != null)
        val nonNullTb = tb!!
        nonNullTb.setHostEmbedsComponents(true)
        val navbar = synthesizedTitleNavbar ?: WebNavbarImpl().also { synthesizedTitleNavbar = it }
        val navDom = navbar.nativeWidget
        navDom.clearChildren()
        navDom.classList.addAll(
            listOf(
                "navbar",
                "navbar-expand",
                "d-flex",
                "align-items-center",
                "genericui-titlebar-navbar"
            )
        )

        val effectiveTitle = if (nonNullTb.showWindowTitle) titleText else ""
        navbar.setBrandTitle(effectiveTitle)
        if (nonNullTb.heightPx != null) {
            navDom.style["min-height"] = "${nonNullTb.heightPx}px"
        } else {
            navDom.style.remove("min-height")
        }

        val hasIcon = nonNullTb.showWindowIcon && !nonNullTb.iconPath.isNullOrBlank()
        if (hasIcon) {
            navDom.setAttribute("data-brand-icon", nonNullTb.iconPath!!)
        } else {
            navDom.removeAttribute("data-brand-icon")
        }

        if (hasIcon || effectiveTitle.isNotEmpty()) {
            val brandEl = WebDomElement("span").apply {
                classList.addAll(listOf("navbar-brand", "d-flex", "align-items-center", "me-2"))
                setAttribute("data-navbar-placement", "left")
            }
            if (hasIcon) {
                val iconEl = WebDomElement("img").apply {
                    classList.addAll(listOf("navbar-brand-icon", "me-2"))
                    setAttribute("src", nonNullTb.iconPath!!)
                    setAttribute("alt", "")
                }
                brandEl.appendChild(iconEl)
            }
            if (effectiveTitle.isNotEmpty()) {
                brandEl.textContent = effectiveTitle
            }
            navDom.appendChild(brandEl)
        }

        fun mountTitleBarComponent(comp: com.nexustools.genericui.core.AbstractComponent, placement: String, extraClasses: List<String>) {
            (comp.parent as? AbstractContainer)?.removeChild(comp)
            comp.mountedInTitleBar = true
            val compImpl = comp.nativeImpl ?: if (comp.isVisible) comp.ensureNativeImpl() else null
            if (compImpl != null) {
                (compImpl.nativeWidget as? WebDomElement)?.remove()
                for (cls in extraClasses) {
                    compImpl.addClass(cls)
                }
                compImpl.setAttribute("data-mounted-in-titlebar", "true")
                compImpl.setAttribute("data-navbar-placement", placement)
                navbar.addChildNative(compImpl, -1)
            }
        }

        for (comp in nonNullTb.leadingComponents) {
            mountTitleBarComponent(comp, "left", listOf("navbar-leading-item", "me-2"))
        }
        for (comp in nonNullTb.centerComponents) {
            mountTitleBarComponent(comp, "center", listOf("navbar-center-item", "mx-auto"))
        }

        if (mb != null) {
            val mbDom = mb.nativeWidget as? WebDomElement
            if (mbDom != null) {
                mbDom.remove()
                mbDom.classList.addAll(listOf("ms-auto", "navbar-embedded-menubar"))
                mbDom.setAttribute("data-navbar-placement", "right")
                navDom.appendChild(mbDom)
                navDom.setAttribute("data-has-embedded-menubar", "true")
            }
        } else {
            navDom.removeAttribute("data-has-embedded-menubar")
        }

        for ((idx, comp) in nonNullTb.trailingComponents.withIndex()) {
            val alignClass = if (mb == null && idx == 0) "ms-auto" else "ms-2"
            mountTitleBarComponent(comp, "right", listOf("navbar-trailing-item", alignClass))
        }

        menuSlot.clearChildren()
        menuSlot.remove()
        if (activeApp != null) {
            activeApp.attachTopMenuBar(navDom)
        } else {
            if (navSlot.parent == null) {
                nativeWidget.insertChildAt(0, navSlot)
            }
            navSlot.clearChildren()
            navSlot.appendChild(navDom)
        }
    }

    override fun enterPictureInPicture(config: PipConfig): Boolean {
        pipActive = true
        return true
    }

    override fun exitPictureInPicture() {
        pipActive = false
    }

    override fun isInPictureInPicture(): Boolean = pipActive

    override fun showWindow() {
        setVisible(true)
        val appRoot = WebApplicationImpl.activeApplication?.nativeApplication
        if (appRoot != null && nativeWidget.parent == null) {
            appRoot.appendChild(nativeWidget)
        }
        if (titleBarInstructions != null || menuBarImpl != null) {
            rebuildWebWindowChrome()
        }
        val vw = WebBrowserDomBridge.getViewportWidth(windowSize.width)
        val vh = WebBrowserDomBridge.getViewportHeight(windowSize.height)
        if (nativeWidget.getAttribute("data-tool-modal") != "true") {
            windowSize = Size(vw, vh)
            nativeWidget.clientWidth = vw
            nativeWidget.clientHeight = vh
            onResizeListener?.invoke(WindowConfiguration(widthPx = vw, heightPx = vh))
        }
        bringToFront()
        onFocusListener?.invoke(true)
        scheduleLayoutRefresh()
    }

    override fun closeWindow() {
        val allow = onCloseRequestedListener?.invoke() ?: true
        if (allow) {
            setVisible(false)
        }
    }

    override fun configureAsToolModalWindow(
        ownerWindow: WindowNativeImpl<*>?,
        showInTaskbar: Boolean,
        minimizable: Boolean,
        alwaysOnTopOfOwner: Boolean
    ) {
        ownerWindowRef?.attachedToolWindows?.remove(this)
        val webOwner = ownerWindow as? WebWindowImpl
        ownerWindowRef = webOwner
        alwaysOnTopOfOwnerFlag = alwaysOnTopOfOwner
        nativeWidget.setAttribute("data-tool-modal", "true")
        nativeWidget.setAttribute("data-taskbar-entry", showInTaskbar.toString())
        nativeWidget.setAttribute("data-minimizable", minimizable.toString())
        nativeWidget.setAttribute("data-always-on-top-of-owner", alwaysOnTopOfOwner.toString())
        if (webOwner != null) {
            webOwner.attachedToolWindows.add(this)
            val ownerId = webOwner.nativeWidget.id.ifEmpty { "main-window" }
            nativeWidget.setAttribute("data-owner-window", ownerId)
        }
        if (alwaysOnTopOfOwner) {
            nativeWidget.style["z-index"] = allocateNextZIndex().toString()
        }
    }

    override fun bringToFront() {
        val myZ = allocateNextZIndex()
        nativeWidget.style["z-index"] = myZ.toString()
        for (toolWin in attachedToolWindows) {
            if (toolWin.alwaysOnTopOfOwnerFlag) {
                toolWin.nativeWidget.style["z-index"] = allocateNextZIndex().toString()
            }
        }
        for (dockWin in attachedDockWindows) {
            if (dockWin.isAlwaysOnTopOfOwner) {
                dockWin.bringToFront()
            }
        }
    }

    override fun dispose() {
        ownerWindowRef?.attachedToolWindows?.remove(this)
        ownerWindowRef = null
        attachedToolWindows.clear()
        attachedDockWindows.clear()
        super.dispose()
    }

    override fun setOnWindowResize(listener: ((WindowConfiguration) -> Unit)?) {
        onResizeListener = listener
    }

    override fun setOnWindowFocusChanged(listener: ((Boolean) -> Unit)?) {
        onFocusListener = listener
    }

    override fun setOnWindowCloseRequested(listener: (() -> Boolean)?) {
        onCloseRequestedListener = listener
    }
}
