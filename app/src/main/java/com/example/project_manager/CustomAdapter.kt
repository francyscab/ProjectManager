package com.example.project_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project_manager.models.ItemsViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.CircularProgressIndicator


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

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_activity, parent, false)

        return ViewHolder(view,mListener)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mList[position]

        // Set project title
        holder.titleView.text = item.title

        // Set progress
        holder.progressIndicator.progress = item.progress
        holder.progressText.text = "${item.progress}%"

        // Set priority chip
        val (chipColor, chipText) = when (item.priority) {
            "High" -> Pair(R.color.red, "Alta")
            "Medium" -> Pair(R.color.yellow, "Media")
            "Low" -> Pair(R.color.green, "Bassa")
            else -> Pair(R.color.gray, "---")
        }
        holder.priorityChip.apply {
            setChipBackgroundColorResource(chipColor)
            text = chipText
        }

        // Set deadline
        if (item.deadline.isNotEmpty()) {
            holder.deadlineText.text = item.deadline
            holder.deadlineText.visibility = View.VISIBLE
        } else {
            holder.deadlineText.visibility = View.GONE
        }

        // Store IDs
        holder.projectId = item.projectId
        holder.taskId = item.taskId ?: ""
        holder.subtaskId = item.subtaskId ?: ""
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to text
    class ViewHolder(itemView: View, listener: onItemClickListener) :
        RecyclerView.ViewHolder(itemView) {

        val titleView: TextView = itemView.findViewById(R.id.titolo)
        val progressIndicator: CircularProgressIndicator =
            itemView.findViewById(R.id.progressCircle)
        val progressText: TextView = itemView.findViewById(R.id.progressText)
        val priorityChip: Chip = itemView.findViewById(R.id.priorityChip)
        val deadlineText: TextView = itemView.findViewById(R.id.deadlineText)

        var projectId: String = ""
        var taskId: String = ""
        var subtaskId: String = ""

        init {
            itemView.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }
    }
}
