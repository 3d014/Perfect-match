package com.example.coupleswipe.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.coupleswipe.dialogs.InvitationResponseDialog
import com.example.coupleswipe.services.InvitationListenerService

abstract class BaseActivity : AppCompatActivity() {
    private val invitationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                InvitationListenerService.ACTION_INVITATION_RECEIVED -> {
                    val invitationId =
                        intent.getStringExtra(InvitationListenerService.EXTRA_INVITATION_ID)
                            ?: return
                    val categoryName =
                        intent.getStringExtra(InvitationListenerService.EXTRA_CATEGORY_NAME)
                            ?: return
                    val inviterEmail =
                        intent.getStringExtra(InvitationListenerService.EXTRA_INVITER_EMAIL)
                            ?: return
                    showInvitationDialog(invitationId, categoryName, inviterEmail)
                }

                InvitationListenerService.ACTION_GAME_SESSION_STARTED -> {
                    val gameSessionId =
                        intent.getStringExtra(InvitationListenerService.EXTRA_GAME_SESSION_ID)
                            ?: return
                    val categoryName =
                        intent.getStringExtra(InvitationListenerService.EXTRA_CATEGORY_NAME)
                            ?: return
                    val inviterEmail =
                        intent.getStringExtra(InvitationListenerService.EXTRA_INVITER_EMAIL)
                            ?: return
                    handleGameSessionStarted(gameSessionId, categoryName, inviterEmail)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(InvitationListenerService.ACTION_INVITATION_RECEIVED)
            addAction(InvitationListenerService.ACTION_GAME_SESSION_STARTED)
        }
        registerReceiver(invitationReceiver, filter, RECEIVER_NOT_EXPORTED) // Simplified registerReceiver
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(invitationReceiver)
    }

    private fun showInvitationDialog(
        invitationId: String,
        categoryName: String,
        inviterEmail: String
    ) {
        val dialog = InvitationResponseDialog.newInstance(
            invitationId = invitationId,
            categoryName = categoryName,
            inviterEmail = inviterEmail
        ) { accepted ->
            handleInvitationResponse(invitationId, accepted)
        }

        dialog.show(supportFragmentManager, "invitation_response")
    }

    protected open fun handleInvitationResponse(invitationId: String, accepted: Boolean) {
        // Default implementation
        if (accepted) {
            Toast.makeText(this, "Invitation accepted!", Toast.LENGTH_SHORT).show()
            // Navigate to game activity
        } else {
            Toast.makeText(this, "Invitation declined", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleGameSessionStarted(
        gameSessionId: String,
        categoryName: String,
        inviterEmail: String
    ) {
        Toast.makeText(
            this,
            "Game session started by $inviterEmail for $categoryName!",
            Toast.LENGTH_SHORT
        ).show()
        val intent = Intent(this, SwipeGameActivity::class.java).apply {
            putExtra("GAME_SESSION_ID", gameSessionId)
        }
        startActivity(intent)
        // Optionally finish() if this activity should not remain in the back stack
    }
}