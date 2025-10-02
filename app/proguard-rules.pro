# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn com.google.devtools.ksp.processing.SymbolProcessorProvider

#================================================================================
# Moshi Rules
#================================================================================
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# Keep Moshi annotations
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault

# Keep JsonQualifier and Json annotations
-keep,allowobfuscation,allowshrinking @interface com.squareup.moshi.JsonQualifier
-keep,allowobfuscation,allowshrinking @interface com.squareup.moshi.Json

# Keep Moshi core classes
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclassmembers class com.squareup.moshi.** { *; }

# Keep all model classes and their members
-keep class com.aught.wakawaka.data.** { *; }
-keepclassmembers class com.aught.wakawaka.data.** {
    <init>(...);
    <fields>;
}

# Keep generic signature of classes used with Moshi (needed for reflection)
-keepattributes Signature
-keepattributes *Annotation*

# Keep custom JsonAdapter classes
-keep class * extends com.squareup.moshi.JsonAdapter {
    <init>(...);
    <fields>;
}

# Moshi uses reflection to look up adapters
-keepclassmembers class ** {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}

# If you're using Moshi's Kotlin code gen (KSP)
-keep class **JsonAdapter {
    <init>(...);
    <fields>;
    <methods>;
}

#================================================================================
# Retrofit Rules
#================================================================================
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Keep API service interfaces
-keep interface com.aught.wakawaka.data.** { *; }
-keep interface com.aught.wakawaka.workers.** { *; }

# Keep annotation default values (e.g., retrofit2.http.Field).
-keepattributes AnnotationDefault

# Keep inherited services.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

#================================================================================
# OkHttp and Okio Rules
#================================================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*
