package com.nexustools.genericui.web

import com.nexustools.genericui.core.AbstractContainer
import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.enums.window.DisplayCutoutMode
import com.nexustools.genericui.enums.window.WindowState
import com.nexustools.genericui.event.KeyEvent
import com.nexustools.genericui.geometry.Size
import com.nexustools.genericui.nativeimpl.shell.MenuBarNativeImpl
import com.nexustools.genericui.nativeimpl.shell.WindowNativeImpl
import com.nexustools.genericui.window.PipConfig
import com.nexustools.genericui.window.TitleBar
import com.nexustools.genericui.window.TitleCapabilities
import com.nexustools.genericui.window.WindowAspectRatioHelper
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
    private var minWindowSize: Size? = null
    private var windowState: WindowState = WindowState.NORMAL
    private var stateBeforeFullscreen: WindowState = WindowState.NORMAL
    private var fullscreenFlag: Boolean = false
    private var alwaysOnTopFlag: Boolean = false
    private var aspectRatioValue: Double? = null
    private var baselineZIndex: String? = null
    private var hasBeenBroughtToFront: Boolean = false
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
        nativeWidget.setAttribute("data-always-on-top", "false")
        nativeWidget.appendChild(contentSlot)
        modalCloseButton.addEventListener("click") {
            closeWindow()
        }
        nativeWidget.addEventListener("pointerdown") {
            bringToFront()
        }
        WebBrowserDomBridge.registerWindowResize { w, h ->
            if (!isNonMainModalWindow) {
                val resolved = clampSizeForState(Size(w, h))
                windowSize = resolved
                nativeWidget.clientWidth = resolved.width
                nativeWidget.clientHeight = resolved.height
                if (resolved.width != lastNotifiedWidthPx || resolved.height != lastNotifiedHeightPx) {
                    lastNotifiedWidthPx = resolved.width
                    lastNotifiedHeightPx = resolved.height
                    onResizeListener?.invoke(currentWindowConfiguration(resolved.width, resolved.height))
                }
            }
        }
        WebBrowserDomBridge.registerFullscreenChange { isBrowserFullscreen ->
            if (!isBrowserFullscreen && fullscreenFlag) {
                setFullscreen(false)
                onResizeListener?.invoke(currentWindowConfiguration(windowSize.width, windowSize.height))
            }
        }
        WebBrowserDomBridge.registerWindowKeyEvents(
            onKeyDown = { ke ->
                val root = activeWindow?.nativeWidget ?: return@registerWindowKeyEvents
                root.dispatchFromBrowser("keydown", ke)
                dispatchKeyEventToDescendants(root, "keydown", ke, isKeyDown = true)
            },
            onKeyUp = { ke ->
                val root = activeWindow?.nativeWidget ?: return@registerWindowKeyEvents
                root.dispatchFromBrowser("keyup", ke)
                dispatchKeyEventToDescendants(root, "keyup", ke, isKeyDown = false)
            }
        )
    }

    private fun dispatchKeyEventToDescendants(
        root: WebDomElement,
        type: String,
        event: KeyEvent,
        isKeyDown: Boolean
    ) {
        val stack = ArrayList<WebDomElement>(root.children.size)
        for (i in root.children.lastIndex downTo 0) {
            stack.add(root.children[i])
        }
        while (stack.isNotEmpty()) {
            val el = stack.removeAt(stack.lastIndex)
            val impl = el.ownerNativeImpl as? WebBaseNativeImpl
            if (impl != null && (!impl.isVisible() || !impl.isEnabled())) continue
            if (el.style["display"] == "none" || el.attributes["disabled"] != null) continue
            val hasListener = if (impl != null) {
                if (isKeyDown) impl.hasKeyDownListener() else impl.hasKeyUpListener()
            } else {
                el.eventListeners[type]?.isNotEmpty() == true
            }
            if (hasListener) {
                el.dispatchFromBrowser(type, event)
            }
            for (i in el.children.lastIndex downTo 0) {
                stack.add(el.children[i])
            }
        }
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
            nativeWidget.addEventListener("click") { ev ->
                val targetDom = ev as? WebDomElement
                val clickedOutsideDialog = targetDom == null ||
                    targetDom === nativeWidget ||
                    !modalDialog.containsDomElement(targetDom)
                if (isNonMainModalWindow && alwaysOnTopOfOwnerFlag && clickedOutsideDialog) {
                    closeWindow()
                }
            }
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
            nativeWidget.setStyle("-webkit-backdrop-filter", "blur(6px)", true)
            nativeWidget.setStyle("backdrop-filter", "blur(6px)", true)
            nativeWidget.setStyle("pointer-events", "auto", true)
        } else {
            nativeWidget.setStyle("background-color", "rgba(0, 0, 0, 0.35)", true)
            nativeWidget.setStyle("-webkit-backdrop-filter", "blur(6px)", true)
            nativeWidget.setStyle("backdrop-filter", "blur(6px)", true)
            nativeWidget.setStyle("pointer-events", "auto", true)
        }
        syncAspectRatioStyle()
    }

    private fun modalWidgetAttach() {
        nativeWidget.appendChild(modalDialog)
    }

    private fun currentWindowConfiguration(widthPx: Int, heightPx: Int): WindowConfiguration =
        WindowConfiguration(
            widthPx = widthPx,
            heightPx = heightPx,
            windowState = if (fullscreenFlag) WindowState.FULLSCREEN else windowState,
            isFullscreen = fullscreenFlag,
            isInPictureInPicture = pipActive
        )

    internal fun notifyPostLayoutResize() {
        if (isNonMainModalWindow) return
        val vw = WebBrowserDomBridge.getViewportWidth(windowSize.width)
        val vh = WebBrowserDomBridge.getViewportHeight(windowSize.height)
        val resolved = clampSizeForState(Size(vw, vh))
        windowSize = resolved
        nativeWidget.clientWidth = resolved.width
        nativeWidget.clientHeight = resolved.height
        if (resolved.width != lastNotifiedWidthPx || resolved.height != lastNotifiedHeightPx) {
            lastNotifiedWidthPx = resolved.width
            lastNotifiedHeightPx = resolved.height
            onResizeListener?.invoke(currentWindowConfiguration(resolved.width, resolved.height))
        }
    }

    companion object {
        private const val ALWAYS_ON_TOP_BASE_Z_INDEX: Int = 100000
        internal var activeWindow: WebWindowImpl? = null
        private var layoutRefreshPending: Boolean = false
        private var nextWindowZIndex: Int = 1000
        private var nextModalZIndex: Int = 2200
        private var nextAlwaysOnTopZIndex: Int = ALWAYS_ON_TOP_BASE_Z_INDEX

        internal fun allocateNextZIndex(): Int {
            nextWindowZIndex += 1
            return nextWindowZIndex
        }

        internal fun allocateAlwaysOnTopZIndex(): Int {
            nextAlwaysOnTopZIndex = maxOf(
                nextAlwaysOnTopZIndex + 1,
                allocateNextZIndex(),
                ALWAYS_ON_TOP_BASE_Z_INDEX
            )
            return nextAlwaysOnTopZIndex
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
        contentSlot.insertChildAt(index, dom.resolveMountElement())
        scheduleLayoutRefresh()
    }

    override fun removeChildNative(childImpl: NativeImpl<*>) {
        val dom = childImpl.nativeWidget as? WebDomElement ?: return
        val mountDom = dom.resolveMountElement()
        if (!contentSlot.removeChild(mountDom)) {
            contentSlot.removeChild(dom)
        }
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

    private fun isWindowedNormal(): Boolean =
        !fullscreenFlag && windowState == WindowState.NORMAL

    private fun syncAspectRatioStyle() {
        val ratio = aspectRatioValue
        if (ratio != null && isWindowedNormal()) {
            nativeWidget.style["aspect-ratio"] = ratio.toString()
            nativeWidget.setAttribute("data-aspect-ratio", ratio.toString())
            if (isNonMainModalWindow) {
                modalContent.style["aspect-ratio"] = ratio.toString()
            }
        } else {
            nativeWidget.style.remove("aspect-ratio")
            nativeWidget.removeAttribute("data-aspect-ratio")
            modalContent.style.remove("aspect-ratio")
        }
    }

    private fun clampSizeForState(size: Size): Size {
        val ratio = aspectRatioValue
        val min = minWindowSize
        return if (ratio != null && isWindowedNormal()) {
            WindowAspectRatioHelper.clampClientSize(size, ratio, min)
        } else if (min != null) {
            WindowAspectRatioHelper.clampClientSize(size, null, min)
        } else {
            size
        }
    }

    private fun applyResolvedWindowSize(resolvedSize: Size, notifyResize: Boolean) {
        windowSize = resolvedSize
        nativeWidget.clientWidth = resolvedSize.width
        nativeWidget.clientHeight = resolvedSize.height
        if (notifyResize) {
            val listener = onResizeListener
            if (listener != null) {
                lastNotifiedWidthPx = resolvedSize.width
                lastNotifiedHeightPx = resolvedSize.height
                listener.invoke(currentWindowConfiguration(resolvedSize.width, resolvedSize.height))
            }
        }
    }

    override fun setWindowSize(size: Size) {
        val isModalWin = isNonMainModalWindow || nativeWidget.getAttribute("data-tool-modal") == "true"
        val baseSize = if (WebBrowserDomBridge.isBrowserEnvironment && !isModalWin) {
            Size(
                WebBrowserDomBridge.getViewportWidth(size.width),
                WebBrowserDomBridge.getViewportHeight(size.height)
            )
        } else {
            size
        }
        val resolvedSize = clampSizeForState(baseSize)
        applyResolvedWindowSize(resolvedSize, notifyResize = true)
        if (isModalWin) {
            ensureNonMainBootstrapModalStructure()
        }
    }

    override fun getWindowSize(): Size = windowSize

    override fun getMinWindowSize(): Size? = minWindowSize

    override fun setMinWindowSize(minSize: Size?) {
        minWindowSize = minSize
        if (minSize != null) {
            nativeWidget.setAttribute("data-min-width", minSize.width.toString())
            nativeWidget.setAttribute("data-min-height", minSize.height.toString())
            val adjusted = clampSizeForState(windowSize)
            if (adjusted != windowSize) {
                setWindowSize(adjusted)
            }
        } else {
            nativeWidget.removeAttribute("data-min-width")
            nativeWidget.removeAttribute("data-min-height")
        }
    }

    override fun setAlwaysOnTop(alwaysOnTop: Boolean) {
        val prev = alwaysOnTopFlag
        alwaysOnTopFlag = alwaysOnTop
        nativeWidget.setAttribute("data-always-on-top", alwaysOnTop.toString())
        if (alwaysOnTop) {
            if (!prev) {
                val currentZ = nativeWidget.style["z-index"]
                if (currentZ != null && (currentZ.toIntOrNull() ?: 0) < ALWAYS_ON_TOP_BASE_Z_INDEX) {
                    baselineZIndex = currentZ
                }
            }
            nativeWidget.style["z-index"] = allocateAlwaysOnTopZIndex().toString()
        } else {
            val restored = baselineZIndex
            baselineZIndex = null
            if (restored != null) {
                nativeWidget.style["z-index"] = restored
            } else if (hasBeenBroughtToFront) {
                nativeWidget.style["z-index"] = allocateNextZIndex().toString()
            } else {
                nativeWidget.style.remove("z-index")
            }
        }
    }

    override fun isAlwaysOnTop(): Boolean = alwaysOnTopFlag

    override fun setAspectRatio(aspectRatio: Double?) {
        WindowAspectRatioHelper.validateAspectRatio(aspectRatio)
        aspectRatioValue = aspectRatio
        syncAspectRatioStyle()
        if (aspectRatio != null && isWindowedNormal()) {
            val clamped = clampSizeForState(windowSize)
            if (clamped != windowSize) {
                applyResolvedWindowSize(clamped, notifyResize = false)
            }
        }
    }

    override fun getAspectRatio(): Double? = aspectRatioValue

    override fun setWindowState(state: WindowState) {
        if (state == WindowState.FULLSCREEN) {
            if (!fullscreenFlag) {
                setFullscreen(true)
            }
            return
        }
        windowState = state
        if (fullscreenFlag) {
            stateBeforeFullscreen = state
            setFullscreen(false)
            return
        }
        syncAspectRatioStyle()
        if (state == WindowState.NORMAL && aspectRatioValue != null) {
            val clamped = clampSizeForState(windowSize)
            if (clamped != windowSize) {
                applyResolvedWindowSize(clamped, notifyResize = false)
            }
        }
    }

    override fun getWindowState(): WindowState = windowState

    override fun setFullscreen(fullscreen: Boolean, cutoutMode: DisplayCutoutMode) {
        if (fullscreen && !fullscreenFlag) {
            if (windowState != WindowState.FULLSCREEN) {
                stateBeforeFullscreen = windowState
            }
            windowState = WindowState.FULLSCREEN
        } else if (!fullscreen && fullscreenFlag) {
            if (windowState == WindowState.FULLSCREEN) {
                windowState = if (stateBeforeFullscreen == WindowState.FULLSCREEN) {
                    WindowState.NORMAL
                } else {
                    stateBeforeFullscreen
                }
            }
        }
        fullscreenFlag = fullscreen
        if (fullscreen) {
            nativeWidget.classList.add("fullscreen")
        } else {
            nativeWidget.classList.remove("fullscreen")
            (fullscreenElementImpl?.nativeWidget as? WebDomElement)?.classList?.remove("genericui-fullscreen-element")
            fullscreenElementImpl = null
        }
        syncAspectRatioStyle()
        if (!fullscreen && windowState == WindowState.NORMAL && aspectRatioValue != null) {
            val clamped = clampSizeForState(windowSize)
            if (clamped != windowSize) {
                applyResolvedWindowSize(clamped, notifyResize = false)
            }
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
            val resolved = clampSizeForState(Size(vw, vh))
            windowSize = resolved
            nativeWidget.clientWidth = resolved.width
            nativeWidget.clientHeight = resolved.height
            lastNotifiedWidthPx = resolved.width
            lastNotifiedHeightPx = resolved.height
            onResizeListener?.invoke(currentWindowConfiguration(resolved.width, resolved.height))
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
        if (alwaysOnTopOfOwner || alwaysOnTopFlag) {
            val ownerZ = webOwner?.nativeWidget?.style?.get("z-index")?.toIntOrNull() ?: 0
            val toolZ = if (alwaysOnTopFlag || ownerZ >= ALWAYS_ON_TOP_BASE_Z_INDEX) {
                maxOf(ownerZ + 1, allocateAlwaysOnTopZIndex())
            } else {
                allocateNextZIndex().also { z ->
                    baselineZIndex = z.toString()
                }
            }
            nativeWidget.style["z-index"] = toolZ.toString()
        }
    }

    override fun bringToFront() {
        hasBeenBroughtToFront = true
        val myZ = if (alwaysOnTopFlag) {
            allocateAlwaysOnTopZIndex()
        } else {
            allocateNextZIndex().also { z ->
                baselineZIndex = z.toString()
            }
        }
        nativeWidget.style["z-index"] = myZ.toString()
        for (toolWin in attachedToolWindows) {
            if (toolWin.alwaysOnTopOfOwnerFlag || toolWin.alwaysOnTopFlag) {
                val toolZ = if (toolWin.alwaysOnTopFlag || alwaysOnTopFlag) {
                    maxOf(myZ + 1, allocateAlwaysOnTopZIndex())
                } else {
                    allocateNextZIndex().also { z ->
                        toolWin.baselineZIndex = z.toString()
                    }
                }
                toolWin.nativeWidget.style["z-index"] = toolZ.toString()
            }
        }
        for (dockWin in attachedDockWindows) {
            if (dockWin.isAlwaysOnTopOfOwner) {
                dockWin.bringToFront()
                if (alwaysOnTopFlag) {
                    val dockZ = dockWin.nativeWidget.style["z-index"]?.toIntOrNull() ?: 0
                    if (dockZ <= myZ) {
                        dockWin.nativeWidget.style["z-index"] = maxOf(myZ + 1, allocateAlwaysOnTopZIndex()).toString()
                    }
                }
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
