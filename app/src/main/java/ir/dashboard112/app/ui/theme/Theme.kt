package ir.dashboard112.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import ir.dashboard112.app.data.preferences.ThemeModePreference

enum class AppAccentPalette(
    val key: String,
    val labelFa: String,
    val lightPrimary: Color,
    val darkPrimary: Color
) {
    BLUE("BLUE", "آبی کلاسیک", Color(0xFF1D4ED8), Color(0xFF60A5FA)),
    EMERALD("EMERALD", "سبز زمردی", Color(0xFF047857), Color(0xFF34D399)),
    INDIGO("INDIGO", "نیلی مدرن", Color(0xFF4338CA), Color(0xFF818CF8)),
    AMBER("AMBER", "کهربایی گرم", Color(0xFFB45309), Color(0xFFFBBF24)),
    ROSE("ROSE", "سرخابی تیره", Color(0xFFBE123C), Color(0xFFFB7185));

    companion object {
        fun fromKey(key: String): AppAccentPalette =
            entries.firstOrNull { it.key == key } ?: BLUE
    }
}

@Composable
fun Dashboard112Theme(
    themeMode: ThemeModePreference = ThemeModePreference.LIGHT,
    primaryColorKey: String = "BLUE",
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeModePreference.LIGHT -> false
        ThemeModePreference.DARK -> true
        ThemeModePreference.SYSTEM -> isSystemInDarkTheme()
    }
    val palette = AppAccentPalette.fromKey(primaryColorKey)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = palette.darkPrimary,
            onPrimary = Color(0xFF0F172A),
            background = Color(0xFF0F172A),
            surface = Color(0xFF1E293B),
            onBackground = Color(0xFFF8FAFC),
            onSurface = Color(0xFFF1F5F9)
        )
    } else {
        lightColorScheme(
            primary = palette.lightPrimary,
            onPrimary = Color.White,
            background = Color(0xFFF8FAFC),
            surface = Color.White,
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF1E293B)
        )
    }

    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = baseDensity.density,
        fontScale = baseDensity.fontScale * fontScale.coerceIn(0.85f, 1.25f)
    )

    val persianTypography = Typography(
        headlineMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 32.sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
    )

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = persianTypography,
            content = content
        )
    }
}
