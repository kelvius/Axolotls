package com.example.axolotls

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axolotls.ui.AuthViewModel
import com.example.axolotls.ui.screens.HomeScreen
import com.example.axolotls.ui.screens.LoginScreen
import com.example.axolotls.ui.screens.RegisterScreen
import com.example.axolotls.ui.screens.SettingsScreen
import com.example.axolotls.ui.screens.SplashScreen
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

enum class Screen {
    Splash, Login, Register, Home, Settings
}

@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel()) {
    val authState by authViewModel.uiState.collectAsState()
    var currentScreen by rememberSaveable { mutableStateOf(Screen.Splash.name) }
    var isGuest by rememberSaveable { mutableStateOf(false) }

    // If user becomes authenticated, go to Home; if logged out, go to Login
    val isLoggedIn = authState.currentUser != null
    if (isLoggedIn && (currentScreen == Screen.Login.name || currentScreen == Screen.Register.name)) {
        currentScreen = Screen.Home.name
    } else if (!isLoggedIn && !isGuest && (currentScreen == Screen.Home.name || currentScreen == Screen.Settings.name)) {
        currentScreen = Screen.Login.name
    }

    when (currentScreen) {
        Screen.Splash.name -> {
            SplashScreen(
                onFinished = { currentScreen = Screen.Login.name }
            )
        }
        Screen.Login.name -> {
            LoginScreen(
                isLoading = authState.isLoading,
                errorMessage = authState.errorMessage,
                onLogin = { email, password -> authViewModel.login(email, password) },
                onNavigateToRegister = {
                    authViewModel.clearError()
                    currentScreen = Screen.Register.name
                },
                onClearError = { authViewModel.clearError() },
                onGuestLogin = {
                    isGuest = true
                    currentScreen = Screen.Home.name
                }
            )
        }
        Screen.Register.name -> {
            RegisterScreen(
                isLoading = authState.isLoading,
                errorMessage = authState.errorMessage,
                onRegister = { email, password -> authViewModel.register(email, password) },
                onNavigateToLogin = {
                    authViewModel.clearError()
                    currentScreen = Screen.Login.name
                },
                onClearError = { authViewModel.clearError() }
            )
        }
        Screen.Home.name -> {
            HomeScreen(
                onLogout = {
                    isGuest = false
                    authViewModel.logout()
                    currentScreen = Screen.Login.name
                },
                onNavigateToSettings = { currentScreen = Screen.Settings.name }
            )
        }
        Screen.Settings.name -> {
            SettingsScreen(
                userEmail = if (isGuest) "Guest" else authState.currentUser?.email,
                onBack = { currentScreen = Screen.Home.name },
                onLogout = {
                    isGuest = false
                    authViewModel.logout()
                    currentScreen = Screen.Login.name
                }
            )
        }
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
