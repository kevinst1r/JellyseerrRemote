package com.ecp.jellyseerrremote.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ecp.jellyseerrremote.repo.DiscoverCategory
import com.ecp.jellyseerrremote.ui.screens.DiscoverCategoryScreen
import com.ecp.jellyseerrremote.ui.screens.DiscoverScreen
import com.ecp.jellyseerrremote.ui.screens.LoginScreen
import com.ecp.jellyseerrremote.ui.screens.SearchScreen
import com.ecp.jellyseerrremote.ui.screens.SettingsScreen
import com.ecp.jellyseerrremote.ui.theme.AppTheme
import com.ecp.jellyseerrremote.vm.MainViewModel

object Routes {
    const val SHELL = "shell"
    const val SETTINGS = "settings"
    const val LOGIN = "login"
    const val DISCOVER = "discover"
    const val DISCOVER_CATEGORY = "discover_category"
}

@Composable
fun App() {
    AppTheme {
        val nav = rememberNavController()
        val vm: MainViewModel = viewModel()

        NavHost(navController = nav, startDestination = Routes.SHELL) {
            composable(Routes.SHELL) {
                SearchScreen(
                    vm = vm,
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                    onOpenDiscover = { nav.navigate(Routes.DISCOVER) }
                )
            }
            composable(Routes.DISCOVER) {
                DiscoverScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onSeeMore = { category ->
                        nav.navigate("${Routes.DISCOVER_CATEGORY}/${category.name}")
                    }
                )
            }
            composable("${Routes.DISCOVER_CATEGORY}/{categoryName}") { backStackEntry ->
                val name = backStackEntry.arguments?.getString("categoryName") ?: ""
                val category = DiscoverCategory.entries.find { it.name == name } ?: DiscoverCategory.TRENDING
                DiscoverCategoryScreen(
                    category = category,
                    vm = vm,
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) }
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    vm = vm,
                    onDone = { nav.popBackStack() }
                )
            }
        }
    }
}
