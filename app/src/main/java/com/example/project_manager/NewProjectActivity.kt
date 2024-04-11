package com.example.project_manager

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

class NewProjectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_project_form)

        findViewById<Button>(R.id.pickDate).setOnClickListener {
            val newFragment = DatePickerFragment()
            newFragment.show(supportFragmentManager, "datePicker")
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
            val title=findViewById<EditText>(R.id.titleNewProject)
            val leader=findViewById<Spinner>(R.id.projectLeaderSpinner)
            val scadenza=findViewById<Button>(R.id.pickDate)
            val subTask=ArrayList<String>()
            for(i in 0 until linearLayout.childCount -1){
                val editText=linearLayout.getChildAt(i) as EditText
                val subTaskName=editText.text.toString()
                if(subTaskName.isNotEmpty()){
                    subTask.add(subTaskName)
                }
            }

        }

    }

}