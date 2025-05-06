package dev.riss.muse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Define the single, app-specific color scheme using the new colors
private val AppColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryWhite,
    secondary = SecondaryPaleBlue,
    onSecondary = OnSecondaryNavy,
    tertiary = TertiaryYellow, // Tertiary Yellow for sparse accents
    background = BackgroundNavy,
    onBackground = OnBackgroundOffWhite,
    surface = SurfaceNavyLight,
    onSurface = OnSurfaceOffWhite,
    error = ErrorSoftCoral,
    onError = OnErrorWhite
    // Add other specific overrides if needed, e.g., surfaceVariant, outline
)

@Composable
fun MuseTheme(
    // Remove darkTheme and dynamicColor parameters as we're using a fixed theme
    // darkTheme: Boolean = isSystemInDarkTheme(),
    // dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Remove the logic for selecting scheme based on darkTheme/dynamicColor
    // val colorScheme = when { ... }

    // Directly use the new AppColorScheme
    val colorScheme = AppColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}