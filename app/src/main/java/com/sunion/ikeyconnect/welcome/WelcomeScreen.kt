package com.sunion.ikeyconnect.welcome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sunion.ikeyconnect.account.LoginViewModel
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import com.sunion.ikeyconnect.R

@OptIn(InternalCoroutinesApi::class)
@Composable
fun WelcomeScreen (
    viewModel: LoginViewModel,
    toHome: () -> Unit,
    toLogin: () -> Unit,
    logOut: () -> Unit
){
    WelcomeScreen()

    LaunchedEffect(key1 = Unit) {
        while (true) {
            delay(2000)
            viewModel.checkSignedIn(
                onSuccess = toHome,
                onFailure = toLogin
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WelcomeScreen (){
    val animVisibleState = remember { MutableTransitionState(false) }
        .apply { targetState = false }

    animVisibleState.targetState = true

    AnimatedVisibility(
        visibleState = animVisibleState,
        enter = scaleIn(
            animationSpec = tween(durationMillis = 2000), 0f, TransformOrigin.Center
        ),
        modifier = Modifier.background(Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.logo_mark),
                contentDescription = "iKey",
                modifier = Modifier
                    .size(90.dp)
            )
        }
    }

}

@Composable
@Preview(showSystemUi = true)
private fun Preview(){
    WelcomeScreen()
}