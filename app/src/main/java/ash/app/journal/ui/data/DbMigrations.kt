package ash.app.journal.ui.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create a pristine temporary table matching your new non-nullable Kotlin schema exactly
        // Ensure you match your primary key setups (e.g., id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS journal_entries_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                details TEXT NOT NULL,
                mediaPath TEXT,
                mediaType TEXT NOT NULL,
                orderIndex INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                colorTag TEXT NOT NULL DEFAULT 'DEFAULT'
            )
        """.trimIndent()
        )

        // 2. Transfer all records across, dynamically transforming legacy values inline using SQLite CASE statements
        db.execSQL(
            """
            INSERT INTO journal_entries_new (id, title, details, mediaPath, mediaType, orderIndex, timestamp, colorTag)
            SELECT 
                id, 
                title, 
                details, 
                mediaPath, 
                mediaType,
                orderIndex,
                timestamp,
                CASE 
                    WHEN hexColor = '#EF5350' THEN 'RED'
                    WHEN hexColor = '#FFEE58' THEN 'YELLOW'
                    WHEN hexColor = '#66BB6A' THEN 'GREEN'
                    WHEN hexColor = '#42A5F5' THEN 'BLUE'
                    WHEN hexColor IS NULL THEN 'DEFAULT'
                    ELSE 'DEFAULT'
                END
            FROM journal_entries
        """.trimIndent()
        )

        // 3. Destructively clear out the old table layout structural constraints
        db.execSQL("DROP TABLE journal_entries")

        // 4. Rename the polished temporary table to step into the production line
        db.execSQL("ALTER TABLE journal_entries_new RENAME TO journal_entries")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create the new table strictly in this migration
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `link_metadata` (
                `url` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `imageUrl` TEXT NOT NULL,
                `fetchedAt` INTEGER NOT NULL,
                PRIMARY KEY(`url`)
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create new table without orderIndex
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `journal_entries_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `details` TEXT NOT NULL,
                `colorTag` TEXT NOT NULL,
                `mediaType` TEXT NOT NULL,
                `mediaPath` TEXT,
                `timestamp` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // 2. Copy data across
        db.execSQL(
            """
            INSERT INTO `journal_entries_new` (`id`, `title`, `details`, `colorTag`, `mediaType`, `mediaPath`, `timestamp`)
            SELECT `id`, `title`, `details`, `colorTag`, `mediaType`, `mediaPath`, `timestamp` FROM `journal_entries`
            """.trimIndent()
        )

        // 3. Drop old table
        db.execSQL("DROP TABLE `journal_entries` ")

        // 4. Rename new table
        db.execSQL("ALTER TABLE `journal_entries_new` RENAME TO `journal_entries` ")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recent_searches` (
                `query` TEXT NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                PRIMARY KEY(`query`)
            )
        """.trimIndent()
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Promote entries containing [card](url=...) or http links to LINK type if they were TEXT
        db.execSQL(
            """
            UPDATE journal_entries 
            SET mediaType = 'LINK' 
            WHERE mediaType = 'TEXT' 
            AND (details LIKE '%[card](url=%' OR details LIKE '%http%')
        """.trimIndent()
        )

        // 2. Optional: If sqlite replace is available, sanitize the syntax in text
        // (SQLite's built-in REPLACE handles exact string replacements)
        db.execSQL(
            """
            UPDATE journal_entries 
            SET details = REPLACE(REPLACE(details, '[card](url=', '<'), ')', '>')
            WHERE details LIKE '%[card](url=%'
        """.trimIndent()
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE journal_entries ADD COLUMN reminderTimestamp INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE journal_entries ADD COLUMN isReminderCompleted INTEGER NOT NULL DEFAULT 0")
    }
}
