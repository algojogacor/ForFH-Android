# kotlinx-serialization (Reflection-less serializer lookup)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.aryariap.forfh.**$$serializer { *; }
-keepclassmembers class com.aryariap.forfh.** { *** Companion; }
-keepclasseswithmembers class com.aryariap.forfh.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Retrofit
-keepattributes Signature, Exceptions
# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
