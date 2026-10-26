package com.nexustools.genericui.web

import com.nexustools.genericui.res.Resources
import com.nexustools.genericui.resources.DefaultResourceResolverNativeImpl
import com.nexustools.genericui.resources.ResourceID
import com.nexustools.genericui.resources.ResourceType

/**
 * Web (`:web`) implementation of [com.nexustools.genericui.resources.ResourceResolverNativeImpl]
 * that maps file-backed [ResourceID] handles (`DRAWABLE`, `RAW`, `FONT`, `XML`) to relative Web
 * asset URLs (`"assets/<type>/<fileName>"`) staged by `compileGenericUiResources`.
 */
class WebResourceResolverImpl : DefaultResourceResolverNativeImpl() {

    override fun resolveUrl(resId: ResourceID): String {
        val registered = Resources.activeTable.findResourceId(resId.id)
            ?: Resources.activeTable.findResourceId(resId.type, resId.name)
            ?: resId
        val relPath = registered.relativePath.ifEmpty { resId.relativePath }
        val fileName = relPath.substringAfterLast('/').ifEmpty {
            val drawable = Resources.activeTable.getDrawable(resId)
            val ext = drawable?.format?.ifEmpty { "svg" } ?: when (resId.type) {
                ResourceType.DRAWABLE -> "svg"
                ResourceType.FONT -> "ttf"
                ResourceType.XML -> "xml"
                else -> "bin"
            }
            "${resId.name}.$ext"
        }
        return "assets/${resId.type.directoryName}/$fileName"
    }
}
