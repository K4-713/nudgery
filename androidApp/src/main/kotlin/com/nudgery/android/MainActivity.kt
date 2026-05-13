package com.nudgery.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.nudgery.shared.platformName

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NudgeryApp()
        }
    }
}

@Composable
fun NudgeryApp() {
    Text(text = "Running on ${platformName()}")
}
