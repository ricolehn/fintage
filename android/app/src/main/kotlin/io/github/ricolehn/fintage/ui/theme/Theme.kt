package io.github.ricolehn.fintage.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.ricolehn.fintage.R

/*
 * The look of Fintage: an almost black base in the dark theme and warm sand in the light theme, glass
 * panels with hairline edges, white (or espresso) play buttons and a restrained champagne gold accent.
 * The values come from the tokens of the Fintage theme for Jellyfin.
 */

/** The ease of Fintage, cubic-bezier(0.22, 1, 0.36, 1): quick to start, slow to settle. */
val FintageEase = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

object Palette {
    val Ink = Color(0xFF0B0B0D)
    val Gold = Color(0xFFD7B87F)
    val GoldLight = Color(0xFFEACF9C)
    val GoldDeep = Color(0xFFC9A66E)
    val PlayWhite = Color(0xFFF4F3EF)
    val Espresso = Color(0xFF524132)
    val Sand = Color(0xFFEDE8DE)
    val Bronze = Color(0xFF896734)
    val Heart = Color(0xFFE5484D)
    val TmdbGreen = Color(0xFF90CEA1)
    val TmdbBlue = Color(0xFF01B4E4)
}

@Immutable
data class FintageColors(
    val isDark: Boolean,
    val glass: Color,
    val glassStrong: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val ring: Color,
    val playContainer: Color,
    val playContent: Color,
    val cardTop: Color,
    val cardBottom: Color,
    val dimText: Color,
    val faintText: Color,
    val divider: Color,
    val grain: Color,
    val backgroundGlow: Color,
    val scrim: Color,
    val chipSelected: Color,
    val onChipSelected: Color,
) {
    val gold: Color get() = Palette.Gold
    val goldGradient: Brush get() = Brush.verticalGradient(listOf(Palette.GoldLight, Palette.GoldDeep))
    val starGradient: Brush get() = Brush.linearGradient(listOf(Palette.TmdbGreen, Palette.TmdbBlue))
    val cardGradient: Brush get() = Brush.verticalGradient(listOf(cardTop, cardBottom))
}

private val DarkFintage = FintageColors(
    isDark = true,
    glass = Color(0x9E12121C),
    glassStrong = Color(0xD9141417),
    glassBorder = Color(0x14FFFFFF),
    glassHighlight = Color(0x0FFFFFFF),
    ring = Color(0x17FFFFFF),
    playContainer = Palette.PlayWhite,
    playContent = Palette.Ink,
    cardTop = Color(0xFF1C1C1F),
    cardBottom = Color(0xFF111113),
    dimText = Color(0xFFA3A29D),
    faintText = Color(0xFF6F6E6A),
    divider = Color(0x0FFFFFFF),
    grain = Color.White,
    backgroundGlow = Color(0xFF1B1B1F),
    scrim = Color(0xFF0B0B0D),
    chipSelected = Palette.PlayWhite,
    onChipSelected = Palette.Ink,
)

private val LightFintage = FintageColors(
    isDark = false,
    glass = Color(0xB8FAF6EF),
    glassStrong = Color(0xE6F7F3EC),
    glassBorder = Color(0x1A4F3F2C),
    glassHighlight = Color(0xB3FDFBF7),
    ring = Color(0x1A4F3F2C),
    playContainer = Palette.Espresso,
    playContent = Color(0xFFFAF7F1),
    cardTop = Color(0xFFF0EBE2),
    cardBottom = Color(0xFFE5DFD3),
    dimText = Color(0xFF6E6559),
    faintText = Color(0xFF9A9183),
    divider = Color(0x144F3F2C),
    grain = Color(0xFF4D3824),
    backgroundGlow = Color(0xFFF6F2EB),
    scrim = Color(0xFFEDE8DE),
    chipSelected = Palette.Espresso,
    onChipSelected = Color(0xFFFAF7F1),
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Palette.Gold,
    onPrimary = Color(0xFF1B160D),
    primaryContainer = Color(0xFF3B3222),
    onPrimaryContainer = Palette.GoldLight,
    inversePrimary = Palette.Bronze,
    secondary = Palette.PlayWhite,
    onSecondary = Palette.Ink,
    secondaryContainer = Color(0xFF2A2A2E),
    onSecondaryContainer = Palette.PlayWhite,
    tertiary = Palette.TmdbBlue,
    onTertiary = Palette.Ink,
    tertiaryContainer = Color(0xFF15323B),
    onTertiaryContainer = Color(0xFFBDEBF7),
    background = Palette.Ink,
    onBackground = Color(0xFFECEBE8),
    surface = Palette.Ink,
    onSurface = Color(0xFFECEBE8),
    surfaceVariant = Color(0xFF1C1C1F),
    onSurfaceVariant = Color(0xFFA3A29D),
    surfaceTint = Palette.Gold,
    inverseSurface = Color(0xFFECEBE8),
    inverseOnSurface = Color(0xFF1B1B1E),
    error = Color(0xFFE5695E),
    onError = Palette.Ink,
    errorContainer = Color(0xFF5C1F1F),
    onErrorContainer = Color(0xFFFFDAD5),
    outline = Color(0xFF2F2F33),
    outlineVariant = Color(0xFF1F1F23),
    scrim = Color.Black,
    surfaceBright = Color(0xFF2A2A2E),
    surfaceDim = Palette.Ink,
    surfaceContainerLowest = Color(0xFF070708),
    surfaceContainerLow = Color(0xFF121214),
    surfaceContainer = Color(0xFF161619),
    surfaceContainerHigh = Color(0xFF1C1C1F),
    surfaceContainerHighest = Color(0xFF242428),
)

private val LightScheme: ColorScheme = lightColorScheme(
    primary = Palette.Bronze,
    onPrimary = Color(0xFFFCF9F3),
    primaryContainer = Color(0xFFEBDCC0),
    onPrimaryContainer = Color(0xFF3D2D14),
    inversePrimary = Palette.Gold,
    secondary = Palette.Espresso,
    onSecondary = Color(0xFFFAF7F1),
    secondaryContainer = Color(0xFFDDD4C6),
    onSecondaryContainer = Color(0xFF332D26),
    tertiary = Color(0xFF0E7C99),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCDE9F1),
    onTertiaryContainer = Color(0xFF07313D),
    background = Palette.Sand,
    onBackground = Color(0xFF332D26),
    surface = Palette.Sand,
    onSurface = Color(0xFF332D26),
    surfaceVariant = Color(0xFFE5DFD3),
    onSurfaceVariant = Color(0xFF6E6559),
    surfaceTint = Palette.Bronze,
    inverseSurface = Color(0xFF332D26),
    inverseOnSurface = Color(0xFFF5F1E9),
    error = Color(0xFFA13A30),
    onError = Color.White,
    errorContainer = Color(0xFFF6D8D3),
    onErrorContainer = Color(0xFF410D08),
    outline = Color(0xFFD4CBBC),
    outlineVariant = Color(0xFFE0D9CC),
    scrim = Color.Black,
    surfaceBright = Color(0xFFFAF7F1),
    surfaceDim = Color(0xFFDDD6C9),
    surfaceContainerLowest = Color(0xFFFAF7F1),
    surfaceContainerLow = Color(0xFFF5F1E9),
    surfaceContainer = Color(0xFFF0EBE2),
    surfaceContainerHigh = Color(0xFFE9E3D8),
    surfaceContainerHighest = Color(0xFFE0D9CC),
)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun style(size: Int, weight: FontWeight, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

val FintageTypography = Typography(
    displayLarge = style(48, FontWeight.Bold, 54, -0.025),
    displayMedium = style(40, FontWeight.Bold, 46, -0.022),
    displaySmall = style(34, FontWeight.Bold, 40, -0.02),
    headlineLarge = style(30, FontWeight.Bold, 36, -0.02),
    headlineMedium = style(26, FontWeight.SemiBold, 32, -0.018),
    headlineSmall = style(22, FontWeight.SemiBold, 28, -0.014),
    titleLarge = style(20, FontWeight.SemiBold, 26, -0.012),
    titleMedium = style(16, FontWeight.SemiBold, 22, -0.006),
    titleSmall = style(14, FontWeight.SemiBold, 20, 0.0),
    bodyLarge = style(16, FontWeight.Normal, 24, -0.004),
    bodyMedium = style(14, FontWeight.Normal, 21, 0.0),
    bodySmall = style(12, FontWeight.Normal, 17, 0.004),
    labelLarge = style(14, FontWeight.SemiBold, 20, 0.0),
    labelMedium = style(12, FontWeight.Medium, 16, 0.01),
    labelSmall = style(11, FontWeight.SemiBold, 14, 0.08),
)

val FintageShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

/** The radius of the cards, 0.75em in the theme. */
val CardShape = RoundedCornerShape(12.dp)

val LocalFintageColors = staticCompositionLocalOf { DarkFintage }
val LocalFilmGrain = staticCompositionLocalOf { true }

@Composable
fun FintageTheme(darkTheme: Boolean = true, filmGrain: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalFintageColors provides if (darkTheme) DarkFintage else LightFintage,
        LocalFilmGrain provides filmGrain,
    ) {
        val scheme = if (darkTheme) DarkScheme else LightScheme
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive(),
            typography = FintageTypography,
            shapes = FintageShapes,
        ) {
            // the pages are drawn on the background without a Surface, so text and icons take its color here
            CompositionLocalProvider(LocalContentColor provides scheme.onBackground, content = content)
        }
    }
}

object Fintage {
    val colors: FintageColors
        @Composable get() = LocalFintageColors.current
}
