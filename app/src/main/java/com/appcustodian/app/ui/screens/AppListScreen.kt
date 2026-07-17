package com.appcustodian.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.appcustodian.app.data.model.ForceStopResult
import com.appcustodian.app.ui.components.AccessibilityPermissionDialog
import com.appcustodian.app.ui.components.AppActionSheet
import com.appcustodian.app.ui.components.AppListItem
import com.appcustodian.app.ui.components.ForceStopResultDialog
import com.appcustodian.app.ui.AppListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    onRequestForceStop: (packageName: String) -> Unit,
    onOpenApp: (packageName: String) -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Custodian") },
                actions = {
                    TextButton(onClick = { viewModel.toggleIncludeSystemApps() }) {
                        Text(if (state.includeSystemApps) "Hide system" else "Show system")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.apps, key = { it.packageName }) { app ->
                        AppListItem(app = app, onClick = { viewModel.onAppSelected(app) })
                    }
                }
            }

            if (!state.hasAccessibilityPermission) {
                AccessibilityPermissionDialog(
                    onOpenSettings = onOpenAccessibilitySettings,
                    onDismiss = { /* user can keep browsing; open/restart still works */ }
                )
            }

            state.selectedApp?.let { app ->
                AppActionSheet(
                    app = app,
                    onDismiss = { viewModel.dismissSelection() },
                    onForceStop = {
                        viewModel.dismissSelection()
                        if (state.hasAccessibilityPermission) {
                            viewModel.armForceStopTarget(app.packageName)
                            onRequestForceStop(app.packageName)
                        } else {
                            onOpenAccessibilitySettings()
                        }
                    },
                    onOpen = {
                        viewModel.dismissSelection()
                        onOpenApp(app.packageName)
                    }
                )
            }

            state.lastResult?.let { result ->
                val (isSuccess, appName, detail) = when (result) {
                    is ForceStopResult.Success -> Triple(true, result.packageName, null as String?)
                    is ForceStopResult.Failed -> Triple(false, result.packageName, result.reason)
                }
                ForceStopResultDialog(
                    isSuccess = isSuccess,
                    appName = appName,
                    detail = detail,
                    onRestart = if (isSuccess) { { onOpenApp(appName); viewModel.dismissResult() } } else null,
                    onDismiss = { viewModel.dismissResult() }
                )
            }
        }
    }
}