package com.example.project_manager

import android.content.ContentValues
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.project_manager.models.ItemsViewModel


class CustomAdapter(private val mList: ArrayList<ItemsViewModel>) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    // create new views

    private lateinit var mListener : onItemClickListener
    interface onItemClickListener{
        fun onItemClick(position : Int)
    }
    fun setOnItemClickListener(listener: onItemClickListener){
        mListener = listener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder{
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_view_design, parent, false)

        return ViewHolder(view,mListener)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val ItemsViewModel = mList[position]

        Log.d(ContentValues.TAG,"ITEMVIEWMODEL: $ItemsViewModel")

        // sets the text to the textview from our itemHolder class
        holder.textView.text = ItemsViewModel.title
        val labelNew: TextView=holder.itemView.findViewById(R.id.new_label)

        holder.projectId=ItemsViewModel.projectId.toString()
        holder.taskId=ItemsViewModel.taskId.toString()
        holder.subtaskId=ItemsViewModel.subtaskId.toString()

        val priorityIndicator: View = holder.itemView.findViewById(R.id.priorityIndicator)

        val color = when (ItemsViewModel.priority) {
            "High" -> ContextCompat.getColor(holder.itemView.context, R.color.red)
            "Medium" -> ContextCompat.getColor(holder.itemView.context, R.color.yellow)
            "Low" -> ContextCompat.getColor(holder.itemView.context, R.color.green)
            else -> ContextCompat.getColor(holder.itemView.context, R.color.gray) // Default
        }

        priorityIndicator.setBackgroundColor(color)



    }
    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to text
    class ViewHolder(ItemView: View, listener: onItemClickListener) : RecyclerView.ViewHolder(ItemView) {
        val textView: TextView = itemView.findViewById(R.id.titolo)
        var projectId: String = "" // Changed to String
        var taskId: String = "" // Changed to String
        var subtaskId: String = "" // Changed to String

        init {
            itemView.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }
    }
}


