package com.example.project_manager

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.AdapterView
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

        val err_profile_image=findViewById<TextView>(R.id.errore_profile_immage)
        val err_name=findViewById<TextView>(R.id.errore_nome)
        val err_surname=findViewById<TextView>(R.id.errore_surname)
        val err_role=findViewById<TextView>(R.id.errore_role)
        val err_email=findViewById<TextView>(R.id.errore_email)
        val err_pw1=findViewById<TextView>(R.id.errore_pw1)
        val err_pw2=findViewById<TextView>(R.id.errore_pw2)

        //gestione selezione immagine profilo
        findViewById<Button>(R.id.button_select_profile_image).setOnClickListener {
            checkAndRequestPermissions()
        }


        val button_signup=findViewById<Button>(R.id.button_signin_signup)


        button_signup.setOnClickListener {

            err_name.setText("")
            err_role.setText("")
            err_email.setText("")
            err_pw1.setText("")
            err_pw2.setText("")

            val name = findViewById<EditText>(R.id.name_signin_field).text.toString()
            val surname = findViewById<EditText>(R.id.surname_signin_field).text.toString()
            val roleSelected = findViewById<Spinner>(R.id.role_signin_field)
            val email = findViewById<EditText>(R.id.email_signin_field).text.toString()
            val pw1 = findViewById<EditText>(R.id.password_signin_field).text.toString()
            val pw2 = findViewById<EditText>(R.id.password_conf_signin_field).text.toString()

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
                val role: Role = when (roleSelected.selectedItem.toString().uppercase()) {
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

    private fun check_all_data(name: String, surname:String, role: Spinner, email:String, pw1:String, pw2:String, err_profile_image:TextView, err_name:TextView, err_surname:TextView, err_role:TextView, err_email: TextView, err_pw1: TextView, err_pw2: TextView): Boolean{
        var check_campi=true;
        if(globalImageUri==null){
            err_profile_image.setText("missing profile image")
            check_campi=false
        }

        if(name==""){
            err_name.setText("missing name")
            check_campi=false;
        }
        if(surname==""){
            err_name.setText("missing name")
            check_campi=false;
        }
        if(surname==""){
            err_name.setText("missing surname")
            check_campi=false;
        }
        if(role.selectedItemPosition== AdapterView.INVALID_POSITION){
            err_role.setText("select your business role")
            check_campi=false;
        }
        if(!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\$".toRegex())){
            err_email.setText("missing email")
            check_campi=false
        }
        if(pw1==""){
            err_pw1.setText("missing password")
            check_campi=false
        }
        if(pw1.length<=6){
            err_pw1.setText("password too short")
            check_campi=false
        }
        if(pw2==""){
            err_pw2.setText("missing password")
            check_campi=false
        }
        if(pw1!=pw2){
            check_campi=false
            err_pw2.setText("not matching")
        }

        if(check_campi) {
            err_name.setText("")
            err_surname.setText("")
            err_role.setText("")
            err_email.setText("")
            err_pw1.setText("")
            err_pw2.setText("")
            return true
        }
        return false
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
