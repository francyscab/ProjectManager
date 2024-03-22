package com.example.project_manager

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val err_name=findViewById<TextView>(R.id.errore_nome)
        val err_role=findViewById<TextView>(R.id.errore_role)
        val err_email=findViewById<TextView>(R.id.errore_email)
        val err_pw1=findViewById<TextView>(R.id.errore_pw1)
        val err_pw2=findViewById<TextView>(R.id.errore_pw2)

        err_name.setText("")
        err_role.setText("")
        err_email.setText("")
        err_pw1.setText("")
        err_pw2.setText("")

        val button_signup=findViewById<Button>(R.id.button_signin_signup)


        button_signup.setOnClickListener {
            val name=findViewById<EditText>(R.id.name_signin_field).text.toString()
            val role=findViewById<Spinner>(R.id.role_signin_field)
            val email=findViewById<EditText>(R.id.email_signin_field).text.toString()
            val pw1=findViewById<EditText>(R.id.password_signin_field).text.toString()
            val pw2=findViewById<EditText>(R.id.password_conf_signin_field).text.toString()

            var check_campi=true;
            if(name==""){
                err_name.setText("missing name")
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
                err_role.setText("")
                err_email.setText("")
                err_pw1.setText("")
                err_pw2.setText("")

                auth.createUserWithEmailAndPassword(email, pw1)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                baseContext,
                                "User created. Log in to enter the restricted area.",
                                Toast.LENGTH_SHORT
                            ).show()
                            //startActivity(Intent(this, LoginActivity::class.java))
                            //finish()

                            val currentUser = auth.currentUser

                            Log.d(TAG, "currentuser= $currentUser")
                            if (currentUser != null) {
                                val user = HashMap<String, Any>()
                                user["name"] = name
                                user["role"] = role.selectedItem.toString() // converti l'elemento selezionato in una stringa
                                user["email"] = email

                                Log.d(TAG, "user= $user")

                                // Aggiungi un nuovo documento con un ID generato
                                db.collection("utenti")
                                    .add(user)
                                    .addOnSuccessListener { documentReference ->
                                        Log.d(TAG,  "DocumentSnapshot added with ID: ${documentReference.id}"
                                        )
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w(TAG, "Error adding document", e)
                                    }
                                startActivity(Intent(this, LoginActivity::class.java))
                            }
                        } else {
                            Toast.makeText(
                                baseContext,
                                "Sign Up failed. Try again after some time.",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e(TAG, "Error creating user: ${task.exception}")
                        }
                    }
                    .addOnFailureListener(this) { e ->
                        Log.e(TAG, "error creating user", e)
                    }
            }
            else{
                check_campi=true;
                return@setOnClickListener
            }

        }
    }

}