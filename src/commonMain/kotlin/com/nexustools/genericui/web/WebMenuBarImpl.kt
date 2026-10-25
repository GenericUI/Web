package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.shell.MenuBarNativeImpl

/**
 * Web implementation of [MenuBarNativeImpl] rendered as `<genericui:menubar class="menubar">`.
 */
class WebMenuBarImpl : WebBaseContainerNativeImpl("genericui:menubar", "menubar"), MenuBarNativeImpl<WebDomElement>
