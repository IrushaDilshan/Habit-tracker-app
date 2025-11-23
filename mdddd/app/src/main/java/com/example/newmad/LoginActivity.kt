package com.example.newmad

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        val toSignIn = findViewById<Button>(R.id.login_to_signin)
        toSignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        val loginButton = findViewById<Button>(R.id.login_button)
        loginButton.setOnClickListener {
            // TODO: Add authentication logic here
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }
}
