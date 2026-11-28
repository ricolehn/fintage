package io.github.ricolehn.fintage.util

import android.graphics.Bitmap
import android.util.LruCache
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.withSign

/**
 * Decodes the BlurHash strings Jellyfin sends for every image. They give each card a soft placeholder
 * in its own colors while the image loads, and the glow behind the cards of the detail pages.
 */
object BlurHash {
    private const val CHARS =
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#$%*+,-.:;=?@[]^_{|}~"

    private val cache = LruCache<String, Bitmap>(96)

    fun decode(hash: String?, width: Int = 32, height: Int = 32, punch: Float = 1f): Bitmap? {
        if (hash.isNullOrEmpty() || hash.length < 6) return null
        val cacheKey = "$hash:$width:$height:$punch"
        cache.get(cacheKey)?.let { return it }
        val bitmap = runCatching { decodeInternal(hash, width, height, punch) }.getOrNull() ?: return null
        cache.put(cacheKey, bitmap)
        return bitmap
    }

    /** The average color of the image as ARGB, from the first component of the hash. */
    fun averageColor(hash: String?): Int? {
        if (hash.isNullOrEmpty() || hash.length < 6) return null
        return runCatching {
            val value = decode83(hash, 2, 6)
            (0xFF shl 24) or value
        }.getOrNull()
    }

    private fun decodeInternal(hash: String, width: Int, height: Int, punch: Float): Bitmap? {
        val sizeFlag = decode83(hash, 0, 1)
        val numY = sizeFlag / 9 + 1
        val numX = sizeFlag % 9 + 1
        if (hash.length != 4 + 2 * numX * numY) return null
        val maxAc = (decode83(hash, 1, 2) + 1) / 166f
        val colors = Array(numX * numY) { i ->
            if (i == 0) {
                val value = decode83(hash, 2, 6)
                floatArrayOf(srgbToLinear(value shr 16), srgbToLinear((value shr 8) and 255), srgbToLinear(value and 255))
            } else {
                val value = decode83(hash, 4 + i * 2, 6 + i * 2)
                val scale = maxAc * punch
                floatArrayOf(
                    signedPow2((value / (19 * 19) - 9) / 9f) * scale,
                    signedPow2(((value / 19) % 19 - 9) / 9f) * scale,
                    signedPow2((value % 19 - 9) / 9f) * scale,
                )
            }
        }
        val cosX = Array(width) { x -> FloatArray(numX) { i -> cos(PI * x * i / width).toFloat() } }
        val cosY = Array(height) { y -> FloatArray(numY) { j -> cos(PI * y * j / height).toFloat() } }
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0f
                var g = 0f
                var b = 0f
                for (j in 0 until numY) {
                    for (i in 0 until numX) {
                        val basis = cosX[x][i] * cosY[y][j]
                        val color = colors[j * numX + i]
                        r += color[0] * basis
                        g += color[1] * basis
                        b += color[2] * basis
                    }
                }
                pixels[y * width + x] =
                    (0xFF shl 24) or (linearToSrgb(r) shl 16) or (linearToSrgb(g) shl 8) or linearToSrgb(b)
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun decode83(text: String, from: Int, to: Int): Int {
        var value = 0
        for (i in from until to) {
            val index = CHARS.indexOf(text[i])
            require(index >= 0) { "Invalid BlurHash" }
            value = value * 83 + index
        }
        return value
    }

    private fun signedPow2(value: Float): Float = value.pow(2).withSign(value)

    private fun srgbToLinear(value: Int): Float {
        val v = value / 255f
        return if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
    }

    private fun linearToSrgb(value: Float): Int {
        val v = value.coerceIn(0f, 1f)
        val srgb = if (v <= 0.0031308f) v * 12.92f else 1.055f * v.pow(1 / 2.4f) - 0.055f
        return (srgb * 255 + 0.5f).toInt().coerceIn(0, 255)
    }
}
