-keep class com.example.** { *; }
-keep interface com.example.** { *; }
-keepclassmembers class com.example.** { *; }

# Kotlin reflection
-keep class kotlin.** { *; }
-keep interface kotlin.** { *; }

# Coroutines
-keep class kotlinx.coroutines.** { *; }
-keep interface kotlinx.coroutines.** { *; }

# Android Jetpack
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Compose
-keep class androidx.compose.** { *; }

-dontwarn java.lang.invoke.*
-dontwarn android.content.pm.PackageManager
