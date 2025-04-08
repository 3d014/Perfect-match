package com.example.coupleswipe.activity


import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.coupleswipe.R
import com.example.coupleswipe.api.TMDBApiClient
import com.example.coupleswipe.fragments.FilterSelection
import com.example.coupleswipe.fragments.MoviesFragment
import com.example.coupleswipe.repository.InvitationRepository
import com.example.coupleswipe.utils.convertFiltersToTMDBParams
import com.example.coupleswipe.utils.genreMap
import com.example.coupleswipe.viewModels.MoviesFilterViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class NewGame : BaseActivity() {

    private lateinit var db: FirebaseFirestore
    private val moviesFilterViewModel: MoviesFilterViewModel by viewModels()
    private lateinit var sendInvitationButton: Button
    private lateinit var inviteField: EditText
    private val invitationRepository = InvitationRepository()
    private var statusListener: ListenerRegistration? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_game)
        db = FirebaseFirestore.getInstance()
        val categoryName = intent.getStringExtra("CATEGORY_NAME") ?: ""

        Log.d("NewGame", "Loading filters for category: $categoryName")
        when (categoryName.lowercase()) {
            "movies" -> loadMoviesFragment(categoryName)
            else -> showUnsupportedCategory()
        }

        inviteField = findViewById(R.id.teammateEmail)
        sendInvitationButton = findViewById(R.id.inviteButton)

        sendInvitationButton.setOnClickListener {
            val currentFilters = moviesFilterViewModel.getCurrentFilters()
            val teammateEmail = inviteField.text.toString().trim()

            if (teammateEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(teammateEmail).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            // Clear any existing listener
            statusListener?.remove()

            invitationRepository.createInvitation(
                categoryName = categoryName,
                teammateEmail = teammateEmail,
                filters = currentFilters,
                onSuccess = { invitationId ->
                    runOnUiThread {
                        Toast.makeText(this, "Invitation sent! Waiting for response...", Toast.LENGTH_SHORT).show()
                        inviteField.text.clear()

                        // Start listening for status changes
                        setupStatusListener(invitationId)
                    }
                },
                onError = { exception ->
                    runOnUiThread {
                        Toast.makeText(this, "Failed to send: ${exception.message}", Toast.LENGTH_SHORT).show()
                        Log.e("NewGame", "Invitation error", exception)
                    }
                }
            )
        }
    }

    private fun setupStatusListener(invitationId: String) {
        statusListener = invitationRepository.listenForStatusChanges(
            invitationId = invitationId,
            onStatusChanged = { newStatus ->
                runOnUiThread {
                    when (newStatus) {
                        "accepted" -> {
                            Toast.makeText(this, "Invitation accepted! Starting game...", Toast.LENGTH_SHORT).show()
                            // startActivity(Intent(this, GameActivity::class.java))
                            // finish()
                        }
                        "declined" -> {
                            Toast.makeText(this, "Invitation was declined", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this, "Status changed: $newStatus", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this, "Error listening for updates", Toast.LENGTH_SHORT).show()
                    Log.e("NewGame", "Status listener error", error)
                }
            }
        )
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

    override fun onDestroy() {
        super.onDestroy()
        // Clean up the listener when the activity is destroyed
        statusListener?.remove()
    }

}