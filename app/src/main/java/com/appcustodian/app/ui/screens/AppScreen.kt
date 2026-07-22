package com.appcustodian.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.appcustodian.app.R
import com.appcustodian.app.data.model.ForceStopResult
import com.appcustodian.app.ui.AppListViewModel
import com.appcustodian.app.ui.ForceStopAttempt
import com.appcustodian.app.ui.components.AccessibilityPermissionDialog
import com.appcustodian.app.ui.components.AppActionSheet
import com.appcustodian.app.ui.components.AppListItem
import com.appcustodian.app.ui.components.ForceStopResultDialog
import com.appcustodian.app.ui.theme.HeaderBackground
import com.appcustodian.app.ui.theme.OnHeaderBackground

/**
 * Main application list screen.
 *
 * Displays installed applications with real-time searching, system app filtering,
 * per-app action bottom sheets, accessibility permission prompts, and result dialogs.
 *
 * @param viewModel The state management [AppListViewModel] backing this screen.
 * @param onRequestForceStop Callback triggered when launching the force-stop flow for a specific package name.
 * @param onOpenApp Callback triggered when launching or reopening a target app package.
 * @param onOpenAccessibilitySettings Callback directing the user to system Accessibility Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    onRequestForceStop: (packageName: String) -> Unit,
    onOpenApp: (packageName: String) -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    /** Observe UI state emissions from the ViewModel. */
    val state by viewModel.uiState.collectAsState()

    /**
     * Local state tracking if the user explicitly dismissed the accessibility dialog during this session.
     * Prevents the dialog from immediately reappearing before the permission state updates.
     */
    var accessibilityDialogDismissed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // Distinctly shaded top app bar hosting the app title.
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HeaderBackground,
                    titleContentColor = OnHeaderBackground,
                    actionIconContentColor = OnHeaderBackground
                ),
                modifier = Modifier.shadow(elevation = 4.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Elevated container toolbar for search query input and system app toggles
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    // Real-time search field for app names or package names
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search apps or package name") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter chips row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeaderFilterChip(
                            label = if (state.includeSystemApps) "Hide system" else "Show system",
                            selected = state.includeSystemApps,
                            onClick = { viewModel.toggleIncludeSystemApps() }
                        )
                    }
                }
            }

            // Main app list container with overlay dialogs
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    val visibleApps = state.visibleApps
                    if (visibleApps.isEmpty()) {
                        // Empty search state notice
                        Text(
                            text = if (state.searchQuery.isNotBlank())
                                "No apps match \"${state.searchQuery}\"."
                            else "No apps found.",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Lazy list rendering filtered installed applications
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(visibleApps, key = { it.packageName }) { app ->
                                AppListItem(app = app, onClick = { viewModel.onAppSelected(app) })
                            }
                        }
                    }
                }

                // Prompt user for accessibility permissions if missing
                if (!state.hasAccessibilityPermission && !accessibilityDialogDismissed) {
                    AccessibilityPermissionDialog(
                        onOpenSettings = onOpenAccessibilitySettings,
                        onDismiss = { accessibilityDialogDismissed = true }
                    )
                }

                // Per-app quick action dialog sheet (Force Stop / Relaunch)
                state.selectedApp?.let { app ->
                    AppActionSheet(
                        app = app,
                        onDismiss = { viewModel.dismissSelection() },
                        onForceStop = {
                            viewModel.dismissSelection()
                            when (val decision = viewModel.decideForceStop(app)) {
                                is ForceStopAttempt.Proceed -> {
                                    // Arm the target package and request navigation to Settings App Info page
                                    viewModel.armForceStopTarget(decision.packageName)
                                    onRequestForceStop(decision.packageName)
                                }
                                ForceStopAttempt.NeedsAccessibilityPermission -> onOpenAccessibilitySettings()
                            }
                        },
                        onOpen = {
                            viewModel.dismissSelection()
                            onOpenApp(app.packageName)
                        }
                    )
                }

                // Force-stop result alert dialog (Success / Failed)
                state.lastResult?.let { result ->
                    val (isSuccess, appName, detail) = when (result) {
                        is ForceStopResult.Success -> Triple(true, result.packageName, null as String?)
                        is ForceStopResult.Failed -> Triple(false, result.packageName, result.reason)
                    }
                    ForceStopResultDialog(
                        isSuccess = isSuccess,
                        appName = appName,
                        detail = detail,
                        onRestart = if (isSuccess) {
                            {
                                onOpenApp(appName)
                                viewModel.dismissResult()
                            }
                        } else null,
                        onDismiss = { viewModel.dismissResult() }
                    )
                }
            }
        }
    }
}

/**
 * Filter chip component used in the screen header toolbar.
 *
 * @param label The text label displayed inside the chip.
 * @param selected `true` if the filter chip is active, `false` otherwise.
 * @param onClick Callback triggered when tapping the chip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeaderFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}