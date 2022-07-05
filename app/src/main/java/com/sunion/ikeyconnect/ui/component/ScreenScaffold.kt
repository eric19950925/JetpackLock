package com.sunion.ikeyconnect.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme

@Composable
fun ScreenScaffold(
    appTopBar: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = horizontalAlignment
    ) {
        appTopBar()
        IKeyDivider()
        content()
    }
}

@Composable
fun ScreenScaffoldWithTopAppBar(
    onNaviUpClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    ScreenScaffold(
        appTopBar = {
            IKeyTopAppBar(onNaviUpClick = onNaviUpClick, title = title)
        },
        horizontalAlignment = horizontalAlignment,
        modifier = modifier
    ) {
        content()
    }
}

@Preview
@Composable
private fun PreviewScreenScaffold() {
    FuhsingSmartLockV2AndroidTheme {
        ScreenScaffold(
            appTopBar = {
                IKeyTopAppBar(
                    onNaviUpClick = {},
                    title = stringResource(id = R.string.member_account)
                )
            },
        ) { }
    }
}

@Preview
@Composable
private fun PreviewScreenScaffoldWithTopAppBar() {
    FuhsingSmartLockV2AndroidTheme {
        ScreenScaffoldWithTopAppBar(
            onNaviUpClick = {},
            title = stringResource(id = R.string.member_account)
        ) { }
    }
}