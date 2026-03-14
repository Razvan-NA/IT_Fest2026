package com.falldetector.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var isMonitoring = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()

        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val btnToggle = findViewById<Button>(R.id.btnToggle)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // Afiseaza numele userului
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("name") ?: ""
                    tvUserName.text = "Salut, $name!"
                }
        }

        tvStatus.text = "Monitorizare: ACTIV ✅"
        FallDetectionService.startService(this)

        btnToggle.setOnClickListener {
            if (isMonitoring) {
                FallDetectionService.stopService(this)
                tvStatus.text = "Monitorizare: OPRIT ❌"
                btnToggle.text = "Pornește monitorizarea"
                isMonitoring = false
            } else {
                FallDetectionService.startService(this)
                tvStatus.text = "Monitorizare: ACTIV ✅"
                btnToggle.text = "Oprește monitorizarea"
                isMonitoring = true
            }
        }

        btnLogout.setOnClickListener {
            FallDetectionService.stopService(this)
            auth.signOut()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val toRequest = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), 100)
        }
    }
}