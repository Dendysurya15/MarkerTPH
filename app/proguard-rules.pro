# Keep your models and preserve their generic signatures
-keepclassmembers class com.cbi.markertph.data.model.** { *; }
-keepclasseswithmembers class com.cbi.markertph.data.model.** { *; }

# Keep API interfaces and responses
-keep class com.cbi.markertph.data.api.** { *; }
-keepclassmembers class com.cbi.markertph.data.api.** { *; }

# Retrofit config
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

 -keep,allowobfuscation,allowshrinking interface retrofit2.Call
 -keep,allowobfuscation,allowshrinking class retrofit2.Response

 -keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson specific rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers enum * { *; }

# Keep TypeToken and its generic signatures
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Explicitly preserve all serialization members
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep the GZIP related classes
-keep class java.io.ByteArrayOutputStream { *; }
-keep class java.util.zip.GZIPInputStream { *; }
-keep class java.util.zip.GZIPOutputStream { *; }

# Keep Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepclassmembers class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.android.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep LiveData and ViewModel
-keep class androidx.lifecycle.** { *; }
-keep class androidx.arch.core.** { *; }

# Keep Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep your specific response classes
-keep class com.cbi.markertph.data.model.FetchResponseTPH { *; }
-keep class com.cbi.markertph.data.model.UploadResponse { *; }
-keep class com.cbi.markertph.data.model.BatchUploadRequest { *; }

# Keep TypeToken and generic type information
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Keep R8 specific rules
-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile