package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = StrawberryRed,
    onPrimary = Color.White,
    primaryContainer = StrawberryTint,
    onPrimaryContainer = StrawberryDark,
    secondary = EmeraldGreen,
    onSecondary = Color.White,
    secondaryContainer = EmeraldTint,
    onSecondaryContainer = EmeraldDark,
    tertiary = CrateAmber,
    onTertiary = Color.White,
    tertiaryContainer = CrateAmberTint,
    onTertiaryContainer = CrateAmberDark,
    background = BentoBackground,
    onBackground = SlateInk,
    surface = BentoSurface,
    onSurface = SlateInk,
    surfaceVariant = BentoSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    outline = BentoBorder,
    outlineVariant = Color(0xFFF1F5F9),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkStrawberry,
    onPrimary = Color(0xFF4C0519),
    primaryContainer = Color(0xFF881337),
    onPrimaryContainer = Color(0xFFFFE4E6),
    secondary = DarkEmerald,
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = DarkBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = DarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StrawberryAratProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
