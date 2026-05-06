package com.piash.modernvibe.videoplayer.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.piash.modernvibe.videoplayer.ui.screens.AboutScreen
import com.piash.modernvibe.videoplayer.ui.screens.FeaturesScreen
import com.piash.modernvibe.videoplayer.ui.screens.FolderVideosScreen
import com.piash.modernvibe.videoplayer.ui.screens.HomeScreen
import com.piash.modernvibe.videoplayer.ui.screens.LibraryScreen
import com.piash.modernvibe.videoplayer.ui.screens.PlayerScreen
import com.piash.modernvibe.videoplayer.ui.screens.SettingsScreen
import java.net.URLEncoder

private data class TopDest(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    TopDest("home", "Home", Icons.Filled.Home),
    TopDest("library", "Library", Icons.Filled.VideoLibrary),
    TopDest("player", "Player", Icons.Filled.PlayArrow),
    TopDest("features", "Features", Icons.Filled.Star),
    TopDest("settings", "Settings", Icons.Filled.Settings),
    TopDest("about", "About", Icons.Filled.Info),
)

@Composable
fun VibeApp(initialUri: Uri? = null) {
    VibeTheme {
        val navController = rememberNavController()
        var pendingUri by remember { mutableStateOf<Uri?>(initialUri) }
        val startRoute = if (initialUri != null) "player" else "home"

        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        Scaffold(
            bottomBar = {
                NavigationBar {
                    destinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        ) { padding: PaddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                NavHost(
                    navController = navController,
                    startDestination = startRoute,
                    enterTransition = {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) + fadeIn()
                    },
                    exitTransition = {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) + fadeOut()
                    },
                    popEnterTransition = {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) + fadeIn()
                    },
                    popExitTransition = {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) + fadeOut()
                    }
                ) {
                    composable("home") {
                        HomeScreen(
                            onPlayUri = { uri ->
                                pendingUri = uri
                                navController.navigate("player")
                            },
                            onOpenLibrary = { navController.navigate("library") },
                            onOpenFeatures = { navController.navigate("features") },
                            onOpenAbout = { navController.navigate("about") },
                            onOpenSettings = { navController.navigate("settings") },
                        )
                    }
                    composable("library") {
                        LibraryScreen(
                            onPlayUri = { uri ->
                                pendingUri = uri
                                navController.navigate("player")
                            },
                            onOpenFolder = { folderPath ->
                                val encoded = URLEncoder.encode(folderPath, Charsets.UTF_8.name())
                                navController.navigate("folder/$encoded")
                            }
                        )
                    }
                    composable(
                        "folder/{path}",
                        arguments = listOf(navArgument("path") { type = NavType.StringType })
                    ) { entry ->
                        val rawPath = entry.arguments?.getString("path").orEmpty()
                        val decoded = java.net.URLDecoder.decode(rawPath, Charsets.UTF_8.name())
                        FolderVideosScreen(
                            folderPath = decoded,
                            onPlay = { v ->
                                pendingUri = v.uri
                                navController.navigate("player")
                            }
                        )
                    }
                    composable("player") {
                        PlayerScreen(uri = pendingUri)
                    }
                    composable("features") { FeaturesScreen() }
                    composable("settings") { SettingsScreen() }
                    composable("about") { AboutScreen() }
                }
            }
        }
    }
}
