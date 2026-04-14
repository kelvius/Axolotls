package com.example.axolotls

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.axolotls.ui.screens.HomeScreen
import com.example.axolotls.ui.screens.LoginScreen
import com.example.axolotls.ui.theme.AxolotlsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AxolotlsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }

    if (isLoggedIn) {
        HomeScreen(
            onLogout = { isLoggedIn = false }
        )
    } else {
        LoginScreen(
            onLoginSuccess = { isLoggedIn = true }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    AxolotlsTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation()
        }
    }
}
