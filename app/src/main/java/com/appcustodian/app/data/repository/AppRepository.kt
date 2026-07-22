package com.appcustodian.app.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.appcustodian.app.data.model.AppInfo
import com.appcustodian.app.util.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Core system packages where "Force stop" is disabled by Android.
 * Excluded to prevent landing the user on un-actionable settings screens.
 */
private val PROTECTED_PACKAGES = setOf(
    "android",
    "com.android.systemui",
    "com.android.settings",
    "com.android.phone",
    "com.android.shell"
)

/**
 * Repository for querying installed applications and usage history.
 */
class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Retrieves installed apps sorted by last used time (if usage access is granted),
     * falling back to alphabetical sorting.
     */
    suspend fun getInstalledApps(includeSystemApps: Boolean): List<AppInfo> =
        withContext(Dispatchers.Default) {
            val usageMap = if (PermissionUtils.hasUsageAccess(context)) getUsageMap() else emptyMap()

            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .asSequence()
                .filter { it.packageName != context.packageName }
                .filter { it.packageName !in PROTECTED_PACKAGES }
                .filter { app ->
                    val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    includeSystemApps || !isSystem
                }
                .map { app ->
                    AppInfo(
                        packageName = app.packageName,
                        appName = app.loadLabel(packageManager).toString(),
                        icon = runCatching { app.loadIcon(packageManager) }.getOrNull(),
                        isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        hasLauncherActivity =
                            packageManager.getLaunchIntentForPackage(app.packageName) != null,
                        lastUsedTimeMillis = usageMap[app.packageName] ?: 0L
                    )
                }
                .sortedWith(
                    compareByDescending<AppInfo> { it.lastUsedTimeMillis }
                        .thenBy { it.appName.lowercase() }
                )
                .toList()
        }

    /**
     * Queries last used timestamps for installed apps over the past 30 days.
     */
    private fun getUsageMap(): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.DAYS.toMillis(30)
        return usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end)
            .associate { it.packageName to it.lastTimeUsed }
    }

    /**
     * Creates a launcher intent for [packageName] configured for a clean cold start.
     */
    fun getLaunchIntent(packageName: String): Intent? =
        packageManager.getLaunchIntentForPackage(packageName)?.apply {
            // Ensure proper task flags for launching outside an Activity
            // and forcing a fresh process launch post-force-stop.
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        }
}