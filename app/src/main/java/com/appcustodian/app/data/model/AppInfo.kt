package com.appcustodian.app.data.model

import android.graphics.drawable.Drawable

/**
 * Immutable UI model for one installed application.
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val lastUsedTimeMillis: Long = 0L
)

/**
 * Result of an attempted force-stop automation run, surfaced from the
 * Accessibility Service back up to the UI layer.
 */
sealed class ForceStopResult {
    data class Success(val packageName: String) : ForceStopResult()
    data class Failed(val packageName: String, val reason: String) : ForceStopResult()
}