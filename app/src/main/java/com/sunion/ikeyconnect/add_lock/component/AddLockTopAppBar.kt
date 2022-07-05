package com.sunion.ikeyconnect.add_lock.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunion.ikeyconnect.ui.component.IKeyTopAppBar
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.R

@Composable
fun AddLockTopAppBar(
    title: String,
    onNaviUpClick: () -> Unit,
    step: String,
    modifier: Modifier = Modifier,
    totalStep: String = "5"
) {
    IKeyTopAppBar(
        onNaviUpClick = onNaviUpClick,
        title = title,
        actions = {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
                        append(step)
                    }
                    withStyle(style = SpanStyle(color = colorResource(id = R.color.grayACBECA))) {
                        append(" / $totalStep")
                    }
                },
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 30.dp)
            )
        },
        modifier = modifier
    )
}

@Preview
@Composable
private fun Preview() {
    FuhsingSmartLockV2AndroidTheme {
        AddLockTopAppBar("Title", {}, step = "2")
    }
}