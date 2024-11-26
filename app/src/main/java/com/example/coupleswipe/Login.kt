package com.example.coupleswipe


import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Link UI components
        emailEditText = findViewById(R.id.emailField)
        passwordEditText = findViewById(R.id.passwordField)
        loginButton = findViewById(R.id.login_button)
        val registerTextView: TextView = findViewById(R.id.register_btn)

        // Handle register navigation
        registerTextView.setOnClickListener {
            val intent = Intent(this, Registration::class.java)
            startActivity(intent)
        }

        // Handle login button click
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (!isValidEmail(email)) {
                showToast("Please enter a valid email address.")
            } else if (password.isEmpty()) {
                showToast("Please enter your password.")
            } else {
                loginUser(email, password)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // Check if user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToMain() // If logged in, navigate to MainActivity
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Login successful
                    showToast("Login successful!")
                    navigateToMain()
                } else {
                    // Login failed
                    showToast("Login failed: ${task.exception?.message}")
                }
            }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMain() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish() // Prevent the user from returning to the login screen
    }
}
