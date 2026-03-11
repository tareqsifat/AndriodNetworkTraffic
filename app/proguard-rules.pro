# Proguard rules for NetworkMonitor
-keepattributes *Annotation*
-keep class com.example.networkmonitor.data.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
