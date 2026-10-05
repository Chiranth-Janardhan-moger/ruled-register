package com.chiranth7.regibook.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chiranth7.regibook.BuildConfig
import com.chiranth7.regibook.R
import com.chiranth7.regibook.ui.components.NotebookPaperBackground
import com.chiranth7.regibook.ui.components.NotebookRowHeight
import com.chiranth7.regibook.util.AppThemeMode
import com.chiranth7.regibook.util.MarginLineColor
import com.chiranth7.regibook.util.SettingsManager
import com.chiranth7.regibook.util.update.UpdateManager
import com.chiranth7.regibook.util.update.UpdateState
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    updateManager: UpdateManager,
    onNavigateToAddPigmi: () -> Unit,
    onNavigateToAddLic: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLang by settingsManager.currentLanguage.collectAsStateWithLifecycle()
    val currentTheme by settingsManager.themeMode.collectAsStateWithLifecycle()
    val currentMarginColor by settingsManager.marginLineColor.collectAsStateWithLifecycle()
    val updateState by updateManager.updateState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var showAddTypeDialog by remember { mutableStateOf(false) }

    val inkColor = MaterialTheme.colorScheme.onBackground
    val secondaryInk = MaterialTheme.colorScheme.onSurfaceVariant

    BackHandler {
        onNavigateBack()
    }

    // Dialog asking: Add Pigmi or LIC?
    if (showAddTypeDialog) {
        Dialog(onDismissRequest = { showAddTypeDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Add New Entry",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = inkColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Choose the register to add a record into:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                        color = secondaryInk
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Option 1: Pigmi Register
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddTypeDialog = false
                                onNavigateToAddPigmi()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Pigmi Register",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = inkColor
                                )
                                Text(
                                    text = "Daily collection account",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                                    color = secondaryInk
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 2: LIC Register
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddTypeDialog = false
                                onNavigateToAddLic()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CardMembership,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "LIC Policy Card",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = inkColor
                                )
                                Text(
                                    text = "Insurance premium reminder",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                                    color = secondaryInk
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = { showAddTypeDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", fontFamily = FontFamily.Serif)
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        NotebookPaperBackground(
            lineSpacing = NotebookRowHeight,
            topStartOffset = 56.dp,
            showMarginLine = false,
            showRuledLines = false
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = inkColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    ),
                    color = inkColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Section 1: Quick Actions (Add Entry)
                Text(
                    text = "ACTIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddTypeDialog = true }
                        .testTag("settings_add_entry_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Entry",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Add New Entry",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = inkColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Create a new Pigmi or LIC account",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                                color = secondaryInk
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section 2: Theme
                Text(
                    text = stringResource(R.string.theme).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = secondaryInk,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SettingOptionRow(
                    title = stringResource(R.string.theme_light),
                    isSelected = currentTheme == AppThemeMode.LIGHT,
                    onClick = { settingsManager.setThemeMode(AppThemeMode.LIGHT) },
                    testTag = "theme_option_light"
                )

                SettingOptionRow(
                    title = stringResource(R.string.theme_dark),
                    isSelected = currentTheme == AppThemeMode.DARK,
                    onClick = { settingsManager.setThemeMode(AppThemeMode.DARK) },
                    testTag = "theme_option_dark"
                )

                SettingOptionRow(
                    title = stringResource(R.string.theme_system),
                    isSelected = currentTheme == AppThemeMode.SYSTEM,
                    onClick = { settingsManager.setThemeMode(AppThemeMode.SYSTEM) },
                    testTag = "theme_option_system"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Section 3: Margin Line Color
                Text(
                    text = stringResource(R.string.margin_line_color).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = secondaryInk,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SettingOptionRow(
                    title = stringResource(R.string.color_red),
                    isSelected = currentMarginColor == MarginLineColor.RED,
                    onClick = { settingsManager.setMarginLineColor(MarginLineColor.RED) },
                    testTag = "margin_color_red"
                )

                SettingOptionRow(
                    title = stringResource(R.string.color_black),
                    isSelected = currentMarginColor == MarginLineColor.BLACK,
                    onClick = { settingsManager.setMarginLineColor(MarginLineColor.BLACK) },
                    testTag = "margin_color_black"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Section 4: Language
                Text(
                    text = stringResource(R.string.language).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = secondaryInk,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SettingOptionRow(
                    title = "English",
                    isSelected = currentLang == SettingsManager.LANG_ENGLISH,
                    onClick = { settingsManager.setLanguage(SettingsManager.LANG_ENGLISH) },
                    testTag = "language_option_english"
                )

                SettingOptionRow(
                    title = "ಕನ್ನಡ",
                    isSelected = currentLang == SettingsManager.LANG_KANNADA,
                    onClick = { settingsManager.setLanguage(SettingsManager.LANG_KANNADA) },
                    testTag = "language_option_kannada"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Section 5: App Updates & About
                Text(
                    text = "ABOUT & UPDATES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = secondaryInk,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = inkColor,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stringResource(R.string.about_description),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Serif,
                                lineHeight = 18.sp
                            ),
                            color = secondaryInk
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    updateManager.checkForUpdates(isManualCheck = true)
                                }
                            },
                            enabled = updateState !is UpdateState.Checking,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (updateState is UpdateState.Checking) "Checking GitHub..." else "Check for Updates",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingOptionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val inkColor = MaterialTheme.colorScheme.onBackground
    val activePillBg = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else androidx.compose.ui.graphics.Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(
                color = activePillBg,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 16.sp
            ),
            color = inkColor,
            modifier = Modifier.weight(1f)
        )

        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = inkColor.copy(alpha = 0.4f)
            ),
            modifier = Modifier.size(24.dp)
        )
    }
}
