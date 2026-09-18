# ====================================================================
# General Android & ProGuard Settings
# ====================================================================

# Preserve debugging information in stack traces (essential for Crashlytics)
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

# Keep Javascript interfaces if WebView is used
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ====================================================================
# Project Custom Classes & Models
# ====================================================================

# Keep model classes intact (crucial for Retrofit/Gson parsing)
-keep class com.messages.smart.sms.models.** { *; }
-keepclassmembers class com.messages.smart.sms.models.** { *; }

# Keep service and interface classes
-keep class com.messages.smart.sms.services.** { *; }
-keep class com.messages.smart.sms.interfaces.** { *; }
-keep class com.messages.smart.sms.common.** { *; }

# ====================================================================
# Gson
# ====================================================================

-dontwarn sun.misc.Unsafe
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.annotations.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ====================================================================
# Retrofit 2
# ====================================================================

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}

# ====================================================================
# Glide
# ====================================================================

-keep public class * extends com.github.bumptech.glide.module.AppGlideModule { *; }
-keep public class * extends com.github.bumptech.glide.module.LibraryGlideModule { *; }
-keep class com.github.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
-keep class com.github.bumptech.glide.integration.okhttp3.OkHttpLibraryGlideModule { *; }
-keepclassmembers class * {
    @com.github.bumptech.glide.annotation.GlideOption <methods>;
    @com.github.bumptech.glide.annotation.GlideType <methods>;
}
-dontwarn com.github.bumptech.glide.load.resource.bitmap.VideoDecoder
-dontwarn com.github.bumptech.glide.load.resource.gif.GifFrameLoader

# ====================================================================
# Shimmer (Facebook)
# ====================================================================

-keep class com.facebook.shimmer.** { *; }
-dontwarn com.facebook.shimmer.**

# ====================================================================
# Google Mobile Ads (AdMob)
# ====================================================================

-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.**

# ====================================================================
# Google User Messaging Platform (UMP / ConsentInformation)
# ====================================================================

-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.ump.**

# ====================================================================
# Facebook Audience Network & Mediation
# ====================================================================

-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**

# ====================================================================
# Firebase (Analytics, Config, Crashlytics)
# ====================================================================

-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**