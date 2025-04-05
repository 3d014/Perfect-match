package com.example.coupleswipe.repositories

import android.util.Log
import com.example.coupleswipe.model.Category
import com.google.firebase.firestore.FirebaseFirestore

class CategoryRepository(private val firestore: FirebaseFirestore) {

    fun getAllCategories(onResult: (List<Category>) -> Unit) {
        firestore.collection("categories")
            .get()
            .addOnSuccessListener { snapshot ->
                val categories = snapshot.documents.mapNotNull { doc ->
                    try {
                        Category(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            description = doc.getString("description") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("CategoryRepo", "Error parsing category ${doc.id}", e)
                        null
                    }
                }
                Log.d("CategoryRepo", "Fetched ${categories.size} categories") // Debug log
                onResult(categories)
            }
            .addOnFailureListener { e ->
                Log.e("CategoryRepo", "Error getting categories", e)
                onResult(emptyList())
            }
    }
}