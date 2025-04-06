package com.example.coupleswipe.activity

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.coupleswipe.R
import com.example.coupleswipe.fragments.MoviesFragment
import com.example.coupleswipe.repository.InvitationRepository
import com.example.coupleswipe.viewModels.MoviesFilterViewModel
import com.google.firebase.firestore.FirebaseFirestore

class NewGame : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private val moviesFilterViewModel: MoviesFilterViewModel by viewModels()
    private lateinit var sendInvitationButton: Button
    private lateinit var inviteField: EditText
    private val invitationRepository = InvitationRepository()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_game)

        db = FirebaseFirestore.getInstance()


        val categoryName = intent.getStringExtra("CATEGORY_NAME") ?: ""

        Log.d("NewGame", "Loading filters for category: $categoryName ")
        when (categoryName.lowercase()) {
            "movies" -> loadMoviesFragment(categoryName)

            else -> showUnsupportedCategory()
        }
        inviteField = findViewById(R.id.teammateEmail)
        sendInvitationButton = findViewById(R.id.inviteButton)
        sendInvitationButton.setOnClickListener {
            val currentFilters = moviesFilterViewModel.getCurrentFilters()
            val teammateEmail = inviteField.text.toString().trim()
            Log.d("NewGame", "Retrieved ${currentFilters.size} filters")

            // Example of accessing filters (just for demonstration)
            currentFilters.forEach { filter ->
                Log.d("NewGame", "Filter: ${filter.filterName}, Values: ${filter.selectedValues}")
            }
            if (teammateEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(teammateEmail).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            invitationRepository.createInvitation(
                categoryName = intent.getStringExtra("CATEGORY_NAME") ?: "",
                teammateEmail = teammateEmail,
                filters = currentFilters,
                onSuccess = { invitationId ->
                    runOnUiThread {
                        Toast.makeText(this, "Invitation sent!", Toast.LENGTH_SHORT).show()
                        // Optional: Close activity or clear fields
                        inviteField.text.clear()
                    }
                },
                onError = { exception ->
                    runOnUiThread {
                        Toast.makeText(this, "Failed to send: ${exception.message}", Toast.LENGTH_SHORT).show()
                        Log.e("NewGame", "Invitation error", exception)
                    }
                }
            )
            // Here you would call your invitation repository
            // invitationRepository.createInvitation(categoryName, currentFilters)
        }




    }

    private fun loadMoviesFragment(categoryName: String) {
        val fragment = MoviesFragment().apply {
            arguments = Bundle().apply {
                putString("CATEGORY_NAME", categoryName)
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.filterFragmentContainer, fragment)
            .commit()
    }

    private fun showUnsupportedCategory() {
        Toast.makeText(this, "This category isn't supported yet", Toast.LENGTH_SHORT).show()
        finish()
    }

}