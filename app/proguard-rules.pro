# R8 rules for the release and perf builds. The libraries (Media3, Room, Hilt, WorkManager, Glance, Coil,
# Firebase) ship their own consumer rules.

# Keep line numbers in crash reports; the source file name is not needed.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# The Cast framework reads the OptionsProvider's class name from the manifest meta-data and creates it by reflection.
-keep class com.mobyle.abbay.presentation.cast.AbbayCastOptions { <init>(); }

# Read-along's speech engine: Vosk calls into its native library through JNA, which finds classes, fields and
# callbacks by name at run time. JNA's desktop-only references (AWT) don't exist on Android.
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-keep class org.vosk.** { *; }
-dontwarn java.awt.**
