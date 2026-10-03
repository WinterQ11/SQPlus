package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LuxuryDarkColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = ObsidianBlack,
    primaryContainer = GoldContainer,
    onPrimaryContainer = OnGoldContainer,
    secondary = AntiqueGold,
    onSecondary = ObsidianBlack,
    secondaryContainer = ElevatedSurface,
    onSecondaryContainer = SoftIvory,
    tertiary = PlatinumSilver,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = SoftIvory,
    surface = DeepCharcoal,
    onSurface = SoftIvory,
    surfaceVariant = GraphiteCard,
    onSurfaceVariant = MutedTaupe,
    surfaceContainer = ElevatedSurface,
    surfaceContainerHigh = ElevatedSurfaceHigh,
    outline = BorderSubtle,
    outlineVariant = BorderSubtle.copy(alpha = 0.5f),
    error = MutedBurgundy,
    onError = SoftIvory,
    errorContainer = BurgundyContainer,
    onErrorContainer = OnBurgundyContainer
)

@Composable
fun SQPlusTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // SQPlus is engineered as an exclusive, consistent luxury dark interface
    MaterialTheme(
        colorScheme = LuxuryDarkColorScheme,
        typography = Typography,
        content = content
    )
}
