# Proguard rules for HHKungfu TV
-keep class com.hhkungfu.tv.data.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn org.jsoup.**
-dontwarn okhttp3.**
-dontwarn okio.**
