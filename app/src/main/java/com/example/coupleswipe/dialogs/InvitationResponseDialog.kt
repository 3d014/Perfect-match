package com.example.coupleswipe.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.coupleswipe.R
import com.example.coupleswipe.repository.InvitationRepository

class InvitationResponseDialog : DialogFragment() {

    private lateinit var invitationId: String
    private lateinit var categoryName: String
    private lateinit var inviterEmail: String
    private var onResponseCallback: ((Boolean) -> Unit)? = null

    private val invitationRepository = InvitationRepository()

    companion object {
        private const val ARG_INVITATION_ID = "invitationId"
        private const val ARG_CATEGORY_NAME = "categoryName"
        private const val ARG_INVITER_EMAIL = "inviterEmail"

        fun newInstance(
            invitationId: String,
            categoryName: String,
            inviterEmail: String,
            callback: (Boolean) -> Unit
        ): InvitationResponseDialog {
            val dialog = InvitationResponseDialog()
            dialog.arguments = Bundle().apply {
                putString(ARG_INVITATION_ID, invitationId)
                putString(ARG_CATEGORY_NAME, categoryName)
                putString(ARG_INVITER_EMAIL, inviterEmail)
            }
            dialog.onResponseCallback = callback
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            invitationId = it.getString(ARG_INVITATION_ID) ?: ""
            categoryName = it.getString(ARG_CATEGORY_NAME) ?: ""
            inviterEmail = it.getString(ARG_INVITER_EMAIL) ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.invitation_response_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val messageText = view.findViewById<TextView>(R.id.invitation_message)
        val acceptButton = view.findViewById<Button>(R.id.accept_button)
        val declineButton = view.findViewById<Button>(R.id.decline_button)

        messageText.text = "$inviterEmail has invited you to swipe $categoryName together!"

        acceptButton.setOnClickListener {
            invitationRepository.updateInvitationStatus(
                invitationId = invitationId,
                status = "accepted",
                onSuccess = {
                    onResponseCallback?.invoke(true)
                    dismiss()
                },
                onError = { _ ->
                    dismiss()
                }
            )
        }

        declineButton.setOnClickListener {
            invitationRepository.updateInvitationStatus(
                invitationId = invitationId,
                status = "declined",
                onSuccess = {
                    onResponseCallback?.invoke(false)
                    dismiss()
                },
                onError = { _ ->
                    dismiss()
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
