package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.typography.MarkdownViewNativeImpl

/**
 * Web implementation of [MarkdownViewNativeImpl] rendered as `<genericui:markdownview class="markdown-body">`.
 */
class WebMarkdownViewImpl : WebBaseNativeImpl("genericui:markdownview", "markdown-body"), MarkdownViewNativeImpl<WebDomElement> {
    private var markdownSource: String = ""

    override fun setMarkdown(markdown: String) {
        markdownSource = markdown
        nativeWidget.textContent = markdown
    }

    override fun getMarkdown(): String = markdownSource
}
