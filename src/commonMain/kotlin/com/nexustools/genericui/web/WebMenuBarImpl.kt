package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.shell.MenuBarNativeImpl

/**
 * Web implementation of [MenuBarNativeImpl] rendered as a Bootstrap `.navbar-nav` menu bar row.
 */
class WebMenuBarImpl : WebBaseContainerNativeImpl("nav", "menubar"), MenuBarNativeImpl<WebDomElement>
