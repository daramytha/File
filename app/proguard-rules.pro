# Keep data classes
-keep class com.example.filemanager.** { *; }

# Kotlin metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
