package com.appcustodian.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appcustodian.app.data.model.AppInfo
import com.appcustodian.app.data.model.ForceStopResult
import com.appcustodian.app.data.repository.AppRepository
import com.appcustodian.app.service.ForceStopAccessibilityService
import com.appcustodian.app.util.ForceStopBridge
import com.appcustodian.app.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state representation for the application list screen.
 *
 * @property apps Complete list of installed applications retrieved from the repository.
 * @property isLoading `true` when applications are actively being queried from the system.
 * @property includeSystemApps `true` if system apps are included in the results list.
 * @property searchQuery Active text filter applied against app titles and package names.
 * @property hasAccessibilityPermission `true` if the required accessibility service is enabled in system settings.
 * @property selectedApp The currently selected [AppInfo] instance for showing the action sheet, or `null`.
 * @property lastResult The outcome of the most recent force-stop operation, or `null`.
 */
data class AppListUiState(
    val apps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true,
    val includeSystemApps: Boolean = true,
    val searchQuery: String = "",
    val hasAccessibilityPermission: Boolean = false,
    val selectedApp: AppInfo? = null,
    val lastResult: ForceStopResult? = null
) {
    /**
     * Filtered list of applications derived by applying [searchQuery] to [apps].
     */
    val visibleApps: List<AppInfo>
        get() = apps.filter { app ->
            searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
        }
}

/**
 * Result state determined prior to executing a force-stop attempt on a package.
 */
sealed class ForceStopAttempt {
    /**
     * Represents readiness to proceed with force-stopping the specified [packageName].
     *
     * @property packageName The package name targeted for force-stop.
     */
    data class Proceed(val packageName: String) : ForceStopAttempt()

    /**
     * Indicates that the operation cannot proceed until the user grants Accessibility Service permissions.
     */
    object NeedsAccessibilityPermission : ForceStopAttempt()
}

/**
 * ViewModel responsible for managing application list state, handling search/filtering,
 * verifying accessibility permissions, and bridging force-stop requests to background services.
 *
 * @param application The current application context provided by AndroidViewModel.
 */
class AppListViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository used to fetch installed app details and launch intents. */
    private val repository = AppRepository(application)

    /** Full class name of the accessibility service used for permission checks. */
    private val serviceClassName = ForceStopAccessibilityService::class.java.name

    /** Mutable internal state flow backing [uiState]. */
    private val _uiState = MutableStateFlow(AppListUiState())

    /** Immutable observable state flow exposing current UI state. */
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        // Collect asynchronous force-stop execution results from the service bridge
        viewModelScope.launch {
            ForceStopBridge.results.collect { result ->
                _uiState.value = _uiState.value.copy(lastResult = result)
            }
        }
        refreshPermissionState()
        loadApps()
    }

    /**
     * Re-evaluates whether the required accessibility service is currently enabled in system settings.
     */
    fun refreshPermissionState() {
        val enabled = PermissionUtils.isAccessibilityServiceEnabled(
            getApplication(), serviceClassName
        )
        _uiState.value = _uiState.value.copy(hasAccessibilityPermission = enabled)
    }

    /**
     * Fetches installed applications from [repository] based on current filter configurations.
     */
    fun loadApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val apps = repository.getInstalledApps(_uiState.value.includeSystemApps)
            _uiState.value = _uiState.value.copy(apps = apps, isLoading = false)
        }
    }

    /**
     * Toggles the system apps visibility setting and reloads the application list.
     */
    fun toggleIncludeSystemApps() {
        _uiState.value = _uiState.value.copy(includeSystemApps = !_uiState.value.includeSystemApps)
        loadApps()
    }

    /**
     * Updates the active search query filter.
     *
     * @param query The text string entered into the search field.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    /**
     * Evaluates permission requirements and returns a [ForceStopAttempt] decision for the given [app].
     *
     * @param app The target application selected for force-stop.
     * @return [ForceStopAttempt.Proceed] if permissions are present, otherwise [ForceStopAttempt.NeedsAccessibilityPermission].
     */
    fun decideForceStop(app: AppInfo): ForceStopAttempt {
        return if (_uiState.value.hasAccessibilityPermission) {
            ForceStopAttempt.Proceed(app.packageName)
        } else {
            ForceStopAttempt.NeedsAccessibilityPermission
        }
    }

    /**
     * Sets the currently selected application to trigger action sheet presentation.
     *
     * @param app The application instance selected by the user.
     */
    fun onAppSelected(app: AppInfo) {
        _uiState.value = _uiState.value.copy(selectedApp = app)
    }

    /**
     * Clears current application selection, dismissing the action sheet dialog.
     */
    fun dismissSelection() {
        _uiState.value = _uiState.value.copy(selectedApp = null)
    }

    /**
     * Clears the latest force-stop result, dismissing the outcome dialog.
     */
    fun dismissResult() {
        _uiState.value = _uiState.value.copy(lastResult = null)
    }

    /**
     * Arms [ForceStopBridge] with [packageName] to signal target readiness to the accessibility service.
     *
     * @param packageName The package name to target.
     */
    fun armForceStopTarget(packageName: String) {
        ForceStopBridge.armTarget(packageName)
    }

    /**
     * Retrieves a cold-start launcher intent for the specified application package.
     *
     * @param packageName Target package name to launch.
     * @return Configured [android.content.Intent], or `null` if the app lacks a launcher activity.
     */
    fun getLaunchIntent(packageName: String) = repository.getLaunchIntent(packageName)
}