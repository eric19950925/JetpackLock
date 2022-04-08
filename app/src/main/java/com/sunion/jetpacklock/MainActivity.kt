package com.sunion.jetpacklock

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
//            Text("Hello World!")
            SimpleComposable()
        }
    }
}

@Composable
fun SimpleComposable() {
    if (LocalInspectionMode.current) {
        // Show this text in a preview window
        Text("Hello preview user!")
    } else {
        // Show this text in the app:
        Text("Hello World!")
    }
}

@Composable
@Preview(showSystemUi = true)
fun MainActivityPreview(){
    SimpleComposable()
}