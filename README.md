# JourNal-android

A lightweight, local-first, offline-only personal micro-journaling platform built to master modern Android MVVM architecture, Jetpack Compose, and Room DB.
Inspired by a streamlined text-focused micro-blogging experience.

## 🚀 Key Features Implemented

- **Local-First Architecture**: Powered by Room DB as the single source of truth (SSOT).
- **Session Preservation**: ViewModel-tracked draft states protect entry text from accidental sheet dismissals.
- **Dynamic Reordering**: Custom, zero-drift drag-and-drop mechanics built using native Compose gesture trackers.
- **Media & Design Refinements**: High-legibility accent color bars, non-cropping full-width media previews, and low-profile ergonomic action panels.
- **Native Dual-Media Sharing**: Secure image-and-text sharing via Android's FileProvider system sheet hooks.

## 🛠️ Architecture & Tech Stack

- **UI Framework**: Jetpack Compose (100% declarative UI)
- **Architecture Pattern**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Asynchronous Pipeline**: Kotlin Coroutines & Asynchronous Reactive `StateFlow` streams
- **Local Database**: Room Persistence Library (SQLite abstraction)
- **Image Loading**: Coil3 (Async image fetching engine)
