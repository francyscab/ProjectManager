package com.example.project_manager

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_manager.models.Chat
import com.example.project_manager.models.Role
import com.example.project_manager.models.User
import com.example.project_manager.services.ChatService
import com.example.project_manager.services.UserService
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatListActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatListAdapter: ChatListAdapter

    val chatService = ChatService()
    val userService = UserService()

    private val chats = mutableListOf<Chat>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        setupViews()
        loadChats()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerViewChatList)

        val startChatButton: Button = findViewById(R.id.buttonStartNewChat)
        startChatButton.setOnClickListener {
            showSelectUserDialog()
        }

        chatListAdapter = ChatListAdapter(
            chats = chats,
            lifecycleScope = lifecycleScope,
            onChatSelected = { chat ->
                openChat(chat)
            }
        )

        recyclerView.apply {
            adapter = chatListAdapter
            layoutManager = LinearLayoutManager(this@ChatListActivity)
        }
    }

    private fun loadChats() {
        lifecycleScope.launch {
            try {
                val userChats = chatService.getCurrentUserChats()
                chats.clear()
                chats.addAll(userChats)
                chatListAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Log.e("ChatListActivity", "Error loading chats", e)
            }
        }
    }

    private fun showSelectUserDialog() {
        lifecycleScope.launch {
            try {
                val role=userService.getCurrentUserRole()
                val userId=userService.getCurrentUserId()!!
                val users = when (role) {
                    Role.Developer -> getDeveloperUsers(userId)
                    Role.Leader-> getUsersForLeader()
                    Role.Manager -> getUsersForManager()
                    else -> emptyList()
                }
                showUserSelectionDialog(users)
            } catch (e: Exception) {
                Log.e("ChatListActivity", "Error loading users", e)
            }
        }
    }

    private suspend fun getDeveloperUsers(userId: String): List<User> {
        try {
            val uniqueUsers = mutableMapOf<String, User>()

            val developers = userService.getMyDeveloperCollegue(userId)
            developers.forEach { developer ->
                uniqueUsers[developer.uid] = developer
            }

            val leaders = userService.getLeadersOfDeveloper(userId)
            leaders.forEach { leader ->
                uniqueUsers[leader.uid] = leader
            }

            Log.d(TAG, "Found ${uniqueUsers.size} unique users (developers and leaders)")

            return uniqueUsers.values.toList()

        } catch (e: Exception) {
            Log.e(TAG, "Error in getDeveloperUsers", e)
            return emptyList()
        }
    }

    private suspend fun getUsersForLeader(): List<User> {
        try {
            val currentUser = userService.getCurrentUser() ?: return emptyList()
            val uniqueUsers = mutableMapOf<String, User>()

            val developers = userService.getUsersByRole(Role.Developer)
            developers.forEach { developer ->
                uniqueUsers[developer.uid] = developer
            }

            val managers = userService.getMyManagers(currentUser.uid)
            managers.forEach { manager ->
                uniqueUsers[manager.uid] = manager
            }

            uniqueUsers.remove(currentUser.uid)

            return uniqueUsers.values.toList()

        } catch (e: Exception) {
            Log.e(TAG, "Error in getUsersForLeader", e)
            return emptyList()
        }
    }

    private suspend fun getUsersForManager(): List<User> {
        val currentUser = userService.getCurrentUser() ?: return emptyList()
        val leaders=userService.getLeaderOfManager(currentUser.uid)
        return leaders
    }

    private fun showUserSelectionDialog(users: List<User>) {
        if (users.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("No Users Available")
                .setMessage("No users found to start a chat with.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Create array of TextViews with tags
        val items = users.map { user ->
            TextView(this).apply {
                text = "${user.name} ${user.surname}"
                tag = user.uid  // Store the user ID as a tag
                setPadding(50, 30, 50, 30) // Add some padding for better appearance
            }
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select User")
            .setItems(items.map { it.text.toString() }.toTypedArray()) { _, which ->
                // Retrieve the ID from the tag
                val selectedUserId = items[which].tag as String
                // Find the full user object using the ID
                val selectedUser = users.find { it.uid == selectedUserId }
                selectedUser?.let { startChatWithUser(it) }
            }
            .show()
    }

    private fun startChatWithUser(user: User) {
        lifecycleScope.launch {
            try {
                val chatId = chatService.startChatWithUser(user.uid)
                if (chatId != null) {
                    openChat(chatId)
                }
            } catch (e: Exception) {
                Log.e("ChatListActivity", "Error starting chat", e)
                AlertDialog.Builder(this@ChatListActivity)
                    .setTitle("Error")
                    .setMessage("Failed to start chat. Please try again.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun openChat(chat: Chat) {
        openChat(chat.chatId)
    }

    private fun openChat(chatId: String) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("chatId", chatId)
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, LoggedActivity::class.java)
        startActivity(intent)
        finish()
    }


}
