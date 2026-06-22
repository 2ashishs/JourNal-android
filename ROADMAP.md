# 📋 RoadMap

## 🛠️ Infrastructure & Aesthetics (New additions)

- 🎨 Material 3 Color System: Transitioning our hardcoded colors into a dynamic Material 3 Theme utilizing ColorScheme (supporting seamless Light and Dark mode transitions without breaking text contrast).

- ✨ Personalized Splash Screen & Brand Identity: Implementing the modern Android SplashScreen API (handling the cold-start window smoothly on Android 12+) and designing a cohesive visual landing identity.

## 🎙️ Media & Core Features

- Audio Clips Integration: Setting up the Android MediaRecorder API to capture short local voice notes and saving their file URIs to Room.

- Video Clips Integration: Wiring up the Activity Result Contracts for video capture and embedding a compact local ExoPlayer/Media3 view.

- Color Filter System: Adding interactive color tokens right in the Top App Bar to stream filtered database queries (WHERE hexColor = :selectedColor) in real-time.

## 🐛 Refinements & Future-Proofing

- The Bottom Sheet "Bounce" Fix: Debugging the Material 3 ModalBottomSheet state machine to prevent that annoying rubber-banding trigger when swiping closed.

- Accessibility Overhaul: Implementing proper semantic properties, custom contentDescription states, and minimum touch target footprints to make our journal work seamlessly with TalkBack.

- UI-UX Freshness for GenZ / Kids: Injecting vibrant, bento-grid inspired design aesthetics, micro-interactions, haptic feedback triggers, and playful typography variants to give the app a high-energy vibe.