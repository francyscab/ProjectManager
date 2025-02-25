package com.example.project_manager

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project_manager.models.FileModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FilesAdapter(private val files: ArrayList<FileModel>) :
    RecyclerView.Adapter<FilesAdapter.FileViewHolder>() {

    class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.fileName)
        val fileDate: TextView = view.findViewById(R.id.fileDate)
        val downloadButton: Button = view.findViewById(R.id.downloadButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]
        holder.fileName.text = file.name
        holder.fileDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            .format(Date(file.uploadedAt))

        holder.downloadButton.setOnClickListener {
            // Implement download functionality
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(file.downloadUrl)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = files.size
}