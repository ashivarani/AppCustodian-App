package com.appcustodian.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.appcustodian.app.R

/**
 * Primary accent color used for main interactive elements, focus borders, and active highlights.
 * Refers to `@color/primary`.
 */
val Primary: Color
    @Composable get() = colorResource(R.color.primary)

/**
 * High-contrast foreground content color for elements layered over the [Primary] color.
 * Refers to `@color/on_primary`.
 */
val OnPrimary: Color
    @Composable get() = colorResource(R.color.on_primary)

/**
 * Destructive/error state color used for high-severity actions such as "Force Stop".
 * Refers to `@color/danger`.
 */
val Danger: Color
    @Composable get() = colorResource(R.color.danger)

/**
 * General background color for application screens and container surfaces.
 * Refers to `@color/background`.
 */
val Background: Color
    @Composable get() = colorResource(R.color.background)

/**
 * Surface color for elevated components such as app cards, dialogs, and search toolbars.
 * Refers to `@color/surface`.
 */
val Surface: Color
    @Composable get() = colorResource(R.color.surface)

/**
 * Secondary text and iconography color used for package names, subtitles, and disabled hints.
 * Refers to `@color/on_surface_variant`.
 */
val OnSurfaceVariant: Color
    @Composable get() = colorResource(R.color.on_surface_variant)

/**
 * Dark contrast background color specifically reserved for the top header app bar.
 * Refers to `@color/header_background`.
 */
val HeaderBackground: Color
    @Composable get() = colorResource(R.color.header_background)

/**
 * High-contrast foreground content color for text and icons placed inside the [HeaderBackground].
 * Refers to `@color/on_header_background`.
 */
val OnHeaderBackground: Color
    @Composable get() = colorResource(R.color.on_header_background)