package com.example.project_manager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private val TAG = "UserProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // Inizializza Firestore
        firestore = FirebaseFirestore.getInstance()

        // Recupera i riferimenti ai componenti della UI
        val profileImageView = findViewById<ImageView>(R.id.profileImageView)
        val nameTextView = findViewById<TextView>(R.id.nameTextView)
        val roleTextView = findViewById<TextView>(R.id.roleTextView)
        val emailTextView = findViewById<TextView>(R.id.emailTextView)

        // Recupera l'email dell'utente attualmente loggato
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email

        if (email.isNullOrEmpty()) {
            Log.e(TAG, "Nessuna email trovata per l'utente loggato")
            finish()
            return
        }

        // Carica i dati utente
        firestore.collection("utenti")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.e(TAG, "Nessun utente trovato con l'email: $email")
                    finish()
                } else {
                    val userDoc = documents.first()
                    val name = userDoc.getString("name") ?: "N/A"
                    val role = userDoc.getString("role") ?: "N/A"
                    val emailFromDb = userDoc.getString("email") ?: "N/A"

                    nameTextView.text = name
                    roleTextView.text = role
                    emailTextView.text = emailFromDb

                    val profileImageUrl = userDoc.getString("profile_image_url")
                    if (!profileImageUrl.isNullOrEmpty()) {
                        // Carica l'immagine del profilo (puoi usare una libreria come Picasso o Glide)
                        // Picasso.get().load(profileImageUrl).into(profileImageView)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Errore durante il recupero dei dati: ${exception.message}")
                finish()
            }

        // Listener su bottone logout
        val logoutButton = findViewById<Button>(R.id.logout_button)
        logoutButton.setOnClickListener {
            // Chiama la funzione per il logout
            logoutUser()
        }
    }

    // Funzione che gestisce il logout
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
