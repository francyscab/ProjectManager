package com.example.project_manager

import android.content.ContentValues
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore

class NewProjectActivity : AppCompatActivity() {

    private lateinit var role:String
    private lateinit var creator:String
    private lateinit var projectId:String
    private lateinit var taskid:String
    private lateinit var tipoForm: String
    private lateinit var err_spinner: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_project_form)

        role=intent.getStringExtra("role")?:""
        creator=intent.getStringExtra("creator")?:""
        projectId=intent.getStringExtra("project-id")?:""
        taskid=intent.getStringExtra("task-id")?:""
        tipoForm = intent.getStringExtra("tipo_form")?:""
        Log.d(ContentValues.TAG, "sto chiamando newprojectactivity con role $role, creator $creator, projectId $projectId, taskid $taskid, tipoForm $tipoForm")

        val spinnerElement = findViewById<Spinner>(R.id.projectElementSpinner)
        val db = FirebaseFirestore.getInstance()

        findViewById<Button>(R.id.pickDate).setOnClickListener {
            val newFragment = DatePickerFragment().newInstance("pickDate")
            newFragment.show(supportFragmentManager, "datePicker")
        }


        //se nuovo prog è stato chiamato da un tasto per creare un nuovo task o nuovo progetto
        var spinner=""
        if(tipoForm=="progetto"|| tipoForm=="task"){
            if(tipoForm=="task"){
                //sto creando un task
                val typeNewTextView = findViewById<TextView>(R.id.typeNew)
                typeNewTextView.text = "NEW TASK"
                //cerco i developer per lo spinner
                spinner="Developer"
            }
            else if(tipoForm=="progetto"){
                //sto creando un nuovo progetto
                Log.w(ContentValues.TAG, "STO cercando i developer")
                val typeNewTextView = findViewById<TextView>(R.id.typeNew)
                typeNewTextView.text = "NEW PROJECT"
                //cerco i leader per lo spinner
                spinner="Leader"
            }
            val spinnerNames = ArrayList<String>()


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
        }

        val spinnerLayout = findViewById<LinearLayout>(R.id.spinnerLinearLayout)
        //if(tipoForm=="progetto"|| tipoForm=="task"){spinnerLayout.visibility=GONE }
        if(tipoForm=="progetto"|| tipoForm=="task"){spinnerLayout.visibility= VISIBLE}
        else{spinnerLayout.visibility=GONE }
        val buttonSave=findViewById<Button>(R.id.buttonSave)

        buttonSave.setOnClickListener {
            val title = findViewById<EditText>(R.id.titleNewProject).text.toString()
            val scadenza = findViewById<Button>(R.id.pickDate).text.toString()
            val descrizione=findViewById<EditText>(R.id.descrizioneNewProject).text.toString()

            val err_title = findViewById<TextView>(R.id.errore_titolo)
            val err_date = findViewById<TextView>(R.id.errore_date)

            //spinner solo se task o sottotask
            if(tipoForm=="progetto"|| tipoForm=="task"){
                err_spinner = findViewById<TextView>(R.id.errore_date)
            }

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
                if(tipoForm=="progetto"|| tipoForm=="task"){
                    err_spinner.setText("")
                }
                err_date.setText("")
                err_descrizione.setText("")

                //in base a cosa ho boisogno creo prog o task o sottotask

                val nuovo = hashMapOf(
                    "titolo" to title,
                    "scadenza" to scadenza,
                    "descrizione" to descrizione,
                    "progress" to 0
                )

                nuovo["assegnato"] = false.toString()

                Log.d(ContentValues.TAG, "tipo form $tipoForm")

                //aggiungo un progetto
                if(tipoForm=="progetto"){
                    nuovo["creator"] = creator
                    nuovo["${spinner.toLowerCase()}"] = spinnerElement.selectedItem.toString()

                    Log.d(ContentValues.TAG, "STO AGGIUNGENDO UN NUOVO $tipoForm= $nuovo")
                    db.collection("progetti")
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
                                //dopo averlo creato apro la schermata del nuovo progetto
                                val intent = Intent(this, ProjectActivity::class.java)
                                intent.putExtra("projectId",title)
                                intent.putExtra("role",role )
                                intent.putExtra("name",creator)

                                startActivity(intent)
                            }.addOnFailureListener { exception ->
                                Log.w(ContentValues.TAG, "Error adding document", exception)
                            }
                        }
                }

                //aggiungo un task
                else if (tipoForm=="task"){
                    nuovo["${spinner.toLowerCase()}"] = spinnerElement.selectedItem.toString()

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
                                Log.w(ContentValues.TAG, "role $role")
                                val intent = Intent(this, ProjectActivity::class.java)
                                intent.putExtra("taskId", title)
                                intent.putExtra("projectId", projectId)
                                intent.putExtra("role",role )
                                startActivity(intent)
                            }.addOnFailureListener { exception ->
                                Log.w(ContentValues.TAG, "Error adding document", exception)
                            }
                        }
                }else if (tipoForm == "subtask") {
                    Log.d(ContentValues.TAG, "STO AGGIUNGENDO UN NUOVO $tipoForm= $nuovo")
                    // Get the task ID from the intent or wherever it's stored

                    db.collection("progetti")
                        .document(projectId)
                        .collection("task")
                        .document(taskid)
                        .collection("subtask")
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
                                //dopo averlo creato apro la schermata del nuovo sottotask
                                Log.w(ContentValues.TAG, "sto chiamando project activity per visualizzare il nuovo task")
                                Log.w(ContentValues.TAG, "subtaskid $title")
                                Log.w(ContentValues.TAG, "taskid $taskid")
                                Log.w(ContentValues.TAG, "projectid $projectId")

                                val intent = Intent(this, ProjectActivity::class.java)
                                intent.putExtra("subtaskId", title)
                                intent.putExtra("taskId", taskid)
                                intent.putExtra("projectId", projectId)
                                intent.putExtra("role",role )
                                startActivity(intent)
                            }
                                .addOnFailureListener { exception ->
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