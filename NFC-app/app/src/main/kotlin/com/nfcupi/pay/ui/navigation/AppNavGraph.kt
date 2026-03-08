package com.nfcupi.pay.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nfcupi.pay.ui.screens.receive.ReceiveScreen
import com.nfcupi.pay.ui.screens.settings.SettingsScreen

@Composable
fun AppNavGraph() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "receive") {
        composable("receive")  { ReceiveScreen(onNavigateToSettings = { nav.navigate("settings") }) }
        composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
    }
}
