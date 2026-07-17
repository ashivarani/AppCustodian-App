package com.appcustodian.app.util

import com.appcustodian.app.data.model.ForceStopResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The ViewModel and the AccessibilityService run in the same process, so we don't
 * need AIDL/Binder or a broadcast — a simple in-memory singleton is enough to pass
 * "which package should be force-stopped next" down to the service, and to pass the
 * automation result back up to the UI.
 *
 * targetPackage is set right before we launch the App Info screen for that package,
 * and cleared once the service consumes it (or the user backs out without it firing).
 */
object ForceStopBridge {

    private val _targetPackage = MutableStateFlow<String?>(null)
    val targetPackage = _targetPackage.asStateFlow()

    private val _results = MutableSharedFlow<ForceStopResult>(extraBufferCapacity = 1)
    val results = _results.asSharedFlow()

    fun armTarget(packageName: String) {
        _targetPackage.value = packageName
    }

    fun clearTarget() {
        _targetPackage.value = null
    }

    fun consumeTarget(): String? {
        val pkg = _targetPackage.value
        _targetPackage.value = null
        return pkg
    }

    fun emitResult(result: ForceStopResult) {
        _results.tryEmit(result)
    }
}