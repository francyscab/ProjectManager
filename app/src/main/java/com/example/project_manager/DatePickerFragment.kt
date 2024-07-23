package com.example.project_manager

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.DatePicker
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DatePickerFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {

    private var buttonID: String?=null

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        buttonID=arguments?.getString("id_button")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the current date as the default date in the picker.
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        // Create a new instance of DatePickerDialog and return it.
        return DatePickerDialog(requireContext(), this, year, month, day)

    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        // Do something with the date the user picks.
        val selectedDate = Calendar.getInstance()
        selectedDate.set(year, month, day)

        // Format the date as desired (e.g., dd/MM/yyyy)
        val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.time)

        // Update the text of the button with the selected date
        buttonID?.let {
            val resId = resources.getIdentifier(it, "id", activity?.packageName)
            (activity?.findViewById<Button>(resId))?.text = formattedDate
        }

    }

    fun newInstance(buttonID: String):DatePickerFragment{
        val fragment=DatePickerFragment()
        val args=Bundle()
        args.putString("id_button",buttonID)
        fragment.arguments=args
        return fragment
    }
}