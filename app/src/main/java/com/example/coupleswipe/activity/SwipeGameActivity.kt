package com.example.coupleswipe.activity

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.coupleswipe.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction
import android.app.Dialog
import android.widget.Button

data class Movie(
    val id: String = "",
    val title: String = "",
    val imageUrl: String = ""
)

class SwipeGameActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var cardStackView: CardStackView
    private lateinit var adapter: MovieCardAdapter
    private lateinit var layoutManager: CardStackLayoutManager // Moved to class level
    private var movieListId: String = ""
    private var gameSessionId: String = ""
    private val movies = mutableListOf<Movie>()
    private val userLikes = mutableMapOf<String, Boolean>() // movieId -> liked

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swipe_game)

        cardStackView = findViewById(R.id.card_stack_view)

        gameSessionId = intent.getStringExtra("GAME_SESSION_ID") ?: run {
            Toast.makeText(this, "No game session ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inicijalizacija CardStackView-a
        layoutManager = CardStackLayoutManager(this, object : CardStackListener {
            override fun onCardDragging(direction: Direction?, ratio: Float) {}
            override fun onCardSwiped(direction: Direction?) {
                val position = layoutManager.topPosition - 1 // Now accessible
                val movie = movies.getOrNull(position) ?: return
                val liked = direction == Direction.Right
                userLikes[movie.id] = liked
                saveUserSwipe(movie.id, liked)
            }
            override fun onCardRewound() {}
            override fun onCardCanceled() {}
            override fun onCardAppeared(view: View?, position: Int) {}
            override fun onCardDisappeared(view: View?, position: Int) {
                if (position == movies.size - 1) {
                    onSwipingFinished()
                }
            }
        }).apply {
            setDirections(listOf(Direction.Left, Direction.Right)) // Samo levo i desno
            setCanScrollHorizontal(true)
            setCanScrollVertical(false)
            setVisibleCount(3) // Broj vidljivih kartica
            setSwipeThreshold(0.3f) // Prag za svajp
        }

        cardStackView.layoutManager = layoutManager
        adapter = MovieCardAdapter(movies)
        cardStackView.adapter = adapter

        // Učitaj filmove
        loadMovies()

    }

    private fun loadMovies() {
        db.collection("gameSessions").document(gameSessionId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    movieListId = document.getString("movieListId") ?: ""
                    db.collection("movies").document(movieListId).collection("movieList")
                        .get()
                        .addOnSuccessListener { snapshot ->
                            movies.clear()
                            snapshot.documents.forEach { doc ->
                                val movie = doc.toObject(Movie::class.java)?.copy(id = doc.id)
                                if (movie != null) movies.add(movie)
                            }
                            adapter.notifyDataSetChanged()
                            if (movies.isEmpty()) {
                                Toast.makeText(this, "No movies found", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error loading movies: ${e.message}", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                } else {
                    Toast.makeText(this, "Game session not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading session: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun saveUserSwipe(movieId: String, liked: Boolean) {
        val userEmail = auth.currentUser?.email ?: return
        val swipeData = mapOf(
            "userEmail" to userEmail,
            "movieId" to movieId,
            "liked" to liked,
            "timestamp" to FieldValue.serverTimestamp()
        )
        db.collection("gameSessions").document(gameSessionId)
            .collection("swipes").document("${userEmail}_$movieId")
            .set(swipeData)
            .addOnSuccessListener {
                Log.d("SwipeGame", "Swipe saved: $movieId -> $liked")
            }
            .addOnFailureListener { e ->
                Log.e("SwipeGame", "Error saving swipe", e)
            }
    }

    private fun onSwipingFinished() {
        val userEmail = auth.currentUser?.email ?: return
        db.collection("gameSessions").document(gameSessionId)
            .update("finishedUsers", FieldValue.arrayUnion(userEmail))
            .addOnSuccessListener {
                checkForMatch()
            }
            .addOnFailureListener { e ->
                Log.e("SwipeGame", "Error updating finished users", e)
            }
    }

    private fun checkForMatch() {
        db.collection("gameSessions").document(gameSessionId).get()
            .addOnSuccessListener { document ->
                val finishedUsers = document.get("finishedUsers") as? List<String> ?: emptyList()
                val inviterEmail = document.getString("inviterEmail") ?: ""
                val inviteeEmail = document.getString("inviteeEmail") ?: ""
                if (finishedUsers.contains(inviterEmail) && finishedUsers.contains(inviteeEmail)) {
                    findMatchingMovie()
                } else {
                    Toast.makeText(this, "Waiting for the other player...", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun showMatchedMovieDialog(movie: Movie) {
        val dialog = Dialog(this).apply {
            setContentView(R.layout.dialog_matched_movie)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCancelable(false)

            findViewById<TextView>(R.id.matchedMovieTitle).text = movie.title
            findViewById<ImageView>(R.id.matchedMovieImage).load(movie.imageUrl) {
                placeholder(R.drawable.placeholder_image)
            }

            findViewById<Button>(R.id.closeButton).setOnClickListener {
                dismiss()
                finish() // Zatvori aktivnost nakon što se dialog zatvori
            }
        }

        dialog.show()
    }
    private fun findMatchingMovie() {
        db.collection("gameSessions").document(gameSessionId).collection("swipes")
            .get()
            .addOnSuccessListener { snapshot ->
                val swipes = snapshot.documents.mapNotNull { it.toObject(SwipeData::class.java) }
                val inviterLikes = swipes.filter { it.userEmail == auth.currentUser?.email && it.liked }
                    .map { it.movieId }
                val inviteeLikes = swipes.filter { it.userEmail != auth.currentUser?.email && it.liked }
                    .map { it.movieId }
                val commonLikes = inviterLikes.intersect(inviteeLikes.toSet())

                if (commonLikes.isNotEmpty()) {
                    val matchedMovieId = commonLikes.random()
                    db.collection("movies").document(movieListId).collection("movieList")
                        .document(matchedMovieId).get()
                        .addOnSuccessListener { doc ->
                            val movie = doc.toObject(Movie::class.java)
                            if (movie != null) {
                                showMatchedMovieDialog(movie)
                            } else {
                                Toast.makeText(this, "Error loading matched movie", Toast.LENGTH_LONG).show()
                                finish()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error loading movie details", Toast.LENGTH_LONG).show()
                            Log.e("SwipeGame", "Error loading movie", e)
                            finish()
                        }
                } else {
                    Toast.makeText(this, "No matching movies found", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error finding matches", Toast.LENGTH_LONG).show()
                Log.e("SwipeGame", "Error finding matches", e)
                finish()
            }
    }
}

data class SwipeData(
    val userEmail: String = "",
    val movieId: String = "",
    val liked: Boolean = false
)

class MovieCardAdapter(private val movies: List<Movie>) : RecyclerView.Adapter<MovieCardAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.movie_title)
        val image: ImageView = itemView.findViewById(R.id.movie_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val movie = movies[position]
        holder.title.text = movie.title
        holder.image.load(movie.imageUrl) {
            placeholder(R.drawable.placeholder_image) // Dodaj placeholder sliku u drawable
        }
    }

    override fun getItemCount(): Int = movies.size
}