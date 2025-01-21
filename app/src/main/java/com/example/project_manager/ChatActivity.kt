package com.example.project_manager;

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.io.Console

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerViewMessages: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSendMessage: ImageButton

    private lateinit var messagesAdapter: MessagesAdapter
    private val messages = mutableListOf<Message>()

    private lateinit var db: FirebaseFirestore
    private var chatId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        Log.d("ChatActivity", "CHAT activity")

        // Inizializza Firebase
        db = FirebaseFirestore.getInstance()

        // Recupera il chatId passato tramite Intent
        chatId = intent.getStringExtra("chatId")
        Log.d("ChatActivity", "ChatId: $chatId")

        // Configura la RecyclerView
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages)
        recyclerViewMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        messagesAdapter = MessagesAdapter(messages)
        recyclerViewMessages.adapter = messagesAdapter

        // Campo di input e pulsante di invio
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSendMessage = findViewById(R.id.buttonSendMessage)

        buttonSendMessage.setOnClickListener {
            sendMessage()
        }

        // Carica i messaggi
        loadMessages()
    }

    private fun loadMessages() {
        if (chatId == null) return
        db.collection("chat")
                        .document(chatId!!)
            .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("ChatActivity", "Listen failed.", e)
                return@addSnapshotListener
            }

            messages.clear()
            for (doc in snapshots!!) {
                val message = doc.toObject(Message::class.java)
                messages.add(message)
            }
            messagesAdapter.notifyDataSetChanged()
            recyclerViewMessages.scrollToPosition(messages.size - 1)
        }
    }

    private fun sendMessage() {
        if (chatId == null || editTextMessage.text.isEmpty()) return
        val message = Message(
                senderId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                text = editTextMessage.text.toString(),
                timestamp = System.currentTimeMillis()
        )

        db.collection("chat")
                .document(chatId!!)
            .collection("messages")
                .add(message)
                .addOnSuccessListener {
            editTextMessage.text.clear()
            recyclerViewMessages.scrollToPosition(messages.size - 1)
        }
            .addOnFailureListener {
            Toast.makeText(this, "Errore durante l'invio del messaggio", Toast.LENGTH_SHORT).show()
        }
    }

}
