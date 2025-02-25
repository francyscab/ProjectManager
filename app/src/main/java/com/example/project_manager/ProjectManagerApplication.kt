package com.example.project_manager

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class ProjectManagerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inizializza Firebase
        FirebaseApp.initializeApp(this)

        // Inizializza App Check
        try {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            Log.d("FirebaseAppCheck", "App Check inizializzato con successo")
        } catch (e: Exception) {
            Log.e("FirebaseAppCheck", "Errore nell'inizializzazione di App Check", e)
        }
    }
}