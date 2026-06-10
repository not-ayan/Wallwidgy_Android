package com.notayan.wallwidgy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notayan.wallwidgy.repository.FavoritesRepository
import com.notayan.wallwidgy.ui.screens.FavoritesScreen
import com.notayan.wallwidgy.ui.screens.HomeScreen
import com.notayan.wallwidgy.ui.screens.WallpaperDetailScreen
import com.notayan.wallwidgy.ui.screens.AboutScreen
import com.notayan.wallwidgy.ui.theme.WallwidgyTheme
import com.notayan.wallwidgy.ui.viewmodel.WallpaperViewModel
import com.notayan.wallwidgy.ui.viewmodel.WallpaperViewModelFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val favoritesRepository = FavoritesRepository(this)
        val factory = WallpaperViewModelFactory(favoritesRepository)

        setContent {
            val monetEnabled by favoritesRepository.monetEnabled.collectAsState(initial = true)
            val customAccentColor by favoritesRepository.customAccentColor.collectAsState(initial = 0xFF4C663B.toInt())

            WallwidgyTheme(
                monetEnabled = monetEnabled,
                customAccentColor = customAccentColor
            ) {
                val navController = rememberNavController()
                val viewModel: WallpaperViewModel = viewModel(factory = factory)

                LaunchedEffect(intent) {
                    val navigateTo = intent?.getStringExtra("navigate_to")
                    if (navigateTo == "about") {
                        navController.navigate("about") {
                            launchSingleTop = true
                        }
                    }
                }

                val context = LocalContext.current
                LaunchedEffect(viewModel) {
                    viewModel.checkForUpdates(context, showNotification = true)
                }
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val isSystemDark = isSystemInDarkTheme()
                val bgColor = if (isSystemDark) Color(0xFF0A0A0A) else Color(0xFFFAF9F6)

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = bgColor,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (currentRoute != "detail/{fileName}") {
                            NavigationBar(
                                containerColor = bgColor,
                                tonalElevation = 0.dp,
                                windowInsets = WindowInsets(0, 0, 0, 0),
                                modifier = Modifier
                                    .height(56.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == "home" || currentRoute == null,
                                    onClick = { 
                                        if (currentRoute != "home" && currentRoute != null) {
                                            navController.navigate("home") {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    icon = { 
                                        Icon(
                                            if (currentRoute == "home" || currentRoute == null) Icons.Default.Home else Icons.Outlined.Home, 
                                            contentDescription = "Home"
                                        ) 
                                    }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "favorites",
                                    onClick = { 
                                        if (currentRoute != "favorites") {
                                            navController.navigate("favorites") {
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    icon = { 
                                        Icon(
                                            if (currentRoute == "favorites") Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, 
                                            contentDescription = "Favorites"
                                        ) 
                                    }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "about",
                                    onClick = { 
                                        if (currentRoute != "about") {
                                            navController.navigate("about") {
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    icon = { 
                                        Icon(
                                            if (currentRoute == "about") Icons.Default.Info else Icons.Outlined.Info, 
                                            contentDescription = "About"
                                        ) 
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding()) // ONLY pad bottom for navigation bar
                    ) {
                        composable(
                            route = "home",
                            enterTransition = { fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) },
                            exitTransition = { fadeOut(animationSpec = tween(220, easing = FastOutLinearInEasing)) }
                        ) {
                            HomeScreen(viewModel = viewModel) { wallpaper ->
                                try {
                                    val encodedFileName = URLEncoder.encode(wallpaper.fileName, StandardCharsets.UTF_8.toString())
                                    navController.navigate("detail/$encodedFileName")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        composable(
                            route = "favorites",
                            enterTransition = { fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) },
                            exitTransition = { fadeOut(animationSpec = tween(220, easing = FastOutLinearInEasing)) }
                        ) {
                            FavoritesScreen(viewModel) { wallpaper ->
                                try {
                                    val encodedFileName = URLEncoder.encode(wallpaper.fileName, StandardCharsets.UTF_8.toString())
                                    navController.navigate("detail/$encodedFileName")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        composable(
                            route = "about",
                            enterTransition = { fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) },
                            exitTransition = { fadeOut(animationSpec = tween(220, easing = FastOutLinearInEasing)) }
                        ) {
                            AboutScreen(
                                viewModel = viewModel,
                                onNavigateHome = {
                                    navController.navigate("home") {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(
                            route = "detail/{fileName}",
                            arguments = listOf(navArgument("fileName") { type = NavType.StringType }),
                            enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(300))
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(300))
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(300))
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(300))
                            }
                        ) { backStackEntry ->
                            val fileName = backStackEntry.arguments?.getString("fileName")
                            val uiState by viewModel.uiState.collectAsState()
                            if (uiState is com.notayan.wallwidgy.ui.viewmodel.UiState.Success) {
                                val wallpapers = (uiState as com.notayan.wallwidgy.ui.viewmodel.UiState.Success).wallpapers
                                val wallpaper = wallpapers.find { it.fileName == fileName }
                                if (wallpaper != null) {
                                    WallpaperDetailScreen(
                                        wallpaper = wallpaper,
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() },
                                        onNavigateToWallpaper = { similar ->
                                            try {
                                                val encodedFileName = URLEncoder.encode(similar.fileName, StandardCharsets.UTF_8.toString())
                                                navController.navigate("detail/$encodedFileName") {
                                                    popUpTo("detail/{fileName}") { inclusive = true }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
