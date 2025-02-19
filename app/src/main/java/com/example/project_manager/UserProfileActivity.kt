/*package com.example.project_manager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.project_manager.models.User
import com.example.project_manager.services.UserService
import com.example.project_manager.repository.FileRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

class UserProfileActivity : AppCompatActivity() {

    private val TAG = "UserProfileActivity"
    val userService= UserService()
    val fileRepository= FileRepository()

    private lateinit var profileImage: CircleImageView
    private lateinit var nameText: TextView
    private lateinit var surnameText: TextView
    private lateinit var emailText: TextView
    private lateinit var roleText: TextView
    private lateinit var skillsChipGroup: ChipGroup
    private lateinit var logoutButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        initializeViews()

        lifecycleScope.launch {
            val user = userService.getCurrentUser()!!
            setUserData(user)
            setupSkills(getUserSkills(user)) // l'utente non ha ancora un campo skill
        }

        logoutButton.setOnClickListener {
            logoutUser()
        }
    }

    private fun initializeViews() {
        profileImage = findViewById(R.id.profileImage)
        nameText = findViewById(R.id.nameText)
        surnameText = findViewById(R.id.surnameText)
        emailText = findViewById(R.id.emailText)
        roleText = findViewById(R.id.roleText)
        skillsChipGroup = findViewById(R.id.skillsChipGroup)
        logoutButton = findViewById(R.id.logoutButton)
    }

    private suspend fun setUserData(user: User) {
        nameText.text = user.name
        surnameText.text = user.surname
        emailText.text = user.email
        roleText.text = user.role.toString()

        fileRepository.loadProfileImage(this, profileImage, user.profile_image_url)
    }

    private fun setupSkills(skills: List<String>) {
        skillsChipGroup.removeAllViews() // Pulisce le chip esistenti

        skills.forEach { skill ->
            val chip = Chip(this).apply {
                text = skill
                setChipBackgroundColorResource(R.color.progress_foreground)
                setTextColor(ContextCompat.getColor(context, R.color.white))
                isClickable = false
            }
            skillsChipGroup.addView(chip)
        }
    }

    //ottenere le skill dell'utente
    private fun getUserSkills(user: User): List<String> {
        //esempio
        return listOf("Kotlin", "Android", "Firebase")
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()

        val preferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        preferences.edit().clear().apply()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

}
*/