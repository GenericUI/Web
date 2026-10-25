package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.layout.ContainerNativeImpl

/**
 * Web implementation of [ContainerNativeImpl] generating `<genericui:container>` (or inferred
 * `<genericui:<componentname>>` / explicit semantic HTML tag such as `<div>`, `<header>`, `<footer>`, etc.).
 */
class WebContainerImpl(
    classPrefix: String = "container",
    tagName: String = WebNamespaceSpec.inferTagForClassPrefix(classPrefix)
) : WebBaseContainerNativeImpl(
    WebNamespaceSpec.inferTagForClassPrefix(classPrefix, tagName),
    classPrefix
), ContainerNativeImpl<WebDomElement>
