package com.example.newmad.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.newmad.HomeActivity

@Composable
fun OnboardingScreen3(navController: NavController) {
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Get started now!", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))
            Button(onClick = { 
                val intent = Intent(context, HomeActivity::class.java)
                context.startActivity(intent)
                (context as? Activity)?.finish()
            }) {
                Text("Start")
            }
        }
    }
}

