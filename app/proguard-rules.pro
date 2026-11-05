# ==============================================================================
# WeekFlow v4.0.0 ProGuard & R8 Optimization Rules
# ==============================================================================

-keepattributes *Annotation*, InnerClasses, Signature, SourceFile, LineNumberTable, EnclosingMethod

# Preserve all WeekFlow application classes, Glance widget, Room entities/DAOs, and Serialization models
-keep class com.weekflow.app.** { *; }
-keep interface com.weekflow.app.** { *; }
-keep enum com.weekflow.app.** { *; }

# --- Kotlinx Serialization ---
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep,allowobfuscation,allowshrinking class * {
    @kotlinx.serialization.Serializable class *;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Room Database & Generated _Impl classes ---
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class **.*_Impl { *; }
-dontwarn androidx.room.paging.**

# --- Jetpack Glance & DataStore Proto (Critical for Widget in Release Build) ---
-keep class androidx.glance.** { *; }
-keep interface androidx.glance.** { *; }
-keep class androidx.datastore.** { *; }
-keep interface androidx.datastore.** { *; }

# --- Android Jetpack Compose ---
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }

# --- AndroidX Core, Koin & Navigation ---
-keep class org.koin.** { *; }
-dontwarn androidx.navigation.**
-dontwarn kotlinx.serialization.**
-dontwarn org.koin.**
