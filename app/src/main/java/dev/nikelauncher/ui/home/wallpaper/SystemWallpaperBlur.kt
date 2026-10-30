// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.wallpaper

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.PixelFormat
import android.os.Build
import android.view.WindowManager
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.function.Consumer

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext === this) null else baseContext.activity()
    else -> null
}

/** System compositor support can change at runtime, e.g. in battery saver. */
@Composable
internal fun rememberSystemBlurAvailable(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(WindowManager::class.java) }
    var available by remember(manager) {
        mutableStateOf(Build.VERSION.SDK_INT >= 31 && manager?.isCrossWindowBlurEnabled == true)
    }
    DisposableEffect(manager) {
        if (Build.VERSION.SDK_INT < 31 || manager == null) {
            available = false
            onDispose { }
        } else {
            val listener = Consumer<Boolean> { available = it }
            available = manager.isCrossWindowBlurEnabled
            manager.addCrossWindowBlurEnabledListener(context.mainExecutor, listener)
            onDispose { manager.removeCrossWindowBlurEnabledListener(listener) }
        }
    }
    return available
}

/** Blur the compositor's actual wallpaper, including its system crop and position. */
@Composable
internal fun SystemWallpaperBlur(enabled: Boolean, strength: Int) {
    val activity = LocalContext.current.activity()
    val available = rememberSystemBlurAvailable()
    val window = activity?.window
    val currentStrength by rememberUpdatedState(strength)
    DisposableEffect(window, enabled, available) {
        if (Build.VERSION.SDK_INT >= 31 && window != null && enabled && available) {
            val attrs = window.attributes
            val oldRadius = attrs.blurBehindRadius
            val oldFormat = attrs.format
            val hadBlur = attrs.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND != 0
            var active = true
            fun applyBlur() {
                if (!active) return
                val current = window.attributes
                val radius = (currentStrength.coerceIn(0, 100) * 1.5f).toInt()
                if (current.blurBehindRadius != radius ||
                    current.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND == 0) {
                    current.setBlurBehindRadius(radius)
                    current.flags = current.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                    window.attributes = current
                }
            }
            window.setFormat(PixelFormat.TRANSLUCENT)
            applyBlur()
            val decor = window.decorView
            val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
                if (focused) applyBlur()
            }
            decor.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
            // Reapply after the opening frame in case another window update races the gesture.
            decor.post { applyBlur() }
            onDispose {
                active = false
                if (decor.viewTreeObserver.isAlive) decor.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
                val current = window.attributes
                current.setBlurBehindRadius(oldRadius)
                window.attributes = current
                if (!hadBlur) window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.setFormat(oldFormat)
            }
        } else onDispose { }
    }
    SideEffect {
        if (Build.VERSION.SDK_INT >= 31 && window != null && enabled && available) {
            val attrs = window.attributes
            val radius = (strength.coerceIn(0, 100) * 1.5f).toInt()
            if (attrs.blurBehindRadius != radius ||
                attrs.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND == 0) {
                attrs.setBlurBehindRadius(radius)
                attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                window.attributes = attrs
            }
        }
    }
}
