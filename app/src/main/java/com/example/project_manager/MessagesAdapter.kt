package com.example.project_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessagesAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewMessage: TextView = itemView.findViewById(R.id.textViewMessage)
        val textViewTimestamp: TextView = itemView.findViewById(R.id.textViewTimestamp)
        val messageContainer: LinearLayout = itemView.findViewById(R.id.messageContainer) // Aggiungi questa riga
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.textViewMessage.text = message.text

        // Formatta il timestamp
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedDate = sdf.format(Date(message.timestamp))
        holder.textViewTimestamp.text = formattedDate

        // Differenzia il layout: inviato (a destra) o ricevuto (a sinistra)
        val isSentByCurrentUser = message.senderId == FirebaseAuth.getInstance().currentUser?.uid

        val layoutParams = holder.messageContainer.layoutParams as LinearLayout.LayoutParams
        if (isSentByCurrentUser) {
            // Messaggio inviato: posiziona a destra
            layoutParams.gravity = android.view.Gravity.END
            holder.messageContainer.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.sent_message_background)
        } else {
            // Messaggio ricevuto: posiziona a sinistra
            layoutParams.gravity = android.view.Gravity.START
            holder.messageContainer.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.received_message_background)
        }
        holder.messageContainer.layoutParams = layoutParams
    }



    override fun getItemCount(): Int = messages.size
}
