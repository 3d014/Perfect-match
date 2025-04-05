package com.example.coupleswipe.repositories

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class InvitationRepository @Inject constructor(
    private val firestore: FirebaseFirestore = Firebase.firestore
) {

    suspend fun generateInvitation(
        senderId: String,
        categoryId: String
    ): Result<String> {
        return try {
            val invitationRef = firestore.collection("invitations").document()
            val invitationData = hashMapOf(
                "senderId" to senderId,
                "categoryId" to categoryId,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp()
            )

            invitationRef.set(invitationData).await()
            Result.Success(invitationRef.id)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun acceptInvitation(
        invitationId: String,
        receiverId: String
    ): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val invitationRef = firestore.collection("invitations").document(invitationId)
                val snapshot = transaction.get(invitationRef)

                if (!snapshot.exists()) {
                    throw Exception("Invitation not found")
                }

                when (snapshot.getString("status")) {
                    "accepted" -> throw Exception("Invitation already accepted")
                    "expired" -> throw Exception("Invitation has expired")
                    "pending" -> {
                        transaction.update(
                            invitationRef,
                            mapOf(
                                "receiverId" to receiverId,
                                "status" to "accepted",
                                "acceptedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    else -> throw Exception("Invalid invitation status")
                }
            }.await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getInvitationDetails(invitationId: String): Result<InvitationDetails> {
        return try {
            val snapshot = firestore.collection("invitations")
                .document(invitationId)
                .get()
                .await()

            if (!snapshot.exists()) {
                return Result.Error(Exception("Invitation not found"))
            }

            Result.Success(
                InvitationDetails(
                    senderId = snapshot.getString("senderId") ?: "",
                    categoryId = snapshot.getString("categoryId") ?: "",
                    status = snapshot.getString("status") ?: "pending",
                    createdAt = snapshot.getDate("createdAt"),
                    acceptedAt = snapshot.getDate("acceptedAt")
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    data class InvitationDetails(
        val senderId: String,
        val categoryId: String,
        val status: String,
        val createdAt: Date?,
        val acceptedAt: Date?
    )
}

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}