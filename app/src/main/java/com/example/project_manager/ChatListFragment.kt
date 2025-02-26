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
import com.example.project_manager.repository.UserRepository
import com.example.project_manager.services.ChatService
import com.example.project_manager.services.UserService
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
    private val userRepository = UserRepository()
    private val db = FirebaseFirestore.getInstance()

    // Data
    private val chats = mutableListOf<Chat>()

    // Listeners
    private var userChatsListener: ListenerRegistration? = null
    private val chatListeners = mutableMapOf<String, ListenerRegistration>()

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
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            setupListeners()
        }
    }

    override fun onPause() {
        super.onPause()
        removeAllListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeAllListeners()
    }

    private fun removeAllListeners() {
        // Rimuove il listener della collezione chat dell'utente
        userChatsListener?.remove()
        userChatsListener = null

        // Rimuove tutti i listener delle singole chat
        chatListeners.forEach { (_, listener) ->
            listener.remove()
        }
        chatListeners.clear()
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

    private suspend fun setupListeners() {
        val currentUserId = userService.getCurrentUserId() ?: return

        // Inizializza la lista con le chat attuali
        loadChats()

        // Ascolta i cambiamenti nella collezione "chat" dell'utente
        userChatsListener = db.collection("utenti")
            .document(currentUserId)
            .collection("chat")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Errore durante l'ascolto delle chat dell'utente", error)
                    return@addSnapshotListener
                }

                if (snapshots == null) return@addSnapshotListener

                // Processa i cambiamenti ai documenti (aggiunti, modificati, rimossi)
                for (dc in snapshots.documentChanges) {
                    val chatId = dc.document.id

                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            // Configura un nuovo listener per questa chat se non esiste già
                            if (!chatListeners.containsKey(chatId)) {
                                setupChatListener(chatId)
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            // Rimuove il listener per questa chat
                            chatListeners[chatId]?.remove()
                            chatListeners.remove(chatId)

                            // Rimuove la chat dalla lista
                            val chatToRemove = chats.find { it.chatId == chatId }
                            if (chatToRemove != null) {
                                chats.remove(chatToRemove)
                                chatListAdapter.notifyDataSetChanged()
                            }
                        }
                        else -> { /* Ignora modifiche */ }
                    }
                }
            }
    }

    private fun setupChatListener(chatId: String) {
        val chatListener = db.collection("chat")
            .document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Errore durante l'ascolto della chat $chatId", error)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener

                lifecycleScope.launch {
                    // Ricarica tutte le chat per assicurarsi che siano sincronizzate
                    loadChats()
                }
            }

        chatListeners[chatId] = chatListener
    }

    private suspend fun loadChats() {
        try {
            val userChats = chatService.getCurrentUserChats()

            // Aggiorna la lista mantenendo l'ordine
            chats.clear()
            chats.addAll(userChats)
            chatListAdapter.notifyDataSetChanged()

            // Assicurati che ci sia un listener per ogni chat
            userChats.forEach { chat ->
                if (!chatListeners.containsKey(chat.chatId)) {
                    setupChatListener(chat.chatId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading chats", e)
            if (isAdded) {
                showError("Error loading chats")
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
                // Ottieni l'ID dell'utente corrente
                val currentUserId = userService.getCurrentUserId() ?: return@launch

                // Crea una nuova chat
                val chatId = chatService.startChatWithUser(user.uid)

                if (chatId != null) {
                    // Aggiungi la chat alla collezione dell'utente corrente
                    val success1 = userRepository.addChatToUser(currentUserId, chatId, user.uid)

                    // Aggiungi la chat alla collezione dell'altro utente
                    val success2 = userRepository.addChatToUser(user.uid, chatId, currentUserId)

                    if (success1 && success2) {
                        // Apri direttamente la ChatActivity con l'ID della chat
                        val intent = Intent(requireContext(), ChatActivity::class.java)
                        intent.putExtra("chatId", chatId)
                        startActivity(intent)
                    } else {
                        Log.e(TAG, "Errore nell'aggiungere la chat alle collezioni degli utenti")
                        showError("Failed to setup chat")
                    }
                } else {
                    showError("Failed to start chat")
                }
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