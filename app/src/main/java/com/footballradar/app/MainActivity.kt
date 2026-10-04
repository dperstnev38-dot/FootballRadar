package com.footballradar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.footballradar.app.ui.MainScreen
import com.footballradar.app.ui.theme.FootballRadarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FootballRadarTheme {
                MainScreen()
            }
        }
    }
}
