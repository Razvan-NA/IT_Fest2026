package com.falldetector.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class FallAlertActivity : AppCompatActivity() {

    private var countDownTimer: CountDownTimer? = null
    private val sonarAnimators = mutableListOf<AnimatorSet>()

    companion object {
        const val NOTIFICATION_ID = 999
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, null)
        }

        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.cancel(NOTIFICATION_ID)

        setContentView(R.layout.activity_fall_alert)

        startCountdown()
        startSonarAnimation()

        findViewById<AppCompatButton>(R.id.btnImOk).setOnClickListener {
            stopEverything()
            FallDetectionService.cancelAlert(this)
            finish()
        }
    }

    private fun startCountdown() {
        val tvCountdown = findViewById<TextView>(R.id.tvCountdown)

        // Sincronizat cu serviciul
        val elapsed = System.currentTimeMillis() - FallDetectionService.fallDetectedAt
        val remaining = (15000 - elapsed).coerceAtLeast(0)

        if (remaining <= 0) {
            stopEverything()
            finish()
            return
        }

        countDownTimer = object : CountDownTimer(remaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvCountdown.text = "${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                tvCountdown.text = "0"
                stopEverything()
                finish()
            }
        }.start()
    }

    private fun startSonarAnimation() {
        listOf(
            Pair(R.id.ring1, 0L),
            Pair(R.id.ring2, 800L)
        ).forEach { (id, delay) ->
            val ring = findViewById<View>(id)

            val scaleX = ObjectAnimator.ofFloat(ring, "scaleX", 1f, 1.3f)
            val scaleY = ObjectAnimator.ofFloat(ring, "scaleY", 1f, 1.3f)
            val alpha = ObjectAnimator.ofFloat(ring, "alpha", 0.5f, 0f)

            val anim = AnimatorSet()
            anim.playTogether(scaleX, scaleY, alpha)
            anim.duration = 2400
            anim.startDelay = delay
            anim.interpolator = AccelerateDecelerateInterpolator()
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ring.scaleX = 1f
                    ring.scaleY = 1f
                    ring.alpha = 0.5f
                    anim.start()
                }
            })
            anim.start()
            sonarAnimators.add(anim)
        }
    }

    private fun stopEverything() {
        countDownTimer?.cancel()
        sonarAnimators.forEach { it.cancel() }
        sonarAnimators.clear()
        FallDetectionService.stopAlarmStatic(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopEverything()
    }
}