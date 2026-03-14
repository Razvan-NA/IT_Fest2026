package com.falldetector.app

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FallReceivedActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context, name: String, lat: String, lng: String) {
            val intent = Intent(context, FallReceivedActivity::class.java).apply {
                putExtra("name", name)
                putExtra("lat", lat)
                putExtra("lng", lng)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen
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

        // Dismiss keyguard
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, null)
        }

        // Cancel the full-screen notification
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.cancel(FallReceivedService.ALERT_NOTIF_ID)

        setContentView(R.layout.activity_fall_received)

        val name = intent.getStringExtra("name") ?: "Utilizator"
        val lat = intent.getStringExtra("lat") ?: "0"
        val lng = intent.getStringExtra("lng") ?: "0"

        findViewById<TextView>(R.id.tvFallName).text = "⚠️ $name a căzut!"
        findViewById<TextView>(R.id.tvFallLocation).text = "Locație: $lat, $lng"

        findViewById<Button>(R.id.btnOpenMaps).setOnClickListener {
            stopReceivedService()
            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($name)")
            val mapsIntent = Intent(Intent.ACTION_VIEW, uri)
            mapsIntent.setPackage("com.google.android.apps.maps")
            if (mapsIntent.resolveActivity(packageManager) != null) {
                startActivity(mapsIntent)
            } else {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://maps.google.com/?q=$lat,$lng")
                )
                startActivity(browserIntent)
            }
            finish()
        }

        findViewById<Button>(R.id.btnOk).setOnClickListener {
            stopReceivedService()
            finish()
        }
    }

    private fun stopReceivedService() {
        val intent = Intent(this, FallReceivedService::class.java)
        stopService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopReceivedService()
    }
}