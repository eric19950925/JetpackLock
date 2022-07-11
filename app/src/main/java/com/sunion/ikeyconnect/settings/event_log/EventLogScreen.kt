package com.sunion.ikeyconnect.settings.event_log

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.domain.model.sunion_service.EventGetResponse
import com.sunion.ikeyconnect.isTodayOrYesterdayOrElse
import com.sunion.ikeyconnect.ui.component.BackTopAppBar
import com.sunion.ikeyconnect.ui.component.CircularIndeterminateProgressBar
import com.sunion.ikeyconnect.ui.component.IKeyDivider
import com.sunion.ikeyconnect.ui.component.LoadingScreen
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventLogScreen(
    viewModel: EventLogViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val uiState = viewModel.uiState.collectAsState().value
    EventLogScreen(
        uiState = uiState,
        onNaviUpClick = { navController.popBackStack() },
        deviceName = viewModel.deviceName?:"",
        modifier = modifier,
    )
    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect {
            when (it) {
//                EventLogUiEvent.DeleteLockSuccess ->
//                    navController.popBackStack(HomeRoute.Home.route, false)
                EventLogUiEvent.EventLogFail -> {
//                    Toast.makeText(context, "Cannot get event log", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (viewModel.isLoading.value)
        LoadingScreen()
}

@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventLogScreen(
    uiState: EventLogUiState,
    onNaviUpClick: () -> Unit,
    deviceName: String,
    modifier: Modifier = Modifier,
){
    Scaffold(
        topBar = {
            BackTopAppBar(
                onNaviUpClick = onNaviUpClick,
                title = stringResource(id = R.string.setting_event_log)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 5.dp, start = 20.dp)
                    .background(Color.White)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_lock_main),
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .padding(10.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(colorResource(id = R.color.primary))
                )
                Text(
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp),
                    text = deviceName,
                    fontSize = 16.sp,
                    style = TextStyle(color = colorResource(id = R.color.primary)),
                )
            }
            IKeyDivider()

            val groupedEvents = uiState.events.groupBy { it.Millisecond.isTodayOrYesterdayOrElse() }

            LazyColumn(modifier = Modifier.wrapContentHeight()) {
                groupedEvents.forEach { (groupHeader, events) ->
                    stickyHeader {
                        Text(
                            text = groupHeader,
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colorResource(id = R.color.white))
                                .padding(8.dp),
                            color = colorResource(id = R.color.primary)
                        )
                    }
                    items(
                        items = events,
                        itemContent = { event ->
                            LogListItem(event)
                        },
                    )
                }
            }
            CircularIndeterminateProgressBar(uiState.isLoading)
            Column(
                modifier = Modifier
                    .alpha(if(!uiState.isLoading && uiState.events.isEmpty())1f else 0f)
            ) {
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_200)))
                Text(
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    text = "Empty",
                    fontSize = 16.sp,
                    style = TextStyle(color = colorResource(id = R.color.popup_text)),
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LogListItem(event: EventGetResponse.LockGeneral.Events) {
    when (event.Type) {
        "0" -> {
            EventRow(text = R.string.log_type_0, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.redE60a17)
        }
        "1" -> {
            EventRow(text = R.string.log_type_1, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.log_error)
        }
        "2" -> {
            EventRow(text = R.string.log_type_2, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.redE60a17)
        }
        "3" -> {
            EventRow(text = R.string.log_type_3, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.log_error)
        }
        "4" -> {
            EventRow(text = R.string.log_type_4, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.redE60a17)
        }
        "5" -> {
            EventRow(text = R.string.log_type_5, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.log_error)
        }
        "6" -> {
            EventRow(text = R.string.log_type_6, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.redE60a17)
        }
        "7" -> {
            EventRow(text = R.string.log_type_7, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.log_error)
        }
        "8" -> {
            EventRow(text = R.string.log_type_8, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.enable_green)
        }
        "9" -> {
            EventRow(text = R.string.log_type_9, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.log_error)
        }
        "10" -> {
            EventRow(text = R.string.log_type_10, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.enable_green)
        }
        "11" -> {
            EventRow(text = R.string.log_type_11, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.log_error)
        }
        "12" -> {
            EventRow(text = R.string.log_type_12, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.enable_green)
        }
        "13" -> {
            EventRow(text = R.string.log_type_13, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.log_error)
        }
        "80" -> {
            EventRow(text = R.string.log_type_80, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.primary)
        }
        "81" -> {
            EventRow(text = R.string.log_type_81, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.primary)
        }
        "82" -> {
            EventRow(text = R.string.log_type_82, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.primary)
        }
        "128" -> {
            EventRow(text = R.string.log_type_128, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.log_error)
        }
        "129" -> {
            EventRow(text = R.string.log_type_129, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.log_error)
        }
        "130" -> {
            EventRow(text = R.string.log_type_130, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.log_error)
        }
        "131" -> {
            EventRow(text = R.string.log_type_131, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.log_error)
        }
        else -> {
            EventRow(text = R.string.log_unknown_type, TimeStamp = event.Millisecond, Actor = event.extraDetail?.Actor.toString(), R.color.log_error)
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Preview(device = Devices.PIXEL)
@Composable
private fun PreviewEventLogScreen() {
    FuhsingSmartLockV2AndroidTheme {
        EventLogScreen(
            uiState = EventLogUiState(),
            onNaviUpClick = {},
            deviceName = "New_Lock"
        )
    }
}