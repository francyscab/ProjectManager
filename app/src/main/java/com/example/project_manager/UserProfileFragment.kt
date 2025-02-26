package com.example.project_manager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.project_manager.models.User
import com.example.project_manager.repository.FileRepository
import com.example.project_manager.services.UserService
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

private const val ARG_USER_ID = "userId"


class ProfileFragment : Fragment() {

    private var userId: String? = null


    private val TAG = "ProfileFragment"
    val userService = UserService()
    val fileRepository = FileRepository()

    private lateinit var profileImage: CircleImageView
    private lateinit var nameText: TextView
    private lateinit var surnameText: TextView
    private lateinit var emailText: TextView
    private lateinit var roleText: TextView
    private lateinit var skillsChipGroup: ChipGroup
    private lateinit var logoutButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(ARG_USER_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.profile_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)

        lifecycleScope.launch {
            val user = userService.getCurrentUser()!!
            setUserData(user)
            setupSkills(getUserSkills(user))
        }

        logoutButton.setOnClickListener {
            logoutUser()
        }
    }

    private fun initializeViews(view: View) {
        profileImage = view.findViewById(R.id.profileImage)
        nameText = view.findViewById(R.id.nameText)
        surnameText = view.findViewById(R.id.surnameText)
        emailText = view.findViewById(R.id.emailText)
        roleText = view.findViewById(R.id.roleText)
        skillsChipGroup = view.findViewById(R.id.skillsChipGroup)
        logoutButton = view.findViewById(R.id.logoutButton)
    }

    private suspend fun setUserData(user: User) {
        nameText.text = user.name
        surnameText.text = user.surname
        emailText.text = user.email
        roleText.text = user.role.toString()

        fileRepository.loadProfileImage(requireContext(), profileImage, user.profile_image_url)
    }

    private fun setupSkills(skills: List<String>) {
        skillsChipGroup.removeAllViews()

        skills.forEach { skill ->
            val chip = Chip(requireContext()).apply {
                text = skill
                setChipBackgroundColorResource(R.color.progress_foreground)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                isClickable = false
            }
            skillsChipGroup.addView(chip)
        }
    }

    private fun getUserSkills(user: User): List<String> {
        return listOf("Kotlin", "Android", "Firebase")
    }

    private fun logoutUser() {
        // Rimuovi tutti i listener prima del logout
        NotificationManager.getInstance(requireContext()).removeAllListeners()

        // Procedi con il logout come fai ora
        FirebaseAuth.getInstance().signOut()

        val preferences = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        preferences.edit().clear().apply()

        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    companion object {
        private const val TAG = "ProfileFragment"
        @JvmStatic
        fun newInstance(userId: String? = null) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId)
                }
            }
    }
}