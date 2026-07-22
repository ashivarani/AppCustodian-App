package com.appcustodian.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.appcustodian.app.service.ForceStopAccessibilityService
import com.appcustodian.app.ui.AppListViewModel
import com.appcustodian.app.ui.screens.AppListScreen
import com.appcustodian.app.ui.theme.AppCustodianTheme
import com.appcustodian.app.util.NotificationHelper
import com.appcustodian.app.util.PermissionUtils

/**
 * Main launcher activity for App Custodian.
 *
 * Serves as the primary entry point, orchestrating screen navigation, observing accessibility
 * state changes, managing runtime notification permissions, and launching system settings screens.
 */
class MainActivity : ComponentActivity() {

    /** ViewModel instance managing app list state and bridge interactions. */
    private val viewModel: AppListViewModel by viewModels()

    /** Full component class name for the background accessibility service. */
    private val serviceClassName = ForceStopAccessibilityService::class.java.name

    /**
     * Activity result launcher for requesting `POST_NOTIFICATIONS` runtime permissions on Android 13+ (API 33+).
     */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* Permission result handled gracefully */ }

    /**
     * Listener observing system-wide accessibility service state changes.
     * Automatically brings App Custodian back to the foreground when the accessibility service is granted.
     */
    private val accessibilityStateChangeListener =
        AccessibilityManager.AccessibilityStateChangeListener { enabled ->
            if (enabled && PermissionUtils.isAccessibilityServiceEnabled(this, serviceClassName)) {
                viewModel.refreshPermissionState()
                bringToFront()
            }
        }

    /**
     * Re-launches [MainActivity] using single-top activity flags to bring the current task
     * back to the front without recreating the task stack.
     */
    @SuppressLint("UnsafeIntentLaunch")
    private fun bringToFront() {
        val intent = intent.apply {
            addFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
        startActivity(intent)
    }

    /**
     * Called when the activity is first created. Initializes notification channels, requests runtime
     * permissions, registers accessibility listeners, and renders the Jetpack Compose UI.
     *
     * @param savedInstanceState If non-null, this activity is being re-constructed from a previous saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure notification channels exist before attempting to post results
        NotificationHelper.ensureChannel(this)

        // Prompt for runtime notification permissions on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Register accessibility state listener
        val accessibilityManager =
            getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        accessibilityManager.addAccessibilityStateChangeListener(accessibilityStateChangeListener)

        // Set Jetpack Compose layout content
        setContent {
            AppCustodianTheme {
                AppListScreen(
                    viewModel = viewModel,
                    onRequestForceStop = { packageName ->
                        startActivity(PermissionUtils.appInfoIntent(packageName))
                    },
                    onOpenApp = { packageName ->
                        val launchIntent = viewModel.getLaunchIntent(packageName)
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                        } else {
                            // Display toast if target package has no launcher activity
                            Toast.makeText(
                                this,
                                "This app has no launchable screen, so it can't be reopened automatically.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    onOpenAccessibilitySettings = {
                        startActivity(PermissionUtils.accessibilitySettingsIntent())
                    }
                )
            }
        }
    }

    /**
     * Called when the activity resumes interaction with the user.
     * Re-queries accessibility permission status and updates installed application data.
     */
    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissionState()
        viewModel.loadApps()
    }

    /**
     * Called when the activity is being destroyed. Unregisters active accessibility listeners.
     */
    override fun onDestroy() {
        val accessibilityManager =
            getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        accessibilityManager.removeAccessibilityStateChangeListener(accessibilityStateChangeListener)
        super.onDestroy()
    }
}