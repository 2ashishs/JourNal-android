package ash.app.journal.ui

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

object JournalColors {
    val DefaultSurface = Color(0xFFFFFFFF)

    // Your four distinct primary color options
    val Red = "#EF5350"
    val Yellow = "#FFEE58"
    val Green = "#66BB6A"
    val Blue = "#42A5F5"

    val Palette = listOf(Red, Yellow, Green, Blue)

    fun fromHex(hex: String?, fallback: Color = DefaultSurface): Color {
        if (hex == null) return fallback
        return try {
            Color(hex.toColorInt())
        } catch (_: Exception) {
            fallback
        }
    }
}