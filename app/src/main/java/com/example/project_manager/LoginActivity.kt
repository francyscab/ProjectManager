package com.example.project_manager

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.project_manager.models.Role
import com.example.project_manager.repository.NotificationHelper
import com.example.project_manager.services.UserService
import com.example.project_manager.repository.UserRepository
import com.example.project_manager.services.ChatService
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private val userRepository = UserRepository()
    private val userService = UserService()
    private val chatService= ChatService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already logged in before setting content view
        if (userRepository.isLogged()) {
            setupNotificationsAndNavigate()
            return
        }

        setContentView(R.layout.activity_login)
        setupLoginButton()
        setUpRegisterLink()
    }

    private fun setupNotificationsAndNavigate() {
        // Use a coroutine to handle notifications before navigating
        lifecycleScope.launch {
            try {
                setupNotifications()
                // Navigate to home after notifications are set up
                withContext(Dispatchers.Main) {
                    navigateToHome()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up notifications", e)
                // Fallback to navigation even if notifications fail
                navigateToHome()
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setUpRegisterLink() {
        val registerLink = findViewById<TextView>(R.id.registerLink)
        registerLink.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        registerLink.apply {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            setTextColor(ContextCompat.getColor(context, R.color.progress_foreground))
        }
    }

    private fun setupLoginButton() {
        val loginButton = findViewById<MaterialButton>(R.id.loginButton)
        val emailField = findViewById<TextInputEditText>(R.id.emailInput)
        val passwordField = findViewById<TextInputEditText>(R.id.passwordInput)
        val errorText = findViewById<TextView>(R.id.error)

        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            errorText.text = ""

            when {
                email.isEmpty() -> {
                    errorText.text = getString(R.string.inserisci_la_mail)
                }
                password.isEmpty() -> {
                    errorText.text = getString(R.string.inserisci_la_password)
                }
                else -> {
                    userRepository.login(
                        email,
                        password,
                        onSuccess = {
                            navigateToHome()
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "Login failed", exception)
                            errorText.text = getString(R.string.password_o_email_non_corretti)
                        }
                    )
                }
            }
        }
    }

    private suspend fun setupNotifications() {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser ?: return

            // Ottieni il ruolo dell'utente
            val userDocument = FirebaseFirestore.getInstance()
                .collection("utenti")
                .document(currentUser.uid)
                .get()
                .await()

            val roleString = userDocument.getString("role")
            Log.d(TAG, "Role retrieved: $roleString")
            val role = roleString?.let { Role.valueOf(it) } ?: return

            // Configura i listener usando NotificationManager
            NotificationManager.getInstance(this).setupListeners(currentUser.uid, role)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up notifications", e)
        }
    }
}