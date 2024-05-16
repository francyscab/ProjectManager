package com.example.project_manager

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class NewProjectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_project_form)

        val db = FirebaseFirestore.getInstance()

        findViewById<Button>(R.id.pickDate).setOnClickListener {
            val newFragment = DatePickerFragment()
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
            val editText = EditText(this)
            editText.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            editText.hint = "Nome del sotto-task"
            linearLayout.addView(editText,linearLayout.childCount-1)
        }

        buttonSave.setOnClickListener {
            val title=findViewById<EditText>(R.id.titleNewProject).text.toString()
            val scadenza=findViewById<Button>(R.id.pickDate).text.toString()
            val subTask=ArrayList<String>()

            val err_title=findViewById<TextView>(R.id.errore_titolo)
            val err_date=findViewById<TextView>(R.id.errore_date)
            val err_leader=findViewById<TextView>(R.id.errore_projectLeader)

            var check_campi=true;

            if(title==""){
                err_title.setText("missing title")
                check_campi=false;
            }
            if(scadenza==""){
                err_date.setText("missing date")
                check_campi=false;
            }
            if(leader.selectedItemPosition== AdapterView.INVALID_POSITION){
                err_leader.setText("select the leader of this project")
                check_campi=false;
            }
            for(i in 0 until linearLayout.childCount -1){
                val editText=linearLayout.getChildAt(i) as EditText
                val subTaskName=editText.text.toString()
                if(subTaskName.isNotEmpty()){
                    subTask.add(subTaskName)
                }
                else{
                    Toast.makeText(baseContext, "aggiungi almeno un sottotask", Toast.LENGTH_SHORT).show()
                    check_campi=false;
                }
            }

            if(check_campi) {
                err_title.setText("")
                err_leader.setText("")
                err_date.setText("")

                val nuovoProgetto= hashMapOf(
                    "titolo" to title,
                    "leader" to leader.selectedItem.toString(),
                    "scadenza" to scadenza
                )
                nuovoProgetto["assegnato"]= false.toString()

                Log.d(ContentValues.TAG, "nuovoProgetto= $nuovoProgetto")
                db.collection("progetti")
                    .document(title)
                    .set(nuovoProgetto)
                    .addOnSuccessListener{ documentReference ->
                        val batch=db.batch()
                        for(sottoTask in subTask){
                            val sottoTaskDoc=db.collection("progetti").document(title).collection("sottotask").document(sottoTask)
                            batch.set(sottoTaskDoc, hashMapOf("nome" to sottoTask))
                        }
                        batch.commit().addOnSuccessListener{
                            Toast.makeText(baseContext, "progetto e sottotask creati con successo", Toast.LENGTH_SHORT).show(

                            )


                            //chimata da aggiustare
                            val intent = Intent(this, ProjectActivity::class.java)
                            intent.putExtra("projectId", title) // "projectId" è il nome dell'extra, projectId è l'ID del progetto
                            startActivity(intent)


                            //startActivity(Intent(this, LoggedActivity::class.java))
                        }.addOnFailureListener { exception ->
                            Log.w(ContentValues.TAG, "Error adding document", exception)
                        }
                    }

            }
        }

    }
}