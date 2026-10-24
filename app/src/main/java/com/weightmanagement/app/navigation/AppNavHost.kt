package com.weightmanagement.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.weightmanagement.app.ui.StartViewModel
import com.weightmanagement.app.ui.activity.ActivityScreen
import com.weightmanagement.app.ui.backup.BackupScreen
import com.weightmanagement.app.ui.dashboard.DashboardScreen
import com.weightmanagement.app.ui.goal.GoalScreen
import com.weightmanagement.app.ui.nutrition.NutritionScreen
import com.weightmanagement.app.ui.onboarding.OnboardingScreen
import com.weightmanagement.app.ui.profile.ProfileScreen
import com.weightmanagement.app.ui.settings.SettingsScreen
import com.weightmanagement.app.ui.water.WaterScreen
import com.weightmanagement.app.ui.weight.WeightScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val WEIGHT = "weight"
    const val NUTRITION = "nutrition"
    const val ACTIVITY = "activity"
    const val WATER = "water"
    const val PROFILE = "profile"
    const val GOAL = "goal"
    const val SETTINGS = "settings"
    const val BACKUP = "backup"
}

private val TAB_ITEMS = listOf(
    Triple(Routes.DASHBOARD, "首页", Icons.Filled.Home),
    Triple(Routes.WEIGHT, "体重", Icons.Filled.FitnessCenter),
    Triple(Routes.NUTRITION, "饮食", Icons.Filled.Restaurant),
    Triple(Routes.ACTIVITY, "运动", Icons.Filled.DirectionsRun),
    Triple(Routes.WATER, "饮水", Icons.Filled.LocalDrink),
    Triple(Routes.GOAL, "目标", Icons.Filled.Flag),
    Triple(Routes.PROFILE, "我的", Icons.Filled.Person)
)

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startViewModel: StartViewModel = hiltViewModel()
) {
    val onboarded by startViewModel.isOnboarded.collectAsStateWithLifecycle()

    if (onboarded == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp))
        }
        return
    }

    val start = if (onboarded == true) Routes.DASHBOARD else Routes.ONBOARDING
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottom = TAB_ITEMS.any { it.first == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottom) {
                NavigationBar {
                    val items = TAB_ITEMS
                    val navDest = navBackStackEntry?.destination
                    items.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            selected = navDest?.hierarchy?.any { it.route == route } == true,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.ONBOARDING) { OnboardingScreen(navController) }
            composable(Routes.DASHBOARD) { DashboardScreen(navController) }
            composable(Routes.WEIGHT) { WeightScreen(navController) }
            composable(Routes.NUTRITION) { NutritionScreen(navController) }
            composable(Routes.ACTIVITY) { ActivityScreen(navController) }
            composable(Routes.WATER) { WaterScreen(navController) }
            composable(Routes.GOAL) { GoalScreen(navController) }
            composable(Routes.PROFILE) { ProfileScreen(navController) }
            composable(Routes.SETTINGS) { SettingsScreen(navController) }
            composable(Routes.BACKUP) { BackupScreen(navController) }
        }
    }
}
