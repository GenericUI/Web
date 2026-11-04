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
 * Implements the 4-state Web chrome rules for the main window:
 * - Neither `TitleBar` nor `MenuBar` (`titleBar == null && menuBar == null`): renders no navbar/titlebar chrome at all.
 * - `MenuBar` only (`titleBar == null && menuBar != null`): renders only the menubar (no titlebar navbar).
 * - `TitleBar` only (`titleBar != null && menuBar == null`): renders a Bootstrap navbar with the titlebar's icon,
 *   title, and leading/center/trailing components (detached from any body parent so they appear only once).
 * - `TitleBar` + `MenuBar` (`titleBar != null && menuBar != null`): renders a Bootstrap navbar with the title, icon,
 *   and leading/center components on the left and the `MenuBar` + trailing components on the right.
 *
 * All non-main windows (`data-tool-modal="true"` or secondary windows opened while a primary window is active)
 * render as centered Bootstrap `.modal` overlays with `.modal-dialog.modal-dialog-centered.modal-dialog-scrollable`
 * -> `.modal-content` -> `.modal-header` + `.modal-body`, semi-transparent black backdrop, and focus trapping
 * when blocking focus to the main app.
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
    private var fullscreenElementImpl: NativeImpl<*>? = null
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

    // Bootstrap modal wrapper elements used when this window is a non-main / tool-modal window
    private var isNonMainModalWindow: Boolean = false
    val modalDialog = WebDomElement("div").apply {
        classList.addAll(listOf("modal-dialog", "modal-dialog-centered", "modal-dialog-scrollable"))
        style["width"] = "100%"
        style["max-width"] = "min(720px, calc(100vw - 1rem))"
        style["margin"] = "auto"
        style["pointer-events"] = "auto"
    }
    val modalContent = WebDomElement("div").apply {
        classList.addAll(listOf("modal-content", "shadow-lg"))
        style["width"] = "100%"
        style["max-height"] = "calc(100dvh - 1rem)"
        style["overflow"] = "hidden"
        style["display"] = "flex"
        style["flex-direction"] = "column"
    }
    val modalHeader = WebDomElement("div").apply {
        classList.addAll(listOf("modal-header", "py-2", "px-3"))
        style["flex-shrink"] = "0"
    }
    val modalTitleElement = WebDomElement("h5").apply {
        classList.addAll(listOf("modal-title", "fs-6", "fw-semibold", "mb-0"))
    }
    val modalCloseButton = WebDomElement("button").apply {
        setAttribute("type", "button")
        classList.add("btn-close")
        setAttribute("data-bs-dismiss", "modal")
        setAttribute("aria-label", "Close")
    }
    val modalBody = WebDomElement("div").apply {
        classList.add("modal-body")
        style["overflow-y"] = "auto"
        style["overflow-x"] = "hidden"
        style["flex"] = "1 1 auto"
        style["min-height"] = "0"
    }

    private var onResizeListener: ((WindowConfiguration) -> Unit)? = null
    private var lastNotifiedWidthPx: Int = -1
    private var lastNotifiedHeightPx: Int = -1
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
        modalCloseButton.addEventListener("click") {
            closeWindow()
        }
        nativeWidget.addEventListener("pointerdown") {
            bringToFront()
        }
        WebBrowserDomBridge.registerWindowResize { w, h ->
            if (!isNonMainModalWindow) {
                windowSize = Size(w, h)
                nativeWidget.clientWidth = w
                nativeWidget.clientHeight = h
                if (w != lastNotifiedWidthPx || h != lastNotifiedHeightPx) {
                    lastNotifiedWidthPx = w
                    lastNotifiedHeightPx = h
                    onResizeListener?.invoke(WindowConfiguration(widthPx = w, heightPx = h))
                }
            }
        }
        WebBrowserDomBridge.registerWindowKeyEvents(
            onKeyDown = { ke ->
                activeWindow?.nativeWidget?.dispatchFromBrowser("keydown", ke)
            },
            onKeyUp = { ke ->
                activeWindow?.nativeWidget?.dispatchFromBrowser("keyup", ke)
            }
        )
    }

    private fun ensureNonMainBootstrapModalStructure() {
        if (!isNonMainModalWindow) {
            isNonMainModalWindow = true
            nativeWidget.classList.addAll(listOf("modal", "fade", "show"))
            nativeWidget.setAttribute("tabindex", "-1")
            nativeWidget.setAttribute("role", "dialog")
            nativeWidget.setAttribute("aria-modal", alwaysOnTopOfOwnerFlag.toString())

            contentSlot.remove()
            modalTitleElement.textContent = titleText
            if (modalTitleElement.parent == null) modalHeader.appendChild(modalTitleElement)
            if (modalCloseButton.parent == null) modalHeader.appendChild(modalCloseButton)
            if (modalHeader.parent == null) modalContent.appendChild(modalHeader)
            if (contentSlot.parent == null) modalBody.appendChild(contentSlot)
            if (modalBody.parent == null) modalContent.appendChild(modalBody)
            if (modalDialog.parent == null) modalWidgetAttach()
        }
        val w = windowSize.width.coerceAtLeast(320)
        val h = windowSize.height.coerceAtLeast(220)
        modalDialog.style["max-width"] = "min(${w}px, calc(100vw - 1rem))"
        modalContent.style["max-height"] = "min(${h}px, calc(100dvh - 1rem))"

        nativeWidget.setStyle("position", "fixed", true)
        nativeWidget.setStyle("inset", "0", true)
        nativeWidget.setStyle("top", "0", true)
        nativeWidget.setStyle("left", "0", true)
        nativeWidget.setStyle("width", "100vw", true)
        nativeWidget.setStyle("height", "100dvh", true)
        nativeWidget.setStyle("margin", "0", true)
        nativeWidget.setStyle("padding", "0.5rem", true)
        nativeWidget.setStyle("box-sizing", "border-box", true)
        nativeWidget.setStyle("display", "flex", true)
        nativeWidget.setStyle("align-items", "center", true)
        nativeWidget.setStyle("justify-content", "center", true)
        if (alwaysOnTopOfOwnerFlag) {
            nativeWidget.setStyle("background-color", "rgba(0, 0, 0, 0.55)", true)
            nativeWidget.setStyle("pointer-events", "auto", true)
        } else {
            nativeWidget.setStyle("background-color", "rgba(0, 0, 0, 0.35)", true)
            nativeWidget.setStyle("pointer-events", "auto", true)
        }
    }

    private fun modalWidgetAttach() {
        nativeWidget.appendChild(modalDialog)
    }

    internal fun notifyPostLayoutResize() {
        if (isNonMainModalWindow) return
        val vw = WebBrowserDomBridge.getViewportWidth(windowSize.width)
        val vh = WebBrowserDomBridge.getViewportHeight(windowSize.height)
        windowSize = Size(vw, vh)
        nativeWidget.clientWidth = vw
        nativeWidget.clientHeight = vh
        if (vw != lastNotifiedWidthPx || vh != lastNotifiedHeightPx) {
            lastNotifiedWidthPx = vw
            lastNotifiedHeightPx = vh
            onResizeListener?.invoke(WindowConfiguration(widthPx = vw, heightPx = vh))
        }
    }

    companion object {
        internal var activeWindow: WebWindowImpl? = null
        private var layoutRefreshPending: Boolean = false
        private var nextWindowZIndex: Int = 1000
        private var nextModalZIndex: Int = 2200

        internal fun allocateNextZIndex(): Int {
            nextWindowZIndex += 1
            return nextWindowZIndex
        }

        internal fun allocateNextModalZIndex(): Int {
            nextModalZIndex += 2
            return nextModalZIndex
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
        modalTitleElement.textContent = title
        if (!isNonMainModalWindow) {
            WebBrowserDomBridge.setDocumentTitle(title)
            if (titleBarInstructions != null) {
                rebuildWebWindowChrome()
            }
        }
    }

    override fun getTitle(): String = titleText

    override fun setWindowSize(size: Size) {
        val isModalWin = isNonMainModalWindow || nativeWidget.getAttribute("data-tool-modal") == "true"
        val resolvedSize = if (WebBrowserDomBridge.isBrowserEnvironment && !isModalWin) {
            Size(
                WebBrowserDomBridge.getViewportWidth(size.width),
                WebBrowserDomBridge.getViewportHeight(size.height)
            )
        } else {
            size
        }
        windowSize = resolvedSize
        nativeWidget.clientWidth = resolvedSize.width
        nativeWidget.clientHeight = resolvedSize.height
        if (isModalWin) {
            ensureNonMainBootstrapModalStructure()
        }
        val listener = onResizeListener
        if (listener != null) {
            lastNotifiedWidthPx = resolvedSize.width
            lastNotifiedHeightPx = resolvedSize.height
            listener.invoke(WindowConfiguration(widthPx = resolvedSize.width, heightPx = resolvedSize.height))
        }
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
            (fullscreenElementImpl?.nativeWidget as? WebDomElement)?.classList?.remove("genericui-fullscreen-element")
            fullscreenElementImpl = null
        }
        rebuildWebWindowChrome()
    }

    override fun isFullscreen(): Boolean = fullscreenFlag

    override fun setFullscreenElement(elementImpl: NativeImpl<*>?, cutoutMode: DisplayCutoutMode) {
        (fullscreenElementImpl?.nativeWidget as? WebDomElement)?.classList?.remove("genericui-fullscreen-element")
        fullscreenElementImpl = elementImpl
        val dom = elementImpl?.nativeWidget as? WebDomElement
        if (dom != null) {
            dom.classList.add("genericui-fullscreen-element")
            WebBrowserDomBridge.requestElementFullscreen(dom)
            setFullscreen(true, cutoutMode)
        } else {
            WebBrowserDomBridge.exitFullscreen()
            setFullscreen(false, cutoutMode)
        }
    }

    override fun getFullscreenElement(): NativeImpl<*>? = fullscreenElementImpl

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
        if (isNonMainModalWindow) return
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
        val isSecondary = nativeWidget.getAttribute("data-tool-modal") == "true" ||
            (activeWindow != null && activeWindow !== this && activeWindow?.nativeWidget?.parent != null)
        if (isSecondary) {
            ensureNonMainBootstrapModalStructure()
        }
        setVisible(true)
        if (isNonMainModalWindow) {
            nativeWidget.setStyle("display", "flex", true)
        }
        val appRoot = WebApplicationImpl.activeApplication?.nativeApplication
        if (appRoot != null && nativeWidget.parent == null) {
            appRoot.appendChild(nativeWidget)
        }
        if (!isNonMainModalWindow && (titleBarInstructions != null || menuBarImpl != null)) {
            rebuildWebWindowChrome()
        }
        val vw = WebBrowserDomBridge.getViewportWidth(windowSize.width)
        val vh = WebBrowserDomBridge.getViewportHeight(windowSize.height)
        if (!isNonMainModalWindow) {
            windowSize = Size(vw, vh)
            nativeWidget.clientWidth = vw
            nativeWidget.clientHeight = vh
            lastNotifiedWidthPx = vw
            lastNotifiedHeightPx = vh
            onResizeListener?.invoke(WindowConfiguration(widthPx = vw, heightPx = vh))
        } else if (alwaysOnTopOfOwnerFlag) {
            WebBrowserDomBridge.installModalFocusTrap(nativeWidget)
        }
        bringToFront()
        onFocusListener?.invoke(true)
        scheduleLayoutRefresh()
    }

    override fun closeWindow() {
        val allow = onCloseRequestedListener?.invoke() ?: true
        if (allow) {
            WebBrowserDomBridge.releaseModalFocusTrap(nativeWidget)
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
        ensureNonMainBootstrapModalStructure()
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
        WebBrowserDomBridge.releaseModalFocusTrap(nativeWidget)
        ownerWindowRef?.attachedToolWindows?.remove(this)
        ownerWindowRef = null
        attachedToolWindows.clear()
        attachedDockWindows.clear()
        super.dispose()
    }

    override fun setOnWindowResize(listener: ((WindowConfiguration) -> Unit)?) {
        onResizeListener = listener
    }

    override fun setOnWindowFocusChanged(listener: ((Boolean) -> Unit)? ) {
        onFocusListener = listener
    }

    override fun setOnWindowCloseRequested(listener: (() -> Boolean)?) {
        onCloseRequestedListener = listener
    }
}
