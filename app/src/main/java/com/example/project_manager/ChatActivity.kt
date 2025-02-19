package com.example.project_manager;

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_manager.models.Message
import com.example.project_manager.repository.FileRepository
import com.example.project_manager.services.ChatService
import com.example.project_manager.services.UserService
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {
    private lateinit var recyclerViewMessages: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSendMessage:ImageView
    private lateinit var messagesAdapter: MessagesAdapter
    private lateinit var chatPartnerImage: CircleImageView
    private lateinit var chatPartnerName: TextView

    private val chatService = ChatService()
    private val userService = UserService()
    private val fileRepository = FileRepository()
    private var chatId: String? = null
    private val messages = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_activity_2)

        intent.getStringExtra("chatId")?.let {
            chatId = it
            setupViews(chatId!!)
            loadMessages()

            // Reset unread counter when chat is opened
            lifecycleScope.launch {
                try {
                    chatService.resetUnreadCounter(chatId!!)
                } catch (e: Exception) {
                    Log.e("ChatActivity", "Error resetting unread counter", e)
                }
            }
        } ?: run {
            Log.e("ChatActivity", "No chatId provided")
            Toast.makeText(this, "Error: Chat ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    private fun setupViews(chatId:String) {
        loadChatPartnerInfo(chatId)
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages)
        recyclerViewMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        messagesAdapter = MessagesAdapter(messages)
        recyclerViewMessages.adapter = messagesAdapter

        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSendMessage = findViewById(R.id.buttonSendMessage)
        chatPartnerImage = findViewById(R.id.chatPartnerImage)
        chatPartnerName = findViewById(R.id.chatPartnerName)
        buttonSendMessage = findViewById(R.id.buttonSendMessage)

        buttonSendMessage.setOnClickListener {
            sendMessage(chatId)
        }
    }

    private fun loadChatPartnerInfo(chatId: String) {
        lifecycleScope.launch {
            try {
                val currentUserId = userService.getCurrentUserId() ?: return@launch
                val userIds = chatId.split("_")
                if (userIds.size != 2) {
                    Log.e("ChatActivity", "Invalid chat ID format")
                    return@launch
                }
                val chatPartnerId = if (userIds[0] == currentUserId) userIds[1] else userIds[0]
                val chatPartner = userService.getUserById(chatPartnerId)
                chatPartner?.let { user ->
                    // Set chat partner name
                    chatPartnerName.text = "${user.name} ${user.surname}"

                    // Load profile image
                    fileRepository.loadProfileImage(
                        this@ChatActivity,
                        chatPartnerImage,
                        user.profile_image_url
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error loading chat partner info", e)
                Toast.makeText(
                    this@ChatActivity,
                    "Error loading chat partner info",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            try {
                val chatId = chatId ?: return@launch
                val loadedMessages = chatService.getChatMessages(chatId)
                messages.clear()
                messages.addAll(loadedMessages)
                messagesAdapter.notifyDataSetChanged()
                recyclerViewMessages.scrollToPosition(messages.size - 1)
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error loading messages", e)
                Toast.makeText(
                    this@ChatActivity,
                    "Error loading messages",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun sendMessage(chatId: String) {
        val messageText = editTextMessage.text.toString().trim()
        if (messageText.isEmpty() || chatId == null) return

        lifecycleScope.launch {
            try {
                val success = chatService.sendMessage(chatId!!, messageText)
                if (success) {
                    editTextMessage.text.clear()
                    loadMessages() // Reload messages to show the new one
                } else {
                    Toast.makeText(
                        this@ChatActivity,
                        "Failed to send message",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error sending message", e)
                Toast.makeText(
                    this@ChatActivity,
                    "Error sending message",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Navigate back to ChatListActivity
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
