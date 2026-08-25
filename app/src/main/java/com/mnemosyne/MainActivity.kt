package com.mnemosyne

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mnemosyne.ui.theme.MnemosyneTheme
import com.mnemosyne.nav.MnemosyneNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MnemosyneTheme {
                MnemosyneNavHost()
            }
        }
    }
}
