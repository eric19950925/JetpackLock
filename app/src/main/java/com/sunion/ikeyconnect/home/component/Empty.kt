package com.sunion.ikeyconnect.home.component

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.sunion.ikeyconnect.ui.theme.colorPrimaryMediumSize18
import com.sunion.ikeyconnect.R
import com.sunion.ikeyconnect.ui.theme.FuhsingSmartLockV2AndroidTheme

@Composable
fun Empty(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .decoderFactory(
                    if (Build.VERSION.SDK_INT >= 28) {
                        ImageDecoderDecoder.Factory()
                    } else {
                        GifDecoder.Factory()
                    }
                )
//                .data(R.raw.logo_jump)
                .data(R.drawable.logo_mark)
                .build(),
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

@Preview(showBackground = true)
@Composable
private fun Preview() {
    FuhsingSmartLockV2AndroidTheme {
        Empty(modifier = Modifier.fillMaxWidth())
    }
}