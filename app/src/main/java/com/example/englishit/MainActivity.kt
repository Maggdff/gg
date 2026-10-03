package com.example.englishit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Content.load(applicationContext)
        Tts.init(applicationContext)
        val store = Store(applicationContext)
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(store)
                }
            }
        }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) {
        darkColorScheme(primary = Color(0xFF78C800), onPrimary = Color.Black)
    } else {
        lightColorScheme(primary = Color(0xFF58A700), onPrimary = Color.White)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
