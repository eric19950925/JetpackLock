package com.sunion.jetpacklock.welcome

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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sunion.jetpacklock.R
import com.sunion.jetpacklock.account.LoginViewModel
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay

@OptIn(InternalCoroutinesApi::class)
@Composable
fun WelcomeScreen (
    viewModel: LoginViewModel,
    toHome: () -> Unit,
    toLogin: () -> Unit,
){
    WelcomeScreen()

    LaunchedEffect(key1 = Unit) {
        while (true) {
            delay(2000)
            if(viewModel.checkSignedIn()) toHome() else toLogin()
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