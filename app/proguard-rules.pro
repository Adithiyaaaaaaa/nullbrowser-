# NullBrowser Production ProGuard Rules

# Strip all logging for production
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Preserve JNI entries
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep the NativeLib and its methods
-keep class com.nullbrowser.privacy.NativeLib { *; }

# Obfuscate everything else
-repackageclasses ''
-allowaccessmodification
-overloadaggressively

# WebView safety
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}
