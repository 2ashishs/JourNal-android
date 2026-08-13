package ash.app.journal.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class TagColorScheme(
    val tagRed: Color,
    val tagYellow: Color,
    val tagGreen: Color,
    val tagBlue: Color,
    val tagDefault: Color = Color.Transparent
)
