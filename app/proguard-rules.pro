# Add project specific ProGuard rules here.

# Keep Room entities
-keep class com.jnetaol.binoculars.data.model.** { *; }

# Keep Coil
-keep class coil.** { *; }
-dontwarn coil.**

# CameraX
-keep class androidx.camera.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Compose
-dontwarn androidx.compose.**
