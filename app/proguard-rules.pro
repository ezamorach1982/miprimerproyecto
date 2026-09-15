# Gson: conserva los campos de los modelos de datos deserializados por reflexión.
-keep class com.funtv.player.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**

# Media3 / ExoPlayer
-dontwarn androidx.media3.**
