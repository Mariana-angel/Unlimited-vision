package com.example.unlimitedvision.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TealHeader = Color(0xFF4E8B99)
val DarkTealBg = Color(0xFF23333A)
val CardBgTeal = Color(0xFF385563)
val DarkOliveButton = Color(0xFF2D3827)
val OliveButtonText = Color(0xFFDCDA9E)
val LightBlueButton = Color(0xFFBDE3F0)
val DarkRedButton = Color(0xFF8B2626)
val OrangeAction = Color(0xFFE88132)
val LinkCyan = Color(0xFF38B6FF)

private val DarkColorScheme = darkColorScheme(
    primary = TealHeader,
    secondary = DarkOliveButton,
    background = DarkTealBg,
    surface = CardBgTeal,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = TealHeader,
    secondary = DarkOliveButton,
    background = DarkTealBg,
    surface = CardBgTeal,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun UnlimitedVisionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
