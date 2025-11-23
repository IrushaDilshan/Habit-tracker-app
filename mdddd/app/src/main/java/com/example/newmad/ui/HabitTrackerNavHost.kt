package com.example.newmad.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun HabitTrackerNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("onboarding1") { OnboardingScreen1(navController) }
        composable("onboarding2") { OnboardingScreen2(navController) }
        composable("onboarding3") { OnboardingScreen3(navController) }
    }
}
