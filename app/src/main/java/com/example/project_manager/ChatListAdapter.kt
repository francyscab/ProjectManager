package com.example.project_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListAdapter(
    private val chats: List<Chat>,
    private val onChatSelected: (Chat) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewProfile: ImageView = itemView.findViewById(R.id.imageViewProfile)
        val textViewChatName: TextView = itemView.findViewById(R.id.textViewChatName)
        val textViewLastMessage: TextView = itemView.findViewById(R.id.textViewLastMessage)
        val textViewTimestamp: TextView = itemView.findViewById(R.id.textViewTimestamp)
        val textViewUnreadCount: TextView = itemView.findViewById(R.id.textViewUnreadCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_list, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]

        // Imposta l'ultimo messaggio
        holder.textViewLastMessage.text = chat.lastMessage

        // Imposta il timestamp
        holder.textViewTimestamp.text =
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(chat.timestamp))

        // Carica l'immagine del profilo
        /*Glide.with(holder.itemView.context)
            .load(chat.profileImageUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .circleCrop()
            .into(holder.imageViewProfile)*/

        // Mostra il badge dei messaggi non letti (se > 0)
        if (chat.unreadCount > 0) {
            holder.textViewUnreadCount.visibility = View.VISIBLE
            holder.textViewUnreadCount.text = chat.unreadCount.toString()
        } else {
            holder.textViewUnreadCount.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onChatSelected(chat)
        }
    }

    override fun getItemCount(): Int = chats.size
}
