# --- Room Database Rules ---
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-dontwarn androidx.room.paging.**

# --- Coil Image Loader Rules ---
-keep class coil3.** { *; }
-dontwarn coil3.**

# --- Media3 / ExoPlayer Rules ---
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.ui.** { *; }
-dontwarn androidx.media3.**

# --- Kotlin Coroutines & Serialization ---
-keepclassmembers class * {
    @kotlinx.coroutines.InternalCoroutinesApi *;
}