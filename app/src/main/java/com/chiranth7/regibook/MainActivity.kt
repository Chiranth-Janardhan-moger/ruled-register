package com.chiranth7.regibook

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.chiranth7.regibook.features.lic.viewmodel.LicViewModel
import com.chiranth7.regibook.features.lic.viewmodel.LicViewModelFactory
import com.chiranth7.regibook.features.pigmi.viewmodel.PigmiViewModel
import com.chiranth7.regibook.features.pigmi.viewmodel.PigmiViewModelFactory
import com.chiranth7.regibook.ui.screens.RegisterNavGraph
import com.chiranth7.regibook.ui.theme.RegisterBookTheme
import com.chiranth7.regibook.util.AppThemeMode
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as RegisterApplication

        setContent {
            val currentLang by app.settingsManager.currentLanguage.collectAsStateWithLifecycle()
            val themeMode by app.settingsManager.themeMode.collectAsStateWithLifecycle()

            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = remember(themeMode, isSystemDark) {
                when (themeMode) {
                    AppThemeMode.LIGHT -> false
                    AppThemeMode.DARK -> true
                    AppThemeMode.SYSTEM -> isSystemDark
                }
            }

            val locale = remember(currentLang) { Locale(currentLang) }
            val baseConfig = LocalConfiguration.current
            val configuration = remember(currentLang, baseConfig) {
                Configuration(baseConfig).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
            }

            CompositionLocalProvider(
                LocalConfiguration provides configuration
            ) {
                RegisterBookTheme(darkTheme = isDarkTheme) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val navController = rememberNavController()

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
}
