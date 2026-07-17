package com.appcustodian.app.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.appcustodian.app.data.model.ForceStopResult
import com.appcustodian.app.util.ForceStopBridge

/**
 * Runs only while the user has manually enabled it under
 * Settings > Accessibility > Installed apps > App Custodian.
 *
 * It only ever looks at windows belonging to com.android.settings (restricted via
 * accessibility_service_config.xml), and only acts when App Custodian itself armed
 * a target package right before opening that screen — it does not act on its own,
 * and does not read anything outside the Settings App Info page.
 *
 * IMPORTANT / KNOWN LIMITATION:
 * The exact button text ("Force stop" / "FORCE STOP" / localized strings) and the
 * view hierarchy of the Settings app differ across Android versions and OEM skins
 * (Samsung OneUI, MIUI, ColorOS, stock AOSP, etc). The text list below covers stock
 * AOSP/Pixel English strings. You will likely need to extend FORCE_STOP_LABELS and
 * CONFIRM_LABELS for other languages/OEMs, and re-test whenever Settings gets a
 * redesign. If the button is greyed out (protected system package), no click will
 * be dispatched and we report a Failed result after the timeout.
 */
class ForceStopAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingTargetPackage: String? = null
    private var awaitingConfirmDialog = false
    private var timeoutRunnable: Runnable? = null

    private val forceStopLabels = listOf("Force stop", "FORCE STOP")
    private val confirmLabels = listOf("OK", "Force stop", "FORCE STOP")

    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Pick up a newly-armed target the moment its App Info screen opens.
        if (pendingTargetPackage == null) {
            val armed = ForceStopBridge.consumeTarget() ?: return
            pendingTargetPackage = armed
            awaitingConfirmDialog = false
            scheduleTimeout(armed)
        }

        val root = rootInActiveWindow ?: return
        val target = pendingTargetPackage ?: return

        if (!awaitingConfirmDialog) {
            val forceStopNode = findClickableNodeByText(root, forceStopLabels)
            if (forceStopNode != null) {
                forceStopNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                awaitingConfirmDialog = true
            }
        } else {
            val confirmNode = findClickableNodeByText(root, confirmLabels)
            if (confirmNode != null) {
                confirmNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                finishSuccess(target)
            }
        }
    }

    private fun findClickableNodeByText(
        root: AccessibilityNodeInfo,
        labels: List<String>
    ): AccessibilityNodeInfo? {
        for (label in labels) {
            val matches = root.findAccessibilityNodeInfosByText(label)
            for (node in matches) {
                val clickable = nearestClickableAncestorOrSelf(node)
                if (clickable != null && clickable.isEnabled) {
                    return clickable
                }
            }
        }
        return null
    }

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

    private fun scheduleTimeout(packageName: String) {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        val runnable = Runnable {
            if (pendingTargetPackage == packageName) {
                ForceStopBridge.emitResult(
                    ForceStopResult.Failed(
                        packageName,
                        "Couldn't find an enabled \"Force stop\" button on this screen " +
                                "(it may be protected, or the button label differs on this device)."
                    )
                )
                resetState()
            }
        }
        timeoutRunnable = runnable
        mainHandler.postDelayed(runnable, 8_000L)
    }

    private fun finishSuccess(packageName: String) {
        ForceStopBridge.emitResult(ForceStopResult.Success(packageName))
        resetState()
    }

    private fun resetState() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
        pendingTargetPackage = null
        awaitingConfirmDialog = false
    }

    override fun onInterrupt() {
        resetState()
    }
}