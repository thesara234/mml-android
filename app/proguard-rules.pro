# Gson maps JSON onto these classes by field name — keep them intact.
-keep class nz.co.mmltonga.app.data.model.** { *; }
-keepattributes Signature, *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
# Retrofit
-keepattributes Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * { @retrofit2.http.* <methods>; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
