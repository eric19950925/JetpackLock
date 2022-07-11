package com.sunion.ikeyconnect.home.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.*
import com.sunion.ikeyconnect.BuildConfig
import com.sunion.ikeyconnect.domain.blelock.BluetoothConnectState
import com.sunion.ikeyconnect.home.HomeUiState
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.domain.model.*
import com.sunion.ikeyconnect.getLockState
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme

@OptIn(ExperimentalPagerApi::class)
@Composable
fun Locks(
    locks: List<WiFiLock>,
    pagerState: PagerState,
    onAutoUnlockClock: (String) -> Unit,
    onManageClick: (String) -> Unit,
    onUserCodeClick: (String) -> Unit,
    onSettingClick: (String) -> Unit,
    onLockClick: () -> Unit,
    onLockNameChange: (String, String) -> Unit,
    getUpdateTime: (String) -> Int?,
    networkAvailable: Boolean,
    onSaveNameClick: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        HorizontalPager(
            count = locks.count(),
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            val lock = locks[page]
            val isBleDisconnected = !lock.LockState.Connected

            Lock(
                lock = lock,
                getUpdateTime = getUpdateTime,
                networkAvailable = networkAvailable,
                isBleDisconnected = isBleDisconnected,
                onLockNameChange = onLockNameChange,
                onLockClick = onLockClick,
                onAutoUnlockClock = onAutoUnlockClock,
                onManageClick = onManageClick,
                onUserCodeClick = onUserCodeClick,
                onSettingClick = onSettingClick,
                onSaveNameClick = onSaveNameClick,
                macAddress = lock.Attributes.Bluetooth.MACAddress,
                isWifi = true,
                name = lock.Attributes.DeviceName,
                permission = "lock.permission",
                isLoading = isLoading,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = dimensionResource(id = R.dimen.space_37))
        ) {
            HorizontalPagerIndicator(
                pagerState = pagerState,
                indicatorWidth = dimensionResource(id = R.dimen.space_6),
                modifier = Modifier.padding(16.dp),
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_35)))
            Text(
                text = stringResource(id = R.string.launcher_version, BuildConfig.VERSION_NAME),
                style = TextStyle(
                    color = colorResource(id = R.color.primaryVariant),
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun Lock(
    lock: WiFiLock,
    getUpdateTime: (String) -> Int?,
    networkAvailable: Boolean,
    isBleDisconnected: Boolean,
    onLockNameChange: (String, String) -> Unit,
    onLockClick: () -> Unit,
    onAutoUnlockClock: (String) -> Unit,
    onManageClick: (String) -> Unit,
    onUserCodeClick: (String) -> Unit,
    onSettingClick: (String) -> Unit,
    onSaveNameClick: (String) -> Unit,
    macAddress: String,
    isWifi: Boolean,
    name: String,
    permission: String,
    isLoading: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_14)))
        if (isWifi)
            ConnectStatus(
                getUpdateTime = getUpdateTime,
                networkAvailable = /*lock.useWifi &&*/ networkAvailable,
                isBleDisconnected = /*!lock.useWifi &&*/ isBleDisconnected,
                macAddress = macAddress
            )

        LockName(
            name = name,
            onLockNameChange = { onLockNameChange(macAddress, it) },
            onSaveNameClick = { onSaveNameClick(macAddress) },
            isEnabled = lock.LockState.Connected
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.space_56)))
        LockStatusImage(lock = lock, onLockClick = onLockClick, isLoading = isLoading)

        Spacer(modifier = Modifier.weight(1f))
        ActionRow(
            isDisconnected = isBleDisconnected,
            onAutoUnlockClock = onAutoUnlockClock,
            onManageClick = onManageClick,
            onUserCodeClick = onUserCodeClick,
            onSettingClick = onSettingClick,
            thingName = lock.ThingName,
            permission = permission,
            macAddress = macAddress
        )
    }
}

@Composable
private fun ConnectStatus(
    getUpdateTime: (String) -> Int?,
    networkAvailable: Boolean,
    isBleDisconnected: Boolean,
    modifier: Modifier = Modifier,
    macAddress: String,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(id = R.dimen.space_29),
                end = dimensionResource(id = R.dimen.space_24)
            )
    ) {
        getUpdateTime(macAddress)?.let {
            UpdateTime(it)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(id = R.drawable.ic_wifi),
            contentDescription = null,
            tint = if (networkAvailable) colorResource(id = R.color.primary)
            else colorResource(id = R.color.primaryVariant),
            modifier = Modifier.size(dimensionResource(id = R.dimen.space_20))
        )
        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_6)))
        Image(
            painter = painterResource(
                id = if (isBleDisconnected || networkAvailable) R.drawable.ic_bluetooth_disconnected
                else R.drawable.ic_bluetooth_main
            ),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(id = R.dimen.space_20))
        )
    }
}

@Composable
private fun UpdateTime(hours: Int) {
    Row(
        modifier = Modifier
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = colorResource(id = R.color.primaryVariant)
                ),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(
                horizontal = dimensionResource(id = R.dimen.space_11),
                vertical = dimensionResource(id = R.dimen.space_4)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_icon_ionic_ios_time),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(id = R.dimen.space_12))
        )
        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_6)))
        Text(
            text = stringResource(id = R.string.updated_hours_ago, hours.toString()),
            style = TextStyle(
                color = colorResource(id = R.color.primaryVariant),
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
private fun LockName(
    name: String,
    onLockNameChange: (String) -> Unit,
    onSaveNameClick: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var editable by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.padding(top = dimensionResource(id = R.dimen.space_72)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val style = TextStyle(
            color = if (isEnabled) MaterialTheme.colors.primary
            else MaterialTheme.colors.primaryVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        if (editable)
            BasicTextField(
                value = name,
                onValueChange = {
                    if (it.length <= 20)
                        onLockNameChange(it)
                },
                textStyle = style.copy(textAlign = TextAlign.Center),
                maxLines = 1
            )
        else
            Text(text = name, style = style)
        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.space_20)))
        val color = MaterialTheme.colors.primaryVariant
        Image(
            painter = painterResource(
                id = if (editable) R.drawable.ic_check
                else
                    if (isEnabled) R.drawable.ic_edit
                    else R.drawable.ic_edit_disabled
            ),
            contentDescription = null,
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.space_19))
                .clickable(onClick = {
                    if (!isEnabled)
                        return@clickable
                    editable = !editable
                    if (editable)
                        onSaveNameClick()
                })
                .run {
                    if (editable)
                        then(Modifier.drawBehind {
                            drawRoundRect(
                                color = color,
                                style = Stroke(1.8.dp.toPx()),
                                cornerRadius = CornerRadius(10f, 10f)
                            )
                        })
                    else this
                }
        )
    }
}

@Composable
private fun LockStatusImage(
    lock: WiFiLock,
    onLockClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {

    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0F,
        targetValue = 360F,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        )
    )
    Box(modifier = Modifier.wrapContentHeight().wrapContentWidth(), Alignment.Center) {
        Image(
            painter = painterResource(
                id =
                when {
//                lock.isProcessing -> R.drawable.vector_lock_state_loading
                    !lock.LockState.Connected -> R.drawable.vector_lock_state_not_connected
                    lock.LockState.Direction == "unknown" -> R.drawable.vector_lock_state_bolt_required
                    else ->
                        when (lock.LockState.Deadbolt.getLockState()) {
                            LockStatus.LOCKED -> R.drawable.vector_lock_state_locked
                            LockStatus.UNLOCKED -> R.drawable.vector_lock_state_unlocked
//                    else -> R.drawable.vector_lock_state_loading
                            else -> R.drawable.vector_lock_state_not_connected
                        }
                }
            ),
            contentDescription = null,
            modifier = modifier
                .size(dimensionResource(id = R.dimen.space_240))
//            .run {
//                if (lock.isProcessing)
//                    rotate(rotate)
//                else this
//            }
                .clickable(onClick = onLockClick)
        )
        Image(
            painter = painterResource(id = R.drawable.vector_lock_state_loading),
            contentDescription = null,
            modifier = modifier
                .size(dimensionResource(id = R.dimen.space_240))
                .graphicsLayer {
                    rotationZ = angle
                }.alpha(if(!isLoading)0f else 1f)
        )
    }

}

@Composable
private fun ActionRow(
    isDisconnected: Boolean,
    onAutoUnlockClock: (String) -> Unit,
    onManageClick: (String) -> Unit,
    onUserCodeClick: (String) -> Unit,
    onSettingClick: (String) -> Unit,
    thingName: String,
    permission: String,
    macAddress: String,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!isDisconnected)
            ActionButton(
                onClick = { if (!isDisconnected) onAutoUnlockClock(macAddress) },
                textResId = R.string.toolbar_title_auto_un_lock,
                iconResId = R.drawable.ic_lock_open_white,
                modifier = Modifier.alpha(if (isDisconnected) 0f else 1f)
            )
        if (permission == DeviceToken.PERMISSION_ALL && !isDisconnected)
            ActionButton(
                onClick = { if (!isDisconnected) onManageClick(macAddress) },
                textResId = R.string.toolbar_users,
                iconResId = R.drawable.ic_managers,
                modifier = Modifier.alpha(if (isDisconnected) 0f else 1f)
            )
        if (permission == DeviceToken.PERMISSION_ALL && !isDisconnected)
            ActionButton(
                onClick = { if (!isDisconnected) onUserCodeClick(macAddress) },
                textResId = R.string.toolbar_user_code,
                iconResId = R.drawable.ic_user_code,
                modifier = Modifier.alpha(if (isDisconnected) 0f else 1f)
            )
        ActionButton(
            onClick = { onSettingClick(thingName) },
            textResId = R.string.toolbar_setting,
            iconResId = R.drawable.ic_setting,
        )
    }
}


@Composable
private fun ActionButton(
    onClick: () -> Unit,
    @StringRes textResId: Int,
    @DrawableRes iconResId: Int,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colors.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(dimensionResource(id = R.dimen.space_24))
        )
        Text(
            text = stringResource(id = textResId),
            style = TextStyle(color = color, fontSize = 13.sp)
        )
    }
}

@OptIn(ExperimentalPagerApi::class)
@Preview
@Preview(device = Devices.NEXUS_5)
@Composable
private fun Preview(@PreviewParameter(LocksPreviewParameterProvider::class) uiState: HomeUiState) {
    FuhsingSmartLockV2AndroidTheme {
        Locks(
            locks = uiState.locks,
            pagerState = rememberPagerState(),
            onAutoUnlockClock = {},
            onManageClick = {},
            onUserCodeClick = {},
            onSettingClick = {},
            onLockClick = {},
            onLockNameChange = { _, _ -> },
            getUpdateTime = { 2 },
            onSaveNameClick = {},
            networkAvailable = true,
            isLoading = false
        )
    }
}

class LocksPreviewParameterProvider : PreviewParameterProvider<HomeUiState> {
    override val values = sequenceOf(
        HomeUiState(
            locks = mutableListOf(
//                ConnectedDevice(
//                    macAddress = "WiFiLock",
//                    connectionState = BluetoothConnectState.CONNECTED,
//                    permission = DeviceToken.PERMISSION_ALL,
//                    name = "Lock1 PERMISSION_ALL",
//                    createdAt = 0,
//                    lockOrientation = LockOrientation.Left,
//                    isLocked = LockStatus.UNLOCKED,
//                    battery = null,
//                    batteryStatus = null,
//                    isVacationModeOn = null,
//                    isSoundOn = null,
//                    displayIndex = 0,
//                    isWifi = true,
//                    useWifi = true
//                )
                WiFiLock(
                  ThingName = "",
                  Attributes = DeviceAttributes(
                      Bluetooth = DeviceBleInfo("","","",""),
                      DeviceName = "new_lock",
                      Syncing = true,
                      VacationMode = true,
                  ),
                  LockState = Reported(
                      Battery = 100,
                      Rssi = 10,
                      Status = 88,
                      RegistryVersion = 87,
                      AccessCodeTime = 121212,
                      Deadbolt = "unlock",
                      Direction = "",
                      Searchable = 123456,
                      Connected = true
                  )
                ),
            )
        ),
//        HomeUiState(
//            locks = listOf(
//                ConnectedDevice(
//                    macAddress = "WiFiLock",
//                    connectionState = BluetoothConnectState.CONNECTED,
//                    permission = DeviceToken.PERMISSION_ALL,
//                    name = "Lock1 PERMISSION_ALL",
//                    createdAt = 0,
//                    lockOrientation = LockOrientation.Left,
//                    isLocked = LockStatus.UNLOCKED,
//                    battery = null,
//                    batteryStatus = null,
//                    isVacationModeOn = null,
//                    isSoundOn = null,
//                    displayIndex = 0,
//                    isWifi = true,
//                    useWifi = false
//                )
//            )
//        ),
//        HomeUiState(
//            locks = listOf(
//                ConnectedDevice(
//                    macAddress = "BleLock NO PERMISSION",
//                    connectionState = BluetoothConnectState.CONNECTED,
//                    permission = "",
//                    name = "Lock1",
//                    createdAt = 0,
//                    lockOrientation = LockOrientation.Left,
//                    isLocked = LockStatus.LOCKED,
//                    battery = null,
//                    batteryStatus = null,
//                    isVacationModeOn = null,
//                    isSoundOn = null,
//                    displayIndex = 0
//                )
//            )
//        ),
//        HomeUiState(
//            locks = listOf(
//                DisconnectedDevice(
//                    macAddress = "Lock1",
//                    connectionState = BluetoothConnectState.DISCONNECTED,
//                    permission = DeviceToken.PERMISSION_NONE,
//                    name = "Lock1 DISCONNECTED",
//                    createdAt = 0,
//                    displayIndex = 0
//                )
//            )
//        ),
//        HomeUiState(
//            locks = listOf(
//                BoltOrientationFailDevice(
//                    macAddress = "Lock1",
//                    connectionState = BluetoothConnectState.DISCONNECTED,
//                    permission = DeviceToken.PERMISSION_NONE,
//                    name = "Lock1 BoltOrientationFail",
//                    createdAt = 0,
//                    displayIndex = 0
//                )
//            )
//        )
    )
}
