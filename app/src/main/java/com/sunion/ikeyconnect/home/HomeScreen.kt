package com.sunion.ikeyconnect.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.ui.theme.colorPrimaryMediumSize18
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onAddLockClick: () -> Unit,
    onPersonClick: () -> Unit,
    showGuile: Boolean,
    modifier: Modifier = Modifier,
    onShowGuideClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_20)))
                    Image(
                        painter = painterResource(id = R.drawable.ic_list),
                        contentDescription = "List",
                        modifier = Modifier
                            .size(dimensionResource(id = R.dimen.space_32))
                            .clickable { coroutineScope.launch { scaffoldState.drawerState.open() } }
                    )
                },
                actions = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_add_circle),
                        contentDescription = "Add",
                        modifier = Modifier
                            .size(dimensionResource(id = R.dimen.space_32))
                            .clickable(onClick = onAddLockClick)
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_8)))
                    Image(
                        painter = painterResource(id = R.drawable.ic_person_circle),
                        contentDescription = "Person",
                        modifier = Modifier
                            .size(dimensionResource(id = R.dimen.space_32))
                            .clickable(onClick = onPersonClick)
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_20)))
                },
                title = {},
                backgroundColor = MaterialTheme.colors.primary
            )
        },
        drawerContent = {},
        drawerBackgroundColor = Color.White,
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.logo_mark),
                    contentDescription = "logo",
                    modifier = Modifier.size(dimensionResource(id = R.dimen.space_91))
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_57)))
                Text(
                    text = stringResource(id = R.string.add_lock_no_locks),
                    style = MaterialTheme.typography.colorPrimaryMediumSize18
                )
            }
        }
    }

//    if (showGuile)
//        HomeGuideScreen(modifier = Modifier.clickable(onClick = onShowGuideClick))
}

@Preview
@Composable
private fun Preview() {
    FuhsingSmartLockV2AndroidTheme {
        HomeScreen(onAddLockClick = {}, onPersonClick = {}, showGuile = true) {}
    }
}

@Preview(device = Devices.NEXUS_5)
@Composable
private fun Preview2() {
    FuhsingSmartLockV2AndroidTheme {
        HomeScreen(onAddLockClick = {}, onPersonClick = {}, showGuile = false) {}
    }
}


@Preview(device = Devices.PIXEL_C)
@Composable
private fun Preview3() {
    FuhsingSmartLockV2AndroidTheme {
        HomeScreen(onAddLockClick = {}, onPersonClick = {}, showGuile = true) {}
    }
}