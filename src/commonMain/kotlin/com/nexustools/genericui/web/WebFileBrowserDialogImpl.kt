package com.nexustools.genericui.web

import com.nexustools.genericui.component.dialog.FileBrowserCallback
import com.nexustools.genericui.component.dialog.FileBrowserRequest
import com.nexustools.genericui.component.dialog.FileTypeFilter
import com.nexustools.genericui.component.dialog.SelectedFile
import com.nexustools.genericui.enums.dialog.FileBrowserMode
import com.nexustools.genericui.nativeimpl.dialog.FileBrowserDialogNativeImpl
import com.nexustools.genericui.nativeimpl.shell.WindowNativeImpl

/**
 * Web native file and folder browser implementation ([FileBrowserDialogNativeImpl]).
 *
 * Simulates a native OS file/folder dialog by creating an invisible, un-interactable `<input type="file">`
 * DOM element (`display: none; position: fixed; left: -9999px; opacity: 0; pointer-events: none; tabindex: -1; aria-hidden: true`),
 * configuring `webkitdirectory` / `directory` (in `SELECT_FOLDER` mode), `multiple` (in `OPEN_MULTIPLE_FILES` mode),
 * and `accept` (translated from [FileTypeFilter] extensions and MIME types), listening for `change` and `cancel`
 * (with window `focus` fallback), performing post-selection filter validation, and removing the temporary `<input>`
 * element from the DOM.
 */
class WebFileBrowserDialogImpl : FileBrowserDialogNativeImpl {

    var activeHiddenInput: WebDomElement? = null
        private set

    override fun showFileBrowser(
        parentWindow: WindowNativeImpl<*>?,
        request: FileBrowserRequest,
        callback: FileBrowserCallback
    ) {
        cleanupActiveInput()

        val input = WebDomElement("input").apply {
            setAttribute("type", "file")
            setAttribute("tabindex", "-1")
            setAttribute("aria-hidden", "true")
            style["display"] = "none"
            style["position"] = "fixed"
            style["left"] = "-9999px"
            style["top"] = "-9999px"
            style["width"] = "0px"
            style["height"] = "0px"
            style["opacity"] = "0"
            style["pointer-events"] = "none"
            style["visibility"] = "hidden"
        }

        when (request.mode) {
            FileBrowserMode.SELECT_FOLDER -> {
                input.webkitdirectory = true
                input.directory = true
                input.setAttribute("webkitdirectory", "true")
                input.setAttribute("directory", "true")
            }
            FileBrowserMode.OPEN_MULTIPLE_FILES -> {
                input.multiple = true
                input.setAttribute("multiple", "true")
            }
            FileBrowserMode.OPEN_SINGLE_FILE, FileBrowserMode.SAVE_FILE -> {
                input.multiple = false
            }
        }

        val acceptAttr = if (request.mode == FileBrowserMode.SELECT_FOLDER) {
            ""
        } else {
            FileTypeFilter.toWebAcceptAttribute(request.filters)
        }
        if (acceptAttr.isNotEmpty()) {
            input.accept = acceptAttr
            input.setAttribute("accept", acceptAttr)
        }

        val hostDom = (parentWindow?.nativeWidget as? WebDomElement)
        hostDom?.appendChild(input)
        activeHiddenInput = input

        var settled = false
        val activeFilter = request.filters.getOrNull(request.selectedFilterIndex)
            ?: request.filters.firstOrNull()

        input.addEventListener("change") { payload ->
            if (settled) return@addEventListener
            settled = true
            @Suppress("UNCHECKED_CAST")
            val rawFiles = (payload as? List<SelectedFile>) ?: emptyList()
            val validated = if (request.mode == FileBrowserMode.SELECT_FOLDER || request.filters.isEmpty()) {
                rawFiles
            } else {
                rawFiles.filter { file -> matchesFilters(file, request.filters) }
            }
            cleanupActiveInput()
            if (validated.isNotEmpty()) {
                callback.onFilesSelected(validated, activeFilter)
            } else {
                callback.onCancelled()
            }
        }

        input.addEventListener("cancel") {
            if (settled) return@addEventListener
            settled = true
            cleanupActiveInput()
            callback.onCancelled()
        }

        input.addEventListener("window-focus-fallback") {
            if (settled) return@addEventListener
            settled = true
            cleanupActiveInput()
            callback.onCancelled()
        }

        // Programmatically trigger the hidden file/folder input
        input.click()
    }

    /**
     * Post-selection validation for wildcard, extension, and MIME filters not strictly enforced by browser pickers.
     */
    fun matchesFilters(file: SelectedFile, filters: List<FileTypeFilter>): Boolean {
        if (file.isDirectory || filters.isEmpty()) return true
        return filters.any { filter ->
            val extMatches = filter.matchesFileName(file.name)
            val rawMime = file.mimeType
            val mimeMatches = rawMime != null && filter.mimeTypes.any { pattern ->
                val clean = pattern.trim().lowercase()
                val fileMime = rawMime.lowercase()
                when {
                    clean == "*/*" -> true
                    clean.endsWith("/*") -> fileMime.startsWith(clean.removeSuffix("*"))
                    else -> fileMime == clean
                }
            }
            extMatches || mimeMatches
        }
    }

    private fun cleanupActiveInput() {
        activeHiddenInput?.remove()
        activeHiddenInput = null
    }

    override fun dispose() {
        cleanupActiveInput()
    }
}
