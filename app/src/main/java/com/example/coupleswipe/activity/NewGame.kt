package com.example.coupleswipe.activity


import android.content.Intent
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
import com.example.coupleswipe.model.Movie
import com.example.coupleswipe.repository.InvitationRepository
import com.example.coupleswipe.utils.convertFiltersToTMDBParams
import com.example.coupleswipe.utils.genreMap
import com.example.coupleswipe.viewModels.MoviesFilterViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.util.UUID

class NewGame : BaseActivity() {

    private lateinit var db: FirebaseFirestore
    private val moviesFilterViewModel: MoviesFilterViewModel by viewModels()
    private lateinit var sendInvitationButton: Button
    private lateinit var inviteField: EditText
    private val invitationRepository = InvitationRepository()
    private var statusListener: ListenerRegistration? = null
    private val auth = FirebaseAuth.getInstance()


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
                            Toast.makeText(this, "Invitation accepted! Fetching movies...", Toast.LENGTH_SHORT).show()
                            fetchAndStoreMovies(invitationId)
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

    private fun fetchAndStoreMovies(invitationId: String) {
        // Fetch invitation details to get filters
        invitationRepository.getInvitation(
            invitationId = invitationId,
            onSuccess = { invitationData ->
                val filters = invitationData["filters"] as? Map<String, Any> ?: emptyMap()
                val categoryName = invitationData["categoryName"] as? String ?: "movies"
                val inviteeEmail = invitationData["inviteeEmail"] as? String ?: ""

                // Convert filters to TMDB parameters
                val tmdbParams = convertFiltersToTMDBParams(filtersToFilterSelection(filters))

                // Fetch movies from TMDB API
                lifecycleScope.launch {
                    try {
                        val response = TMDBApiClient.tmdbApi.getFilteredMovies(
                            genres = tmdbParams["with_genres"],
                            minDate = tmdbParams["primary_release_date.gte"],
                            maxDate = tmdbParams["primary_release_date.lte"],
                            minRating = tmdbParams["vote_average.gte"]
                        )
                        if (response.isSuccessful) {
                            val movies = response.body()?.results ?: emptyList()
                            storeMoviesAndCreateGameSession(movies, invitationId, inviteeEmail, categoryName)
                        } else {
                            Toast.makeText(this@NewGame, "Failed to fetch movies", Toast.LENGTH_SHORT).show()
                            Log.e("NewGame", "TMDB API error: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@NewGame, "Error fetching movies: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("NewGame", "TMDB fetch error", e)
                    }
                }
            },
            onError = { error ->
                Toast.makeText(this, "Error fetching invitation: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("NewGame", "Invitation fetch error", error)
            }
        )
    }
    private fun filtersToFilterSelection(filters: Map<String, Any>): List<FilterSelection> {
        return filters.map { (filterName, selectedValues) ->
            FilterSelection(filterName, selectedValues as List<Any>)
        }
    }

    private fun storeMoviesAndCreateGameSession(
        movies: List<Movie>,
        invitationId: String,
        inviteeEmail: String,
        categoryName: String
    ) {
        val movieListId = UUID.randomUUID().toString()
        val inviterEmail = auth.currentUser?.email

        val batch = db.batch()
        val movieListRef = db.collection("movies").document(movieListId).collection("movieList")

        movies.forEach { movie ->
            val movieDoc = movieListRef.document(movie.id.toString())
            batch.set(movieDoc, hashMapOf(
                "id" to movie.id.toString(),
                "title" to movie.title,
                "imageUrl" to "https://image.tmdb.org/t/p/w500${movie.posterPath}",
                "releaseDate" to movie.releaseDate,
                "rating" to movie.rating,
                "description" to movie.description
            ))
        }

        batch.commit()
            .addOnSuccessListener {
                Log.d("NewGame", "Movies stored successfully in subcollection with ID: $movieListId")

                // Create game session
                val gameSessionId = UUID.randomUUID().toString()
                val gameSessionData = hashMapOf(
                    "inviterEmail" to inviterEmail,
                    "inviteeEmail" to inviteeEmail,
                    "movieListId" to movieListId,
                    "categoryName" to categoryName,
                    "status" to "active",
                    "createdAt" to System.currentTimeMillis(),
                    "finishedUsers" to listOf<String>()
                )

                db.collection("gameSessions")
                    .document(gameSessionId)
                    .set(gameSessionData)
                    .addOnSuccessListener {
                        Log.d("NewGame", "Game session created with ID: $gameSessionId")
                        Toast.makeText(this, "Game session created! Starting game...", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, SwipeGameActivity::class.java).apply {
                            putExtra("GAME_SESSION_ID", gameSessionId)
                        }
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to create game session: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("NewGame", "Game session creation error", e)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to store movies: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("NewGame", "Movies storage error", e)
            }
    }
}