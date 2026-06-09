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

// Dark Navy and Gold Theme
private val DarkColorScheme = darkColorScheme(
    primary = RichGold,
    onPrimary = DeepNavyBlue,
    primaryContainer = ColorThemeHelper.MediumDarkForest,
    onPrimaryContainer = ColorThemeHelper.LightOlive,
    secondaryContainer = ColorThemeHelper.MediumDarkForest,
    onSecondaryContainer = ColorThemeHelper.LightOlive,
    background = ColorThemeHelper.DeepDarkForest,
    onBackground = ColorThemeHelper.BackgroundSlateLight,
    surface = ColorThemeHelper.SurfaceDarkForest,
    onSurface = ColorThemeHelper.BackgroundSlateLight,
    surfaceVariant = ColorThemeHelper.SurfaceVariantDark,
    outline = ColorThemeHelper.OutlineDark,
    error = ErrorRed
)

// Light High Density Palette (White, Dark Blue, and Gold)
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = OnPrimaryWhite,
    primaryContainer = PrimaryContainerBlueLight,
    onPrimaryContainer = OnPrimaryContainerBlueDark,
    secondaryContainer = SecondaryContainerGold,
    onSecondaryContainer = OnSecondaryContainerGoldDark,
    background = BackgroundLight,
    onBackground = OnBackgroundNavy,
    surface = SurfaceWhite,
    onSurface = OnSurfaceNavy,
    surfaceVariant = SurfaceVariantDividerBlue,
    outline = OutlineGold,
    error = ErrorRed
)

// Helper object to avoid referencing non-declared symbols inside color scheme defs
object ColorThemeHelper {
    val SageGreen = androidx.compose.ui.graphics.Color(0xFF9CD487)
    val DeepDarkForest = androidx.compose.ui.graphics.Color(0xFF0B192C)      // Premium Dark Navy
    val MediumDarkForest = androidx.compose.ui.graphics.Color(0xFF13315C)    // Dark Navy accent
    val SurfaceDarkForest = androidx.compose.ui.graphics.Color(0xFF1C2D42)   // Clean Slate Navy
    val SurfaceVariantDark = androidx.compose.ui.graphics.Color(0xFF22384F)  // Light Slate Navy
    val LightOlive = androidx.compose.ui.graphics.Color(0xFFFDF6E2)          // Soft Gold Tint
    val BackgroundSlateLight = androidx.compose.ui.graphics.Color(0xFFF4F7FC)// Soft Blue White
    val OutlineDark = androidx.compose.ui.graphics.Color(0xFFD4AF37)         // Elegant Gold
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We set dynamicColor default to false so our brand-new High Density design is visible
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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
