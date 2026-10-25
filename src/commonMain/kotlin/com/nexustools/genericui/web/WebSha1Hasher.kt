package com.nexustools.genericui.web

/**
 * Pure Kotlin Multiplatform RFC 3174 SHA-1 digest utility used by `:web` (`WebHtmlTemplateEngine`
 * and `WebWasmJsLoader`) to append cache-busting `?<sha1>` query parameters to script URLs
 * (`{{BOOTSTRAP_JS_URL}}?{{BOOTSTRAP_JS_HASH}}`, loader scripts, and Wasm/JS IR bundles).
 */
object WebSha1Hasher {

    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    /**
     * Computes the 40-character lowercase hexadecimal SHA-1 hash of the UTF-8 encoding of [text].
     */
    fun sha1Hex(text: String): String = sha1Hex(text.encodeToByteArray())

    /**
     * Computes the 40-character lowercase hexadecimal SHA-1 hash of [bytes].
     */
    fun sha1Hex(bytes: ByteArray): String {
        val digest = sha1Bytes(bytes)
        val out = CharArray(40)
        for (i in 0 until 20) {
            val v = digest[i].toInt() and 0xFF
            out[i * 2] = HEX_CHARS[v ushr 4]
            out[i * 2 + 1] = HEX_CHARS[v and 0x0F]
        }
        return out.concatToString()
    }

    /**
     * Appends `?<sha1>` to [url] if not already present.
     */
    fun appendHashQuery(url: String, sha1Hash: String): String {
        val trimmedUrl = url.trim()
        val trimmedHash = sha1Hash.trim()
        if (trimmedUrl.isEmpty() || trimmedHash.isEmpty()) return trimmedUrl
        return if ('?' in trimmedUrl) {
            "$trimmedUrl&$trimmedHash"
        } else {
            "$trimmedUrl?$trimmedHash"
        }
    }

    private fun sha1Bytes(message: ByteArray): ByteArray {
        val msgLen = message.size
        val bitLen = msgLen.toLong() * 8L
        val totalLen = ((msgLen + 8) / 64 + 1) * 64
        val padded = ByteArray(totalLen)
        message.copyInto(padded, 0, 0, msgLen)
        padded[msgLen] = 0x80.toByte()
        for (i in 0 until 8) {
            padded[totalLen - 8 + i] = ((bitLen ushr ((7 - i) * 8)) and 0xFFL).toByte()
        }

        var h0 = 0x67452301
        var h1 = -0x10325477 // 0xEFCDAB89
        var h2 = -0x67452302 // 0x98BADCFE
        var h3 = 0x10325476
        var h4 = -0x3c2d1e10 // 0xC3D2E1F0

        val w = IntArray(80)
        var chunkOffset = 0
        while (chunkOffset < totalLen) {
            for (i in 0 until 16) {
                val idx = chunkOffset + i * 4
                w[i] = ((padded[idx].toInt() and 0xFF) shl 24) or
                    ((padded[idx + 1].toInt() and 0xFF) shl 16) or
                    ((padded[idx + 2].toInt() and 0xFF) shl 8) or
                    (padded[idx + 3].toInt() and 0xFF)
            }
            for (i in 16 until 80) {
                val x = w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]
                w[i] = (x shl 1) or (x ushr 31)
            }

            var a = h0
            var b = h1
            var c = h2
            var d = h3
            var e = h4

            for (i in 0 until 80) {
                val f: Int
                val k: Int
                when {
                    i < 20 -> {
                        f = (b and c) or (b.inv() and d)
                        k = 0x5A827999
                    }
                    i < 40 -> {
                        f = b xor c xor d
                        k = 0x6ED9EBA1
                    }
                    i < 60 -> {
                        f = (b and c) or (b and d) or (c and d)
                        k = -0x70e44324 // 0x8F1BBCDC
                    }
                    else -> {
                        f = b xor c xor d
                        k = -0x359d3e2a // 0xCA62C1D6
                    }
                }
                val temp = ((a shl 5) or (a ushr 27)) + f + e + k + w[i]
                e = d
                d = c
                c = (b shl 30) or (b ushr 2)
                b = a
                a = temp
            }

            h0 += a
            h1 += b
            h2 += c
            h3 += d
            h4 += e
            chunkOffset += 64
        }

        val out = ByteArray(20)
        writeIntBigEndian(h0, out, 0)
        writeIntBigEndian(h1, out, 4)
        writeIntBigEndian(h2, out, 8)
        writeIntBigEndian(h3, out, 12)
        writeIntBigEndian(h4, out, 16)
        return out
    }

    private fun writeIntBigEndian(value: Int, dst: ByteArray, offset: Int) {
        dst[offset] = (value ushr 24).toByte()
        dst[offset + 1] = (value ushr 16).toByte()
        dst[offset + 2] = (value ushr 8).toByte()
        dst[offset + 3] = value.toByte()
    }
}
