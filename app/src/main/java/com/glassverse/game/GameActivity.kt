// app/src/main/java/com/glassverse/game/GameActivity.kt
package com.glassverse.game

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.glassverse.game.utils.StorageManager

class GameActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var storageManager: StorageManager

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        keepScreenOn()
        setContentView(R.layout.activity_game)

        storageManager = StorageManager(this)
        webView = findViewById(R.id.gameWebView)

        setupWebView()
        setupBackPress()
        loadGame()
    }

    private fun hideSystemUI() {
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } catch (e: Exception) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
    }

    private fun keepScreenOn() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setSupportZoom(false)
            displayZoomControls = false
            builtInZoomControls = false
            textZoom = 100
            blockNetworkImage = false
            loadsImagesAutomatically = true

            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = false
            }
        }

        webView.setBackgroundColor(0xFF0A0A1A.toInt())
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        webView.isScrollbarFadingEnabled = true
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // JavaScript Bridge
        val bridge = GameBridge(this, storageManager)
        webView.addJavascriptInterface(bridge, "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Inject saved state
                val savedState = storageManager.loadGameState()
                if (savedState.isNotEmpty()) {
                    val escaped = savedState
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                    view?.evaluateJavascript(
                        "try{if(typeof loadSavedState==='function')loadSavedState('$escaped');}catch(e){console.log(e);}",
                        null
                    )
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                if (BuildConfig.DEBUG) {
                    consoleMessage?.let {
                        android.util.Log.d(
                            "GLASSVERSE_JS",
                            "${it.message()} -- line ${it.lineNumber()} of ${it.sourceId()}"
                        )
                    }
                }
                return true
            }
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript(
                    "try{if(typeof handleBackPress==='function'){handleBackPress();}else{'true';}}catch(e){'true';}",
                ) { result ->
                    if (result == "true" || result == "\"true\"" || result == null) {
                        // Allow default back behavior
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                    // else: handled by JS
                }
            }
        })
    }

    private fun loadGame() {
        webView.loadUrl("file:///android_asset/game.html")
    }

    private fun saveCurrentState() {
        try {
            webView.evaluateJavascript(
                "try{if(typeof getGameState==='function'){AndroidBridge.saveState(JSON.stringify(getGameState()));}}catch(e){}",
                null
            )
        } catch (e: Exception) {
            // WebView might be destroyed
        }
    }

    override fun onPause() {
        saveCurrentState()
        webView.onPause()
        webView.pauseTimers()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.resumeTimers()
        hideSystemUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    override fun onDestroy() {
        saveCurrentState()
        webView.stopLoading()
        webView.removeJavascriptInterface("AndroidBridge")
        webView.destroy()
        super.onDestroy()
    }
}

/**
 * JavaScript Interface Bridge
 * All methods callable from game.html via AndroidBridge.methodName()
 */
class GameBridge(
    private val activity: GameActivity,
    private val storageManager: StorageManager
) {

    @JavascriptInterface
    fun vibrate(milliseconds: Int) {
        try {
            val ms = milliseconds.toLong().coerceIn(1, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(ms)
                }
            }
        } catch (e: Exception) {
            // Vibration not available on this device
        }
    }

    @JavascriptInterface
    fun hapticLight() {
        vibrate(10)
    }

    @JavascriptInterface
    fun hapticMedium() {
        vibrate(30)
    }

    @JavascriptInterface
    fun hapticHeavy() {
        vibrate(60)
    }

    @JavascriptInterface
    fun saveState(json: String) {
        try {
            storageManager.saveGameState(json)
        } catch (e: Exception) {
            // Save failed silently
        }
    }

    @JavascriptInterface
    fun loadState(): String {
        return try {
            storageManager.loadGameState()
        } catch (e: Exception) {
            ""
        }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        return "{\"sdk\":${Build.VERSION.SDK_INT},\"model\":\"${Build.MODEL}\",\"brand\":\"${Build.BRAND}\"}"
    }

    @JavascriptInterface
    fun getAppVersion(): String {
        return try {
            val pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    @JavascriptInterface
    fun clearData() {
        storageManager.clearAll()
    }

    @JavascriptInterface
    fun log(message: String) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d("GLASSVERSE_BRIDGE", message)
        }
    }
}