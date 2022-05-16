package com.sunion.jetpacklock.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sunion.jetpacklock.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.jetpacklock.ui.theme.colorPrimaryBoldSize18
import com.sunion.jetpacklock.R

@Composable
fun IKeyTopAppBar(
    onNaviUpClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    backgroundColor: Color = Color.White,
    naviUpTint: Color? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        navigationIcon = {
            Box(
                Modifier
                    .size(48.dp)
                    .clickable(onClick = onNaviUpClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "back",
                    modifier = Modifier.size(24.dp),
                    tint = naviUpTint
                        ?: LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                )
            }
        },
        title = {
            if (title != null)
                Text(text = title, style = MaterialTheme.typography.colorPrimaryBoldSize18)
        },
        actions = actions,
        backgroundColor = backgroundColor,
        elevation = 0.dp,
        modifier = modifier
    )
}

@Preview
@Composable
private fun Preview() {
    FuhsingSmartLockV2AndroidTheme {
        IKeyTopAppBar({})
    }
}

@Preview
@Composable
private fun PreviewWithTitle() {
    FuhsingSmartLockV2AndroidTheme {
        IKeyTopAppBar({}, title = stringResource(id = R.string.member_account))
    }
}