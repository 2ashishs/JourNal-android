# 📋 Changelog for v1.0.4

- Pressing "clear text" icon in Search box on search page, brings back keyboard focus on Search box
- Refactored `JournalScreenElements` to `HomeScreen`
- Created a new package `journal.ui.screens` for all screens and their elements
- Moved `HomeScreen` and `SearchScreen` into the `screens` package
- Separated out screen elements from `HomeScreen` into `EntryCreateElements`, `EntryDetailsElements` and `MarkdownText`
- `MarkdownText` improvements: added support for `>` Quote blocks, `##` secondary header; code refactorings.
- `MarkdownToolbar` along with `MarkdownToolbarUtils` added to `CreateEntryBottomSheet`
- Media chips icon row issue fixed in `CreateEntryBottomSheet`
- Code line and code block support added in `MarkdownText`
- Tab indent support in `MarkdownToolbar`
- Icon enhancement in `MarkdownToolbar`