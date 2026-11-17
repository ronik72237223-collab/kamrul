package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AppLightColorScheme = lightColorScheme(
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
    surfaceContainerLowest = BentoSurface,
    surfaceContainerLow = BentoSurface,
    surfaceContainer = BentoSurface,
    surfaceContainerHigh = BentoBackground,
    surfaceContainerHighest = BentoSurfaceVariant,
    outline = BentoBorder,
    outlineVariant = Color(0xFFE5E7EB),
    error = Color(0xFFDC2626),
    onError = Color.White
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun appTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = SlateInk,
    unfocusedTextColor = SlateInk,
    disabledTextColor = SlateMuted,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = BentoBackground,
    cursorColor = StrawberryRed,
    focusedBorderColor = StrawberryRed,
    unfocusedBorderColor = BentoBorder,
    focusedLabelColor = StrawberryRed,
    unfocusedLabelColor = SlateMuted,
    focusedPlaceholderColor = SlateMuted,
    unfocusedPlaceholderColor = SlateMuted,
    focusedLeadingIconColor = StrawberryRed,
    unfocusedLeadingIconColor = SlateMuted,
    focusedTrailingIconColor = SlateInk,
    unfocusedTrailingIconColor = SlateMuted
)

@Composable
fun StrawberryAratProTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppLightColorScheme,
        typography = AppTypography,
        shapes = AppShapes
    ) {
        CompositionLocalProvider(LocalContentColor provides SlateInk) {
            content()
        }
    }
}
