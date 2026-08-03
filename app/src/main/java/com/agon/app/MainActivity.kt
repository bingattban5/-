package com.agon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agon.app.ui.components.AppPermissionHandler
import com.agon.app.ui.screens.DownloadsScreen
import com.agon.app.ui.screens.FileManagerScreen
import com.agon.app.ui.screens.HomeScreen
import com.agon.app.ui.screens.ModelsScreen
import com.agon.app.ui.screens.SettingsScreen
import com.agon.app.ui.theme.AgonAppTheme
import com.agon.app.viewmodel.DownloadsViewModel
import com.agon.app.viewmodel.FileManagerViewModel
import com.agon.app.viewmodel.HomeViewModel
import com.agon.app.viewmodel.ModelsViewModel
import com.agon.app.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // إنشاء SettingsViewModel على مستوى Activity لقراءة تفضيل الوضع الليلي
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()

            AgonAppTheme(
                darkTheme = isDarkTheme, // تمرير التفضيل للثيم لتفعيل الوضع الليلي
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

data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainApp() {
    val navController = rememberNavController()

    val navItems = listOf(
        NavItem("home", "الرئيسية", Icons.Filled.Home, Icons.Outlined.Home),
        NavItem("downloads", "التنزيلات", Icons.Filled.Download, Icons.Outlined.Download),
        NavItem("files", "الملفات", Icons.Filled.FolderOpen, Icons.Outlined.FolderOpen),
        NavItem("models", "النماذج", Icons.Filled.Engineering, Icons.Outlined.Engineering),
        NavItem("settings", "الإعدادات", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    AppPermissionHandler(
        onPermissionsGranted = { }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = { FloatingBottomNav(navController, navItems) },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding),
            ) {
                composable("home") {
                    val viewModel: HomeViewModel = hiltViewModel()
                    HomeScreen(viewModel = viewModel)
                }
                composable("downloads") {
                    val viewModel: DownloadsViewModel = hiltViewModel()
                    DownloadsScreen(viewModel = viewModel)
                }
                composable("models") {
                    val viewModel: ModelsViewModel = hiltViewModel()
                    ModelsScreen(viewModel = viewModel)
                }
                composable("files") {
                    val viewModel: FileManagerViewModel = hiltViewModel()
                    FileManagerScreen(viewModel = viewModel)
                }
                composable("settings") {
                    val viewModel: SettingsViewModel = hiltViewModel()
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun FloatingBottomNav(navController: NavHostController, navItems: List<NavItem>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier.height(64.dp)
        ) {
            navItems.forEach { item ->
                val isSelected = currentRoute == item.route

                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "iconColor"
                )

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = iconColor,
                                modifier = Modifier.size(if (isSelected) 28.dp else 24.dp)
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .shadow(
                                            elevation = 4.dp,
                                            shape = CircleShape,
                                            ambientColor = MaterialTheme.colorScheme.primary,
                                            spotColor = MaterialTheme.colorScheme.primary
                                        )
                                )
                            }
                        }
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = iconColor
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Transparent,
                        unselectedIconColor = Color.Transparent,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}
