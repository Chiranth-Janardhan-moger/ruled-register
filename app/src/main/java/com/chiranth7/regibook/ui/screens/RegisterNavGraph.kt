package com.chiranth7.regibook.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.chiranth7.regibook.features.lic.ui.AddEditLicScreen
import com.chiranth7.regibook.features.lic.ui.LicDetailScreen
import com.chiranth7.regibook.features.lic.ui.LicRegisterScreen
import com.chiranth7.regibook.features.lic.viewmodel.LicViewModel
import com.chiranth7.regibook.features.pigmi.ui.AddEditPigmiScreen
import com.chiranth7.regibook.features.pigmi.ui.PigmiRegisterScreen
import com.chiranth7.regibook.features.pigmi.viewmodel.PigmiViewModel
import com.chiranth7.regibook.util.RegisterType
import com.chiranth7.regibook.util.SettingsManager
import com.chiranth7.regibook.util.update.UpdateManager

object Routes {
    const val PIGMI_HOME = "pigmi_home"
    const val PIGMI_ADD_EDIT = "pigmi_add_edit"

    const val LIC_HOME = "lic_home"
    const val LIC_ADD_EDIT = "lic_add_edit"
    const val LIC_DETAILS = "lic_details/{accountId}"

    const val SETTINGS = "settings"

    fun licDetails(id: Long): String = "lic_details/$id"
}

@Composable
fun RegisterNavGraph(
    navController: NavHostController,
    pigmiViewModel: PigmiViewModel,
    licViewModel: LicViewModel,
    settingsManager: SettingsManager,
    updateManager: UpdateManager,
    modifier: Modifier = Modifier
) {
    val startDest = if (settingsManager.activeRegisterType.value == RegisterType.LIC) {
        Routes.LIC_HOME
    } else {
        Routes.PIGMI_HOME
    }

    NavHost(
        navController = navController,
        startDestination = startDest,
        modifier = modifier
    ) {
        // ====================
        // Pigmi Register Feature
        // ====================
        composable(Routes.PIGMI_HOME) {
            PigmiRegisterScreen(
                viewModel = pigmiViewModel,
                settingsManager = settingsManager,
                onSwitchRegister = { newType ->
                    if (newType == RegisterType.LIC) {
                        settingsManager.setActiveRegisterType(RegisterType.LIC)
                        navController.navigate(Routes.LIC_HOME) {
                            popUpTo(Routes.PIGMI_HOME) { inclusive = true }
                        }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(Routes.PIGMI_ADD_EDIT) {
            AddEditPigmiScreen(
                viewModel = pigmiViewModel,
                onNavigateBack = { navController.popBackStackSafely(Routes.PIGMI_ADD_EDIT) },
                onSaved = { navController.popBackStackSafely(Routes.PIGMI_ADD_EDIT) }
            )
        }

        // ====================
        // LIC Policy Cards Feature
        // ====================
        composable(Routes.LIC_HOME) {
            LicRegisterScreen(
                viewModel = licViewModel,
                settingsManager = settingsManager,
                onNavigateToDetails = { accountId ->
                    navController.navigate(Routes.licDetails(accountId))
                },
                onSwitchRegister = { newType ->
                    if (newType == RegisterType.PIGMI) {
                        settingsManager.setActiveRegisterType(RegisterType.PIGMI)
                        navController.navigate(Routes.PIGMI_HOME) {
                            popUpTo(Routes.LIC_HOME) { inclusive = true }
                        }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(Routes.LIC_ADD_EDIT) {
            AddEditLicScreen(
                viewModel = licViewModel,
                onNavigateBack = { navController.popBackStackSafely(Routes.LIC_ADD_EDIT) },
                onSaved = { navController.popBackStackSafely(Routes.LIC_ADD_EDIT) }
            )
        }

        composable(
            route = Routes.LIC_DETAILS,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("accountId") ?: 0L
            LicDetailScreen(
                accountId = id,
                viewModel = licViewModel,
                settingsManager = settingsManager,
                onNavigateBack = { navController.popBackStackSafely(Routes.LIC_DETAILS) },
                onNavigateToEdit = { account ->
                    licViewModel.prepareEditAccount(account)
                    navController.navigate(Routes.LIC_ADD_EDIT)
                }
            )
        }

        // ====================
        // Settings Screen
        // ====================
        composable(Routes.SETTINGS) {
            SettingsScreen(
                settingsManager = settingsManager,
                updateManager = updateManager,
                onNavigateToAddPigmi = {
                    pigmiViewModel.prepareNewAccount()
                    navController.navigate(Routes.PIGMI_ADD_EDIT)
                },
                onNavigateToAddLic = {
                    licViewModel.prepareNewAccount()
                    navController.navigate(Routes.LIC_ADD_EDIT)
                },
                onNavigateBack = { navController.popBackStackSafely(Routes.SETTINGS) }
            )
        }
    }
}

fun NavHostController.popBackStackSafely(expectedRoute: String? = null): Boolean {
    val currentEntry = currentBackStackEntry ?: return false
    if (currentEntry.lifecycle.currentState != androidx.lifecycle.Lifecycle.State.RESUMED) {
        return false
    }
    if (previousBackStackEntry == null) {
        return false
    }
    if (expectedRoute != null) {
        val currentRoute = currentDestination?.route
        if (currentRoute != expectedRoute && currentRoute?.substringBefore('/') != expectedRoute.substringBefore('/')) {
            return false
        }
    }
    return popBackStack()
}
