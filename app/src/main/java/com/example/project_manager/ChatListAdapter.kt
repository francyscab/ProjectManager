package com.example.project_manager

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListAdapter(
    private val chats: List<Chat>,
    private val onChatSelected: (Chat) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

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

        // Recupera l'email dell'utente loggato
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        Log.d("ChatListActivity", "Current User Email: $currentUserEmail")
         val chatuser1 = chat.user1
         val chatuser2 = chat.user2
        Log.d("ChatListActivity", "Chat user1: $chatuser1")
        Log.d("ChatListActivity", "Chat user2: $chatuser2")

        // Mostra il nome dell'altro utente nella chat
        val chatPartnerEmail = if (chat.user1 == currentUserEmail) chat.user2 else chat.user1
        Log.d("ChatListActivity", "Chat Partner Email: $chatPartnerEmail")

        // Carica il nome dell'altro utente dal database
        getUserNameByEmail(chatPartnerEmail) { name ->
            // Aggiorna la UI con il nome dell'altro utente
            holder.textViewChatName.text = name
        }

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

    // Funzione per ottenere il nome dell'utente dall'email
    private fun getUserNameByEmail(email: String, onComplete: (String) -> Unit) {
        // Recupera il nome dell'utente dal database Firestore
        db.collection("utenti")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents.first()
                    val name = document.getString("name") ?: "Nome non trovato"
                    onComplete(name)
                } else {
                    onComplete("Nome non trovato")
                }
            }
            .addOnFailureListener { e ->
                // In caso di errore, restituisci un valore predefinito
                onComplete("Errore nel recupero del nome")
            }
    }
}