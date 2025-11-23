package com.example.newmad

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Onboarding2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding2)
        val nextBtn = findViewById<Button>(R.id.nextButton)
        nextBtn.setOnClickListener {
            startActivity(Intent(this, Onboarding3Activity::class.java))
            finish()
        }
    }
}
