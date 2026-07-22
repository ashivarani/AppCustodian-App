package com.appcustodian.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Custom Material 3 typography definitions for App Custodian.
 *
 * Defines consistent font weights and text sizes used across titles,
 * app list items, package labels, and action dialogs.
 */
val AppTypography = Typography(
    /**
     * Used for main top-level screen titles and prominent headers.
     */
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),

    /**
     * Used for section headers, dialog titles, and card headers.
     */
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp
    ),

    /**
     * Primary body text style, used for app display names in list rows.
     */
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),

    /**
     * Secondary body text style, used for package names, subtitles, and detailed list descriptions.
     */
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),

    /**
     * Label text style, used for filter chips, action button text, and small metadata tags.
     */
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
)