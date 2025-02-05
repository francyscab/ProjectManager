package com.example.project_manager

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.project_manager.services.UserService
import com.example.project_manager.utils.FileRepository
import com.example.project_manager.utils.UserRepository
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private val userRepository = UserRepository()
    private val userService = UserService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    override fun onStart() {
        super.onStart()

        // Check if user is already logged in
        if (userRepository.isLogged()) {
            startActivity(Intent(this, LoggedActivity::class.java))
            finish()
            return
        }

        setupLoginButton()
    }

    private fun setupLoginButton() {
        val loginButton = findViewById<Button>(R.id.button_login)
        val emailField = findViewById<EditText>(R.id.username_field)
        val passwordField = findViewById<EditText>(R.id.password_field)
        val errorText = findViewById<TextView>(R.id.errore_credenziali_login)

        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            errorText.text = "" // Clear previous error messages

            when {
                email.isEmpty() -> {
                    errorText.text = "Please enter your email"
                }
                password.isEmpty() -> {
                    errorText.text = "Please enter your password"
                }
                else -> {
                    // Attempt login
                    userRepository.login(
                        email,
                        password,
                        onSuccess = {
                            // Only navigate to LoggedActivity on successful login
                            val intent = Intent(this, LoggedActivity::class.java)
                            startActivity(intent)
                            finish()
                        },
                        onFailure = { exception ->
                            // Show error message on failed login
                            Log.e(TAG, "Login failed", exception)
                            errorText.text = "Invalid email or password"
                            Toast.makeText(
                                this,
                                "Authentication failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}