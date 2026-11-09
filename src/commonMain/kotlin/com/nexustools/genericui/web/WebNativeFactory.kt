package com.nexustools.genericui.web

import com.nexustools.genericui.component.fragment.helper.FragmentHistoryBridge
import com.nexustools.genericui.component.layout.LayoutFactory
import com.nexustools.genericui.core.NativeFactory
import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.image.ImageLoader
import com.nexustools.genericui.nativeimpl.action.ButtonNativeImpl
import com.nexustools.genericui.nativeimpl.action.DropdownNativeImpl
import com.nexustools.genericui.nativeimpl.action.StyledButtonNativeImpl
import com.nexustools.genericui.nativeimpl.content.ImageNativeImpl
import com.nexustools.genericui.nativeimpl.content.TableNativeImpl
import com.nexustools.genericui.nativeimpl.dialog.DialogWindowNativeImpl
import com.nexustools.genericui.nativeimpl.dialog.FileBrowserDialogNativeImpl
import com.nexustools.genericui.nativeimpl.dialog.StandardDialogsNativeImpl
import com.nexustools.genericui.nativeimpl.dock.*
import com.nexustools.genericui.nativeimpl.document.BookContainerNativeImpl
import com.nexustools.genericui.nativeimpl.feedback.ProgressBarNativeImpl
import com.nexustools.genericui.nativeimpl.feedback.SpinnerNativeImpl
import com.nexustools.genericui.nativeimpl.form.CheckboxNativeImpl
import com.nexustools.genericui.nativeimpl.form.RangeSliderNativeImpl
import com.nexustools.genericui.nativeimpl.form.SelectNativeImpl
import com.nexustools.genericui.nativeimpl.form.TextInputNativeImpl
import com.nexustools.genericui.nativeimpl.io.FileSystemNativeImpl
import com.nexustools.genericui.nativeimpl.layout.ContainerNativeImpl
import com.nexustools.genericui.nativeimpl.layout.DividerNativeImpl
import com.nexustools.genericui.nativeimpl.layout.ScrollViewNativeImpl
import com.nexustools.genericui.nativeimpl.layout.StyledContainerNativeImpl
import com.nexustools.genericui.nativeimpl.lazy.LazyGridNativeImpl
import com.nexustools.genericui.nativeimpl.lazy.LazyListNativeImpl
import com.nexustools.genericui.nativeimpl.lazy.TreeViewNativeImpl
import com.nexustools.genericui.nativeimpl.navigation.BottomNavigationBarNativeImpl
import com.nexustools.genericui.nativeimpl.navigation.NavbarNativeImpl
import com.nexustools.genericui.nativeimpl.net.HttpClientNativeImpl
import com.nexustools.genericui.nativeimpl.net.TcpSocketNativeImpl
import com.nexustools.genericui.nativeimpl.net.UdpSocketNativeImpl
import com.nexustools.genericui.nativeimpl.net.WebSocketNativeImpl
import com.nexustools.genericui.nativeimpl.tabs.TabContainerNativeImpl
import com.nexustools.genericui.nativeimpl.tabs.TabNativeImpl
import com.nexustools.genericui.nativeimpl.overlay.PopoverNativeImpl
import com.nexustools.genericui.nativeimpl.shell.*
import com.nexustools.genericui.nativeimpl.system.*
import com.nexustools.genericui.nativeimpl.typography.MarkdownViewNativeImpl
import com.nexustools.genericui.nativeimpl.typography.TextNativeImpl
import com.nexustools.genericui.threading.UiThreadDispatcherNativeImpl

/**
 * Web (`js` + `wasmJs`) implementation of [NativeFactory].
 *
 * Applies CSS classes directly to the HTML5 DOM (`classList`) while simulating Bootstrap Enum getters/setters,
 * uses an invisible `<input type="file">` for native file/folder browsing ([WebFileBrowserDialogImpl]),
 * and mounts/unmounts DOM nodes lazily in [WebLazyListImpl], [WebLazyGridImpl], and [WebTreeViewImpl].
 */
class WebNativeFactory(
    val templateEngine: WebHtmlTemplateEngine = WebHtmlTemplateEngine()
) : NativeFactory {
    override val backendName: String = "web"
    override val backendTarget: BackendTarget = BackendTarget.Web
    override val layoutFactory: LayoutFactory = WebLayoutFactory
    override val isStyleSheetEngineEnabled: Boolean
        get() = false
    override val ignoresAestheticStylesOnStandardComponents: Boolean
        get() = false

    override val defaultTitleCapabilities: com.nexustools.genericui.window.TitleCapabilities =
        com.nexustools.genericui.window.TitleCapabilities(
            isEmulated = true,
            supportsMinimizeButton = false,
            supportsMaximizeButton = false,
            supportsCloseButton = false,
            supportsControlsPlacement = false,
            supportsCustomHeight = false,
            isTitleBarRequired = false,
            titleBarRendersAsNavbar = true,
            menuBarRendersAsNavbar = false,
            menuBarMergesIntoTitleNavbar = true
        )
    override val usesBrowserManagedImageLoading: Boolean = true
    override val supportsMultipleWindows: Boolean = false
    override val supportsToolModalWindows: Boolean = false
    override val supportsSystemTray: Boolean = false
    override val supportsDirectorySelection: Boolean = false
    override val supportsDirectFileSystemAccess: Boolean = false
    override val providesNativeNavigationChrome: Boolean = true
    override val isWebSplashSupported: Boolean = true

    override fun setWebSplashStatus(statusText: String, type: BootstrapType) {
        WebBrowserDomBridge.setWebSplashStatus(statusText, type)
    }

    override fun setWebSplashProgress(progressPercent: Int, type: BootstrapType) {
        WebBrowserDomBridge.setWebSplashProgress(progressPercent, type)
    }

    override fun setWebSplashError(message: String, stackTrace: String?, title: String) {
        WebBrowserDomBridge.setWebSplashError(message, stackTrace, title)
    }

    override fun finishWebSplash() {
        WebBrowserDomBridge.finishWebSplash(templateEngine.splashConfig.fadeDurationMs)
    }

    init {
        ImageLoader.activeNativeCodec = WebBrowserImageCodecImpl()
        FragmentHistoryBridge.factory = { WebFragmentHistoryBridge() }
        WebApplicationImpl.installSystemThemeHooks()
        WebBrowserDomBridge.registerPreloadedResource(
            key = com.nexustools.genericui.res.Resources.LOADING_IMAGE.toResourceUri(),
            mimeType = "image/svg+xml",
            bytes = com.nexustools.genericui.res.Resources.DEFAULT_LOADING_SVG.encodeToByteArray(),
            text = com.nexustools.genericui.res.Resources.DEFAULT_LOADING_SVG
        )
    }

    // Shell & Window
    override fun createApplication(): ApplicationNativeImpl<*> = WebApplicationImpl(templateEngine)
    override fun createWindow(): WindowNativeImpl<*> = WebWindowImpl()
    override fun createMenuBar(): MenuBarNativeImpl<*> = WebMenuBarImpl()
    override fun createMenu(): MenuNativeImpl<*> = WebMenuImpl()
    override fun createMenuItem(checkable: Boolean, radio: Boolean): MenuItemNativeImpl<*> = WebMenuItemImpl(checkable, radio)
    override fun createContextMenu(): ContextMenuNativeImpl<*> = WebContextMenuImpl()

    // Dockable Panel System (GIMP / Photoshop Studio)
    override fun createDockWorkspace(): DockWorkspaceNativeImpl<*> = WebDockWorkspaceImpl()
    override fun createDockArea(): DockAreaNativeImpl<*> = WebDockAreaImpl()
    override fun createDockGroup(): DockGroupNativeImpl<*> = WebDockGroupImpl()
    override fun createDockPanel(): DockPanelNativeImpl<*> = WebDockPanelImpl()
    override fun createDockWindow(): DockWindowNativeImpl<*> = WebDockWindowImpl()
    override fun createToolPalette(): ToolPaletteNativeImpl<*> = WebToolPaletteImpl()

    // Layout & Printable Book Documents
    override fun createContainer(classPrefix: String): ContainerNativeImpl<*> = WebContainerImpl(classPrefix)
    override fun createContainer(classPrefix: String, tagName: String): ContainerNativeImpl<*> = WebContainerImpl(classPrefix, tagName)
    override fun createStyledContainer(classPrefix: String, tagName: String): StyledContainerNativeImpl<*> =
        WebStyledContainerImpl(classPrefix, tagName)
    override fun createBookContainer(): BookContainerNativeImpl<*> = WebBookContainerImpl()
    override fun createScrollView(): ScrollViewNativeImpl<*> = WebScrollViewImpl()
    override fun createScrollView(tagName: String): ScrollViewNativeImpl<*> = WebScrollViewImpl(tagName.ifBlank { "genericui:scrollview" })
    override fun createDivider(): DividerNativeImpl<*> = WebDividerImpl()

    // Typography & Content
    override fun createText(tagName: String, classPrefix: String): TextNativeImpl<*> = WebTextImpl(tagName, classPrefix)
    override fun createMarkdownView(): MarkdownViewNativeImpl<*> = WebMarkdownViewImpl()
    override fun createImage(): ImageNativeImpl<*> = WebImageImpl()
    override fun createTable(): TableNativeImpl<*> = WebTableImpl()

    // Action & Form
    override fun createButton(classPrefix: String): ButtonNativeImpl<*> = WebButtonImpl(classPrefix)
    override fun createStyledButton(classPrefix: String): StyledButtonNativeImpl<*> = WebStyledButtonImpl(classPrefix)
    override fun createDropdown(): DropdownNativeImpl<*> = WebDropdownImpl()
    override fun createTextInput(multiline: Boolean): TextInputNativeImpl<*> = WebTextInputImpl(multiline)
    override fun createSelect(): SelectNativeImpl<*> = WebSelectImpl()
    override fun createCheckbox(switchStyle: Boolean, radioStyle: Boolean): CheckboxNativeImpl<*> = WebCheckboxImpl(switchStyle, radioStyle)
    override fun createRangeSlider(): RangeSliderNativeImpl<*> = WebRangeSliderImpl()

    // Surface, Tabs, Navigation, Feedback & Overlay
    override fun createTab(closable: Boolean): TabNativeImpl<*> = WebTabImpl(closable)
    override fun createTabContainer(): TabContainerNativeImpl<*> = WebTabContainerImpl()
    override fun createNavbar(): NavbarNativeImpl<*> = WebNavbarImpl()
    override fun createBottomNavigationBar(): BottomNavigationBarNativeImpl<*> = WebBottomNavigationBarImpl()
    override fun createProgressBar(): ProgressBarNativeImpl<*> = WebProgressBarImpl()
    override fun createSpinner(): SpinnerNativeImpl<*> = WebSpinnerImpl()
    override fun createPopover(): PopoverNativeImpl<*> = WebPopoverImpl()

    // Dialogs & Native File/Folder Browser
    override fun createDialogWindow(): DialogWindowNativeImpl<*> = WebDialogWindowImpl()
    override fun createFileBrowserDialog(): FileBrowserDialogNativeImpl = WebFileBrowserDialogImpl()
    override fun createStandardDialogs(): StandardDialogsNativeImpl = WebStandardDialogsImpl()

    // Lazy Virtualized Lists, Grids & Trees
    override fun createLazyList(): LazyListNativeImpl<*> = WebLazyListImpl()
    override fun createLazyGrid(): LazyGridNativeImpl<*> = WebLazyGridImpl()
    override fun createTreeView(): TreeViewNativeImpl<*> = WebTreeViewImpl()

    // System Services & Async I/O / Database
    override fun createUiThreadDispatcher(): UiThreadDispatcherNativeImpl = WebUiThreadDispatcherNativeImpl()
    override fun createUiTimerScheduler(): UiTimerNativeImpl = WebUiTimerImpl()
    override fun createPermissionManager(): PermissionManagerNativeImpl = WebPermissionManagerImpl()
    override fun createSystemTray(): SystemTrayNativeImpl = WebSystemTrayImpl()
    override fun createNotificationManager(): NotificationManagerNativeImpl = WebNotificationManagerImpl()
    override fun createNetworkMonitor(): NetworkMonitorNativeImpl = WebNetworkMonitorImpl()
    override fun createPreferenceStore(appName: String): PreferenceStoreNativeImpl = WebPreferenceStoreImpl(appName)
    override fun createConfigDirectory(appName: String, subdirectory: String): UserDirectoryNativeImpl =
        WebUserDirectoryNativeImpl(appName = appName, subdirectory = subdirectory, scheme = "config")
    override fun createCacheDirectory(appName: String, subdirectory: String): UserDirectoryNativeImpl =
        WebUserDirectoryNativeImpl(appName = appName, subdirectory = subdirectory, scheme = "cache")
    override fun createFileSystem(): FileSystemNativeImpl = WebFileSystemNativeImpl()
    override fun createDatabase(
        databaseName: String,
        filePath: String?,
        version: Int
    ): DatabaseNativeImpl = WebDatabaseNativeImpl(
        databaseName = databaseName,
        filePath = filePath ?: "$databaseName.db",
        version = version
    )
    override fun createResourceResolver(): com.nexustools.genericui.resources.ResourceResolverNativeImpl =
        WebResourceResolverImpl()

    // Async Networking (TCP, UDP, HTTP/HTTPS, WebSocket)
    override fun supportsTcp(): Boolean = false
    override fun supportsUdp(): Boolean = false
    override fun supportsMulticastUdp(): Boolean = false
    override fun createTcpSocket(): TcpSocketNativeImpl =
        throw UnsupportedOperationException("Raw TCP sockets are not supported on the Web backend.")
    override fun createUdpSocket(): UdpSocketNativeImpl =
        throw UnsupportedOperationException("Raw UDP sockets are not supported on the Web backend.")
    override fun createHttpClient(): HttpClientNativeImpl = WebHttpClientNativeImpl()
    override fun createWebSocket(): WebSocketNativeImpl = WebWebSocketNativeImpl()
}
