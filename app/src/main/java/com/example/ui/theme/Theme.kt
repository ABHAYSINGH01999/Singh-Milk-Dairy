package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = NavyPrimary, 
    onPrimary = PureWhite, 
    primaryContainer = NavyLight,
    onPrimaryContainer = PureWhite,
    secondary = GoldSecondary, 
    onSecondary = NavyDark, 
    secondaryContainer = GoldDark,
    onSecondaryContainer = PureWhite,
    tertiary = GoldLight, 
    onTertiary = NavyDark,
    tertiaryContainer = GoldSecondary,
    onTertiaryContainer = NavyDark,
    background = NavyDark, 
    onBackground = PureWhite, 
    surface = NavySurface, 
    onSurface = PureWhite,
    surfaceVariant = NavyLight,
    onSurfaceVariant = PureWhite,
    error = androidx.compose.ui.graphics.Color(0xFFCF6679),
    onError = NavyDark
)

private val LightColorScheme = DarkColorScheme

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is disabled to enforce brand color
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
