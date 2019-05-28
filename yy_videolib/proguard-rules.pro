# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/YY/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

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

-keep public class com.YY.cloud.api.**{*;}
-keep public class com.YY.cloud.editor.**{*;}
-keep public class com.YY.cloud.recorder.**{*;}
-keep public class com.YY.cloud.utils.**{*;}
-keep public class com.YY.cloud.videoplayer.**{*;}
#-keep class * implements YY.cloud.api.IVideoEditor {
#    public <methods>;
#}

#-keep class * implements YY.cloud.api.IVideoRecorder {
#    public <methods>;
#}
#-keep public class com.YY.cloud.impl.tracker.EffectTracker*;
#}
-keep public class net.surina.soundtouch.**{*;}

-keep class com.arcsoft.** {*;}
-dontwarn com.arcsoft.**

-keep class com.YY.YYimage.YYfilter.**{*;}
