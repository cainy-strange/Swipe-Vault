package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SwipeVaultDarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = ObsidianBlack,
    primaryContainer = CharcoalHigh,
    onPrimaryContainer = PureWhite,
    secondary = EmeraldKeep,
    onSecondary = ObsidianBlack,
    secondaryContainer = EmeraldKeepContainer,
    onSecondaryContainer = EmeraldKeep,
    tertiary = AmberSkipAccent,
    onTertiary = ObsidianBlack,
    tertiaryContainer = AmberSkipContainer,
    onTertiaryContainer = AmberSkip,
    error = CrimsonDelete,
    onError = PureWhite,
    errorContainer = CrimsonDeleteContainer,
    onErrorContainer = CrimsonDelete,
    background = ObsidianBlack,
    onBackground = PureWhite,
    surface = CharcoalSurface,
    onSurface = PureWhite,
    surfaceVariant = CharcoalElevated,
    onSurfaceVariant = SilverText,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = CharcoalSurface,
    surfaceContainer = CharcoalElevated,
    surfaceContainerHigh = CharcoalHigh,
    outline = HairlineBorder,
    outlineVariant = ColorTokens.SubtleDivider
)

private object ColorTokens {
    val SubtleDivider = androidx.compose.ui.graphics.Color(0xFF1F2127)
}

val SwipeVaultShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SwipeVaultTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SwipeVaultDarkColorScheme,
        typography = SwipeVaultTypography,
        shapes = SwipeVaultShapes,
        content = content
    )
}
