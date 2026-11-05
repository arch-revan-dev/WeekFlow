package com.weekflow.app.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.weekflow.app.domain.model.AppThemeMode

@Immutable
data class IosColors(
    val background: Color,
    val card: Color,
    val cardSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val separator: Color,
    val accent: Color,
    val isDark: Boolean = false
)

val LocalIosColors = staticCompositionLocalOf {
    IosColors(
        background = IosLightBackground,
        card = IosLightCard,
        cardSecondary = IosLightCardSecondary,
        textPrimary = IosLightTextPrimary,
        textSecondary = IosLightTextSecondary,
        separator = IosLightSeparator,
        accent = IosBlue,
        isDark = false
    )
}

private val LightIosColors = IosColors(
    background = IosLightBackground,
    card = IosLightCard,
    cardSecondary = IosLightCardSecondary,
    textPrimary = IosLightTextPrimary,
    textSecondary = IosLightTextSecondary,
    separator = IosLightSeparator,
    accent = IosBlue,
    isDark = false
)

private val DarkIosColors = IosColors(
    background = IosDarkBackground,
    card = IosDarkCard,
    cardSecondary = IosDarkCardSecondary,
    textPrimary = IosDarkTextPrimary,
    textSecondary = IosDarkTextSecondary,
    separator = IosDarkSeparator,
    accent = IosBlue,
    isDark = true
)

private val LightMaterialColorScheme = lightColorScheme(
    primary = IosBlue,
    onPrimary = Color.White,
    background = IosLightBackground,
    onBackground = IosLightTextPrimary,
    surface = IosLightCard,
    onSurface = IosLightTextPrimary,
    surfaceVariant = IosLightCardSecondary,
    onSurfaceVariant = IosLightTextSecondary,
    outline = Color(0xFFD1D1D6),
    outlineVariant = IosLightSeparator
)

private val DarkMaterialColorScheme = darkColorScheme(
    primary = IosBlue,
    onPrimary = Color.White,
    background = IosDarkBackground,
    onBackground = IosDarkTextPrimary,
    surface = IosDarkCard,
    onSurface = IosDarkTextPrimary,
    surfaceVariant = IosDarkCardSecondary,
    onSurfaceVariant = IosDarkTextSecondary,
    outline = Color(0xFF545458),
    outlineVariant = IosDarkSeparator
)

@Composable
fun iosSwitchColors(): SwitchColors {
    val isDark = LocalIosColors.current.isDark
    return SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = IosGreen,
        checkedBorderColor = Color.Transparent,
        uncheckedThumbColor = Color.White,
        uncheckedTrackColor = if (isDark) Color(0xFF48484C) else Color(0xFFE5E5EA),
        uncheckedBorderColor = Color.Transparent,
        disabledCheckedThumbColor = Color.White.copy(alpha = 0.6f),
        disabledCheckedTrackColor = IosGreen.copy(alpha = 0.4f),
        disabledUncheckedThumbColor = Color.White.copy(alpha = 0.5f),
        disabledUncheckedTrackColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA).copy(alpha = 0.6f)
    )
}

@Composable
fun iosPrimaryButtonColors(containerColor: Color = IosBlue): ButtonColors {
    val isDark = LocalIosColors.current.isDark
    return ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = Color.White,
        disabledContainerColor = if (isDark) containerColor.copy(alpha = 0.32f) else containerColor.copy(alpha = 0.38f),
        disabledContentColor = Color.White.copy(alpha = 0.55f)
    )
}

@Composable
fun WeekFlowTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val iosColors = if (isDark) DarkIosColors else LightIosColors
    val materialColors = if (isDark) DarkMaterialColorScheme else LightMaterialColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalIosColors provides iosColors
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = WeekFlowTypography,
            content = content
        )
    }
}
