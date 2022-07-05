package com.sunion.ikeyconnect.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import com.sunion.ikeyconnect.R

@Composable
fun HomeGuideScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x80000000))
    ) {
        AddLockPromptPopup(
            Modifier
                .size(
                    width = dimensionResource(id = R.dimen.space_250),
                    height = dimensionResource(id = R.dimen.space_90)
                )
                .align(Alignment.TopEnd)
                .offset(-(dimensionResource(id = R.dimen.space_95).value).dp, 40.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = dimensionResource(id = R.dimen.space_6))
                .padding(bottom = dimensionResource(id = R.dimen.space_20))
                .fillMaxWidth()
                .height(dimensionResource(id = R.dimen.space_306))
                .background(Color.White, RoundedCornerShape(8.7.dp))
                .padding(dimensionResource(id = R.dimen.space_24))
        ) {
            CloseButton(modifier = Modifier.align(Alignment.End))
            Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                ImageText(
                    painterResource(id = R.drawable.click_menu),
                    stringResource(id = R.string.guiding_click_menu)
                )
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_18)))
                ImageText(
                    painterResource(id = R.drawable.find_help),
                    stringResource(id = R.string.guiding_find_help)
                )
            }
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_50)))
            Text(
                text = stringResource(id = R.string.feature_highlight_support_guiding),
                style = TextStyle(
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun AddLockPromptPopup(modifier: Modifier) {
    Box(
        modifier = modifier
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_popup_top_background),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_hint_popup_key),
                contentDescription = null,
                modifier = Modifier
                    .weight(0.3f)
                    .width(dimensionResource(id = R.dimen.space_22))
            )
            Text(
                text = stringResource(id = R.string.feature_highlight_add_lock),
                style = TextStyle(
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                modifier = Modifier.weight(0.55f)
            )
            CloseButton(
                Modifier
                    .weight(0.15f)
                    .align(Alignment.Top)
                    .padding(top = 20.dp)
            )
        }
    }
}

@Composable
private fun CloseButton(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.ic_hint_cancel),
        contentDescription = null,
        modifier = modifier.width(dimensionResource(id = R.dimen.space_10))
    )
}

@Composable
private fun ImageText(painter: Painter, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.size(132.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = text,
            style = TextStyle(
                color = colorResource(id = R.color.primaryVariant),
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        )
    }
}

@Preview
@Composable
private fun Preview() {
    FuhsingSmartLockV2AndroidTheme {
        HomeGuideScreen()
    }
}