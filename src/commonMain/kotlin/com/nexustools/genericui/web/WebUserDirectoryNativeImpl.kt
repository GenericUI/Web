package com.nexustools.genericui.web

import com.nexustools.genericui.nativeimpl.system.UserDirectoryNativeImpl

/**
 * Web (`:web`, `js` + `wasmJs`) implementation of [UserDirectoryNativeImpl] backed by
 * `window.localStorage` (via [WebBrowserDomBridge]) with Base64 byte encoding under a
 * per-directory namespace (`genericui:<scheme>:<appName>:<subdirectory>:<fileName>`).
 */
class WebUserDirectoryNativeImpl(
    override val appName: String = "GenericUI",
    override val subdirectory: String = "",
    val scheme: String = "config"
) : UserDirectoryNativeImpl {

    override val fileSeparator: Char = '/'

    private val cleanApp = appName.trim().ifEmpty { "GenericUI" }
    private val cleanSub = subdirectory.trim().trim('/', '\\')

    override val directoryPath: String =
        if (cleanSub.isEmpty()) "$scheme://$cleanApp" else "$scheme://$cleanApp/$cleanSub"

    private val storagePrefix: String =
        if (cleanSub.isEmpty()) "genericui:$scheme:$cleanApp:" else "genericui:$scheme:$cleanApp:$cleanSub:"

    override var isDisposed: Boolean = false
        private set

    private val fallbackEntries = LinkedHashMap<String, ByteArray>()

    override fun ensureDirectoryExists(): Boolean = !isDisposed

    override fun resolveFilePath(fileName: String): String {
        val clean = sanitizeFileName(fileName)
        return "$directoryPath/$clean"
    }

    override fun writeBytes(fileName: String, bytes: ByteArray): Boolean {
        if (isDisposed) return false
        val clean = sanitizeFileName(fileName)
        fallbackEntries[clean] = bytes.copyOf()
        WebBrowserDomBridge.localStorageSetItem(storageKey(clean), encodeBase64(bytes))
        return true
    }

    override fun readBytes(fileName: String): ByteArray? {
        if (isDisposed) return null
        val clean = sanitizeFileName(fileName)
        val raw = WebBrowserDomBridge.localStorageGetItem(storageKey(clean))
        if (raw != null) {
            val decoded = decodeBase64(raw)
            fallbackEntries[clean] = decoded
            return decoded
        }
        return fallbackEntries[clean]?.copyOf()
    }

    override fun exists(fileName: String): Boolean {
        if (isDisposed) return false
        val clean = sanitizeFileName(fileName)
        return WebBrowserDomBridge.localStorageGetItem(storageKey(clean)) != null ||
            fallbackEntries.containsKey(clean)
    }

    override fun fileSize(fileName: String): Long {
        if (isDisposed) return 0L
        return readBytes(fileName)?.size?.toLong() ?: 0L
    }

    override fun delete(fileName: String): Boolean {
        if (isDisposed) return false
        val clean = sanitizeFileName(fileName)
        val existed = exists(clean)
        fallbackEntries.remove(clean)
        WebBrowserDomBridge.localStorageRemoveItem(storageKey(clean))
        return existed
    }

    override fun listFiles(): List<String> {
        if (isDisposed) return emptyList()
        val names = LinkedHashSet<String>()
        for (k in WebBrowserDomBridge.localStorageKeys()) {
            if (k.startsWith(storagePrefix)) {
                val leaf = k.removePrefix(storagePrefix)
                if (leaf.isNotEmpty() && !leaf.startsWith(".")) {
                    names.add(leaf)
                }
            }
        }
        names.addAll(fallbackEntries.keys.filter { !it.startsWith(".") })
        return names.toList()
    }

    override fun clearAll(): Int {
        if (isDisposed) return 0
        val all = listFiles()
        for (name in all) {
            WebBrowserDomBridge.localStorageRemoveItem(storageKey(name))
        }
        fallbackEntries.clear()
        return all.size
    }

    override fun totalByteSize(): Long {
        if (isDisposed) return 0L
        var total = 0L
        for (name in listFiles()) {
            total += fileSize(name)
        }
        return total
    }

    override fun dispose() {
        isDisposed = true
        fallbackEntries.clear()
    }

    private fun storageKey(cleanFileName: String): String = "$storagePrefix$cleanFileName"

    private fun sanitizeFileName(raw: String): String {
        val trimmed = raw.trim().trim('/', '\\').ifEmpty { "unnamed" }
        return trimmed.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    private fun encodeBase64(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val out = StringBuilder(((bytes.size + 2) / 3) * 4)
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else 0
            val triple = (b0 shl 16) or (b1 shl 8) or b2
            out.append(table[(triple ushr 18) and 0x3F])
            out.append(table[(triple ushr 12) and 0x3F])
            out.append(if (i + 1 < bytes.size) table[(triple ushr 6) and 0x3F] else '=')
            out.append(if (i + 2 < bytes.size) table[triple and 0x3F] else '=')
            i += 3
        }
        return out.toString()
    }

    private fun decodeBase64(encoded: String): ByteArray {
        val clean = encoded.filter { !it.isWhitespace() }
        if (clean.isEmpty()) return ByteArray(0)
        val table = IntArray(128) { -1 }
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        for (idx in chars.indices) {
            table[chars[idx].code] = idx
        }
        val out = ArrayList<Byte>((clean.length * 3) / 4)
        var i = 0
        while (i + 3 < clean.length) {
            val c0 = clean[i].code.let { if (it < 128) table[it] else -1 }
            val c1 = clean[i + 1].code.let { if (it < 128) table[it] else -1 }
            val c2 = if (clean[i + 2] == '=') -2 else clean[i + 2].code.let { if (it < 128) table[it] else -1 }
            val c3 = if (clean[i + 3] == '=') -2 else clean[i + 3].code.let { if (it < 128) table[it] else -1 }
            if (c0 < 0 || c1 < 0) break
            val b0 = (c0 shl 2) or (c1 ushr 4)
            out.add((b0 and 0xFF).toByte())
            if (c2 >= 0) {
                val b1 = ((c1 and 0x0F) shl 4) or (c2 ushr 2)
                out.add((b1 and 0xFF).toByte())
                if (c3 >= 0) {
                    val b2 = ((c2 and 0x03) shl 6) or c3
                    out.add((b2 and 0xFF).toByte())
                }
            }
            i += 4
        }
        return out.toByteArray()
    }
}
