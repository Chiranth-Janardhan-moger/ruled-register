# R8 / ProGuard rules for Ruled Register

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Moshi
-keepclassmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep class * implements com.squareup.moshi.JsonAdapter { *; }
-keep class com.chiranth7.regibook.** { *; }

# Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Android Jetpack Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
