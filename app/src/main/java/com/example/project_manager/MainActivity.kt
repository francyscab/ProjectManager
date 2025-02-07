package com.example.project_manager

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.button.MaterialButton
import com.google.firebase.messaging.FirebaseMessaging


class MainActivity : AppCompatActivity() {

    //permesssi per notifiche su android 14+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("PERMISSION FOR API LEVEL >= 33", "GRANTED")
            retrieveDeviceToken()
        } else {
            Log.d("PERMISSION FOR API LEVEL >= 33", "NOT-GRANTED")
        }
    }

    //recupero token associato al dispositivo per notifiche
    private fun retrieveDeviceToken(){
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            val msg = getString(R.string.msg_token_fmt, token)
            Log.d(TAG, "Token recuperato $msg")
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        })
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                retrieveDeviceToken()
                Log.d("PERMISSION FOR API LEVEL >= 33", "ALREADY-GRANTED")
            }
            else {
                // Directly ask for the permission to the user
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }else{
            retrieveDeviceToken()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        findViewById<MaterialButton>(R.id.button_login).setOnClickListener {

            val it = Intent(this, LoginActivity::class.java)
            startActivity(it)
        }

        findViewById<MaterialButton>(R.id.button_signin).setOnClickListener {
            val it = Intent(this, SignInActivity::class.java)
            startActivity(it)
        }
    }

    override fun onStart() {
        super.onStart()
        askNotificationPermission()
    }




}