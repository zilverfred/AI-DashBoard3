package ir.dashboard112.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ir.dashboard112.app.data.local.AppDatabase
import ir.dashboard112.app.data.preferences.UserPreferencesRepository
import ir.dashboard112.app.data.repository.DashboardRepository
import ir.dashboard112.app.notification.ReminderScheduler
import ir.dashboard112.app.ui.screens.DashboardHomeScreen
import ir.dashboard112.app.ui.screens.FinanceModuleScreen
import ir.dashboard112.app.ui.screens.ReportsModuleScreen
import ir.dashboard112.app.ui.screens.SettingsModuleScreen
import ir.dashboard112.app.ui.screens.TasksAndOfficeModuleScreen
import ir.dashboard112.app.ui.screens.VehicleModuleScreen
import ir.dashboard112.app.ui.screens.WorkoutAndSleepModuleScreen
import ir.dashboard112.app.ui.theme.Dashboard112Theme
import ir.dashboard112.app.ui.viewmodel.DashboardViewModel

class Dashboard112Application : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { DashboardRepository(database) }
    val preferencesRepository by lazy { UserPreferencesRepository(this) }

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.ensureNotificationChannels(this)
    }
}

sealed class BottomNavItem(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector
) {
    data object Dashboard : BottomNavItem("dashboard", R.string.nav_dashboard, Icons.Default.Home)
    data object Tasks : BottomNavItem("tasks", R.string.nav_tasks, Icons.Default.CheckCircle)
    data object Workout : BottomNavItem("workout", R.string.nav_workout, Icons.Default.FitnessCenter)
    data object Finance : BottomNavItem("finance", R.string.nav_finance, Icons.Default.Payments)
    data object Vehicle : BottomNavItem("vehicle", R.string.nav_vehicle, Icons.Default.DirectionsCar)
    data object Reports : BottomNavItem("reports", R.string.nav_reports, Icons.Default.Assessment)
    data object Settings : BottomNavItem("settings", R.string.nav_settings, Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as Dashboard112Application
        val viewModel = DashboardViewModel(
            repository = app.repository,
            preferencesRepository = app.preferencesRepository,
            savedStateHandle = SavedStateHandle()
        )
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            Dashboard112Theme(
                themeMode = uiState.preferences.themeMode,
                primaryColorKey = uiState.preferences.primaryColorKey,
                fontScale = uiState.preferences.fontScale
            ) {
                Dashboard112AppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun Dashboard112AppRoot(viewModel: DashboardViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()
    val navItems = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Tasks,
        BottomNavItem.Workout,
        BottomNavItem.Finance,
        BottomNavItem.Vehicle,
        BottomNavItem.Reports,
        BottomNavItem.Settings
    )

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"
                navItems.forEach { item ->
                    val label = stringResource(id = item.titleResId)
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            viewModel.setActiveRoute(item.route)
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(imageVector = item.icon, contentDescription = label) },
                        label = {
                            Text(
                                text = label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Dashboard.route) {
                DashboardHomeScreen(
                    uiState = uiState,
                    onNavigateToRoute = { targetRoute ->
                        navController.navigate(targetRoute) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onToggleTask = viewModel::toggleTaskCompletion
                )
            }
            composable(BottomNavItem.Tasks.route) {
                TasksAndOfficeModuleScreen(uiState = uiState, viewModel = viewModel)
            }
            composable(BottomNavItem.Workout.route) {
                WorkoutAndSleepModuleScreen(uiState = uiState, viewModel = viewModel)
            }
            composable(BottomNavItem.Finance.route) {
                FinanceModuleScreen(uiState = uiState, viewModel = viewModel)
            }
            composable(BottomNavItem.Vehicle.route) {
                VehicleModuleScreen(uiState = uiState, viewModel = viewModel)
            }
            composable(BottomNavItem.Reports.route) {
                ReportsModuleScreen(uiState = uiState)
            }
            composable(BottomNavItem.Settings.route) {
                SettingsModuleScreen(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}
