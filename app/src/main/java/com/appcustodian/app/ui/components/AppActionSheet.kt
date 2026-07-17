package com.appcustodian.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appcustodian.app.data.model.AppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionSheet(
    app: AppInfo,
    onDismiss: () -> Unit,
    onForceStop: () -> Unit,
    onOpen: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(app.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                app.packageName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            ListItem(
                headlineContent = { Text("Force stop") },
                supportingContent = { Text("Stops this app completely, like the button in Settings") },
                leadingContent = {
                    Icon(
                        Icons.Filled.PowerSettingsNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickableRow(onForceStop)
            )

            ListItem(
                headlineContent = { Text("Open / Restart") },
                supportingContent = { Text("Launches the app") },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                modifier = Modifier.clickableRow(onOpen)
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

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
                TextButton(onClick = onRestart) { Text("Reopen app") }
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
                "To force-stop apps for you, App Custodian needs its Accessibility " +
                        "Service turned on. This lets it press the \"Force stop\" button on " +
                        "the Settings screen for the app you choose — it doesn't read anything else."
            )
        },
        confirmButton = { TextButton(onClick = onOpenSettings) { Text("Open Settings") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } }
    )
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)