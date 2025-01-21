package com.example.project_manager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatListActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatListAdapter: ChatListAdapter

    private val chats = mutableListOf<Chat>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.recyclerViewChatList)

        chatListAdapter = ChatListAdapter(chats) { chat ->
            Log.d("ChatListActivity", "Chat selected with ID: ${chat.chatId}")
            openChat(chat)
        }

        recyclerView.adapter = chatListAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadChats()
    }

    private fun loadChats() {
        db.collection("chat")
            .orderBy("timeStamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("ChatListActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                chats.clear()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        // Stampa il contenuto del documento
                        Log.d("ChatListActivity", "Document ID: ${doc.id}, Data: ${doc.data}")

                        // Estrai i dati manualmente dal documento Firestore
                        val chatId = doc.getString("chatID") ?: ""
                        val lastMessage = doc.getString("lastMessage") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: 0L
                        val unreadCount = doc.getLong("unreadCount")?.toInt() ?: 0

                        // Crea l'oggetto Chat manualmente
                        val chat = Chat(
                            chatId = chatId,
                            lastMessage = lastMessage,
                            timestamp = timestamp,
                            unreadCount = unreadCount
                        )

                        // Aggiungi l'oggetto Chat alla lista
                        chats.add(chat)

                        // Debug: stampa l'oggetto Chat aggiunto
                        Log.d("ChatListActivity", "Chat added with ID: ${chat.chatId}")
                    }
                    // Debug: stampa il numero di chat trovate
                    Log.d("ChatListActivity", "Chats found: ${chats.size}")
                } else {
                    Log.d("ChatListActivity", "No documents found.")
                }

                // Aggiorna l'adapter (decommenta quando serve)
                chatListAdapter.notifyDataSetChanged()
            }
    }


    private fun openChat(chat: Chat) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("chatId", chat.chatId)
        startActivity(intent)
    }
}
