package com.example.coupleswipe.repository

import android.util.Log
import com.example.coupleswipe.fragments.FilterSelection
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class InvitationRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun createInvitation(
        categoryName: String,
        teammateEmail: String,
        filters: List<FilterSelection>,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            // Get current user ID
            val currentUserEmail = auth.currentUser?.email ?: run {
                onError(Exception("User not authenticated"))
                return
            }

            // Convert filters to a map format for storage
            val filterData = mutableMapOf<String, Any>()
            filters.forEach { filter ->
                filterData[filter.filterName] = filter.selectedValues
            }

            // Create invitation document
            val invitationId = UUID.randomUUID().toString()
            val invitationData = hashMapOf(
                "id" to invitationId,
                "categoryName" to categoryName,
                "inviterEmail" to currentUserEmail,
                "inviteeEmail" to teammateEmail,
                "filters" to filterData,
                "status" to "pending",
                "createdAt" to System.currentTimeMillis()
            )

            // Save to Firestore
            db.collection("invitations")
                .document(invitationId)
                .set(invitationData)
                .addOnSuccessListener {
                    Log.d("InvitationRepository", "Invitation created successfully")
                    onSuccess(invitationId)
                }
                .addOnFailureListener { e ->
                    Log.e("InvitationRepository", "Failed to create invitation", e)
                    onError(e)
                }

        } catch (e: Exception) {
            Log.e("InvitationRepository", "Error creating invitation", e)
            onError(e)
        }
    }

    fun getInvitation(
        invitationId: String,
        onSuccess: (Map<String, Any>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("invitations")
            .document(invitationId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    onSuccess(document.data as Map<String, Any>)
                } else {
                    onError(Exception("Invitation not found"))
                }
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    fun updateInvitationStatus(
        invitationId: String,
        status: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("invitations")
            .document(invitationId)
            .update("status", status)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }
}