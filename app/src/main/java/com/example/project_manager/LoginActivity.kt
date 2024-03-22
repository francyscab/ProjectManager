package com.example.project_manager

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

    }

    public override fun onStart() {
        super.onStart()
        //var currentUser= auth.currentUser
        //Log.d(ContentValues.TAG,"UTENTE = $currentUser")
        // Check if user is signed in (non-null) and update UI accordingly.
        //if(currentUser != null)
            startActivity(Intent(this,LoggedActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        // Disconnetti l'utente da Firebase
        FirebaseAuth.getInstance().signOut()
    }





}