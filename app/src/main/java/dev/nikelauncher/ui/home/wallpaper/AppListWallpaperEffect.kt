// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import dev.nikelauncher.data.AppListBackgroundEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/** A bounded, pre-rendered image. The list never samples the screen or blurs on each frame. */
@Composable
internal fun AppListWallpaperEffect(
    effect: AppListBackgroundEffect,
    strength: Int,
    image: String?,
    edgesOnly: Boolean,
    visible: Boolean,
    /**
     * How much of the system blur is applied, 0 to 1. The compositor's blur is a window setting,
     * so it arrives all at once unless it is walked up: with this the wallpaper softens as the
     * list comes in and clears again as it goes, instead of snapping.
     */
    blurLevel: Float = 1f,
) {
    val context = LocalContext.current
    val systemBlur = rememberSystemBlurAvailable()
    val useSystemBlur = effect == AppListBackgroundEffect.BLUR && systemBlur
    SystemWallpaperBlur(
        enabled = (visible || blurLevel > 0.01f) && useSystemBlur,
        // In steps, because every change is a round trip to the window manager.
        strength = ((strength * blurLevel.coerceIn(0f, 1f)) / 2f).roundToInt() * 2,
    )
    // Kept composed while the list is closed: reopening reuses the last bitmap.
    val result by produceState<Bitmap?>(null, effect, strength, image, useSystemBlur) {
        if (useSystemBlur || effect == AppListBackgroundEffect.NONE || strength <= 0 || image == null) { value = null; return@produceState }
        delay(180) // avoid decoding for every intermediate slider value
        value = withContext(Dispatchers.IO) {
            runCatching {
                val source = decodeSelected(context, Uri.parse(image))
                source?.let { renderEffect(it, effect, strength.coerceIn(0, 100)) }
            }.getOrNull()
        }
    }
    if (visible && !useSystemBlur && result != null && effect != AppListBackgroundEffect.NONE && strength > 0) {
        Image(bitmap = result!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize().then(
            if (edgesOnly) Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(brush = Brush.radialGradient(
                        0f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color.Black,
                        center = Offset(size.width / 2, size.height / 2), radius = size.width.coerceAtLeast(1f) * 0.7f,
                    ), blendMode = BlendMode.DstIn)
                } else Modifier),
            contentScale = ContentScale.Crop,
            filterQuality = if (effect == AppListBackgroundEffect.PIXEL) FilterQuality.None else FilterQuality.Low)
    }
}

private fun decodeSelected(context: Context, uri: Uri): Bitmap? {
    if (uri.scheme != "content" && uri.scheme != "file") return null
    // A file URI is only accepted inside our own storage (e.g. a restored backup).
    if (uri.scheme == "file") {
        val file = java.io.File(uri.path ?: return null).canonicalFile
        if (!file.path.startsWith(context.filesDir.canonicalPath + java.io.File.separator)) return null
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || bounds.outWidth.toLong() * bounds.outHeight > 40_000_000L) return null
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / sample > 512) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
    return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}

internal fun renderEffect(source: Bitmap, effect: AppListBackgroundEffect, strength: Int): Bitmap {
    if (strength <= 0 || effect == AppListBackgroundEffect.NONE) return source
    if (effect == AppListBackgroundEffect.PIXEL) {
        val maxSide = (160 - strength.coerceIn(0, 100) * 1.45f).roundToInt().coerceAtLeast(12)
        val ratio = (maxSide.toFloat() / max(source.width, source.height)).coerceAtMost(1f)
        return Bitmap.createScaledBitmap(source, (source.width * ratio).roundToInt().coerceAtLeast(1),
            (source.height * ratio).roundToInt().coerceAtLeast(1), false)
    }
    if (effect != AppListBackgroundEffect.BLUR || strength <= 0) return source
    // Three box passes approximate a Gaussian, performed once on a bounded bitmap off the UI thread.
    val ratio = (256f / max(source.width, source.height)).coerceAtMost(1f)
    val scaled = Bitmap.createScaledBitmap(source, (source.width * ratio).roundToInt().coerceAtLeast(1),
        (source.height * ratio).roundToInt().coerceAtLeast(1), true)
    val width = scaled.width
    val height = scaled.height
    val pixels = IntArray(width * height)
    val temp = IntArray(pixels.size)
    scaled.getPixels(pixels, 0, width, 0, 0, width, height)
    val radius = (strength.coerceIn(1, 100) * 0.15f).roundToInt().coerceAtLeast(1)
    repeat(3) { boxPass(pixels, temp, width, height, radius, true); boxPass(temp, pixels, width, height, radius, false) }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}

private fun boxPass(input: IntArray, output: IntArray, w: Int, h: Int, radius: Int, horizontal: Boolean) {
    val lines = if (horizontal) h else w
    val length = if (horizontal) w else h
    fun index(line: Int, pos: Int) = if (horizontal) line * w + pos else pos * w + line
    val count = radius * 2 + 1
    for (line in 0 until lines) {
        var a = 0; var r = 0; var g = 0; var b = 0
        fun add(pixel: Int, sign: Int) {
            a += (pixel ushr 24 and 255) * sign; r += (pixel ushr 16 and 255) * sign
            g += (pixel ushr 8 and 255) * sign; b += (pixel and 255) * sign
        }
        for (offset in -radius..radius) add(input[index(line, offset.coerceIn(0, length - 1))], 1)
        for (pos in 0 until length) {
            output[index(line, pos)] = (a / count shl 24) or (r / count shl 16) or (g / count shl 8) or (b / count)
            add(input[index(line, (pos - radius).coerceIn(0, length - 1))], -1)
            add(input[index(line, (pos + radius + 1).coerceIn(0, length - 1))], 1)
        }
    }
}
