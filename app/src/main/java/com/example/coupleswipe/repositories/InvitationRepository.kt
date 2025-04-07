package com.example.coupleswipe.repository

import android.util.Log
import com.example.coupleswipe.fragments.FilterSelection
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.UUID

class InvitationRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val EXPIRATION_TIME_MS = 20000L
    // Add this new function for real-time status listening
    fun listenForStatusChanges(
        invitationId: String,
        onStatusChanged: (String) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("invitations")
            .document(invitationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val expiresAt = snapshot.getLong("expiresAt") ?: 0L
                    if (expiresAt < System.currentTimeMillis() &&
                        snapshot.getString("status") == "pending") {
                        // Auto-delete expired invitations
                        snapshot.reference.delete()
                        onStatusChanged("expired")
                    } else {
                        val newStatus = snapshot.getString("status") ?: "pending"
                        onStatusChanged(newStatus)
                    }
                } else {
                    onError(Exception("Invitation document not found"))
                }
            }
    }

    // Rest of your existing functions remain the same
    fun createInvitation(
        categoryName: String,
        teammateEmail: String,
        filters: List<FilterSelection>,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val currentUserEmail = auth.currentUser?.email?.lowercase() ?: run {
                onError(Exception("User not authenticated"))
                return
            }
            val targetEmail = teammateEmail.lowercase()

            // 1. Prevent self-invites
            if (currentUserEmail == targetEmail) {
                onError(Exception("Cannot send invitation to yourself"))
                return
            }

            // 2. Check for existing pending invitation
            db.collection("invitations")
                .whereEqualTo("inviterEmail", currentUserEmail)
                .whereEqualTo("inviteeEmail", targetEmail)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        onError(Exception("You already have a pending invitation to this user"))
                        return@addOnSuccessListener
                    }

                    // 3. Create new invitation with expiration
                    val filterData = mutableMapOf<String, Any>()
                    filters.forEach { filter ->
                        filterData[filter.filterName] = filter.selectedValues
                    }

                    val invitationId = UUID.randomUUID().toString()
                    val invitationData = hashMapOf(
                        "id" to invitationId,
                        "categoryName" to categoryName,
                        "inviterEmail" to currentUserEmail,
                        "inviteeEmail" to targetEmail,
                        "filters" to filterData,
                        "status" to "pending",
                        "createdAt" to System.currentTimeMillis(),
                        "expiresAt" to (System.currentTimeMillis() + EXPIRATION_TIME_MS)
                    )

                    db.collection("invitations")
                        .document(invitationId)
                        .set(invitationData)
                        .addOnSuccessListener {
                            Log.d("InvitationRepository", "Invitation created successfully")
                            onSuccess(invitationId)

                            // 4. Schedule automatic expiration
                            scheduleInvitationExpiration(invitationId)
                        }
                        .addOnFailureListener { e ->
                            Log.e("InvitationRepository", "Failed to create invitation", e)
                            onError(e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("InvitationRepository", "Failed to check existing invitations", e)
                    onError(e)
                }

        } catch (e: Exception) {
            Log.e("InvitationRepository", "Error creating invitation", e)
            onError(e)
        }
    }
    private fun scheduleInvitationExpiration(invitationId: String) {
        android.os.Handler().postDelayed({
            db.collection("invitations")
                .document(invitationId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists() &&
                        document.getString("status") == "pending") {
                        // Delete if still pending after timeout
                        document.reference.delete()
                            .addOnSuccessListener {
                                Log.d("InvitationRepository", "Expired invitation deleted: $invitationId")
                            }
                    }
                }
        }, EXPIRATION_TIME_MS)
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