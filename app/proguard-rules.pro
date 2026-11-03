# GhostStream relies on kotlinx serialization and Ktor route data classes.
-keepclassmembers class kotlinx.serialization.** { *; }
-keep class kotlinx.serialization.** { *; }
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn org.slf4j.**

# Ktor Server Rules required for minification (R8)
-keep class io.ktor.** { *; }
-keep class org.slf4j.** { *; }
-keepclassmembers class io.ktor.** { *; }
-keepclassmembers class * implements io.ktor.server.engine.ApplicationEngineFactory { *; }

# WebRTC's native JNI_OnLoad calls the private org.jni_zero.JniInit.init() and
# methods annotated @CalledByNative. Nothing in Java calls them, so R8 removes them
# and the native side aborts with an RTC_CHECK. The AAR ships no consumer rules.
-keep class org.jni_zero.** { *; }
-keepclassmembers class * {
    @org.jni_zero.CalledByNative *;
    @org.jni_zero.CalledByNativeUnchecked *;
    @org.jni_zero.AccessedByNative *;
}

# WebRTC keep rules.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, Exceptions
-keep class org.webrtc.** { *; }
-keep interface org.webrtc.** { *; }
-keepclassmembers class org.webrtc.** { *; }
-dontwarn org.webrtc.**
-keep class * extends org.webrtc.** { *; }
-keep class * implements org.webrtc.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Media3 muxer
-keep class androidx.media3.muxer.** { *; }
-dontwarn androidx.media3.muxer.**
