package com.example.project_manager

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.example.project_manager.models.Role
import com.example.project_manager.services.UserService
import de.hdodenhof.circleimageview.CircleImageView

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


class SignInActivity : AppCompatActivity() {
    private val PICK_IMAGE_REQUEST_CODE = 1001
    private var globalImageUri: Uri? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        var userService= UserService()

        val err_profile_image=findViewById<TextView>(R.id.errore_profile_image)
        val err_name=findViewById<TextView>(R.id.errore_nome)
        val err_surname=findViewById<TextView>(R.id.errore_surname)
        val err_role=findViewById<TextView>(R.id.errore_role)
        val err_email=findViewById<TextView>(R.id.errore_email)
        val err_pw1=findViewById<TextView>(R.id.errore_pw1)
        val err_pw2=findViewById<TextView>(R.id.errore_pw2)

        val nameField = findViewById<EditText>(R.id.name_signin_field)
        val surnameField = findViewById<EditText>(R.id.surname_signin_field)
        val roleSelected = findViewById<AutoCompleteTextView>(R.id.role_signin_field)
        val emailField = findViewById<EditText>(R.id.email_signin_field)
        val pw1Field = findViewById<EditText>(R.id.password_signin_field)
        val pw2Field = findViewById<EditText>(R.id.password_conf_signin_field)



        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.role,
            R.layout.custom_dropdown_item
        )
        roleSelected.setAdapter(adapter)

        //gestione selezione immagine profilo
        findViewById<Button>(R.id.button_select_profile_image).setOnClickListener {
            checkAndRequestPermissions()
        }


        val button_signup=findViewById<Button>(R.id.button_signin_signup)


        button_signup.setOnClickListener {

            val name = nameField.text.toString()
            val surname = surnameField.text.toString()
            val email = emailField.text.toString()
            val pw1 = pw1Field.text.toString()
            val pw2 = pw2Field.text.toString()

            err_name.visibility=View.INVISIBLE
            err_role.visibility=View.INVISIBLE
            err_email.visibility=View.INVISIBLE
            err_pw1.visibility=View.INVISIBLE
            err_pw2.visibility=View.INVISIBLE



            if (check_all_data(
                    name,
                    surname,
                    roleSelected,
                    email,
                    pw1,
                    pw2,
                    err_profile_image,
                    err_name,
                    err_surname,
                    err_role,
                    err_email,
                    err_pw1,
                    err_pw2
                )
            ) {
                val role: Role = when (roleSelected.text.toString().uppercase()) {
                    "LEADER" -> Role.Leader
                    "DEVELOPER" -> Role.Developer
                    "MANAGER" -> Role.Manager
                    else -> throw IllegalArgumentException("Ruolo non valido selezionato")
                }
                userService.signInProcedure(email, pw1, name.capitalizeFirstLetter(), surname.capitalizeFirstLetter(), role, globalImageUri!!,
                    onSuccess = {
                        Log.d(
                            "SignInProcedure",
                            "Utente registrato, immagine caricata, dati salvati e login completato con successo!"
                        )
                        Toast.makeText(this, "Registrazione completata!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                    },
                    onFailure = { exception ->
                        Log.e("SignInProcedure", "Procedura fallita: ${exception.message}")
                        Toast.makeText(this, "Errore: ${exception.message}", Toast.LENGTH_LONG)
                            .show()
                    }
                )

            }
        }


    }

    private fun check_all_data(
        name: String,
        surname: String,
        role: AutoCompleteTextView,
        email: String,
        pw1: String,
        pw2: String,
        err_profile_image: TextView,
        err_name: TextView,
        err_surname: TextView,
        err_role: TextView,
        err_email: TextView,
        err_pw1: TextView,
        err_pw2: TextView
    ): Boolean {
        var check_campi = true

        // Resetta tutti gli errori all'inizio
        err_name.visibility = View.INVISIBLE
        err_surname.visibility = View.INVISIBLE
        err_role.visibility = View.INVISIBLE
        err_email.visibility = View.INVISIBLE
        err_pw1.visibility = View.INVISIBLE
        err_pw2.visibility = View.INVISIBLE

        if (globalImageUri == null) {
            err_profile_image.visibility = View.VISIBLE
            check_campi = false
        }

        if (name.isEmpty()) {
            err_name.visibility = View.VISIBLE
            check_campi = false
        }

        if (surname.isEmpty()) {
            err_surname.visibility = View.VISIBLE
            check_campi = false
        }

        if (role.text.toString().isEmpty()) {
            err_role.visibility = View.VISIBLE
            check_campi = false
        }

        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\$".toRegex())) {
            err_email.visibility = View.VISIBLE
            check_campi = false
        }

        if (pw1.isEmpty()) {
            err_pw1.visibility = View.VISIBLE
            check_campi = false
        }

        if (pw1.length <= 6) {
            err_pw1.visibility = View.VISIBLE
            check_campi = false
        }

        if (pw2.isEmpty()) {
            err_pw2.visibility = View.VISIBLE
            check_campi = false
        }

        if (pw1 != pw2) {
            check_campi = false
            err_pw2.visibility = View.VISIBLE
        }

        return check_campi
    }

    private fun checkAndRequestPermissions() {
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1002)
        } else {
            openGallery()
        }
    }

    private fun openGallery() {
        val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickImageIntent, PICK_IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val selectedImageUri: Uri? = data.data
            Log.d(TAG, "PROVA PROVA PROVA2: $selectedImageUri")
            if (selectedImageUri != null) {
                Log.d(TAG, "PROVA PROVA PROVA: $selectedImageUri")
                // Salva l'URI selezionato per il caricamento
                globalImageUri = selectedImageUri

                val profileImageView = findViewById<CircleImageView>(R.id.profile_image)

                // Sostituisci l'immagine nell'ImageView
                profileImageView.setImageURI(selectedImageUri)

                Toast.makeText(this, "Profile image updated!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFileUriFromContentUri(contentUri: Uri): Uri? {
        return try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(contentUri, "r") ?: return null
            val inputStream = parcelFileDescriptor.fileDescriptor?.let { FileInputStream(it) }
            val tempFile = File.createTempFile("temp_image", ".jpg", cacheDir)
            val outputStream = FileOutputStream(tempFile)

            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            parcelFileDescriptor.close()

            Uri.fromFile(tempFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting content URI to file URI", e)
            null
        }
    }

    fun String.capitalizeFirstLetter(): String {
        return this.firstOrNull()?.uppercase()?.plus(this.substring(1)) ?: ""
    }
}
