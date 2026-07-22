package com.appcustodian.app.util

import com.appcustodian.app.data.model.ForceStopResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-memory thread-safe bridge connecting the UI layer ([com.appcustodian.app.ui.AppListViewModel])
 * and background service ([com.appcustodian.app.service.ForceStopAccessibilityService]).
 *
 * Since both components execute within the same process, this object provides low-latency communication
 * for queuing pending target apps and emitting asynchronous execution results.
 */
object ForceStopBridge {

    /** Tracks the package name armed for an upcoming force-stop attempt. */
    private val targetPackage = MutableStateFlow<String?>(null)

    /** Internal mutable shared flow backing [results]. */
    private val _results = MutableSharedFlow<ForceStopResult>(extraBufferCapacity = 1)

    /**
     * Hot shared flow emitting outcomes ([ForceStopResult]) of force-stop operations
     * back to active UI collectors.
     */
    val results: SharedFlow<ForceStopResult> = _results.asSharedFlow()

    /**
     * Arms the bridge with a target package name right before launching its system App Info screen.
     *
     * @param packageName The package identifier to target.
     */
    fun armTarget(packageName: String) {
        targetPackage.value = packageName
    }

    /**
     * Retrieves and atomically clears the currently armed target package name.
     *
     * Called by the Accessibility Service upon detecting an active App Info window.
     *
     * @return The pending package name to process, or `null` if no target is armed.
     */
    fun consumeTarget(): String? {
        val pkg = targetPackage.value
        targetPackage.value = null
        return pkg
    }

    /**
     * Emits a force-stop outcome result to active subscribers via [results].
     *
     * @param result The [ForceStopResult] (Success or Failed) to broadcast.
     */
    fun emitResult(result: ForceStopResult) {
        _results.tryEmit(result)
    }
}