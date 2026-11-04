package com.nexustools.genericui.web

import com.nexustools.genericui.io.ByteBuffer
import com.nexustools.genericui.nativeimpl.io.FileReadableNativeImpl
import com.nexustools.genericui.nativeimpl.io.FileWritableNativeImpl
import com.nexustools.genericui.nativeimpl.system.UserDirectoryNativeImpl

/**
 * Web (`:web`, `js` + `wasmJs`) implementation of [UserDirectoryNativeImpl] backed directly by
 * browser `window.indexedDB` (via [WebBrowserDomBridge] on dedicated IndexedDB database
 * `"genericui_user_dirs:<appName>"` with object stores `"config"` and `"cache"`) **without** using
 * [com.nexustools.genericui.util.Database] or `window.localStorage`.
 */
class WebUserDirectoryNativeImpl(
    override val appName: String = "GenericUI",
    override val subdirectory: String = "",
    val scheme: String = "config",
    val kind: String = scheme
) : UserDirectoryNativeImpl {

    override val fileSeparator: Char = '/'

    override val usesFileSystemBasePath: Boolean = false

    private val cleanApp: String = appName.trim().ifEmpty { "GenericUI" }
    private val cleanSub: String = subdirectory.trim().trim('/', '\\')
    private val effectiveKind: String = if (kind != scheme) kind else scheme

    /**
     * Object store name inside [indexedDbDatabaseName]: `"cache"` for [com.nexustools.genericui.util.CacheDirectory]
     * or `"config"` for [com.nexustools.genericui.util.ConfigDirectory].
     */
    val storageTableName: String =
        if (effectiveKind.equals("cache", ignoreCase = true)) "cache" else "config"

    /**
     * Dedicated browser IndexedDB database name (`"genericui_user_dirs:<appName>"`) used directly
     * by `ConfigDirectory` and `CacheDirectory`, strictly disjoint from `Database` (`"genericui_db:..."`).
     */
    val indexedDbDatabaseName: String = "genericui_user_dirs:$cleanApp"

    override val directoryPath: String =
        if (cleanSub.isEmpty()) "$storageTableName://$cleanApp" else "$storageTableName://$cleanApp/$cleanSub"

    override var isDisposed: Boolean = false
        private set

    override fun ensureDirectoryExists(): Boolean = !isDisposed

    override fun resolveFilePath(fileName: String): String {
        val clean = sanitizeFileName(fileName)
        return "$directoryPath/$clean"
    }

    override fun writeBytes(fileName: String, bytes: ByteArray): Boolean {
        if (isDisposed) return false
        val key = objectStoreKey(fileName)
        return WebBrowserDomBridge.idbUserDirPut(
            dbNameOrApp = indexedDbDatabaseName,
            storeName = storageTableName,
            key = key,
            bytes = bytes.copyOf()
        )
    }

    override fun readBytes(fileName: String): ByteArray? {
        if (isDisposed) return null
        val key = objectStoreKey(fileName)
        return WebBrowserDomBridge.idbUserDirGet(
            dbNameOrApp = indexedDbDatabaseName,
            storeName = storageTableName,
            key = key
        )
    }

    override fun writeText(fileName: String, text: String): Boolean =
        writeBytes(fileName, text.encodeToByteArray())

    override fun readText(fileName: String): String? =
        readBytes(fileName)?.decodeToString()

    override fun exists(fileName: String): Boolean {
        if (isDisposed) return false
        val key = objectStoreKey(fileName)
        return WebBrowserDomBridge.idbUserDirGet(
            dbNameOrApp = indexedDbDatabaseName,
            storeName = storageTableName,
            key = key
        ) != null
    }

    fun fileExists(fileName: String): Boolean = exists(fileName)

    override fun fileSize(fileName: String): Long {
        if (isDisposed) return 0L
        return readBytes(fileName)?.size?.toLong() ?: 0L
    }

    override fun delete(fileName: String): Boolean {
        if (isDisposed) return false
        val key = objectStoreKey(fileName)
        return WebBrowserDomBridge.idbUserDirDelete(
            dbNameOrApp = indexedDbDatabaseName,
            storeName = storageTableName,
            key = key
        )
    }

    fun deleteFile(fileName: String): Boolean = delete(fileName)

    override fun listFiles(): List<String> {
        if (isDisposed) return emptyList()
        val prefix = if (cleanSub.isEmpty()) "" else "$cleanSub/"
        val rawKeys = WebBrowserDomBridge.idbUserDirKeys(
            dbNameOrApp = indexedDbDatabaseName,
            storeName = storageTableName,
            prefix = prefix
        )
        val out = ArrayList<String>(rawKeys.size)
        for (raw in rawKeys) {
            val leaf = if (prefix.isEmpty()) raw else raw.removePrefix(prefix)
            if (leaf.isNotEmpty() && !leaf.contains('/') && !leaf.startsWith(".")) {
                out.add(leaf)
            }
        }
        return out
    }

    override fun clearAll(): Int {
        if (isDisposed) return 0
        if (cleanSub.isNotEmpty()) {
            return WebBrowserDomBridge.idbUserDirClear(
                dbNameOrApp = indexedDbDatabaseName,
                storeName = storageTableName,
                prefix = "$cleanSub/"
            )
        }
        val files = listFiles()
        var removed = 0
        for (file in files) {
            if (delete(file)) {
                removed++
            }
        }
        return removed
    }

    fun clearDirectory(): Int = clearAll()

    override fun totalByteSize(): Long {
        if (isDisposed) return 0L
        var total = 0L
        for (name in listFiles()) {
            total += fileSize(name)
        }
        return total
    }

    override suspend fun readAsync(fileName: String): ByteBuffer? =
        readBytes(fileName)?.let { ByteBuffer.wrap(it) }

    override suspend fun writeAsync(
        fileName: String,
        buffer: ByteBuffer,
        append: Boolean
    ): Boolean {
        if (isDisposed) return false
        val incoming = buffer.toByteArray()
        return if (append) {
            val existing = readBytes(fileName)
            val merged = if (existing == null || existing.isEmpty()) incoming else existing + incoming
            writeBytes(fileName, merged)
        } else {
            writeBytes(fileName, incoming)
        }
    }

    override suspend fun openReadableAsync(fileName: String): FileReadableNativeImpl? {
        val bytes = readBytes(fileName) ?: return null
        return WebFileReadableNativeImpl(initialBytes = bytes, filePath = resolveFilePath(fileName))
    }

    override suspend fun openWritableAsync(
        fileName: String,
        append: Boolean
    ): FileWritableNativeImpl {
        val initial = if (append) readBytes(fileName) ?: ByteArray(0) else ByteArray(0)
        return WebFileWritableNativeImpl(
            initialBytes = initial,
            filePath = resolveFilePath(fileName),
            appendMode = append,
            onCommit = { committed ->
                writeBytes(fileName, committed)
            }
        )
    }

    override fun dispose() {
        isDisposed = true
    }

    private fun objectStoreKey(rawFileName: String): String {
        val cleanFile = sanitizeFileName(rawFileName)
        return if (cleanSub.isEmpty()) cleanFile else "$cleanSub/$cleanFile"
    }

    private fun sanitizeFileName(raw: String): String {
        val trimmed = raw.trim().trim('/', '\\').ifEmpty { "unnamed" }
        val withoutScheme = if (trimmed.startsWith("$directoryPath/")) {
            trimmed.removePrefix("$directoryPath/")
        } else {
            trimmed
        }
        return withoutScheme.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }
}
