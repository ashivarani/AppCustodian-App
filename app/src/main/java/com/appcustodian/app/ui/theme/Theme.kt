package com.appcustodian.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Light theme color scheme mapping custom palette tokens to Material 3 semantics.
 */
private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    error = Danger,
    background = Background,
    surface = Surface,
    onSurfaceVariant = OnSurfaceVariant
)

/**
 * Dark theme color scheme mapping custom palette tokens to Material 3 semantics.
 */
private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    error = Danger
)

/**
 * Main application theme wrapper for App Custodian.
 *
 * Applies the custom Material 3 color schemes and typography hierarchy across
 * light and dark system themes.
 *
 * @param darkTheme `true` to force dark theme, `false` for light theme. Defaults to system setting via [isSystemInDarkTheme].
 * @param content The Composable content block to be styled by this theme.
 */
@Composable
fun AppCustodianTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}