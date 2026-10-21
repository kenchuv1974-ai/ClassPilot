package com.classpilot.offline

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.classpilot.offline.data.preferences.PreferencesManager
import com.classpilot.offline.ui.navigation.BottomNavItem
import com.classpilot.offline.ui.navigation.ClassPilotNavGraph
import com.classpilot.offline.ui.theme.ClassPilotTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefsManager: PreferencesManager

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by prefsManager.appPreferences.collectAsStateWithLifecycle(
                initialValue = com.classpilot.offline.data.preferences.AppPreferences()
            )

            val darkTheme = when (prefs.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            ClassPilotTheme(darkTheme = darkTheme) {
                // Request POST_NOTIFICATIONS permission on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notifPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
                    LaunchedEffect(Unit) {
                        if (!notifPermission.status.isGranted) notifPermission.launchPermissionRequest()
                    }
                }

                ClassPilotMainContent()
            }
        }
    }
}

@Composable
private fun ClassPilotMainContent() {
    val navController = rememberNavController()
    val backstackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backstackEntry?.destination?.route

    // Determine which bottom nav item is selected
    val selectedItem = BottomNavItem.values().firstOrNull { item ->
        currentRoute == item.screen.route
    }
    val showBottomBar = selectedItem != null

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomNavItem.values().forEach { item ->
                        NavigationBarItem(
                            selected = item == selectedItem,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            ClassPilotNavGraph(navController = navController)
        }
    }
}

