// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Make a portable, bounded image for the cached list background. This never alters the source.
 * The picker grant is only needed during import; the returned file belongs to this app and is
 * embedded by the existing backup format. PNG preserves transparency and avoids recompression.
 */
internal suspend fun importAppListBackground(context: Context, uri: Uri): Uri? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    var file: File? = null
    try {
        require(uri.scheme == "content" || uri.scheme == "file")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        requireNotNull(resolver.openInputStream(uri)).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0 && bounds.outWidth.toLong() * bounds.outHeight <= 40_000_000L)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val decoded = requireNotNull(resolver.openInputStream(uri)).use { BitmapFactory.decodeStream(it, null, options) }
        requireNotNull(decoded)
        val orientation = runCatching {
            requireNotNull(resolver.openInputStream(uri)).use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
        val oriented = if (matrix.isIdentity) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        val scale = (1024f / max(oriented.width, oriented.height)).coerceAtMost(1f)
        val resized = Bitmap.createScaledBitmap(oriented, (oriented.width * scale).roundToInt().coerceAtLeast(1),
            (oriented.height * scale).roundToInt().coerceAtLeast(1), true)
        val directory = File(context.filesDir, "app_list_backgrounds").apply { check(isDirectory || mkdirs()) }
        file = File(directory, UUID.randomUUID().toString() + ".png")
        file.outputStream().buffered().use { check(resized.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        require(file.length() in 1..dev.nikelauncher.data.backup.BackupValues.MAX_ASSET_BYTES.toLong())
        Uri.fromFile(file)
    } catch (_: Exception) {
        file?.delete()
        null
    }
}
