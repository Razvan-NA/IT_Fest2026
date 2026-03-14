package com.falldetector.app

import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FallFCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .update("fcmToken", token)
        Log.d("FCM", "Token updated: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "Message received! Data: ${message.data}")

        val fromName = message.data["fromName"] ?: "Utilizator"
        val lat = message.data["latitude"] ?: "0"
        val lng = message.data["longitude"] ?: "0"

        // Start the foreground service — this works even when app is killed
        // because FCM temporarily wakes the app for data messages
        val intent = Intent(this, FallReceivedService::class.java).apply {
            putExtra("name", fromName)
            putExtra("lat", lat)
            putExtra("lng", lng)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}