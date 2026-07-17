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
 * A handful of well-known packages where the system disables the "Force stop"
 * button entirely (core OS / persistent processes). We hide these from the list
 * rather than let the user tap them and land on a dead-end screen.
 *
 * This list is a best-effort heuristic, not something Android exposes via API —
 * OEMs vary, so treat it as a starting point you may need to extend per device.
 */
private val PROTECTED_PACKAGES = setOf(
    "android",
    "com.android.systemui",
    "com.android.settings",
    "com.android.phone",
    "com.android.shell"
)

class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

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
                        lastUsedTimeMillis = usageMap[app.packageName] ?: 0L
                    )
                }
                .sortedWith(
                    compareByDescending<AppInfo> { it.lastUsedTimeMillis }
                        .thenBy { it.appName.lowercase() }
                )
                .toList()
        }

    private fun getUsageMap(): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.DAYS.toMillis(30)
        return usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end)
            .associate { it.packageName to it.lastTimeUsed }
    }

    fun getLaunchIntent(packageName: String): Intent? =
        packageManager.getLaunchIntentForPackage(packageName)
}