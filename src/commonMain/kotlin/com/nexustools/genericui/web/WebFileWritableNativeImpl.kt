package com.nexustools.genericui.web

import com.nexustools.genericui.io.ByteOrder
import com.nexustools.genericui.nativeimpl.io.DefaultFileWritableNativeImpl

/**
 * Web (`:web`, `js` + `wasmJs`) implementation of [com.nexustools.genericui.nativeimpl.io.FileWritableNativeImpl]
 * backed by browser `WritableStreamDefaultWriter.write()` Promises / `FileSystemWritableFileStream`.
 */
class WebFileWritableNativeImpl(
    initialBytes: ByteArray = ByteArray(0),
    filePath: String? = null,
    appendMode: Boolean = false,
    byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN,
    onWriteChunk: ((ByteArray, Int, Int) -> Int)? = null,
    onFlushCallback: (suspend (ByteArray) -> Unit)? = null,
    onCloseCallback: (suspend () -> Unit)? = null,
    onCommit: (suspend (ByteArray) -> Unit)? = null
) : DefaultFileWritableNativeImpl(
    initialBytes = initialBytes,
    filePath = filePath,
    appendMode = appendMode,
    byteOrder = byteOrder,
    onWriteChunk = onWriteChunk,
    onFlushCallback = onFlushCallback,
    onCloseCallback = onCloseCallback,
    onCommit = onCommit
) {
    val nativeApi: String = "Browser_WritableStreamDefaultWriter_FileSystemWritableFileStream"

    var flushCount: Int = 0
        private set

    override suspend fun flush() {
        if (!isDisposed) {
            flushCount++
        }
        super.flush()
    }
}
