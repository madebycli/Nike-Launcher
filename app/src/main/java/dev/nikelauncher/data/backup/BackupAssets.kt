// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data.backup

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.util.Base64
import dev.nikelauncher.data.PREF_FOLDERS
import dev.nikelauncher.data.PREF_ICON_OVERRIDES
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

/** A file a setting points at, carried inside the backup so the setting still works elsewhere. */
internal data class BackupAsset(val kind: String, val bytes: ByteArray)

internal const val ASSET_FONT = "font"
internal const val ASSET_IMAGE = "image"
internal const val MAX_ASSET_TOTAL_BYTES = 40 * 1024 * 1024

private const val PREF_FONT_FILE = "font_file"

internal fun digest(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

/** Reads at most [limit] bytes and refuses anything longer, so a hostile file cannot fill memory. */
internal fun readBounded(input: InputStream, limit: Int = BackupValues.MAX_FILE_BYTES): ByteArray {
    require(limit >= 0)
    val result = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        require(result.size() <= limit - count)
        result.write(buffer, 0, count)
    }
    return result.toByteArray()
}

internal fun readAssets(array: JSONArray, context: Context): Map<String, BackupAsset> {
    require(array.length() <= BackupValues.MAX_ASSETS)
    var total = 0
    return buildMap {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val id = item.getString("id")
            require(id.matches(Regex("[0-9a-f]{64}")) && id !in this)
            val kind = item.getString("kind")
            val encoded = item.getString("data")
            require(encoded.length <= BackupValues.MAX_ASSET_BYTES * 4 / 3 + 8)
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            total += bytes.size
            require(bytes.size <= BackupValues.MAX_ASSET_BYTES && total <= MAX_ASSET_TOTAL_BYTES && digest(bytes) == id)
            validateAsset(context, bytes, kind)
            put(id, BackupAsset(kind, bytes))
        }
    }
}

/** An asset is decoded once before it is accepted, so a file that is not what it says never lands. */
internal fun validateAsset(context: Context, bytes: ByteArray, kind: String) {
    require(bytes.isNotEmpty())
    when (kind) {
        ASSET_IMAGE -> {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0 && bounds.outWidth.toLong() * bounds.outHeight <= 40_000_000)
        }
        ASSET_FONT -> {
            val file = File.createTempFile("font-validation-", ".font", context.cacheDir)
            try {
                file.writeBytes(bytes)
                Typeface.createFromFile(file)
            } finally {
                file.delete()
            }
        }
        else -> error("Unsupported asset kind")
    }
}

/** Swaps every file reference in [values] for what [replace] returns, given the reference and its kind. */
internal fun rewriteAssets(values: MutableMap<String, Any>, replace: (String, String) -> String) {
    (values[PREF_FONT_FILE] as? String)?.let { values[PREF_FONT_FILE] = replace(it, ASSET_FONT) }
    (values[PREF_ICON_OVERRIDES] as? String)?.let { raw ->
        val icons = JSONObject(raw)
        icons.keys().forEach { key -> icons.put(key, replace(icons.getString(key), ASSET_IMAGE)) }
        values[PREF_ICON_OVERRIDES] = icons.toString()
    }
    (values[PREF_FOLDERS] as? String)?.let { raw ->
        val folders = JSONArray(raw)
        for (index in 0 until folders.length()) {
            val folder = folders.getJSONObject(index)
            folder.optString("icon").takeIf { it.isNotBlank() }?.let { folder.put("icon", replace(it, ASSET_IMAGE)) }
        }
        values[PREF_FOLDERS] = folders.toString()
    }
}
