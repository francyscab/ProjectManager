package com.example.project_manager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private const val ARG_USER_ID = "userId"
private const val ARG_USER_ROLE = "userRole"


class ChatListFragment : Fragment() {
    // Argomenti del fragment
    private var userId: String? = null
    private var userRole: Role? = null

    // Views
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatListAdapter: ChatListAdapter
    private lateinit var startChatButton: MaterialButton

    // Services
    private val chatService = ChatService()
    private val userService = UserService()

    // Data
    private val chats = mutableListOf<Chat>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(ARG_USER_ID)
            userRole = it.getString(ARG_USER_ROLE)?.let { role -> Role.valueOf(role) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.chat_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupRecyclerView()
        setupClickListeners()
        lifecycleScope.launch {
            observeDataChanges()
        }

    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewChatList)
        startChatButton = view.findViewById(R.id.buttonStartNewChat)
    }

    private fun setupRecyclerView() {
        chatListAdapter = ChatListAdapter(
            chats = chats,
            lifecycleScope = lifecycleScope,
            onChatSelected = { chat -> openChat(chat) }
        )

        recyclerView.apply {
            adapter = chatListAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        startChatButton.setOnClickListener {
            showSelectUserDialog()
        }
    }

    private fun loadChats() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userChats = chatService.getCurrentUserChats()
                chats.clear()
                chats.addAll(userChats)
                chatListAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading chats", e)
                showError("Error loading chats")
            }
        }
    }

    private suspend fun observeDataChanges() {
        loadChats()
        val db = FirebaseFirestore.getInstance()
        db.collection("chat")  // Cambia il nome della collezione se necessario
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(ChatListFragment.TAG, "Errore durante l'ascolto delle modifiche", error)
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    lifecycleScope.launch {
                        loadChats()  // Ricarica i dati ogni volta che ci sono modifiche
                    }
                }
            }
    }

    private fun showSelectUserDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
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
                Log.e(TAG, "Error loading users", e)
                showError("Error loading users")
            }
        }
    }

    private fun showUserSelectionDialog(users: List<User>) {
        if (!isAdded) return

        if (users.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("No Users Available")
                .setMessage("No users found to start a chat with.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Select User")
            .setItems(users.map { "${it.name} ${it.surname}" }.toTypedArray()) { _, which ->
                startChatWithUser(users[which])
            }
            .show()
    }

    private fun startChatWithUser(user: User) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val chatId = chatService.startChatWithUser(user.uid)
                chatId?.let { openChat(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting chat", e)
                showError("Failed to start chat")
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

    private fun showError(message: String) {
        if (isAdded) {
            AlertDialog.Builder(requireContext())
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
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

    companion object {
        private const val TAG = "ChatListFragment"

        @JvmStatic
        fun newInstance(userId: String? = null, userRole: String? = null) =
            ChatListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId)
                    putString(ARG_USER_ROLE, userRole)
                }
            }
    }
}