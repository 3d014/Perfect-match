package com.example.coupleswipe.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.coupleswipe.repository.InvitationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentChange

class InvitationListenerService : Service() {
    private val TAG = "InvitationListener"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var invitationListener: ListenerRegistration? = null

    companion object {
        const val ACTION_INVITATION_RECEIVED = "com.example.coupleswipe.INVITATION_RECEIVED"
        const val EXTRA_INVITATION_ID = "invitation_id"
        const val EXTRA_CATEGORY_NAME = "category_name"
        const val EXTRA_INVITER_EMAIL = "inviter_email"
    }

    override fun onCreate() {
        super.onCreate()
        startListeningForInvitations()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startListeningForInvitations() {
        val currentUserEmail = auth.currentUser?.email ?: return

        invitationListener = db.collection("invitations")
            .whereEqualTo("inviteeEmail", currentUserEmail)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Listen failed", error)
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { document ->
                    if (document.type == DocumentChange.Type.ADDED) {
                        val invitation = document.document
                        sendInvitationBroadcast(invitation)
                    }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        invitationListener?.remove()
    }

    private fun sendInvitationBroadcast(invitation: DocumentSnapshot) {
        val broadcastIntent = Intent(ACTION_INVITATION_RECEIVED).apply {
            putExtra(EXTRA_INVITATION_ID, invitation.id)
            putExtra(EXTRA_CATEGORY_NAME, invitation.getString("categoryName"))
            putExtra(EXTRA_INVITER_EMAIL, invitation.getString("inviterEmail"))
        }
        sendBroadcast(broadcastIntent)
        Log.d(TAG, "Broadcast sent for invitation: ${invitation.id}")
    }
}