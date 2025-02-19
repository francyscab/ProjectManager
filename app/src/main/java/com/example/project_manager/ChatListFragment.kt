package com.example.project_manager


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_manager.models.Chat
import com.example.project_manager.models.Role
import com.example.project_manager.models.User
import com.example.project_manager.services.ChatService
import com.example.project_manager.services.UserService
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ChatListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatListAdapter: ChatListAdapter

    private val chatService = ChatService()
    private val userService = UserService()

    private val chats = mutableListOf<Chat>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.chat_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        loadChats()
    }

    private fun setupViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewChatList)

        val startChatButton: MaterialButton = view.findViewById(R.id.buttonStartNewChat)
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
            layoutManager = LinearLayoutManager(requireContext())
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
                Log.e("ChatListFragment", "Error loading chats", e)
            }
        }
    }

    private fun showSelectUserDialog() {
        lifecycleScope.launch {
            try {
                val role = userService.getCurrentUserRole()
                val userId = userService.getCurrentUserId()!!
                val users = when (role) {
                    Role.Developer -> getDeveloperUsers(userId)
                    Role.Leader -> getUsersForLeader()
                    Role.Manager -> getUsersForManager()
                    else -> emptyList()
                }
                showUserSelectionDialog(users)
            } catch (e: Exception) {
                Log.e("ChatListFragment", "Error loading users", e)
            }
        }
    }

    private suspend fun getDeveloperUsers(userId: String): List<User> {
        return try {
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
            uniqueUsers.values.toList()
        } catch (e: Exception) {
            Log.e(TAG, "Error in getDeveloperUsers", e)
            emptyList()
        }
    }

    private suspend fun getUsersForLeader(): List<User> {
        return try {
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
            uniqueUsers.values.toList()
        } catch (e: Exception) {
            Log.e(TAG, "Error in getUsersForLeader", e)
            emptyList()
        }
    }

    private suspend fun getUsersForManager(): List<User> {
        val currentUser = userService.getCurrentUser() ?: return emptyList()
        return userService.getLeaderOfManager(currentUser.uid)
    }

    private fun showUserSelectionDialog(users: List<User>) {
        if (!isAdded) return  // Check if fragment is attached to activity

        if (users.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("No Users Available")
                .setMessage("No users found to start a chat with.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val items = users.map { user ->
            TextView(requireContext()).apply {
                text = "${user.name} ${user.surname}"
                tag = user.uid
                setPadding(50, 30, 50, 30)
            }
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Select User")
            .setItems(items.map { it.text.toString() }.toTypedArray()) { _, which ->
                val selectedUserId = items[which].tag as String
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
                Log.e("ChatListFragment", "Error starting chat", e)
                if (isAdded) {  // Check if fragment is attached
                    AlertDialog.Builder(requireContext())
                        .setTitle("Error")
                        .setMessage("Failed to start chat. Please try again.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    private fun openChat(chat: Chat) {
        openChat(chat.chatId)
    }

    private fun openChat(chatId: String) {
        val intent = Intent(requireContext(), ChatActivity::class.java)
        intent.putExtra("chatId", chatId)
        startActivity(intent)
    }

    companion object {
        private const val TAG = "ChatListFragment"
    }
}