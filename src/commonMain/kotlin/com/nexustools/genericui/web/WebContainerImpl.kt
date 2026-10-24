package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.layout.ContainerNativeImpl

/**
 * Web implementation of [ContainerNativeImpl].
 */
class WebContainerImpl(
    classPrefix: String = "container"
) : WebBaseContainerNativeImpl("div", classPrefix), ContainerNativeImpl<WebDomElement>
