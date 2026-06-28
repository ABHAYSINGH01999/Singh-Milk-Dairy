package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.AppContainer
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel

@Composable
fun MainAppScreen(appContainer: AppContainer) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { AppBottomNavigation(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("splash") {
                com.example.ui.splash.SplashScreen(
                    onTimeout = { 
                        navController.navigate("dashboard") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }
            composable("dashboard") {
                val viewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.provideFactory(appContainer.dairyRepository)
                )
                DashboardScreen(
                    viewModel = viewModel,
                    searchManager = appContainer.searchManager,
                    onNavigateToCustomer = { id -> navController.navigate("customer_profile/$id") },
                    onNavigateToAddCustomer = { navController.navigate("add_customer") },
                    onNavigateToPlanner = { navController.navigate("deliveries") },
                    onNavigateToNotes = { navController.navigate("notes") },
                    onNavigateToSettlements = { navController.navigate("settlements") },
                    onNavigateToDues = { navController.navigate("dues") }
                )
            }
            composable("customers") {
                val viewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.provideFactory(appContainer.dairyRepository)
                )
                com.example.ui.customers.CustomerListScreen(
                    viewModel = viewModel,
                    searchManager = appContainer.searchManager,
                    onNavigateToAdd = { navController.navigate("add_customer") },
                    onNavigateToProfile = { customerId ->
                        navController.navigate("customer_profile/$customerId")
                    }
                )
            }
            composable("deliveries") {
                val viewModel: com.example.ui.planner.DeliveryPlannerViewModel = viewModel(
                    factory = com.example.ui.planner.DeliveryPlannerViewModel.provideFactory(appContainer.dairyRepository)
                )
                com.example.ui.planner.DeliveryPlannerScreen(viewModel = viewModel)
            }
            composable("settings") {
                com.example.ui.settings.SettingsScreen(
                    authManager = appContainer.authManager,
                    syncManager = appContainer.syncManager
                )
            }
            composable("add_customer") {
                val viewModel: com.example.ui.customers.CustomerViewModel = viewModel(
                    factory = com.example.ui.customers.CustomerViewModel.provideFactory(appContainer.dairyRepository)
                )
                com.example.ui.customers.AddCustomerScreen(viewModel = viewModel, customerId = -1, onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = "edit_customer/{customerId}",
                arguments = listOf(androidx.navigation.navArgument("customerId") { type = androidx.navigation.NavType.IntType })
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getInt("customerId") ?: return@composable
                val viewModel: com.example.ui.customers.CustomerViewModel = viewModel(
                    factory = com.example.ui.customers.CustomerViewModel.provideFactory(appContainer.dairyRepository)
                )
                com.example.ui.customers.AddCustomerScreen(viewModel = viewModel, customerId = customerId, onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = "customer_profile/{customerId}",
                arguments = listOf(androidx.navigation.navArgument("customerId") { type = androidx.navigation.NavType.IntType })
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getInt("customerId") ?: return@composable
                val viewModel: com.example.ui.customers.CustomerViewModel = viewModel(
                    factory = com.example.ui.customers.CustomerViewModel.provideFactory(appContainer.dairyRepository)
                )
                com.example.ui.customers.CustomerProfileScreen(
                    viewModel = viewModel,
                    customerId = customerId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddEntry = { id -> navController.navigate("add_entry/$id") },
                    onNavigateToEditEntry = { custId, entryId -> navController.navigate("edit_entry/$custId/$entryId") },
                    onNavigateToEditCustomer = { id -> navController.navigate("edit_customer/$id") },
                    onNavigateToSettlements = { navController.navigate("settlements") }
                )
            }
            composable(
                route = "add_entry/{customerId}",
                arguments = listOf(androidx.navigation.navArgument("customerId") { type = androidx.navigation.NavType.IntType })
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getInt("customerId") ?: return@composable
                val viewModel: com.example.ui.customers.CustomerViewModel = viewModel(
                    factory = com.example.ui.customers.CustomerViewModel.provideFactory(appContainer.dairyRepository)
                )
                com.example.ui.customers.AddDeliveryEntryScreen(
                    viewModel = viewModel,
                    customerId = customerId,
                    entryId = -1,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "edit_entry/{customerId}/{entryId}",
                arguments = listOf(
                    androidx.navigation.navArgument("customerId") { type = androidx.navigation.NavType.IntType },
                    androidx.navigation.navArgument("entryId") { type = androidx.navigation.NavType.IntType }
                )
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getInt("customerId") ?: return@composable
                val entryId = backStackEntry.arguments?.getInt("entryId") ?: return@composable
                val viewModel: com.example.ui.customers.CustomerViewModel = viewModel(
                    factory = com.example.ui.customers.CustomerViewModel.provideFactory(appContainer.dairyRepository)
                )
                com.example.ui.customers.AddDeliveryEntryScreen(
                    viewModel = viewModel,
                    customerId = customerId,
                    entryId = entryId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("notes") {
                val viewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.provideFactory(appContainer.dairyRepository)
                )
                com.example.ui.dashboard.NotesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("settlements") {
                val viewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.provideFactory(appContainer.dairyRepository)
                )
                com.example.ui.dashboard.SettlementHistoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("dues") {
                val viewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.provideFactory(appContainer.dairyRepository)
                )
                com.example.ui.dashboard.UpcomingDuesScreen(
                    viewModel = viewModel,
                    onNavigateToCustomer = { id -> navController.navigate("customer_profile/$id") },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun AppBottomNavigation(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf("dashboard", "customers", "deliveries", "settings")

    if (!showBottomBar) return

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        val navItemColors = androidx.compose.material3.NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.secondary,
            indicatorColor = MaterialTheme.colorScheme.secondary,
            unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
            label = { Text("Home") },
            selected = currentRoute == "dashboard",
            colors = navItemColors,
            onClick = {
                navController.navigate("dashboard") {
                    popUpTo("dashboard") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Customers") },
            label = { Text("Customers") },
            selected = currentRoute == "customers",
            colors = navItemColors,
            onClick = {
                navController.navigate("customers") {
                    popUpTo("dashboard") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Menu, contentDescription = "Deliveries") },
            label = { Text("Deliveries") },
            selected = currentRoute == "deliveries",
            colors = navItemColors,
            onClick = {
                navController.navigate("deliveries") {
                    popUpTo("dashboard") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentRoute == "settings",
            colors = navItemColors,
            onClick = {
                navController.navigate("settings") {
                    popUpTo("dashboard") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}
