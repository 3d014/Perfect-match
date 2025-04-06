package com.example.coupleswipe.model
data class Category(
    val id: String? = null,
    val name: String? = null,
    val imageUrl: String? = null,
    val description: String? = null

) {
    // Helper function to check if valid
    fun isValid(): Boolean {
        return !name.isNullOrEmpty()
    }
}