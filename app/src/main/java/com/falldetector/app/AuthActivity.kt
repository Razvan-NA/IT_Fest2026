package com.falldetector.app

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<AppCompatButton>(R.id.btnLogin)
        val btnGoToSignup = findViewById<AppCompatButton>(R.id.btnGoToSignup)
        val tvError = findViewById<TextView>(R.id.tvError)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            if (email.isEmpty() || pass.isEmpty()) {
                tvError.text = "Completează email și parola"
                return@setOnClickListener
            }
            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener { goToMain() }
                .addOnFailureListener { tvError.text = it.message }
        }

        btnSignup.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            if (email.isEmpty() || pass.isEmpty() || name.isEmpty() || phone.isEmpty()) {
                tvError.text = "Completează toate câmpurile"
                return@setOnClickListener
            }
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener { result ->
                    val uid = result.user!!.uid
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        val user = hashMapOf(
                            "uid" to uid,
                            "name" to name,
                            "email" to email,
                            "phone" to phone,
                            "fcmToken" to token,
                            "lastLatitude" to 0.0,
                            "lastLongitude" to 0.0,
                            "createdAt" to System.currentTimeMillis()
                        )
                        db.collection("users").document(uid).set(user)
                            .addOnSuccessListener { goToMain() }
                    }
                }
                .addOnFailureListener { tvError.text = it.message }
        }
    }

    private fun goToMain() {
        // Actualizeaza tokenul FCM la fiecare login
        val uid = auth.currentUser?.uid
        if (uid != null) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                db.collection("users").document(uid)
                    .update("fcmToken", token)
            }
        }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}