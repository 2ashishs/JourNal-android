package ash.app.journal.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val JournalDarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF141218),      // Deep dark backdrop
    onBackground = Color(0xFFE6E1E5),    // Crisp off-white text
    surface = Color(0xFF1D1B20),         // Sleek card surfaces
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),   // Muted borders and inputs for dark mode
    onSurfaceVariant = Color(0xFFCAC4D0),
)

val JournalLightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFBF8FD),      // Clean, bright baseline canvas
    onBackground = Color(0xFF1D1B20),    // Rich near-black text for reading legibility
    surface = Color(0xFFFFFFFF),         // Bright white card surfaces to make list items pop
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),   // Used for clean borders, dividers, and text field backgrounds
    onSurfaceVariant = Color(0xFF49454F),
)

// Define baseline light variations
private val LightTagColors = TagColorScheme(
    tagRed = Color(0xFFEF5350),
    tagYellow = Color(0xFFFFEE58),
    tagGreen = Color(0xFF66BB6A),
    tagBlue = Color(0xFF42A5F5),
    tagDefault = Color(0xFF848484)
)

// Define desaturated, contrast-safe dark variants
private val DarkTagColors = TagColorScheme(
    tagRed = Color(0xFFE57373),
    tagYellow = Color(0xFFFFF176),
    tagGreen = Color(0xFF81C784),
    tagBlue = Color(0xFF64B5F6),
    tagDefault = Color(0xFFABABAB)
)

// Set up the static hook for your composable tree views
val LocalTagColorScheme = staticCompositionLocalOf { LightTagColors }

// Simple consumer handle object to access them beautifully in UI code
object JournalTheme {
    val tagColors: TagColorScheme
        @Composable
        get() = LocalTagColorScheme.current
}

@Composable
fun JourNaLTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Dynamic color available on Android 12+ is set to False
    content: @Composable () -> Unit
) {
    val targetTagColors = if (darkTheme) DarkTagColors else LightTagColors

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> JournalDarkColorScheme
        else -> JournalLightColorScheme
    }

    CompositionLocalProvider(
        LocalTagColorScheme provides targetTagColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
