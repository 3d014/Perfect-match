package com.example.coupleswipe.activity

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.coupleswipe.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class RegistrationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Link UI components
        emailEditText = findViewById(R.id.email_id)
        passwordEditText = findViewById(R.id.password_id)
        registerButton = findViewById(R.id.registerButton)

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showToast("Please fill out all fields")
            } else if (password.length < 6) {
                showToast("Password must be at least 6 characters")
            } else if (!isValidEmail(email)) {
                showToast("Please enter a valid email")
            } else {
                checkIfUserExistsAndRegister(email, password)
            }
        }
    }

    private fun checkIfUserExistsAndRegister(email: String, password: String) {
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods ?: emptyList<String>()
                    if (signInMethods.isNotEmpty()) {
                        // User already registered
                        showToast("This email is already registered. Please log in.")
                        redirectToLogin()
                    } else {
                        // User not registered, proceed with registration
                        registerUser(email, password)
                    }
                } else {
                    showToast("Error checking email: ${task.exception?.message}")
                }
            }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast("Registration successful!")
                    redirectToLogin()
                } else {
                    showToast("Registration failed: ${task.exception?.message}")
                }
            }
    }

    private fun redirectToLogin() {
        // Redirect user to login activity
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
