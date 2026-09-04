# 📋 Changelog for v1.0.4

- Pressing "clear text" icon in Search box on search page, brings back keyboard focus on Search box
- Refactored `JournalScreenElements` to `HomeScreen`
- Created a new package `journal.ui.screens` for all screens and their elements
- Moved `HomeScreen` and `SearchScreen` into the `screens` package
- Separated out screen elements from `HomeScreen` into `EntryCreateElements`, `EntryDetailsElements` and `MarkdownText`