package nz.co.mmltonga.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* Brand palette — the same tokens as the web app (Moana navy, Kula red, Lagoon teal). */
object Mml {
    val Moana950 = Color(0xFF071F2B)
    val Moana900 = Color(0xFF0B2E3F)
    val Moana800 = Color(0xFF0F3D53)
    val Moana700 = Color(0xFF134E6A)
    val Moana600 = Color(0xFF1A6488)
    val Moana100 = Color(0xFFE3EEF4)
    val Moana50 = Color(0xFFF1F7FA)
    val Kula700 = Color(0xFF9E0F1C)
    val Kula600 = Color(0xFFC1121F)
    val Kula100 = Color(0xFFFBE4E6)
    val Lagoon600 = Color(0xFF0E8C86)
    val Lagoon100 = Color(0xFFD9F2EF)
    val Sun500 = Color(0xFFE9A23B)
    val Sun100 = Color(0xFFFDF0DA)
    val Ink = Color(0xFF10222E)
    val Ink2 = Color(0xFF3C5260)
    val Ink3 = Color(0xFF6A7F8C)
    val Line = Color(0xFFDDE5EA)
    val Canvas = Color(0xFFF4F7F9)
}

/** Status colours (background, foreground) matching the web badges. */
@Immutable
data class Tone(val bg: Color, val fg: Color)

fun statusTone(status: String, dark: Boolean = false): Tone = when (status) {
    "draft" -> Tone(Color(0xFFEEF2F4), Color(0xFF4A5D69))
    "awaiting_payment" -> Tone(Color(0xFFFDF1D8), Color(0xFF8A5A00))
    "ready_for_payout" -> Tone(Color(0xFFE2EFFA), Color(0xFF11578A))
    "on_hold" -> Tone(Color(0xFFFDE7D9), Color(0xFF9A3D07))
    "completed" -> Tone(Color(0xFFDCF3E8), Color(0xFF11683F))
    "cancelled" -> Tone(Color(0xFFFBE4E6), Color(0xFF9E0F1C))
    "refunded" -> Tone(Color(0xFFECE7FB), Color(0xFF5131A3))
    else -> Tone(Color(0xFFEEF2F4), Color(0xFF4A5D69))
}.let { if (dark) Tone(it.fg.copy(alpha = .28f), Color.White.copy(alpha = .92f)) else it }

private val LightColors = lightColorScheme(
    primary = Mml.Moana800,
    onPrimary = Color.White,
    primaryContainer = Mml.Moana100,
    onPrimaryContainer = Mml.Moana900,
    secondary = Mml.Kula600,
    onSecondary = Color.White,
    secondaryContainer = Mml.Kula100,
    onSecondaryContainer = Mml.Kula700,
    tertiary = Mml.Lagoon600,
    tertiaryContainer = Mml.Lagoon100,
    background = Mml.Canvas,
    onBackground = Mml.Ink,
    surface = Color.White,
    onSurface = Mml.Ink,
    surfaceVariant = Mml.Moana50,
    onSurfaceVariant = Mml.Ink3,
    surfaceContainer = Color.White,
    surfaceContainerLow = Mml.Moana50,
    surfaceContainerHigh = Color.White,
    outline = Mml.Line,
    outlineVariant = Color(0xFFEAF0F3),
    error = Mml.Kula700,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8CC7E4),
    onPrimary = Mml.Moana950,
    primaryContainer = Mml.Moana700,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFFF8A93),
    onSecondary = Color(0xFF3B0006),
    secondaryContainer = Mml.Kula700,
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF6FD3CC),
    background = Mml.Moana950,
    onBackground = Color(0xFFE5EEF2),
    surface = Color(0xFF0E2A38),
    onSurface = Color(0xFFE5EEF2),
    surfaceVariant = Color(0xFF15384A),
    onSurfaceVariant = Color(0xFFA8C0CC),
    surfaceContainer = Color(0xFF0E2A38),
    surfaceContainerLow = Color(0xFF113243),
    surfaceContainerHigh = Color(0xFF15384A),
    outline = Color(0xFF2A4C5E),
    outlineVariant = Color(0xFF1C3D4F),
    error = Color(0xFFFF8A93),
)

/** Display numbers and headings use a serif, like Fraunces on the web. */
val DisplayFamily = FontFamily.Serif

private val base = Typography()
val MmlTypography = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineLarge = base.headlineLarge.copy(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    headlineSmall = base.headlineSmall.copy(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

/** Big money figures (tabular, serif). */
val MoneyStyle = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, letterSpacing = (-0.5).sp, fontFeatureSettings = "tnum")

val LocalDark = staticCompositionLocalOf { false }

@Composable
fun MmlTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalDark provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = MmlTypography,
            shapes = Shapes(
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(22.dp),
            ),
            content = content,
        )
    }
}
