package com.sunion.ikeyconnect.settings.event_log

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventRow(
    text: Int,
    TimeStamp: Long,
    Actor: String,
    color: Int,
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
    val newTS = Instant.ofEpochSecond(TimeStamp/1000)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .wrapContentHeight()
            .padding(top = 10.dp)
            .background(Color.White)
    ) {
        ShapeBox(shape = CircleShape, color = colorResource(id = color))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.space_10))
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_16)))
            Text(
                text = newTS,
                fontSize = 18.sp,
                style = TextStyle(color = colorResource(id = R.color.black)),
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_5)))
            Text(
                text = Actor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(color = colorResource(id = R.color.black)),
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_5)))
            Text(
                text = stringResource(id = text),
                fontSize = 16.sp,
                style = TextStyle(color = colorResource(id = R.color.black)),
            )
        }

    }

}

@Composable
fun ShapeBox(shape: Shape, color: Color){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.size(70.dp).padding(vertical = dimensionResource(id = R.dimen.space_16))
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(shape)
                .background(color)
                .align(Alignment.End)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
private fun Preview() {
    FuhsingSmartLockV2AndroidTheme {
        EventRow(text = R.string.log_type_0, TimeStamp = 1657181553000, Actor = "xxx", color = R.color.redE60a17)
    }
}