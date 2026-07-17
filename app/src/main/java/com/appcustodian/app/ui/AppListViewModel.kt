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

data class AppListUiState(
    val apps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true,
    val includeSystemApps: Boolean = true,
    val hasAccessibilityPermission: Boolean = false,
    val selectedApp: AppInfo? = null,
    val lastResult: ForceStopResult? = null
)

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val serviceClassName = ForceStopAccessibilityService::class.java.name

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ForceStopBridge.results.collect { result ->
                _uiState.value = _uiState.value.copy(lastResult = result)
            }
        }
        refreshPermissionState()
        loadApps()
    }

    fun refreshPermissionState() {
        val enabled = PermissionUtils.isAccessibilityServiceEnabled(
            getApplication(), serviceClassName
        )
        _uiState.value = _uiState.value.copy(hasAccessibilityPermission = enabled)
    }

    fun loadApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val apps = repository.getInstalledApps(_uiState.value.includeSystemApps)
            _uiState.value = _uiState.value.copy(apps = apps, isLoading = false)
        }
    }

    fun toggleIncludeSystemApps() {
        _uiState.value = _uiState.value.copy(includeSystemApps = !_uiState.value.includeSystemApps)
        loadApps()
    }

    fun onAppSelected(app: AppInfo) {
        _uiState.value = _uiState.value.copy(selectedApp = app)
    }

    fun dismissSelection() {
        _uiState.value = _uiState.value.copy(selectedApp = null)
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(lastResult = null)
    }

    /** Arms the bridge so the Accessibility Service knows which package to act on. */
    fun armForceStopTarget(packageName: String) {
        ForceStopBridge.armTarget(packageName)
    }

    fun getLaunchIntent(packageName: String) = repository.getLaunchIntent(packageName)
}