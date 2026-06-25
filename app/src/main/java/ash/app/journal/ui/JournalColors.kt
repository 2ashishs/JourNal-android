package ash.app.journal.ui

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

object JournalColors {
    val PastelBackground = Color(0xFFF9F6F0)
    val DefaultCardSurface = Color(0xFFFFFFFF)
    val PrimaryDarkText = Color(0xFF1C1C1A)
    val SecondaryMutedText = Color(0xFF575752)

    // Eisenhower Matrix Semantics
    const val URGENT = "#EF5350"       // Red Accent
    const val IMPORTANT = "#FFEE58"    // Yellow Accent
    const val IN_PROGRESS = "#66BB6A"  // Green Accent
    const val DELAY = "#42A5F5"     // Blue Accent

    val Palette = listOf(URGENT, IMPORTANT, IN_PROGRESS, DELAY)

    fun fromHex(hex: String?, fallback: Color = DefaultCardSurface): Color {
        if (hex == null) return fallback
        return try {
            Color(hex.toColorInt())
        } catch (_: Exception) {
            fallback
        }
    }
}