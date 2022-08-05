package com.sunion.ikeyconnect.add_lock.installation

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.sunion.ikeyconnect.R

@Composable
fun Video(videoPath: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = {
            val videoView = VideoView(it)

            val mediaController = MediaController(it).apply {
                setAnchorView(videoView)
            }

            videoView.setMediaController(mediaController)

            videoView
        },
        update = {
            it.pause()
            it.setVideoPath(videoPath)
            it.start()
        },
        modifier = modifier
    )
}

@Preview
@Composable
private fun PreviewVideoView() {
    Video(videoPath = "android.resource://${LocalContext.current.packageName}/${R.raw.installation_instructions_01}")
}