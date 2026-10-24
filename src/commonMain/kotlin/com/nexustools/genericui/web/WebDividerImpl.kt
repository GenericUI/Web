package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.layout.DividerNativeImpl

/**
 * Web implementation of [DividerNativeImpl] rendered as `<hr>`.
 */
class WebDividerImpl : WebBaseNativeImpl("hr", "dropdown-divider"), DividerNativeImpl<WebDomElement>
