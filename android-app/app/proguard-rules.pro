# Conservar reflexión de JNA y bindings UniFFI en compilaciones R8/ProGuard
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.Structure { *; }
-dontwarn com.sun.jna.**

-keep class com.appnotas.core.** { *; }
-keepclassmembers class com.appnotas.core.** { *; }
