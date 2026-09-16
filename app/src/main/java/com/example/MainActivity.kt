package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TextChatScreen
import com.example.ui.screens.VoiceChatScreen
import com.example.ui.theme.NiraTheme
import com.example.ui.viewmodel.NiraViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: NiraViewModel = viewModel()
            val voiceSettings by viewModel.voiceSettings.collectAsState()
            val errorMessage by viewModel.errorMessage.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(errorMessage) {
                errorMessage?.let {
                    snackbarHostState.showSnackbar(it)
                    viewModel.clearError()
                }
            }

            NiraTheme(themeMode = voiceSettings.themeMode) {
                val navController = rememberNavController()

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToVoiceChat = { navController.navigate("voice_chat") },
                                onNavigateToTextChat = { navController.navigate("text_chat") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("voice_chat") {
                            VoiceChatScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToTextChat = {
                                    navController.navigate("text_chat") {
                                        popUpTo("home")
                                    }
                                }
                            )
                        }
                        composable("text_chat") {
                            TextChatScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToVoiceChat = {
                                    navController.navigate("voice_chat") {
                                        popUpTo("home")
                                    }
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
