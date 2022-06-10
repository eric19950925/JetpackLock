package com.sunion.ikeyconnect.add_lock.pairing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sunion.ikeyconnect.R

@Composable
fun PairingScreen(viewModel: PairingViewModel,
                    navController: NavController,
                    modifier: Modifier = Modifier
) {
    PairingScreen(
        on_C0C1_Click = { viewModel.startPairing() },
        on_C7_Click = {},
        on_PVC_Click = {},
        on_F0_Click = {},
        on_CE_Click = {},
        modifier = modifier
    )
}
@Composable
fun PairingScreen(
    on_C0C1_Click: () -> Unit,
    on_C7_Click: () -> Unit,
    on_PVC_Click: () -> Unit,
    on_F0_Click: () -> Unit,
    on_CE_Click: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Spacer(modifier = Modifier.height(55.dp))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                onClick = on_C0C1_Click
            ){ Text("C0 C1") }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                onClick = on_C7_Click
            ){ Text("C7") }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                onClick = on_PVC_Click
            ){ Text("PVC") }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                onClick = on_F0_Click
            ){ Text("F0") }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                onClick = on_CE_Click
            ){ Text("CE") }
        }
    }
}
@Preview(showSystemUi = true)
@Composable
private fun Preview() {
    PairingScreen(
        on_C0C1_Click = {},
        on_C7_Click = {},
        on_PVC_Click = {},
        on_F0_Click = {},
        on_CE_Click = {})
}
