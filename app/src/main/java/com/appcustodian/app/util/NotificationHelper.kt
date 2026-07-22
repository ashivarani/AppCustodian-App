package com.appcustodian.app.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.appcustodian.app.MainActivity

/**
 * Utility helper responsible for creating notification channels and posting system tray notifications
 * regarding force-stop outcomes (e.g., success or unavailability).
 */
object NotificationHelper {

    /** Unique notification channel identifier for force-stop execution results. */
    private const val CHANNEL_ID = "force_stop_results"

    /** User-visible name string for the force-stop results notification channel. */
    private const val CHANNEL_NAME = "Force stop results"

    /** Base integer offset for generating deterministic notification IDs from package name hash codes. */
    private const val NOTIFICATION_ID_BASE = 9100

    /**
     * Ensures that the required [NotificationChannel] exists in the system settings.
     * Safe to call repeatedly as it checks for existing channel presence before creation.
     *
     * @param context Application context used to retrieve the [NotificationManager].
     */
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Lets you know when a force-stop can't be completed for an app."
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Constructs a [PendingIntent] that brings [MainActivity] to the foreground when tapping a notification.
     *
     * @param context Application context used to create the intent.
     * @param requestCode Unique request code associated with the pending intent.
     * @return A configured [PendingIntent] instance.
     */
    private fun openAppPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Verifies whether the application currently holds notification posting permissions.
     * Always returns `true` on Android 12 (API 32) and below.
     *
     * @param context Application context used to inspect permissions.
     * @return `true` if notifications can be posted, `false` otherwise.
     */
    private fun hasPostPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Safely dispatches a system notification wrapped in explicit permission checks and exception guards.
     *
     * @param context Application context used to post notifications.
     * @param id Unique notification ID.
     * @param notification Built [android.app.Notification] instance ready for dispatch.
     */
    private fun safeNotify(context: Context, id: Int, notification: android.app.Notification) {
        if (!hasPostPermission(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Guards against runtime SecurityExceptions if permission was revoked mid-execution.
        }
    }

    /**
     * Displays a warning notification indicating that an application could not be force-stopped.
     *
     * @param context Application context used to post the notification.
     * @param appName User-facing label of the target app.
     * @param packageName Package identifier used to compute the notification ID.
     * @param reason Human-readable failure description.
     */
    fun showNotAvailable(context: Context, appName: String, packageName: String, reason: String) {
        ensureChannel(context)

        val id = NOTIFICATION_ID_BASE + packageName.hashCode()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Force stop not available for $appName")
            .setContentText(reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent(context, id))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        safeNotify(context, id, notification)
    }

    /**
     * Displays a confirmation notification indicating that an application was successfully force-stopped.
     *
     * @param context Application context used to post the notification.
     * @param appName User-facing label of the target app.
     * @param packageName Package identifier used to compute the notification ID.
     */
    fun showStopped(context: Context, appName: String, packageName: String) {
        ensureChannel(context)

        val id = NOTIFICATION_ID_BASE + packageName.hashCode()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
            .setContentTitle("$appName force-stopped")
            .setContentText("It has been stopped.")
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent(context, id))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        safeNotify(context, id, notification)
    }
}