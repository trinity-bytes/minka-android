package com.trinitybytes.minka.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trinitybytes.minka.features.home.presentation.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Composable
fun MinkaNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> { HomeScreen() }
    }
}
