# 📋 Changelog for v1.0.2

- ProGuard optimization rules for release builds.
- Added CHANGELOG.md
- Allow other apps to share media / text to create Entry
- Markdown support in `Details`
- Fix color related issues
- Migrate DB to change COLUMN `hexColor:String` to `colorTag: EntryColorTag`
- Extract all text to `strings.xml` for transliteration (except `autoTitle` in `JournalViewModel`)
- Display entries in reverse order (latest first).
- Updated move (drag) entry logic for reverse order in `JournalViewModel`.
- MarkDownText support for lists, starting with `* ` and `+ `
- MarkDownText support for inline links `<URL>` and `[URL Text](URL)`