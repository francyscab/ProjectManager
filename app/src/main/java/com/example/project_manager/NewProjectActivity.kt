package com.example.project_manager

import android.content.ContentValues
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
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
import kotlin.math.log

class NewProjectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_project_form)

        val db = FirebaseFirestore.getInstance()

        findViewById<Button>(R.id.pickDate).setOnClickListener {
            val newFragment = DatePickerFragment().newInstance("pickDate")
            newFragment.show(supportFragmentManager, "datePicker")
        }

        findViewById<Button>(R.id.pickDateTask).setOnClickListener {
            val newFragment = DatePickerFragment().newInstance("pickDateTask")
            newFragment.show(supportFragmentManager, "datePicker")
        }

        val leaderNames = ArrayList<String>()
        val leader = findViewById<Spinner>(R.id.projectLeaderSpinner)

        db.collection("utenti")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val role = document.getString("role")
                    if (role == "Leader") {
                        val name = document.getString("name").toString()
                        leaderNames.add(name)
                    }
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, leaderNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                leader.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }

        val linearLayout = findViewById<LinearLayout>(R.id.linearLayout)
        val buttonAdd = findViewById<Button>(R.id.buttonAdd)
        val buttonSave=findViewById<Button>(R.id.buttonSave)

        buttonAdd.setOnClickListener {
            // Crea un EditText per il nome del sotto-task
            val editTextNome = EditText(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                hint = "Nome del task"
            }

            // Aggiungi il EditText al LinearLayout
            linearLayout.addView(editTextNome, linearLayout.childCount - 1)

            // Crea un EditText per la descrizione del task
            val editTextDescrizione = EditText(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                hint = "Descrizione del task"
            }

            // Aggiungi il EditText al LinearLayout
            linearLayout.addView(editTextDescrizione, linearLayout.childCount - 1)

            // Crea un EditText per la scadenza
            val scadenzaTask = Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text=""
                background= getSelectableItemBackground()
                hint = "Seleziona data scadenza"
                id= View.generateViewId()
                val buttonID=id.toString()
                setOnClickListener {
                    val newFragment = DatePickerFragment().newInstance(buttonID)
                    newFragment.show(supportFragmentManager, "datePicker")
                }
            }

            // Aggiungi il pulsante al LinearLayout
            linearLayout.addView(scadenzaTask, linearLayout.childCount - 1)
        }

        
        
        buttonSave.setOnClickListener {
            val title = findViewById<EditText>(R.id.titleNewProject).text.toString()
            val scadenza = findViewById<Button>(R.id.pickDate).text.toString()
            val tasks=ArrayList<Map<String,String>>()

            val err_title = findViewById<TextView>(R.id.errore_titolo)
            val err_date = findViewById<TextView>(R.id.errore_date)
            val err_leader = findViewById<TextView>(R.id.errore_projectLeader)

            var check_campi = true;

            if (title == "") {
                err_title.setText("missing title")
                check_campi = false;
            }
            if (scadenza == "") {
                err_date.setText("missing date")
                check_campi = false;
            }
            if (leader.selectedItemPosition == AdapterView.INVALID_POSITION) {
                err_leader.setText("select the leader of this project")
                check_campi = false;
            }
            for (i in 0 until linearLayout.childCount step 3) {
                val editTextNome = linearLayout.getChildAt(i) as? EditText
                val taskName = editTextNome?.text.toString()

                val editTextDescrizione = linearLayout.getChildAt(i + 1) as? EditText
                val taskDescription = editTextDescrizione?.text.toString()

                val buttonScadenza = linearLayout.getChildAt(i + 2) as? Button
                val taskScadenza = buttonScadenza?.text.toString()

                if (editTextNome == null || editTextDescrizione == null || buttonScadenza == null) {
                    Log.e(ContentValues.TAG, "Error: One or more views are missing.")
                    continue
                }

                Log.d(ContentValues.TAG, "Task Name: $taskName")
                Log.d(ContentValues.TAG, "Task Description: $taskDescription")
                Log.d(ContentValues.TAG, "Task Deadline: $taskScadenza")

                if (taskName.isNotEmpty()) {
                    val task = mapOf(
                        "nome" to taskName,
                        "descrizione" to taskDescription,
                        "scadenza" to taskScadenza
                    )
                    tasks.add(task)
                } else {
                    Toast.makeText(baseContext, "aggiungi almeno un task", Toast.LENGTH_SHORT).show()
                    check_campi = false
                }
            }
            Log.d(ContentValues.TAG, "Tasks array: $tasks")

            if (check_campi) {
                err_title.setText("")
                err_leader.setText("")
                err_date.setText("")

                val nuovoProgetto = hashMapOf(
                    "titolo" to title,
                    "leader" to leader.selectedItem.toString(),
                    "scadenza" to scadenza
                )
                nuovoProgetto["assegnato"] = false.toString()

                Log.d(ContentValues.TAG, "nuovoProgetto= $nuovoProgetto")
                db.collection("progetti")
                    .document(title)
                    .set(nuovoProgetto)
                    .addOnSuccessListener { documentReference ->
                        val batch = db.batch()
                        for (t in tasks) {
                            val taskDoc =
                                db.collection("progetti").document(title).collection("task")
                                    .document(t["nome"]!!)
                            batch.set(taskDoc, t)
                        }
                        batch.commit().addOnSuccessListener {
                            Toast.makeText(
                                baseContext,
                                "progetto e task creati con successo",
                                Toast.LENGTH_SHORT
                            ).show(
                            )


                            //chimata da aggiustare
                            val intent = Intent(this, ProjectActivity::class.java)
                            intent.putExtra(
                                "projectId",
                                title
                            ) // "projectId" è il nome dell'extra, projectId è l'ID del progetto
                            startActivity(intent)


                            //startActivity(Intent(this, LoggedActivity::class.java))
                        }.addOnFailureListener { exception ->
                            Log.w(ContentValues.TAG, "Error adding document", exception)
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