package com.example.project_manager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project_manager.models.User
import com.example.project_manager.services.UserService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class UserProfileActivity : AppCompatActivity() {

    private val TAG = "UserProfileActivity"
    val userService= UserService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)




        lifecycleScope.launch{
            val user =userService.getCurrentUser()!!
            setView(user)
        }

        val logoutButton = findViewById<Button>(R.id.logout_button)
        logoutButton.setOnClickListener {
            logoutUser()
        }
    }


    private fun setView(user: User){
        val profileImageView = findViewById<ImageView>(R.id.profileImageView)
        val nameTextView = findViewById<TextView>(R.id.nameTextView)
        val surnameTextView = findViewById<TextView>(R.id.surnameTextView)
        val roleTextView = findViewById<TextView>(R.id.roleTextView)
        val emailTextView = findViewById<TextView>(R.id.emailTextView)

        nameTextView.text = user.name
        surnameTextView.text = user.surname
        roleTextView.text = user.role.toString()
        emailTextView.text = user.email

    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()

        // Pulisce i dati di sessione memorizzati in SharedPreferences o altro sistema
        val preferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        preferences.edit().clear().apply()

        // Reindirizza l'utente alla schermata di login
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        // Termina l'activity corrente per non tornare indietro con il tasto indietro
        finish()
    }
}
