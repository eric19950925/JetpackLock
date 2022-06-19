package com.sunion.ikeyconnect.home

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.sunion.ikeyconnect.add_lock.AddLockRoute
import com.sunion.ikeyconnect.add_lock.connect_to_wifi.ConnectWifiScreen
import com.sunion.ikeyconnect.add_lock.connect_to_wifi.ConnectWifiViewModel
import com.sunion.ikeyconnect.add_lock.connect_to_wifi.WIfiListScreen
import com.sunion.ikeyconnect.add_lock.connect_to_wifi.WifiListViewModel
import com.sunion.ikeyconnect.add_lock.pairing.PairingScreen
import com.sunion.ikeyconnect.add_lock.pairing.PairingViewModel

fun NavGraphBuilder.homeTestGraph(
    navController: NavController,
//    onAddLockFinish: (String) -> Unit,
    route: String
) {
    navigation(startDestination = HomeTestRoute.Pairing.route, route = route) {
        composable( HomeTestRoute.Pairing.route) { backStackEntry ->
            val viewModel = hiltViewModel<PairingViewModel>()
            val macAddress = "34:6F:24:7B:12:6A"
            viewModel.init(macAddress)
            PairingScreen(viewModel = viewModel, navController = navController)
        }
        composable("${HomeTestRoute.WifiList.route}/{macAddress}") { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            val viewModel = hiltViewModel<WifiListViewModel>()
            viewModel.macAddress ?: viewModel.init(macAddress) //won't do twice now.
            WIfiListScreen(viewModel = viewModel, navController = navController)
        }
        //Todo connect wifi path for init viewModel
        composable("${HomeTestRoute.ConnectWifi.route}/{macAddress}/{ssid}") { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            val ssid = backStackEntry.arguments?.getString("ssid") ?: ""
            val viewModel = hiltViewModel<ConnectWifiViewModel>()
            viewModel.macAddress ?: viewModel.init(macAddress, ssid)
            ConnectWifiScreen(viewModel = viewModel, navController = navController)
        }
    }
}

sealed class HomeTestRoute(val route: String) {
    object Pairing : HomeTestRoute("test_lock_pairing")
    object WifiList : HomeTestRoute("test_lock_wifi_list")
    object ConnectWifi : HomeTestRoute("test_lock_connect_wifi")
}
