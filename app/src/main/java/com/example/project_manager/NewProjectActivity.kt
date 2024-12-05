package com.example.project_manager

import android.content.ContentValues
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore

class NewProjectActivity : AppCompatActivity() {

    private lateinit var role:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_project_form)

        role=intent.getStringExtra("role")?:""

        val db = FirebaseFirestore.getInstance()

        findViewById<Button>(R.id.pickDate).setOnClickListener {
            val newFragment = DatePickerFragment().newInstance("pickDate")
            newFragment.show(supportFragmentManager, "datePicker")
        }

        //se nuovo prog è stato chiamato da un tasto per creare un nuovo task o nuovo progetto
        val tipoForm = intent.getStringExtra("tipo_form")
        var spinner=""
        if(tipoForm=="task"){
            //sto creando un task
            val typeNewTextView = findViewById<TextView>(R.id.typeNew)
            typeNewTextView.text = "NEW TASK"
            //cerco i developer per lo spinner
            spinner="Developer"
        }
        else{
            //sto creando un nuovo progetto
            //cerco i leader per lo spinner
            spinner="Leader"
        }
        val spinnerNames = ArrayList<String>()
        val spinnerElement = findViewById<Spinner>(R.id.projectElementSpinner)

        //riempio lo spinner con i leader o con i developer
        db.collection("utenti")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val role = document.getString("role")
                    if (role == spinner) {
                        val name = document.getString("name").toString()
                        spinnerNames.add(name)
                    }
                }
                val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerNames)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerElement.adapter = spinnerAdapter
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }

        val buttonSave=findViewById<Button>(R.id.buttonSave)

        buttonSave.setOnClickListener {
            val title = findViewById<EditText>(R.id.titleNewProject).text.toString()
            val scadenza = findViewById<Button>(R.id.pickDate).text.toString()
            val descrizione=findViewById<EditText>(R.id.descrizioneNewProject).text.toString()

            val err_title = findViewById<TextView>(R.id.errore_titolo)
            val err_date = findViewById<TextView>(R.id.errore_date)
            val err_spinner = findViewById<TextView>(R.id.errore_date)

            val err_descrizione=findViewById<TextView>(R.id.errore_descrizione)

            var check_campi = true;
            if (title == "") {
                err_title.setText("missing title")
                check_campi = false;
            }
            if (scadenza == "") {
                err_date.setText("missing date")
                check_campi = false;
            }
            if(tipoForm=="progetto"|| tipoForm=="task"){
                //solo per progetti e per task, non per sottotask
                if (spinnerElement.selectedItemPosition == AdapterView.INVALID_POSITION) {
                    err_spinner.setText("select the $spinner of this $tipoForm")
                    check_campi = false;
                }
            }
            if(descrizione=="") {
                err_descrizione.setText("missing descriptiom")
                check_campi=false
            }

            if (check_campi) {
                err_title.setText("")
                //solo per progetti e per task, non per sottotask...fare distinzione!!!
                err_spinner.setText("")
                err_date.setText("")
                err_descrizione.setText("")

                //in base a cosa ho boisogno creo prog o task o sottotask

                val nuovo = hashMapOf(
                    "titolo" to title,
                    "${spinner.toLowerCase()}" to spinnerElement.selectedItem.toString(),
                    "scadenza" to scadenza,
                    "descrizione" to descrizione
                )
                //vale sia per task che per progetti perche quando prog è nuoovo io vedo e creo task, quando task è nuovo il developer deve creare sottotask
                nuovo["assegnato"] = false.toString()

                Log.d(ContentValues.TAG, "nuovo $tipoForm= $nuovo")
                Log.d(ContentValues.TAG, "tipo form $tipoForm")

                //aggiungo un progetto
                if(tipoForm=="progetto"){

                    db.collection("progetti")
                        .document(title)
                        .set(nuovo)
                        .addOnSuccessListener { documentReference ->
                            val batch = db.batch()
                            /*for (t in tasks) {
                                val taskDoc =
                                    db.collection("progetti").document(title).collection("task")
                                        .document(t["nome"]!!)
                                batch.set(taskDoc, t)
                            }*/
                            batch.commit().addOnSuccessListener {
                                Toast.makeText(
                                    baseContext,
                                    "$tipoForm creato con successo",
                                    Toast.LENGTH_SHORT
                                ).show(
                                )
                                //dopo averlo creato apro la schermata del nuovo progetto
                                val intent = Intent(this, ProjectActivity::class.java)
                                intent.putExtra("projectId",title)
                                intent.putExtra("role",role )
                                startActivity(intent)
                            }.addOnFailureListener { exception ->
                                Log.w(ContentValues.TAG, "Error adding document", exception)
                            }
                        }
                }

                //aggiungo un task
                else if (tipoForm=="task"){

                    //recupero l'id el progetto di cui sto creando i task
                    val projectId=intent.getStringExtra("project-id").toString()

                    //aggiungo task alla collezione task del progetto corrente
                    db.collection("progetti")
                        .document(projectId)
                        .collection("task")
                        .document(title)
                        .set(nuovo)
                        .addOnSuccessListener { documentReference ->
                            val batch = db.batch()
                            batch.commit().addOnSuccessListener {
                                Toast.makeText(
                                    baseContext,
                                    "$tipoForm creato con successo",
                                    Toast.LENGTH_SHORT
                                ).show(
                                )
                                //dopo averlo creato apro la schermata del nuovo task
                                Log.w(ContentValues.TAG, "taskid $title")
                                Log.w(ContentValues.TAG, "projectid $projectId")
                                Log.w(ContentValues.TAG, "calling new activity")
                                val intent = Intent(this, ProjectActivity::class.java)
                                intent.putExtra("taskId", title)
                                intent.putExtra("projectId", projectId)
                                startActivity(intent)
                            }.addOnFailureListener { exception ->
                                Log.w(ContentValues.TAG, "Error adding document", exception)
                            }
                        }
                }


            }

        }

    }

    fun getSelectableItemBackground(): Drawable? {
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        return ContextCompat.getDrawable(this, typedValue.resourceId)
    }
}