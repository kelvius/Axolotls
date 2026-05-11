package com.example.axolotls.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BeeYellow,
    secondary = BeeYellowDark,
    tertiary = HoneyAccent,
    background = BeeBlack,
    surface = BeeBlackLight,
    onPrimary = BeeBlack,
    onSecondary = BeeBlack,
    onTertiary = BeeBlack,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF3A3A3A)
)

private val LightColorScheme = lightColorScheme(
    primary = BeeYellowDark,
    secondary = BeeGray,
    tertiary = HoneyAccent,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = BeeBlack,
    onSecondary = Color.White,
    onTertiary = BeeBlack,
    onBackground = BeeBlack,
    onSurface = BeeBlack,
    surfaceVariant = FadedYellow
)

@Composable
fun AxolotlsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
