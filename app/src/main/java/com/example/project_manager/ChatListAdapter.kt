package com.example.project_manager

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.example.project_manager.models.Chat
import com.example.project_manager.models.User
import com.example.project_manager.services.UserService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListAdapter(
    private val chats: List<Chat>,
    private val lifecycleScope: LifecycleCoroutineScope,  // Passa lifecycleScope dal costruttore
    private val onChatSelected: (Chat) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    private val userService = UserService()

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewProfile: ImageView = itemView.findViewById(R.id.imageViewProfile)
        val textViewChatName: TextView = itemView.findViewById(R.id.textViewChatName)
        val textViewLastMessage: TextView = itemView.findViewById(R.id.textViewLastMessage)
        val textViewTimestamp: TextView = itemView.findViewById(R.id.textViewTimestamp)
        val textViewUnreadCount: TextView = itemView.findViewById(R.id.textViewUnreadCount)

        fun bind(chat: Chat) {
            lifecycleScope.launch {
                try {
                    val currentUserId = userService.getCurrentUserId()

                    val chatPartnerId = if (chat.user1 == currentUserId) chat.user2 else chat.user1

                    val chatPartner = userService.getUserById(chatPartnerId)
                    chatPartner?.let { user ->
                        textViewChatName.text = "${chatPartner.name} ${chatPartner.surname}"
                    } ?: run {
                        textViewChatName.text = "Unknown User"
                    }
                } catch (e: Exception) {
                    Log.e("ChatListAdapter", "Error loading user details", e)
                    textViewChatName.text = "Unknown User"
                }
            }

            // Impostazioni non-suspend che possono stare fuori dalla coroutine
            textViewLastMessage.text = chat.lastMessage

            textViewTimestamp.text = SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
            ).format(Date(chat.timestamp))

            if (chat.unreadCount > 0) {
                textViewUnreadCount.visibility = View.VISIBLE
                textViewUnreadCount.text = chat.unreadCount.toString()
            } else {
                textViewUnreadCount.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onChatSelected(chat)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_list, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount(): Int = chats.size
}