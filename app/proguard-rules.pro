-keepattributes JavascriptInterface
-keepattributes *Annotation*

-keep class com.glassverse.game.GameBridge {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class com.glassverse.game.GameActivity$GameBridge {
    @android.webkit.JavascriptInterface <methods>;
}

-keepclassmembers class com.glassverse.game.GameBridge {
    public *;
}

-keep class com.glassverse.game.** { *; }

-dontwarn androidx.**
-keep class androidx.** { *; }

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
    public void *(android.webkit.WebView, java.lang.String);
}