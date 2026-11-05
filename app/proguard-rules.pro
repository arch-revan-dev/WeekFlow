# ==============================================================================
# WeekFlow v4.0.0 ProGuard & R8 Optimization Rules
# ==============================================================================

# --- Kotlinx Serialization ---
-keepattributes *Annotation*, InnerClasses, Signature, SourceFile, LineNumberTable

# Preserve all models annotated with @Serializable and their generated serializers
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

# Preserve WeekFlow domain models, DTOs, entities, and serialization schemas
-keep class com.weekflow.app.domain.model.** { *; }
-keep class com.weekflow.app.data.** { *; }

# --- Room Database ---
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# --- Android Jetpack Compose & Glance ---
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.glance.** { *; }

# --- App Widgets, Receivers & Entry Points ---
-keep class com.weekflow.app.MainActivity { *; }
-keep class com.weekflow.app.WeekFlowApp { *; }
-keep class com.weekflow.app.widget.** { *; }
-keep class com.weekflow.app.core.notifications.** { *; }

# --- AndroidX Core, Koin & Navigation ---
-dontwarn androidx.navigation.**
-dontwarn kotlinx.serialization.**
-dontwarn org.koin.**
