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


# --- Kotlinx Serialization --- 
# Keep classes annotated with @Serializable and their members
-keep,includedescriptorclasses @kotlinx.serialization.Serializable class * { *; }
# Keep generated serializer classes and methods
-keep class **$$serializer { *; }
-keepclassmembers class * { *** Companion; }
-keepclassmembers class **$$serializer { public static *** Companion; }
-keepclassmembers class * { @kotlin.jvm.JvmStatic *** serializer(...); }
# Keep enum serialization methods
-keepclassmembers enum * { @kotlin.jvm.JvmStatic **[] values(); @kotlin.jvm.JvmStatic ** valueOf(java.lang.String); }

# --- Ktor & Dependencies (OkHttp/Okio used by Supabase/Coil) ---
# Keep OkHttp/Okio internal classes often needed by Ktor OkHttp engine and Coil
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-keep interface okio.** { *; }
-dontwarn okio.**

# Keep Ktor classes (may need refinement based on specific Ktor features/plugins used)
-keep class io.ktor.** { *; }
-keep interface io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep Kotlin Coroutines internals potentially used by Ktor/Supabase
-keepclassmembernames class kotlinx.coroutines.internal.** { *; }
-dontwarn kotlinx.coroutines.**

# --- Supabase-Kotlin --- 
# Supabase relies heavily on Ktor and Kotlinx Serialization, rules above should cover most.
# Add specific Supabase rules here if runtime issues are found.
# -keep class io.github.jan.supabase.** { *; } 
# -keep interface io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# --- Coil --- 
# Coil rules (Often covered by OkHttp/Okio, but keep core classes)
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**