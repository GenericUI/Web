package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.content.TableNativeImpl

/**
 * Web implementation of [TableNativeImpl] rendered as `<table class="table">`.
 */
class WebTableImpl : WebBaseContainerNativeImpl("table", "table"), TableNativeImpl<WebDomElement> {
    override fun setHeaders(headers: List<String>) {
        nativeWidget.setAttribute("data-columns", headers.joinToString("|"))
    }

    override fun setRows(rows: List<List<String>>) {
        nativeWidget.setAttribute("data-row-count", rows.size.toString())
    }

    override fun setStriped(striped: Boolean) {
        if (striped) nativeWidget.classList.add("table-striped") else nativeWidget.classList.remove("table-striped")
    }

    override fun setHoverable(hoverable: Boolean) {
        if (hoverable) nativeWidget.classList.add("table-hover") else nativeWidget.classList.remove("table-hover")
    }
}
