package com.chiranth7.regibook

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.chiranth7.regibook.features.lic.viewmodel.LicViewModel
import com.chiranth7.regibook.features.lic.viewmodel.LicViewModelFactory
import com.chiranth7.regibook.features.pigmi.viewmodel.PigmiViewModel
import com.chiranth7.regibook.features.pigmi.viewmodel.PigmiViewModelFactory
import com.chiranth7.regibook.ui.screens.RegisterNavGraph
import com.chiranth7.regibook.ui.theme.RegisterBookTheme
import com.chiranth7.regibook.util.RegisterType
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences("register_book_settings", android.content.Context.MODE_PRIVATE)
        val lang = prefs.getString("selected_language", "en") ?: "en"
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        val localizedContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(localizedContext)
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (overrideConfiguration != null) {
            try {
                val prefs = getSharedPreferences("register_book_settings", android.content.Context.MODE_PRIVATE)
                val lang = prefs.getString("selected_language", "en") ?: "en"
                val locale = Locale.forLanguageTag(lang)
                overrideConfiguration.setLocale(locale)
                overrideConfiguration.setLayoutDirection(locale)
            } catch (_: Exception) {}
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as RegisterApplication

        // If opened from notification, route to LIC register
        if (intent?.getStringExtra("navigate_to") == "lic") {
            app.settingsManager.setActiveRegisterType(RegisterType.LIC)
        }

        // Request notification permission on Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        // Automatic default cloud sync in background
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                com.chiranth7.regibook.features.lic.sync.LicSyncManager.syncPolicies(applicationContext, app.settingsManager)
            } catch (_: Exception) {}
        }

        setContent {
            val fontScale by app.settingsManager.fontScale.collectAsStateWithLifecycle()

            val currentDensity = LocalDensity.current
            val scaledDensity = remember(currentDensity.density, fontScale) {
                Density(
                    density = currentDensity.density,
                    fontScale = fontScale
                )
            }

            CompositionLocalProvider(
                LocalDensity provides scaledDensity
            ) {
                RegisterBookTheme(darkTheme = false) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val navController = rememberNavController()

                        androidx.compose.runtime.LaunchedEffect(navController) {
                            navController.addOnDestinationChangedListener { _, destination, _ ->
                                com.chiranth7.regibook.util.log.AppLogManager.currentScreen = destination.route ?: "home"
                            }
                        }

                        val pigmiViewModel: PigmiViewModel = viewModel(
                            factory = PigmiViewModelFactory(app.pigmiRepository)
                        )
                        val licViewModel: LicViewModel = viewModel(
                            factory = LicViewModelFactory(app.licRepository)
                        )

                        val updateState by app.updateManager.updateState.collectAsStateWithLifecycle()
                        val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            app.updateManager.checkForUpdates(isManualCheck = false)
                        }

                        com.chiranth7.regibook.ui.components.UpdateDialog(
                            updateState = updateState,
                            onConfirmUpdate = { info ->
                                coroutineScope.launch {
                                    app.updateManager.downloadAndInstall(info)
                                }
                            },
                            onDismiss = {
                                app.updateManager.dismissUpdate()
                            }
                        )

                        RegisterNavGraph(
                            navController = navController,
                            pigmiViewModel = pigmiViewModel,
                            licViewModel = licViewModel,
                            settingsManager = app.settingsManager,
                            updateManager = app.updateManager
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val app = application as RegisterApplication
        if (intent.getStringExtra("navigate_to") == "lic") {
            app.settingsManager.setActiveRegisterType(RegisterType.LIC)
        }
    }
}
