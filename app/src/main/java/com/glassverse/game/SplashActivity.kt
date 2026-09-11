// app/src/main/java/com/glassverse/game/SplashActivity.kt
package com.glassverse.game

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContentView(R.layout.activity_splash)

        val titleText = findViewById<TextView>(R.id.titleText)
        val loadingText = findViewById<TextView>(R.id.loadingText)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Animate title
        titleText.animate()
            .alpha(1f)
            .setDuration(1000)
            .setStartDelay(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Animate loading text
        loadingText.animate()
            .alpha(0.6f)
            .setDuration(700)
            .setStartDelay(700)
            .start()

        // Animate progress bar
        progressBar.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(900)
            .start()

        // Progress animation
        val progressAnimator = ValueAnimator.ofInt(0, 100)
        progressAnimator.duration = 2200
        progressAnimator.interpolator = AccelerateDecelerateInterpolator()
        progressAnimator.addUpdateListener { animation ->
            progressBar.progress = animation.animatedValue as Int
        }
        progressAnimator.startDelay = 1000
        progressAnimator.start()

        // Navigate to game
        handler.postDelayed({
            startActivity(Intent(this, GameActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 3500)
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

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}