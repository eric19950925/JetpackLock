package com.sunion.jetpacklock

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val mContext = LocalContext.current
    if (LocalInspectionMode.current) {
        // Show this text in a preview window
        Text("Hello preview user!")
    } else {
        // Show this text in the app:
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Button(
                // ...
//            onClick = myClickFunction
                onClick = {
                    Toast.makeText(mContext, "This is a Sample Toast", Toast.LENGTH_SHORT).show()
                }
            ){
                Text("Button")
            }
        }
    }
}

@Composable
@Preview(showSystemUi = true)
fun MainActivityPreview(){
    SimpleComposable()
}