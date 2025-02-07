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
import com.example.project_manager.repository.FileRepository
import com.example.project_manager.services.UserService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListAdapter(
    private val chats: List<Chat>,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onChatSelected: (Chat) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    private val userService = UserService()
    private val fileRepository = FileRepository()

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
                        textViewChatName.text = "${user.name} ${user.surname}"
                        // Load profile image using FileRepository
                        loadProfileImage(user)
                        // Gestione ultimo messaggio
                        if (chat.lastMessage.isNotEmpty()) {
                            textViewLastMessage.visibility = View.VISIBLE
                            textViewLastMessage.text = chat.lastMessage
                        } else {
                            textViewLastMessage.visibility = View.GONE
                        }

                        // Gestione timestamp
                        if (chat.timestamp > 0) {
                            textViewTimestamp.visibility = View.VISIBLE
                            textViewTimestamp.text = SimpleDateFormat(
                                "dd/MM/yyyy HH:mm",
                                Locale.getDefault()
                            ).format(Date(chat.timestamp))
                        } else {
                            textViewTimestamp.visibility = View.GONE
                        }

                        // Gestione contatore messaggi non letti
                        if (chat.unreadCount > 0 && chat.senderId != userService.getCurrentUserId()) {
                            textViewUnreadCount.visibility = View.VISIBLE
                            textViewUnreadCount.text = chat.unreadCount.toString()
                            textViewUnreadCount.setBackgroundResource(R.drawable.badge_background)
                            textViewLastMessage.setTypeface(null, android.graphics.Typeface.BOLD)
                        } else {
                            textViewUnreadCount.visibility = View.GONE
                            textViewLastMessage.setTypeface(null, android.graphics.Typeface.NORMAL)
                        }

                        itemView.setOnClickListener {
                            onChatSelected(chat)
                        }
                    } ?: run {
                        textViewChatName.text = "Unknown User"
                        imageViewProfile.setImageResource(R.drawable.username)
                    }

                } catch (e: Exception) {
                    Log.e("ChatListAdapter", "Error loading user details", e)
                    textViewChatName.text = "Unknown User"
                    imageViewProfile.setImageResource(R.drawable.username)
                }
            }


        }

        private suspend fun loadProfileImage(user: User) {
            if (user.profile_image_url.isNotEmpty()) {
                fileRepository.loadProfileImage(itemView.context, imageViewProfile, user.profile_image_url)
            } else {
                imageViewProfile.setImageResource(R.drawable.username)
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