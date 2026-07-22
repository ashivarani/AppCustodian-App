package com.appcustodian.app.service

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.appcustodian.app.MainActivity
import com.appcustodian.app.data.model.ForceStopResult
import com.appcustodian.app.util.ForceStopBridge
import com.appcustodian.app.util.NotificationHelper

/**
 * An Accessibility Service that monitors system "App Info" screens when initiated by App Custodian.
 *
 * This service observes the UI tree during manual force-stop operations:
 * - Detects when the "Force stop" button or confirmation dialog appears.
 * - Monitors user interactions (e.g., tapping "Force Stop" and confirming "OK").
 * - Automatically navigates back to App Custodian when the force-stop operation completes or fails.
 * - Detects if an app is already stopped or protected against force-stopping.
 */
@SuppressLint("AccessibilityPolicy")
class ForceStopAccessibilityService : AccessibilityService() {

    /** Main thread handler for scheduling UI polling tasks and timeouts. */
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Package name of the application currently targeted for force-stop, or null if idle. */
    private var pendingTargetPackage: String? = null

    /** Tracks whether the user is currently viewing the system confirmation dialog ("OK" / "Cancel"). */
    private var awaitingConfirmDialog = false

    /** Runnable scheduled to trigger a failure state if the force-stop flow exceeds the allowed duration. */
    private var timeoutRunnable: Runnable? = null

    /** Realtime timestamp (in milliseconds) when the current target was armed. */
    private var armedAtElapsedMillis: Long = 0L

    /** Consecutive counter tracking how many polling cycles detected a disabled "Force stop" button. */
    private var disabledStreak = 0

    /** Consecutive counter tracking how many polling cycles could not locate a "Force stop" node. */
    private var missingStreak = 0

    /** Recurring runnable performing periodic screen evaluations independent of accessibility events. */
    private var pollRunnable: Runnable? = null

    /** Text labels matching the "Force stop" action across different locales and Android OEMs. */
    private val forceStopLabels = listOf("Force stop", "FORCE STOP", "Force Stop")

    /** Text labels matching the positive button in system confirmation dialogs. */
    private val confirmLabels = listOf("OK")

    /** Text labels matching the negative/cancel button in system confirmation dialogs. */
    private val cancelLabels = listOf("Cancel", "CANCEL")

    /**
     * Best-effort view resource IDs for the "Force stop" button in AOSP and custom OEM Settings layouts.
     */
    private val forceStopViewIds = listOf(
        "com.android.settings:id/right_button",     // AOSP action bar (~API 26-29)
        "com.android.settings:id/button1_positive"  // AOSP ActionButtonsPreference (~API 30+)
    )

    /** Resource IDs for the framework AlertDialog positive button ("OK"). */
    private val confirmViewIds = listOf(
        "android:id/button1"
    )

    /**
     * Called when the accessibility service successfully connects to the system.
     * Configures service flags and registers callbacks for the system accessibility shortcut button.
     */
    override fun onServiceConnected() {
        super.onServiceConnected()

        // Ensure FLAG_REPORT_VIEW_IDS is active to enable viewIdResourceName lookup on accessibility nodes.
        runCatching {
            serviceInfo = serviceInfo?.apply {
                flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            }
        }

        // Register callback for the system accessibility floating button / navigation bar shortcut.
        runCatching {
            accessibilityButtonController.registerAccessibilityButtonCallback(
                object : AccessibilityButtonController.AccessibilityButtonCallback() {
                    /**
                     * Handles direct taps on the system accessibility button by returning to App Custodian.
                     *
                     * @param controller The accessibility button controller dispatching the event.
                     */
                    override fun onClicked(controller: AccessibilityButtonController) {
                        launchApp()
                    }
                }
            )
        }
    }

    /**
     * Receives incoming accessibility events emitted by the active foreground window.
     *
     * @param event The accessibility event dispatched by the system UI framework.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // If not actively monitoring a package, check if a new target was armed by App Custodian.
        if (pendingTargetPackage == null) {
            val armed = ForceStopBridge.consumeTarget() ?: return
            armTarget(armed)
        }

        val target = pendingTargetPackage ?: return

        // Evaluate standard App Info view hierarchy if not waiting for a confirmation dialog.
        if (!awaitingConfirmDialog) {
            evaluateAppInfoScreen(target)
            return
        }

        // Monitor clicks on the confirmation dialog ("OK" / "Cancel").
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val sourceText = event.source?.text?.toString()?.trim()
            val eventText = event.text.joinToString(" ").trim()
            val clickedText = sourceText.takeUnless { it.isNullOrEmpty() } ?: eventText

            when {
                confirmLabels.any { it.equals(clickedText, ignoreCase = true) } -> {
                    finishSuccess(target)
                    return
                }
                cancelLabels.any { it.equals(clickedText, ignoreCase = true) } -> {
                    resetState()
                    return
                }
            }
        }

        // Check window state if the confirmation dialog was dismissed without an explicit click event.
        val root = rootInActiveWindow ?: return
        if (findConfirmEnabled(root) == null) {
            if (findForceStopEnabled(root) != null) {
                // Dialog dismissed and user returned to App Info with Force Stop still available.
                resetState()
            } else {
                finishSuccess(target)
            }
        }
    }

    /**
     * Arms the service for a new target app package, resetting internal state and starting timeout/polling timers.
     *
     * @param packageName The package name of the app to monitor.
     */
    private fun armTarget(packageName: String) {
        pendingTargetPackage = packageName
        awaitingConfirmDialog = false
        armedAtElapsedMillis = SystemClock.elapsedRealtime()
        disabledStreak = 0
        missingStreak = 0
        scheduleTimeout(packageName)
        startPolling()
    }

    /**
     * Inspects the current window node hierarchy to determine the state of the "Force stop" action.
     *
     * @param target The package name currently under evaluation.
     */
    private fun evaluateAppInfoScreen(target: String) {
        if (awaitingConfirmDialog) return
        val root = rootInActiveWindow ?: return

        // Transition to confirmation state if the dialog is currently visible.
        if (findConfirmEnabled(root) != null) {
            awaitingConfirmDialog = true
            scheduleTimeout(target)
            return
        }

        val settledEnough = SystemClock.elapsedRealtime() - armedAtElapsedMillis >= DISABLED_CHECK_GRACE_MS
        val forceStopNode = findForceStopAny(root)

        when {
            // Case 1: Force stop button is present but greyed out/disabled.
            forceStopNode != null && !forceStopNode.isEnabled -> {
                disabledStreak++
                missingStreak = 0
                if (settledEnough && disabledStreak >= REQUIRED_STREAK) {
                    val reason = if (isAlreadyStopped(target)) {
                        "This app is already force-stopped."
                    } else {
                        "\"Force stop\" is disabled for this app — it's likely a protected system app."
                    }
                    failNotAvailable(target, reason)
                }
            }
            // Case 2: Force stop button is missing entirely from a fully loaded App Info screen.
            forceStopNode == null && isLikelySettledAppInfoScreen(root) -> {
                missingStreak++
                disabledStreak = 0
                if (settledEnough && missingStreak >= REQUIRED_STREAK) {
                    failNotAvailable(
                        target,
                        "This app doesn't show a \"Force stop\" option here — it's likely a system service without one."
                    )
                }
            }
            // Case 3: Force stop button is present and active.
            else -> {
                disabledStreak = 0
                missingStreak = 0
            }
        }
    }

    /**
     * Verifies if the active active window has finished drawing non-empty content from another app.
     *
     * @param root The root accessibility node of the active window.
     * @return `true` if the root belongs to an external app and contains child nodes.
     */
    private fun isLikelySettledAppInfoScreen(root: AccessibilityNodeInfo): Boolean {
        val windowPackage = root.packageName?.toString()
        if (windowPackage == applicationContext.packageName) return false
        return root.childCount > 0
    }

    /**
     * Starts the repeating poll timer to periodically evaluate the window node tree.
     */
    private fun startPolling() {
        pollRunnable?.let { mainHandler.removeCallbacks(it) }
        val runnable = object : Runnable {
            override fun run() {
                val target = pendingTargetPackage
                if (target != null) {
                    evaluateAppInfoScreen(target)
                    mainHandler.postDelayed(this, POLL_INTERVAL_MS)
                }
            }
        }
        pollRunnable = runnable
        mainHandler.postDelayed(runnable, POLL_INTERVAL_MS)
    }

    /**
     * Cancels active polling tasks and releases the poll runnable reference.
     */
    private fun stopPolling() {
        pollRunnable?.let { mainHandler.removeCallbacks(it) }
        pollRunnable = null
    }

    /**
     * Finds an enabled node matching any of the specified text strings.
     *
     * @param root The root node to search from.
     * @param labels The list of string labels to match.
     * @return The enabled clickable node, or `null` if not found.
     */
    private fun findClickableNodeByText(root: AccessibilityNodeInfo, labels: List<String>): AccessibilityNodeInfo? =
        findClickableAncestorByText(root, labels)?.takeIf { it.isEnabled }

    /**
     * Searches node hierarchy by text and retrieves the nearest clickable ancestor node, regardless of enabled state.
     *
     * @param root The root node to search from.
     * @param labels The list of target text strings.
     * @return The clickable ancestor node, or `null` if missing.
     */
    private fun findClickableAncestorByText(root: AccessibilityNodeInfo, labels: List<String>): AccessibilityNodeInfo? {
        for (label in labels) {
            val matches = root.findAccessibilityNodeInfosByText(label)
            for (node in matches) {
                val clickable = nearestClickableAncestorOrSelf(node)
                if (clickable != null) return clickable
            }
        }
        return null
    }

    /**
     * Searches node hierarchy by resource view ID and retrieves the nearest clickable ancestor node, regardless of enabled state.
     *
     * @param root The root node to search from.
     * @param viewIds The list of view resource ID strings.
     * @return The clickable ancestor node, or `null` if missing.
     */
    private fun findClickableAncestorByViewId(root: AccessibilityNodeInfo, viewIds: List<String>): AccessibilityNodeInfo? {
        for (id in viewIds) {
            val matches = runCatching { root.findAccessibilityNodeInfosByViewId(id) }.getOrDefault(emptyList())
            for (node in matches) {
                val clickable = nearestClickableAncestorOrSelf(node)
                if (clickable != null) return clickable
            }
        }
        return null
    }

    /**
     * Finds an enabled node matching any of the specified view IDs.
     *
     * @param root The root node to search from.
     * @param viewIds The list of view resource ID strings.
     * @return The enabled clickable node, or `null` if missing.
     */
    private fun findClickableNodeByViewId(root: AccessibilityNodeInfo, viewIds: List<String>): AccessibilityNodeInfo? =
        findClickableAncestorByViewId(root, viewIds)?.takeIf { it.isEnabled }

    /**
     * Searches for any "Force stop" node (enabled or disabled) via resource ID first, then falls back to text matching.
     *
     * @param root The root accessibility node to evaluate.
     * @return The matching node if found, otherwise `null`.
     */
    private fun findForceStopAny(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        findClickableAncestorByViewId(root, forceStopViewIds) ?: findClickableAncestorByText(root, forceStopLabels)

    /**
     * Searches for an enabled "Force stop" node via resource ID first, then falls back to text matching.
     *
     * @param root The root accessibility node to evaluate.
     * @return The active matching node if found, otherwise `null`.
     */
    private fun findForceStopEnabled(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        findClickableNodeByViewId(root, forceStopViewIds) ?: findClickableNodeByText(root, forceStopLabels)

    /**
     * Searches for an enabled confirmation dialog button ("OK") via resource ID, then falls back to text matching.
     *
     * @param root The root accessibility node to evaluate.
     * @return The active confirmation node if found, otherwise `null`.
     */
    private fun findConfirmEnabled(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        findClickableNodeByViewId(root, confirmViewIds) ?: findClickableNodeByText(root, confirmLabels)

    /**
     * Traverses parent hierarchy up to a maximum depth of 5 levels to locate a clickable container or node.
     *
     * @param node The initial node to inspect.
     * @return The node itself or its closest clickable parent, or `null` if none found within depth limit.
     */
    private fun nearestClickableAncestorOrSelf(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 5) {
            if (current.isClickable) return current
            current = current.parent
            depth++
        }
        return null
    }

    /**
     * Schedules a timeout callback to trigger a failure when an operation exceeds maximum duration limits.
     *
     * @param packageName The package name being tracked.
     */
    private fun scheduleTimeout(packageName: String) {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        val runnable = Runnable {
            if (pendingTargetPackage == packageName) {
                failNotAvailable(
                    packageName,
                    "Couldn't find an enabled \"Force stop\" button on this screen (it may be protected, or the button label differs)."
                )
            }
        }
        timeoutRunnable = runnable
        mainHandler.postDelayed(runnable, TIMEOUT_MS)
    }

    /**
     * Handles failure scenarios by notifying the UI bridge, posting a notification, resetting state, and navigating back.
     *
     * @param packageName The package name that failed processing.
     * @param reason A descriptive human-readable failure reason.
     */
    private fun failNotAvailable(packageName: String, reason: String) {
        ForceStopBridge.emitResult(ForceStopResult.Failed(packageName, reason))
        NotificationHelper.showNotAvailable(
            applicationContext, resolveAppName(packageName), packageName, reason
        )
        resetState()
        returnToApp()
    }

    /**
     * Handles successful force-stop operations by notifying the UI bridge, posting a notification, and navigating back.
     *
     * @param packageName The package name that was successfully force-stopped.
     */
    private fun finishSuccess(packageName: String) {
        ForceStopBridge.emitResult(ForceStopResult.Success(packageName))
        NotificationHelper.showStopped(applicationContext, resolveAppName(packageName), packageName)
        resetState()
        returnToApp()
    }

    /**
     * Resets service state counters, clearing active timers, target packages, and polling routines.
     */
    private fun resetState() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
        stopPolling()
        pendingTargetPackage = null
        awaitingConfirmDialog = false
        disabledStreak = 0
        missingStreak = 0
    }

    /**
     * Queries PackageManager to determine if the given package currently holds the `FLAG_STOPPED` flag.
     *
     * @param packageName The package name to inspect.
     * @return `true` if the app is currently in a stopped state.
     */
    private fun isAlreadyStopped(packageName: String): Boolean =
        runCatching {
            val flags = packageManager.getApplicationInfo(packageName, 0).flags
            (flags and ApplicationInfo.FLAG_STOPPED) != 0
        }.getOrDefault(false)

    /**
     * Resolves the user-facing application label corresponding to a package name.
     *
     * @param packageName The target app package name.
     * @return The localized application label, or the raw package name on failure.
     */
    private fun resolveAppName(packageName: String): String =
        runCatching {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)

    /**
     * Navigates back to App Custodian by performing a back action or re-launching the main Activity.
     */
    private fun returnToApp() {
        if (!performGlobalAction(GLOBAL_ACTION_BACK)) {
            launchApp()
        }
    }

    /**
     * Launches the [MainActivity] task, bringing App Custodian directly to the foreground.
     */
    private fun launchApp() {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        startActivity(intent)
    }

    /**
     * Called when the accessibility service is interrupted by the system. Resets service tracking state.
     */
    override fun onInterrupt() {
        resetState()
    }

    companion object {
        /** Maximum duration (60s) permitted for a user to complete the manual force-stop flow before timing out. */
        private const val TIMEOUT_MS = 60_000L

        /** Initial grace period (900ms) allowed for the Settings screen to fully load and render nodes. */
        private const val DISABLED_CHECK_GRACE_MS = 900L

        /** Number of consecutive matching polling reads required before declaring a button state settled. */
        private const val REQUIRED_STREAK = 2

        /** Interval duration (500ms) between UI node polling checks. */
        private const val POLL_INTERVAL_MS = 500L
    }
}