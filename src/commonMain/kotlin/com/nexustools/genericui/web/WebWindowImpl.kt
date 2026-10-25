package com.nexustools.genericui.web

import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.enums.window.DisplayCutoutMode
import com.nexustools.genericui.enums.window.WindowState
import com.nexustools.genericui.geometry.Size
import com.nexustools.genericui.nativeimpl.shell.MenuBarNativeImpl
import com.nexustools.genericui.nativeimpl.shell.WindowNativeImpl
import com.nexustools.genericui.window.PipConfig
import com.nexustools.genericui.window.TitleBar
import com.nexustools.genericui.window.WindowConfiguration

/**
 * Web implementation of [WindowNativeImpl].
 * On `:web`, `supportsCustomTitleBar` and `supportsEmbeddedMenuBarInTitleBar` are `false` (complete zero-CPU no-op
 * for `TitleBar` instructions), while `MenuBar`, `Navbar`, and `BottomNavigationBar` mount into dedicated DOM slots.
 */
class WebWindowImpl : WebBaseContainerNativeImpl("div", "genericui-window"), WindowNativeImpl<WebDomElement> {
    override val supportsCustomTitleBar: Boolean = false
    override val supportsEmbeddedMenuBarInTitleBar: Boolean = false
    override val supportsMultiWindow: Boolean = false

    private var titleText: String = "GenericUI"
    private var windowSize: Size = Size(
        WebBrowserDomBridge.getViewportWidth(1280),
        WebBrowserDomBridge.getViewportHeight(800)
    )
    private var windowState: WindowState = WindowState.NORMAL
    private var fullscreenFlag: Boolean = false
    private var pipActive: Boolean = false

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

    init {
        activeWindow = this
        nativeWidget.style["width"] = "100%"
        nativeWidget.style["height"] = "100%"
        nativeWidget.style["position"] = "relative"
        nativeWidget.appendChild(menuSlot)
        nativeWidget.appendChild(navSlot)
        nativeWidget.appendChild(contentSlot)
        nativeWidget.appendChild(bottomNavSlot)
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
    }

    override fun isFullscreen(): Boolean = fullscreenFlag

    override fun applyTitleBarInstructions(titleBar: TitleBar?) {
        // Intentional zero-CPU no-op on :web (supportsCustomTitleBar == false)
    }

    override fun setMenuBarNative(menuBarImpl: MenuBarNativeImpl<*>?) {
        menuSlot.clearChildren()
        val dom = menuBarImpl?.nativeWidget as? WebDomElement ?: return
        menuSlot.appendChild(dom)
    }

    override fun setNavBarNative(navBarImpl: NativeImpl<*>?) {
        navSlot.clearChildren()
        val dom = navBarImpl?.nativeWidget as? WebDomElement ?: return
        navSlot.appendChild(dom)
    }

    override fun setBottomNavigationBarNative(bottomNavImpl: NativeImpl<*>?) {
        bottomNavSlot.clearChildren()
        val dom = bottomNavImpl?.nativeWidget as? WebDomElement ?: return
        bottomNavSlot.appendChild(dom)
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
        val vw = WebBrowserDomBridge.getViewportWidth(windowSize.width)
        val vh = WebBrowserDomBridge.getViewportHeight(windowSize.height)
        windowSize = Size(vw, vh)
        nativeWidget.clientWidth = vw
        nativeWidget.clientHeight = vh
        onFocusListener?.invoke(true)
        onResizeListener?.invoke(WindowConfiguration(widthPx = vw, heightPx = vh))
        scheduleLayoutRefresh()
    }

    override fun closeWindow() {
        val allow = onCloseRequestedListener?.invoke() ?: true
        if (allow) {
            setVisible(false)
        }
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
