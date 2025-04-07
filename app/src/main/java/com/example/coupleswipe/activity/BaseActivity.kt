package com.example.coupleswipe.activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.coupleswipe.dialogs.InvitationResponseDialog
import com.example.coupleswipe.services.InvitationListenerService

abstract class BaseActivity : AppCompatActivity() {
    private val invitationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val invitationId = intent.getStringExtra(InvitationListenerService.EXTRA_INVITATION_ID) ?: return
            val categoryName = intent.getStringExtra(InvitationListenerService.EXTRA_CATEGORY_NAME) ?: return
            val inviterEmail = intent.getStringExtra(InvitationListenerService.EXTRA_INVITER_EMAIL) ?: return

            showInvitationDialog(invitationId, categoryName, inviterEmail)
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(invitationReceiver,
            IntentFilter(InvitationListenerService.ACTION_INVITATION_RECEIVED))
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(invitationReceiver)
    }

    private fun showInvitationDialog(invitationId: String, categoryName: String, inviterEmail: String) {
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
}