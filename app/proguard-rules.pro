# R8 rules for the release and perf builds. The libraries (Media3, Room, Hilt, WorkManager, Glance, Coil,
# Firebase) ship their own consumer rules.

# Keep line numbers in crash reports; the source file name is not needed.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# The Cast framework reads the OptionsProvider's class name from the manifest meta-data and creates it by reflection.
-keep class com.mobyle.abbay.presentation.cast.AbbayCastOptions { <init>(); }
