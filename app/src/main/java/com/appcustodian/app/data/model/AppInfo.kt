package com.appcustodian.app.data.model

import android.graphics.drawable.Drawable

/**
 * Represents an installed application for UI display.
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    /**
     * True if the app has a launchable MAIN/LAUNCHER activity.
     * System components without one are treated as background services,
     * disabling the "Open" action in the UI.
     */
    val hasLauncherActivity: Boolean = true,
    val lastUsedTimeMillis: Long = 0L
)

/**
 * Outcome of an automated force-stop operation performed by the Accessibility Service.
 */
sealed class ForceStopResult {
    data class Success(val packageName: String) : ForceStopResult()
    data class Failed(val packageName: String, val reason: String) : ForceStopResult()
}