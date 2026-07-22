package com.appcustodian.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.appcustodian.app.data.model.AppInfo

/**
 * Centered popup dialog displaying per-app quick actions (Force Stop and Relaunch).
 *
 * Uses a custom [Dialog] with a [Card] layout instead of a bottom sheet so that it renders
 * directly in the center of the screen for quick interaction.
 *
 * @param app The target [AppInfo] model containing app details (name, package, launch ability).
 * @param onDismiss Callback invoked when the user dismisses the dialog (e.g., clicking outside or pressing Cancel).
 * @param onForceStop Callback invoked when the user selects the "Force Stop" action.
 * @param onOpen Callback invoked when the user selects the "Relaunch" action.
 */
@Composable
fun AppActionSheet(
    app: AppInfo,
    onDismiss: () -> Unit,
    onForceStop: () -> Unit,
    onOpen: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                // Header section: App name and package identifier
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action item: Force Stop
                ListItem(
                    headlineContent = { Text("Force Stop") },
                    supportingContent = { Text("Immediately terminates all running processes for this app") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.PowerSettingsNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickableRow(onForceStop)
                )

                // Action item: Relaunch (Conditional on whether a launchable launcher activity exists)
                if (app.hasLauncherActivity) {
                    ListItem(
                        headlineContent = { Text("Relaunch") },
                        supportingContent = { Text("Launches the app and brings it to the foreground") },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.clickableRow(onOpen)
                    )
                } else {
                    // Disabled row shown when the package is a system background service with no entry activity
                    ListItem(
                        headlineContent = { Text("Relaunch") },
                        supportingContent = { Text("No launchable screen for this service") },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Footer action: Cancel/Dismiss button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

/**
 * An [AlertDialog] summarizing the outcome of a force-stop operation.
 *
 * @param isSuccess `true` if the force-stop operation succeeded, `false` otherwise.
 * @param appName Display name of the application that was processed.
 * @param detail Descriptive message or error reason explaining the outcome (used when [isSuccess] is `false`).
 * @param onRestart Optional callback to relaunch the app immediately after a successful force-stop.
 * @param onDismiss Callback invoked when dismissing the dialog.
 */
@Composable
fun ForceStopResultDialog(
    isSuccess: Boolean,
    appName: String,
    detail: String?,
    onRestart: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isSuccess) "App force-stopped" else "Couldn't force-stop") },
        text = {
            Text(
                if (isSuccess) "$appName has been force-stopped."
                else detail ?: "Something went wrong."
            )
        },
        confirmButton = {
            if (isSuccess && onRestart != null) {
                TextButton(onClick = onRestart) { Text("Relaunch") }
            } else {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        },
        dismissButton = {
            if (isSuccess && onRestart != null) {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

/**
 * An [AlertDialog] prompting the user to grant the required Accessibility Service permission.
 *
 * @param onOpenSettings Callback directing the user to system Accessibility Settings.
 * @param onDismiss Callback invoked if the user declines or dismisses the prompt.
 */
@Composable
fun AccessibilityPermissionDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable Accessibility Service") },
        text = {
            Text(
                "App Custodian needs one-time permission to force-stop apps and " +
                        "services on your behalf. Turn it on to keep using this feature."
            )
        },
        confirmButton = { TextButton(onClick = onOpenSettings) { Text("Open Settings") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } }
    )
}

/**
 * Helper extension function to make list item rows clickable across their full surface width.
 *
 * @param onClick Callback triggered when the row is clicked.
 */
private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)