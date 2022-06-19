package com.sunion.ikeyconnect.add_lock

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.sunion.ikeyconnect.add_lock.connect_to_wifi.ConnectWifiScreen
import com.sunion.ikeyconnect.add_lock.connect_to_wifi.ConnectWifiViewModel
import com.sunion.ikeyconnect.add_lock.connect_to_wifi.WIfiListScreen
import com.sunion.ikeyconnect.add_lock.connect_to_wifi.WifiListViewModel
import com.sunion.ikeyconnect.add_lock.pairing.PairingScreen
import com.sunion.ikeyconnect.add_lock.pairing.PairingViewModel
import com.sunion.ikeyconnect.add_lock.scan_qrcode.ScanLockPermissionScreen
import com.sunion.ikeyconnect.add_lock.scan_qrcode.ScanLockQRCodeScreen

fun NavGraphBuilder.addLockGraph(
    navController: NavController,
//    onAddLockFinish: (String) -> Unit,
    route: String
) {
    navigation(startDestination = AddLockRoute.ScanPermission.route, route = route) {
        composable(AddLockRoute.ScanPermission.route) {
            ScanLockPermissionScreen(navController = navController)
        }
        composable(AddLockRoute.Scan.route) {
            ScanLockQRCodeScreen(viewModel = hiltViewModel(), navController = navController)
        }
        composable("${AddLockRoute.Installation.route}/{macAddress}") { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
//            InstallationScreen(
//                navController = navController,
//                macAddress = macAddress
//            )
        }
        composable("${AddLockRoute.InstallationInstructions.route}/{macAddress}") { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
//            InstallationInstructionsScreen(
//                viewModel = hiltViewModel(),
//                navController = navController,
//                macAddress = macAddress
//            )
        }
        composable("${AddLockRoute.Pairing.route}/{macAddress}") { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            val viewModel = hiltViewModel<PairingViewModel>()
            /*viewModel.macAddress ?: */viewModel.init(macAddress)
            PairingScreen(viewModel = viewModel, navController = navController)
        }
        composable("${AddLockRoute.WifiList.route}/{macAddress}") { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            val viewModel = hiltViewModel<WifiListViewModel>()
            viewModel.macAddress ?: viewModel.init(macAddress)
            WIfiListScreen(viewModel = viewModel, navController = navController)
        }
        composable("${AddLockRoute.ConnectWifi.route}/{macAddress}/{ssid}") { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            val ssid = backStackEntry.arguments?.getString("ssid") ?: ""
            val viewModel = hiltViewModel<ConnectWifiViewModel>()
            viewModel.macAddress ?: viewModel.init(macAddress, ssid)
            ConnectWifiScreen(viewModel = viewModel, navController = navController)
        }
        composable("${AddLockRoute.AdminCode.route}/{macAddress}") { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
//            val viewModel = hiltViewModel<AdminCodeViewModel>()
//            viewModel.macAddress ?: viewModel.init(macAddress)
//            AdminCodeScreen(viewModel = viewModel, navController = navController)
        }
        composable("${AddLockRoute.RequestLocation.route}/{macAddress}") { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
//            val viewModel = hiltViewModel<RequestLocationViewModel>()
//            viewModel.macAddress ?: viewModel.init(macAddress)
//            RequestLocationScreen(viewModel = viewModel, navController = navController)
        }
        composable("${AddLockRoute.SetLocation.route}/{macAddress}") { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
//            val viewModel = hiltViewModel<SetLockLocationViewModel>()
//            viewModel.macAddress ?: viewModel.init(macAddress)
//            SetLockLocationScreen(viewModel = viewModel, navController = navController)
        }
        composable("${AddLockRoute.LockOverview.route}/{macAddress}") { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
//            val viewModel = hiltViewModel<LockOverviewViewModel>()
//            viewModel.macAddress ?: viewModel.init(macAddress)
//            LockOverviewScreen(
//                viewModel = viewModel,
//                onCompleteClick = {
//                    navController.popBackStack(HomeRoute.Home.route, false)
//                    viewModel.macAddress?.let { onAddLockFinish(it) }
//                }
//            )
        }
    }
}