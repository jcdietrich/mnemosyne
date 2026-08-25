# Add default ProGuard rules from the Android SDK
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keep class com.google.mediapipe.** { *; }
-keep class io.objectbox.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-dontwarn com.google.api.client.**
-dontwarn com.google.apis.drive.**
