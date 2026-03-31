package com.nfcupi.pay.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nfcupi.pay.ui.screens.receive.ReceiveScreen
import com.nfcupi.pay.ui.screens.settings.SettingsScreen
import com.nfcupi.pay.ui.theme.TapmeBackground

@Composable
fun AppNavGraph() {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = "receive",
        modifier = Modifier.fillMaxSize().background(TapmeBackground),
        enterTransition    = { fadeIn(animationSpec = tween(180)) },
        exitTransition     = { fadeOut(animationSpec = tween(180)) },
        popEnterTransition = { fadeIn(animationSpec = tween(180)) },
        popExitTransition  = { fadeOut(animationSpec = tween(180)) }
    ) {
        composable("receive")  { ReceiveScreen(onNavigateToSettings = { nav.navigate("settings") }) }
        composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
    }
}
