# 📋 RoadMap v1.0.4

## 🛠️ Features

- Reminder entry type with notifications
- Alternate carousel view for entries
- Settings screen, which enables the user,
  - to enter their details for a personalized experience
  - to switch between carousel view and list view
  - to set a biometric app lock
  - to export or import notes, as `.db` or `.json`
  - to delete all entries
  - to switch between dark and light mode
- Timeline / Date separators
- Home screen carousel header card to highlight user achievements
- Privacy notes vault
- On home screen,
  - Summary View: Show only last 3-5 entries
  - Dashboard View: Show weekly post count & total post count
  - Dynamic Filter Bar (Color Tags & Media Types): Show chips below top action bar, where
    - chips are Red-Yellow-Green-Blue-NoColor and Photo-Video-Audio-Link
    - each chip has a count, associated with entries of that chip type
    - tapping on a chip displays entries of that chip type
  - Timeline Scaffolding with Sticky Headers
  - Calendar View
  - Media Gallery View
  - "On this day" carousel card

## 🎨️ Enhancements

- Improve app icon
- Layout Mode Switcher: Toggle between compact list view and a visual 2-column media grid.
- Full screen detail page for each entry (instead of a bottom sheet)
- Add rich text editor support in details
- Add optional expiry date to auto-delete entries
- Move all colors & text to a single file 🗹☐🗷
- Landscape mode possibility or Portrait mode only

## 🐛 Fixes

- UX Fixes for "Photo", "Video", "Audio" row
- Performance: Storage & Battery Usage
- Testing & `lint` integration
- Material 3 Palette & Dark Mode Audit: Standardize container colors, surface tinting, and contrast tokens across themes.
