package com.nexustools.genericui.web

import com.nexustools.genericui.core.NativeFactory
import com.nexustools.genericui.image.ImageLoader
import com.nexustools.genericui.nativeimpl.action.ButtonNativeImpl
import com.nexustools.genericui.nativeimpl.action.DropdownNativeImpl
import com.nexustools.genericui.nativeimpl.content.ImageNativeImpl
import com.nexustools.genericui.nativeimpl.content.TableNativeImpl
import com.nexustools.genericui.nativeimpl.dialog.DialogWindowNativeImpl
import com.nexustools.genericui.nativeimpl.dialog.FileBrowserDialogNativeImpl
import com.nexustools.genericui.nativeimpl.dialog.StandardDialogsNativeImpl
import com.nexustools.genericui.nativeimpl.dock.*
import com.nexustools.genericui.nativeimpl.feedback.ProgressBarNativeImpl
import com.nexustools.genericui.nativeimpl.feedback.SpinnerNativeImpl
import com.nexustools.genericui.nativeimpl.form.CheckboxNativeImpl
import com.nexustools.genericui.nativeimpl.form.RangeSliderNativeImpl
import com.nexustools.genericui.nativeimpl.form.SelectNativeImpl
import com.nexustools.genericui.nativeimpl.form.TextInputNativeImpl
import com.nexustools.genericui.nativeimpl.layout.ContainerNativeImpl
import com.nexustools.genericui.nativeimpl.layout.DividerNativeImpl
import com.nexustools.genericui.nativeimpl.layout.ScrollViewNativeImpl
import com.nexustools.genericui.nativeimpl.lazy.LazyGridNativeImpl
import com.nexustools.genericui.nativeimpl.lazy.LazyListNativeImpl
import com.nexustools.genericui.nativeimpl.lazy.TreeViewNativeImpl
import com.nexustools.genericui.nativeimpl.navigation.BottomNavigationBarNativeImpl
import com.nexustools.genericui.nativeimpl.navigation.NavbarNativeImpl
import com.nexustools.genericui.nativeimpl.tabs.TabContainerNativeImpl
import com.nexustools.genericui.nativeimpl.tabs.TabNativeImpl
import com.nexustools.genericui.nativeimpl.overlay.PopoverNativeImpl
import com.nexustools.genericui.nativeimpl.shell.*
import com.nexustools.genericui.nativeimpl.surface.CollapseNativeImpl
import com.nexustools.genericui.nativeimpl.system.*
import com.nexustools.genericui.nativeimpl.typography.MarkdownViewNativeImpl
import com.nexustools.genericui.nativeimpl.typography.TextNativeImpl

/**
 * Web (`js` + `wasmJs`) implementation of [NativeFactory].
 *
 * Applies CSS classes directly to the HTML5 DOM (`classList`) while simulating Bootstrap Enum getters/setters,
 * uses an invisible `<input type="file">` for native file/folder browsing ([WebFileBrowserDialogImpl]),
 * mounts/unmounts DOM nodes lazily in [WebLazyListImpl], [WebLazyGridImpl], and [WebTreeViewImpl],
 * and drives `Canvas2D`, `GLCanvas` (WebGL2), and `VKCanvas` (WebGPU) via `requestAnimationFrame` with `deltaMs`.
 */
class WebNativeFactory(
    val templateEngine: WebHtmlTemplateEngine = WebHtmlTemplateEngine()
) : NativeFactory {
    override val backendName: String = "web"

    init {
        ImageLoader.activeNativeCodec = WebBrowserImageCodecImpl()
    }

    // Shell & Window
    override fun createApplication(): ApplicationNativeImpl<*> = WebApplicationImpl(templateEngine)
    override fun createWindow(): WindowNativeImpl<*> = WebWindowImpl()
    override fun createMenuBar(): MenuBarNativeImpl<*> = WebMenuBarImpl()
    override fun createMenu(): MenuNativeImpl<*> = WebMenuImpl()
    override fun createMenuItem(checkable: Boolean, radio: Boolean): MenuItemNativeImpl<*> = WebMenuItemImpl(checkable, radio)
    override fun createContextMenu(): ContextMenuNativeImpl<*> = WebContextMenuImpl()
    override fun createSplitPane(): SplitPaneNativeImpl<*> = WebSplitPaneImpl()

    // Dockable Panel System (GIMP / Photoshop Studio)
    override fun createDockWorkspace(): DockWorkspaceNativeImpl<*> = WebDockWorkspaceImpl()
    override fun createDockArea(): DockAreaNativeImpl<*> = WebDockAreaImpl()
    override fun createDockGroup(): DockGroupNativeImpl<*> = WebDockGroupImpl()
    override fun createDockPanel(): DockPanelNativeImpl<*> = WebDockPanelImpl()
    override fun createDockWindow(): DockWindowNativeImpl<*> = WebDockWindowImpl()
    override fun createToolPalette(): ToolPaletteNativeImpl<*> = WebToolPaletteImpl()

    // Layout
    override fun createContainer(classPrefix: String): ContainerNativeImpl<*> = WebContainerImpl(classPrefix)
    override fun createContainer(classPrefix: String, tagName: String): ContainerNativeImpl<*> = WebContainerImpl(classPrefix, tagName)
    override fun createScrollView(): ScrollViewNativeImpl<*> = WebScrollViewImpl()
    override fun createDivider(): DividerNativeImpl<*> = WebDividerImpl()

    // Typography & Content
    override fun createText(tagName: String, classPrefix: String): TextNativeImpl<*> = WebTextImpl(tagName, classPrefix)
    override fun createMarkdownView(): MarkdownViewNativeImpl<*> = WebMarkdownViewImpl()
    override fun createImage(): ImageNativeImpl<*> = WebImageImpl()
    override fun createTable(): TableNativeImpl<*> = WebTableImpl()

    // Action & Form
    override fun createButton(classPrefix: String): ButtonNativeImpl<*> = WebButtonImpl(classPrefix)
    override fun createDropdown(): DropdownNativeImpl<*> = WebDropdownImpl()
    override fun createTextInput(multiline: Boolean): TextInputNativeImpl<*> = WebTextInputImpl(multiline)
    override fun createSelect(): SelectNativeImpl<*> = WebSelectImpl()
    override fun createCheckbox(switchStyle: Boolean, radioStyle: Boolean): CheckboxNativeImpl<*> = WebCheckboxImpl(switchStyle, radioStyle)
    override fun createRangeSlider(): RangeSliderNativeImpl<*> = WebRangeSliderImpl()

    // Surface, Tabs, Navigation, Feedback & Overlay
    override fun createCollapse(): CollapseNativeImpl<*> = WebCollapseImpl()
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

    // System Services
    override fun createPermissionManager(): PermissionManagerNativeImpl = WebPermissionManagerImpl()
    override fun createSystemTray(): SystemTrayNativeImpl = WebSystemTrayImpl()
    override fun createNotificationManager(): NotificationManagerNativeImpl = WebNotificationManagerImpl()
    override fun createNetworkMonitor(): NetworkMonitorNativeImpl = WebNetworkMonitorImpl()
    override fun createPreferenceStore(appName: String): PreferenceStoreNativeImpl = WebPreferenceStoreImpl(appName)
}
