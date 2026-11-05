# ==============================================================================
# WeekFlow v2.0.0 Proguard & R8 Optimization Rules
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

# Preserve WeekFlow domain models and clean export schema fields
-keep class com.weeklie.calendar.domain.model.** { *; }
-keep class com.weeklie.calendar.data.** { *; }

# --- Android Jetpack Compose ---
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }

# --- App Widgets & RemoteViews ---
-keep class com.weeklie.calendar.widget.ScheduleAppWidget { *; }
-keep class com.weeklie.calendar.util.ReminderBroadcastReceiver { *; }
-keep class com.weeklie.calendar.MainActivity { *; }

# --- AndroidX Core & Navigation ---
-dontwarn androidx.navigation.**
-dontwarn kotlinx.serialization.**
