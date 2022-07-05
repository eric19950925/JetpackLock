package com.sunion.ikeyconnect.add_lock.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.ui.component.IKeyDivider
import com.sunion.ikeyconnect.ui.component.ScreenScaffold
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme

@Composable
fun AddLockScreenScaffold(
    onNaviUpClick: () -> Unit,
    title: String,
    step: Int,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    ScreenScaffold(
        appTopBar = {
            AddLockTopAppBar(
                title = title,
                onNaviUpClick = onNaviUpClick,
                step = step.toString(),
            )
        },
        horizontalAlignment = horizontalAlignment,
        modifier = modifier
    ) {
        IKeyDivider()
        content()
    }
}

@Preview
@Composable
private fun PreviewScreenScaffold() {
    FuhsingSmartLockV2AndroidTheme {
        AddLockScreenScaffold(
            onNaviUpClick = {},
            title = stringResource(id = R.string.toolbar_title_installation),
            step = 2
        ) { }
    }
}