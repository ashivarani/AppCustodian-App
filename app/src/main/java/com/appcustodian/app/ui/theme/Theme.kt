package com.appcustodian.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Light theme color scheme mapping custom palette tokens to Material 3 semantics.
 */
@Composable
fun AppCustodianTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Primary,
            onPrimary = OnPrimary,
            error = Danger
        )
    } else {
        lightColorScheme(
            primary = Primary,
            onPrimary = OnPrimary,
            error = Danger,
            background = Background,
            surface = Surface,
            onSurfaceVariant = OnSurfaceVariant
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}