package com.agon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.agon.app.ui.components.AppPermissionHandler
import com.agon.app.ui.screens.DownloadsScreen
import com.agon.app.ui.screens.FileManagerScreen
import com.agon.app.ui.screens.ModelsScreen
import com.agon.app.ui.screens.SettingsScreen
import com.agon.app.ui.screens.browser.BrowserScreen
import com.agon.app.ui.theme.AgonAppTheme
import com.agon.app.viewmodel.BrowserViewModel
import com.agon.app.viewmodel.DownloadsViewModel
import com.agon.app.viewmodel.FileManagerViewModel
import com.agon.app.viewmodel.ModelsViewModel
import com.agon.app.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // قراءة تفضيل الوضع الليلي
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()

            AgonAppTheme(
                darkTheme = isDarkTheme,
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp()
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()

    AppPermissionHandler(
        onPermissionsGranted = { }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            // تم إزالة bottomBar بالكامل
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding),
            ) {
                composable("home") {
                    val viewModel: BrowserViewModel = hiltViewModel()
                    BrowserScreen(viewModel = viewModel, navController = navController)
                }
                composable("downloads") {
                    val viewModel: DownloadsViewModel = hiltViewModel()
                    // تم إضافة navController لتمكين زر الرجوع لاحقاً
                    DownloadsScreen(viewModel = viewModel, navController = navController)
                }
                composable("models") {
                    val viewModel: ModelsViewModel = hiltViewModel()
                    ModelsScreen(viewModel = viewModel, navController = navController)
                }
                composable("files") {
                    val viewModel: FileManagerViewModel = hiltViewModel()
                    FileManagerScreen(viewModel = viewModel, navController = navController)
                }
                composable("settings") {
                    val viewModel: SettingsViewModel = hiltViewModel()
                    SettingsScreen(viewModel = viewModel, navController = navController)
                }
            }
        }
    }
}