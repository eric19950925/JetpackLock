package com.sunion.jetpacklock.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sunion.jetpacklock.account.createAccount.CreateAccountEmailScreen
import com.sunion.jetpacklock.account.createAccount.CreateAccountPasswordScreen
import com.sunion.jetpacklock.account.createAccount.CreateAccountValidateScreen
import com.sunion.jetpacklock.account.forgotPassword.ForgotPasswordScreen
import com.sunion.jetpacklock.R
import com.sunion.jetpacklock.account.createAccount.CreateAccountViewModel
import com.sunion.jetpacklock.account.forgotPassword.ForgotPasswordConfirmScreen
import com.sunion.jetpacklock.account.forgotPassword.ForgotPasswordConfirmViewModel

@Composable
fun  AccountNavigation(onLoginSuccess: () -> Unit) {
    val navController = rememberNavController()
    val createAccountViewModel = hiltViewModel<CreateAccountViewModel>()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(hiltViewModel(), navController, onLoginSuccess)
        }
        composable(AccountNavi.ForgotPassword.route) {
            ForgotPasswordScreen(hiltViewModel(), navController)
        }
        composable(AccountNavi.CreateAccountEmail.route) {
            CreateAccountEmailScreen(createAccountViewModel, navController)
        }

        composable(AccountNavi.CreateAccountPassword.route) {
            CreateAccountPasswordScreen(createAccountViewModel, navController)
        }
        composable(AccountNavi.CreateAccountValidate.route) {
            CreateAccountValidateScreen(createAccountViewModel, navController)
        }
        composable(AccountNavi.CreateAccountSuccess.route) {
            SuccessScreen(
                onClick = { navController.popBackStack(AccountNavi.Login.route, false) },
                title = stringResource(id = R.string.account_welcome),
                text = stringResource(id = R.string.account_your_account_has_been_successfully_created),
                buttonText = stringResource(id = R.string.account_lets_start)
            )
        }
        composable("${AccountNavi.ForgotPasswordConfirm.route}/{email}") { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email")
            if (email == null) {
                navController.popBackStack()
                return@composable
            }
            val viewModel = hiltViewModel<ForgotPasswordConfirmViewModel>()
            viewModel.setEmail(email)
            ForgotPasswordConfirmScreen(viewModel, navController)
        }

        composable(AccountNavi.ForgotPasswordSuccess.route) {
            SuccessScreen(
                onClick = { navController.popBackStack(AccountNavi.Login.route, false) },
                title = stringResource(id = R.string.account_password_reset_successful),
                text = stringResource(id = R.string.account_successful_updated_your_password),
                buttonText = stringResource(id = R.string.account_login)
            )
        }

    }
}
sealed class AccountNavi(val route: String) {
    object Login : AccountNavi("login")
    object CreateAccountEmail : AccountNavi("createAccountEmail")
    object CreateAccountPassword : AccountNavi("createAccountPassword")
    object CreateAccountValidate : AccountNavi("createAccountValidate")
    object CreateAccountSuccess : AccountNavi("createAccountSuccess")
    object ForgotPassword : AccountNavi("forgotPassword")
    object ForgotPasswordConfirm : AccountNavi("forgotPasswordConfirm")
    object ForgotPasswordSuccess : AccountNavi("forgotPasswordSuccess")
}