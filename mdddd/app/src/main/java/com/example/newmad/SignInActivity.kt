package com.example.newmad

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SignInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signin)

        val toLogin = findViewById<Button>(R.id.signin_to_login)
        toLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}

