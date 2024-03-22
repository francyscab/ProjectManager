package com.example.project_manager

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        val button_login=findViewById<Button>(R.id.button_login)
        val button_signin=findViewById<Button>(R.id.button_signin)

        button_login.setOnClickListener {

            val it = Intent(applicationContext, LoginActivity::class.java)
            startActivity(it)
        }
        button_signin.setOnClickListener {
            val it = Intent(applicationContext, SignInActivity::class.java)
            startActivity(it)
        }
    }
}