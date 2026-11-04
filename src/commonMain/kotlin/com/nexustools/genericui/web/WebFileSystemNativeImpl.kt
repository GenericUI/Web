package com.nexustools.genericui.web

import com.nexustools.genericui.component.dialog.FileTypeFilter
import com.nexustools.genericui.io.FileOpenMode
import com.nexustools.genericui.io.FileSystemAvailability
import com.nexustools.genericui.nativeimpl.io.DefaultFileSystemNativeImpl
import com.nexustools.genericui.nativeimpl.io.FileReadableNativeImpl
import com.nexustools.genericui.nativeimpl.io.FileWritableNativeImpl

/**
 * Web (`:web`, `js` + `wasmJs`) implementation of [com.nexustools.genericui.nativeimpl.io.FileSystemNativeImpl]
 * backed by the browser File System Access API (`window.showOpenFilePicker`, `window.showSaveFilePicker`,
 * `window.showDirectoryPicker`) and `<input type="file">` handles without any `indexdb://` scheme.
 */
class WebFileSystemNativeImpl : DefaultFileSystemNativeImpl() {

    override fun isAvailable(): FileSystemAvailability =
        if (WebBrowserDomBridge.isFileSystemAccessSupported) {
            FileSystemAvailability.WithPermission
        } else {
            FileSystemAvailability.Unavailable
        }

    override fun supports(): Boolean =
        WebBrowserDomBridge.isFileSystemAccessSupported

    override fun resolve(vararg segments: String): String {
        val sanitizedSegments = Array(segments.size) { index ->
            val raw = segments[index].trim()
            when {
                raw.startsWith("indexdb://", ignoreCase = true) -> "/" + raw.substring(10).trimStart('/')
                raw.startsWith("indexeddb://", ignoreCase = true) -> "/" + raw.substring(12).trimStart('/')
                else -> raw
            }
        }
        return super.resolve(*sanitizedSegments)
    }

    override suspend fun openReadable(path: String): WebFileReadableNativeImpl? =
        createReadStream(path)

    override suspend fun openWritable(path: String, append: Boolean): WebFileWritableNativeImpl =
        createWriteStream(path, if (append) FileOpenMode.WRITE_APPEND else FileOpenMode.WRITE_TRUNCATE)

    override suspend fun createReadStream(path: String): WebFileReadableNativeImpl? {
        val bytes = readFile(path) ?: return null
        return WebFileReadableNativeImpl(initialBytes = bytes, filePath = normalizeKey(path))
    }

    override suspend fun createWriteStream(
        path: String,
        mode: FileOpenMode
    ): WebFileWritableNativeImpl {
        val norm = normalizeKey(path)
        val append = mode == FileOpenMode.WRITE_APPEND
        val initial = if (append) files[norm] ?: ByteArray(0) else ByteArray(0)
        return WebFileWritableNativeImpl(
            initialBytes = initial,
            filePath = norm,
            appendMode = append,
            onCommit = { committed ->
                writeFile(norm, committed)
            }
        )
    }

    override suspend fun openReadableFromFileDialog(
        title: String,
        initialDirectory: String?,
        filters: List<FileTypeFilter>
    ): Pair<String, FileReadableNativeImpl>? {
        val first = files.entries.firstOrNull() ?: return null
        return first.key to WebFileReadableNativeImpl(initialBytes = first.value.copyOf(), filePath = first.key)
    }

    override suspend fun openReadablesFromFileDialog(
        title: String,
        initialDirectory: String?,
        filters: List<FileTypeFilter>
    ): List<Pair<String, FileReadableNativeImpl>> =
        openReadableFromFileDialog(title, initialDirectory, filters)?.let { listOf(it) } ?: emptyList()

    override suspend fun openWritableFromFileDialog(
        title: String,
        initialDirectory: String?,
        suggestedFileName: String,
        filters: List<FileTypeFilter>
    ): Pair<String, FileWritableNativeImpl>? {
        val target = resolve(initialDirectory ?: "/", suggestedFileName.ifEmpty { "untitled.bin" })
        return target to createWriteStream(target, FileOpenMode.WRITE_TRUNCATE)
    }
}
