package com.example.coupleswipe.activity

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.coupleswipe.R
import com.google.firebase.firestore.FirebaseFirestore

class SwipeActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swipe) // Create this layout if it doesn’t exist

        val gameSessionId = intent.getStringExtra("GAME_SESSION_ID") ?: run {
            Toast.makeText(this, "No game session ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Fetch game session details for verification
        db.collection("gameSessions")
            .document(gameSessionId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val movieListId = document.getString("movieListId") ?: ""
                    val categoryName = document.getString("categoryName") ?: ""
                    Toast.makeText(this, "Game session loaded: $categoryName", Toast.LENGTH_SHORT).show()
                    Log.d("SwipeGameActivity", "MovieListId: $movieListId")
                    // TODO: Fetch movies from movieListId and display them (Step 4)
                } else {
                    Toast.makeText(this, "Game session not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading game session: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("SwipeGameActivity", "Fetch error", e)
                finish()
            }
    }
}