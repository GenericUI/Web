package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.typography.MarkdownViewNativeImpl

/**
 * Web implementation of [MarkdownViewNativeImpl].
 */
class WebMarkdownViewImpl : WebBaseNativeImpl("article", "markdown-body"), MarkdownViewNativeImpl<WebDomElement> {
    private var markdownSource: String = ""

    override fun setMarkdown(markdown: String) {
        markdownSource = markdown
        nativeWidget.textContent = markdown
    }

    override fun getMarkdown(): String = markdownSource
}
