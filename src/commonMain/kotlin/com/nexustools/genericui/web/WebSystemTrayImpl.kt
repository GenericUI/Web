package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.shell.ContextMenuNativeImpl
import com.nexustools.genericui.nativeimpl.system.SystemTrayNativeImpl

/**
 * Web implementation of [SystemTrayNativeImpl] (updates PWA app badge via `navigator.setAppBadge`).
 */
class WebSystemTrayImpl : SystemTrayNativeImpl {
    override val isSupported: Boolean = false

    override fun setIcon(iconPathOrUrl: String?) {}
    override fun setTooltip(tooltip: String?) {}
    override fun setBadgeText(badge: String?) {}
    override fun setContextMenu(menuImpl: ContextMenuNativeImpl<*>?) {}
    override fun setOnTrayIconClicked(listener: (() -> Unit)?) {}
    override fun dispose() {}
}
