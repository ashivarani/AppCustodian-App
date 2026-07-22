package com.appcustodian.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.appcustodian.app.data.model.AppInfo

/**
 * Builds a descriptive subtitle for an application based on its type and launcher status.
 *
 * System packages without launcher activities are labeled as "System service", while user
 * background components without entry points are labeled as "Background component".
 *
 * @param app The [AppInfo] model containing package details.
 * @return A formatted subtitle string combining app category and package name.
 */
private fun subtitleFor(app: AppInfo): String {
    val kind = when {
        app.isSystemApp && !app.hasLauncherActivity -> "System service"
        app.isSystemApp -> "System app"
        !app.hasLauncherActivity -> "Background component"
        else -> null
    }
    return if (kind != null) "$kind · ${app.packageName}" else app.packageName
}

/**
 * Renders a single row item in the main installed application list.
 *
 * Displays the application's icon, title, and detailed subtitle, wrapped in a
 * rounded clickable surface.
 *
 * @param app The [AppInfo] model representing the installed application.
 * @param onClick Callback triggered when the user taps the item row.
 */
@Composable
fun AppListItem(app: AppInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon container box with fallback to default Android icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // Safe conversion of app Drawable to ImageBitmap
            val bitmap: ImageBitmap? = app.icon?.let { drawable ->
                try {
                    drawable.toBitmap(132, 132).asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            }

            // Render custom app icon if valid, otherwise show Android system icon
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = app.appName)
            } else {
                Icon(Icons.Filled.Android, contentDescription = app.appName)
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Application title and package info column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitleFor(app),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}