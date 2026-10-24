package com.nexustools.genericui.web

import com.nexustools.genericui.image.*
import com.nexustools.genericui.image.codec.LibJpegCodec
import com.nexustools.genericui.image.codec.LibPngCodec
import com.nexustools.genericui.image.codec.LibWebpCodec
import com.nexustools.genericui.nativeimpl.image.ImageCodecNativeImpl

/**
 * Web browser (`createImageBitmap` / `HTMLImageElement` + `OffscreenCanvas` / `HTMLCanvasElement.toBlob`)
 * implementation of [ImageCodecNativeImpl].
 */
class WebBrowserImageCodecImpl : ImageCodecNativeImpl {
    override val codecBackendName: String = "web-browser-image-bitmap"

    override fun probeMetadata(source: ImageSource): ImageMetadata? {
        source.memoryBuffer?.let { buf ->
            return ImageMetadata(
                width = buf.width,
                height = buf.height,
                format = ImageFormat.RAW_RGBA,
                pixelFormat = buf.pixelFormat,
                colorSpace = buf.colorSpace
            )
        }
        val bytes = source.encodedBytes ?: return null
        if (bytes.isEmpty()) return null
        return when (ImageFormat.detectFromHeader(bytes)) {
            ImageFormat.JPEG -> LibJpegCodec.probeJpegMetadata(bytes)
            ImageFormat.PNG -> LibPngCodec.probePngMetadata(bytes)
            ImageFormat.WEBP -> LibWebpCodec.probeWebpMetadata(bytes)
            else -> null
        }
    }

    override fun decode(source: ImageSource, options: ImageDecodeOptions): ImageBuffer? {
        source.memoryBuffer?.let { return it.copy() }
        val bytes = source.encodedBytes ?: return null
        if (bytes.isEmpty()) return null
        return when (ImageFormat.detectFromHeader(bytes)) {
            ImageFormat.JPEG -> LibJpegCodec.decode(bytes)
            ImageFormat.PNG -> LibPngCodec.decode(bytes)
            ImageFormat.WEBP -> LibWebpCodec.decode(bytes)
            else -> null
        }
    }

    override fun decodeAnimated(source: ImageSource, options: ImageDecodeOptions): AnimatedImage? {
        val frame = decode(source, options) ?: return null
        return AnimatedImage(
            width = frame.width,
            height = frame.height,
            frames = listOf(ImageFrame(0, frame, durationMs = 100))
        )
    }

    override fun encode(buffer: ImageBuffer, options: ImageEncodeOptions): ByteArray? {
        return when (options.format) {
            ImageFormat.JPEG -> LibJpegCodec.encode(buffer, options.quality)
            ImageFormat.PNG -> LibPngCodec.encode(buffer, options.compressionLevel)
            ImageFormat.RAW_RGBA -> buffer.toRgba8888().bytes
            else -> null
        }
    }

    override fun dispose() {}
}
