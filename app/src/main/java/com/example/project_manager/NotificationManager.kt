import android.content.Context
import com.example.project_manager.models.Role
import com.example.project_manager.repository.NotificationHelper
import com.example.project_manager.services.ChatService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class NotificationManager private constructor(private val context: Context) {
    private val listeners = mutableListOf<ListenerRegistration>()
    private val db = FirebaseFirestore.getInstance()
    private val notificationHelper = NotificationHelper(context, db)

    // Crea un CoroutineScope dedicato per i listener
    private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Questa funzione configura tutti i listener necessari per l'utente
    suspend fun setupListeners(userId: String, role: Role) {
        // Rimuovi eventuali listener precedenti
        removeAllListeners()

        // Configura listener per le chat
        val chatService = ChatService()
        val chats = chatService.getCurrentUserChats()
        notificationHelper.handleNotification(role, userId, "chat", notificationScope, chats)

        // Configura listener specifici in base al ruolo
        when (role) {
            Role.Manager -> {
                notificationHelper.handleNotification(role, userId, "progresso", notificationScope)
            }
            Role.Leader -> {
                notificationHelper.handleNotification(role, userId, "progresso", notificationScope)
                notificationHelper.handleNotification(role, userId, "sollecito", notificationScope)
            }
            Role.Developer -> {
                notificationHelper.handleNotification(role, userId, "progresso", notificationScope)
                notificationHelper.handleNotification(role, userId, "sollecito", notificationScope)
            }
        }

        // Salva tutti i listener creati nella classe NotificationHelper
        listeners.addAll(notificationHelper.getActiveListeners())
    }

    // Rimuovi tutti i listener quando l'utente si disconnette
    fun removeAllListeners() {
        for (listener in listeners) {
            listener.remove()
        }
        listeners.clear()
    }

    companion object {
        @Volatile private var instance: NotificationManager? = null

        fun getInstance(context: Context): NotificationManager {
            return instance ?: synchronized(this) {
                instance ?: NotificationManager(context.applicationContext).also { instance = it }
            }
        }
    }
}