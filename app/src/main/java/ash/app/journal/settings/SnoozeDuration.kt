package ash.app.journal.settings

enum class SnoozeDuration(val label: String, val durationMillis: Long) {
    MINUTES_15("15 minutes", 15 * 60 * 1000L),
    MINUTES_30("30 minutes", 30 * 60 * 1000L),
    HOURS_1("1 hour", 60 * 60 * 1000L),
    HOURS_2("2 hours", 2 * 60 * 60 * 1000L),
    DAYS_1("1 day", 24 * 60 * 60 * 1000L);

    companion object {
        val DEFAULT = HOURS_1

        fun fromMillis(millis: Long): SnoozeDuration {
            return entries.firstOrNull { it.durationMillis == millis } ?: DEFAULT
        }
    }
}