package com.example.project_manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.privacysandbox.tools.core.generator.build
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var notificationManager: NotificationManager ?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the notification channel
        createNotificationChannel(
            "projectmanager.leaderNotification", // Replace with your channel ID
            "Leader Notification", // Replace with your channel name
            "All notificatio for leader" // Replace with your channel description
        )


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

    private fun createNotificationChannel(id: String, name: String, description: String) {
        // Check if the Android version is Oreo or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(id, name, importance).apply {
                this.description = description
            }
            // Get the NotificationManager
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Create the notification channel
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification() {
        // Create an explicit intent for an Activity in your app
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // Build the notification
        val builder = NotificationCompat.Builder(this, "leaderNotification") // Use your channel ID
            .setSmallIcon(R.drawable.ic_notification) // Replace with your notification icon
            .setContentTitle("My Notification Title")
            .setContentText("My Notification Text")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Send the notification
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(1, builder.build())
        }
    }
}