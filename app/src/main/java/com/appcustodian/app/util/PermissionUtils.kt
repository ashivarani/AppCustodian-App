package com.appcustodian.app.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.text.TextUtils

/**
 * Utility helper for evaluating system permissions and constructing intents
 * for system settings screens.
 */
object PermissionUtils {

    /**
     * Checks if the user has granted "Usage Access" (`PACKAGE_USAGE_STATS`) permission.
     *
     * Used primarily for sorting installed apps by recent usage timestamps.
     *
     * @param context Application context used to query [AppOpsManager].
     * @return `true` if "Usage Access" is granted, `false` otherwise.
     */
    @Suppress("DEPRECATION")
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            // Deprecated checkOpNoThrow fallback required for Android 8-9 (APIs 26-28)
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Verifies if the specified Accessibility Service component is actively enabled by the user.
     *
     * @param context Application context used to access content resolvers.
     * @param serviceClassName The target accessibility service class name.
     * @return `true` if the accessibility service is enabled, `false` otherwise.
     */
    fun isAccessibilityServiceEnabled(context: Context, serviceClassName: String): Boolean {
        val expectedComponent = "${context.packageName}/$serviceClassName"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equals(expectedComponent, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    /**
     * Creates an [Intent] to open the system Accessibility Settings page.
     *
     * @return An intent targeting [Settings.ACTION_ACCESSIBILITY_SETTINGS].
     */
    fun accessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    /**
     * Creates an [Intent] directing to the system App Info page for a target package.
     *
     * Launched without `FLAG_ACTIVITY_NEW_TASK` so the settings page lands on top
     * of App Custodian's current activity back stack.
     *
     * @param packageName The package identifier to view details for.
     * @return A configured [Intent] targeting [Settings.ACTION_APPLICATION_DETAILS_SETTINGS].
     */
    fun appInfoIntent(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", packageName, null)
        }
}