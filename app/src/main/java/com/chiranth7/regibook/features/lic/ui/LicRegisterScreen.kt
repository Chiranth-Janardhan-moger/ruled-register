package com.chiranth7.regibook.features.lic.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chiranth7.regibook.R
import com.chiranth7.regibook.features.drawer.RegisterDrawerContent
import com.chiranth7.regibook.features.lic.data.LicAccount
import com.chiranth7.regibook.features.lic.util.PaymentReminderHelper
import com.chiranth7.regibook.features.lic.util.PaymentReminderStatus
import com.chiranth7.regibook.features.lic.viewmodel.LicViewModel
import com.chiranth7.regibook.util.KannadaNameHelper
import com.chiranth7.regibook.util.RegisterType
import com.chiranth7.regibook.util.SettingsManager
import kotlinx.coroutines.launch

/**
 * LIC Screen displaying modern, minimalist white cards with:
 * - Name
 * - Policy Number & Policy Name
 * - Total Years
 * - Next Payment Due Reminder
 * - Last Date of Payment
 */
@Composable
fun LicRegisterScreen(
    viewModel: LicViewModel,
    settingsManager: SettingsManager,
    onNavigateToDetails: (Long) -> Unit,
    onSwitchRegister: (RegisterType) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val agentNumber by settingsManager.agentNumber.collectAsStateWithLifecycle()
    val currentLang by settingsManager.currentLanguage.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    var showProfileDialog by remember { mutableStateOf(false) }
    var accountForPayment by remember { mutableStateOf<LicAccount?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.syncPolicies(context, settingsManager, isSilent = true)
    }

    LaunchedEffect(syncMessage) {
        syncMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearSyncMessage()
        }
    }

    val inkColor = MaterialTheme.colorScheme.onBackground
    val secondaryInk = MaterialTheme.colorScheme.onSurfaceVariant

    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            RegisterDrawerContent(
                activeRegister = RegisterType.LIC,
                onSelectRegister = { selectedType ->
                    coroutineScope.launch { drawerState.close() }
                    if (selectedType != RegisterType.LIC) {
                        onSwitchRegister(selectedType)
                    }
                },
                onNavigateToSettings = {
                    coroutineScope.launch { drawerState.close() }
                    onNavigateToSettings()
                }
            )
        }
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { coroutineScope.launch { drawerState.open() } },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.menu),
                            tint = inkColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Cloud Sync Button
                    if (isSyncing) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                Toast.makeText(context, context.getString(R.string.syncing_policies), Toast.LENGTH_SHORT).show()
                                viewModel.syncPolicies(context, settingsManager)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("sync_policies_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = stringResource(R.string.sync_policies),
                                tint = inkColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = stringResource(R.string.profile),
                            tint = inkColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Policy Cards List
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    if (accounts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.empty_lic),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp
                                ),
                                color = secondaryInk,
                                modifier = Modifier.testTag("empty_lic_text")
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("lic_accounts_list")
                        ) {
                            items(
                                items = accounts,
                                key = { it.id }
                            ) { account ->
                                LicPolicyCard(
                                    account = account,
                                    currentLanguage = currentLang,
                                    onClick = {
                                        viewModel.selectAccount(account.id)
                                        onNavigateToDetails(account.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showProfileDialog) {
            LicProfileDialog(
                agentNumber = agentNumber.ifBlank { SettingsManager.DEFAULT_AGENT_CODE },
                onDismiss = { showProfileDialog = false }
            )
        }

        accountForPayment?.let { acc ->
            MarkPaidDialog(
                account = acc,
                currentLanguage = currentLang,
                onDismiss = { accountForPayment = null },
                onConfirm = { newNext, newLast ->
                    accountForPayment = null
                    viewModel.markPolicyAsPaid(acc, newNext, newLast) {
                        val msg = if (newNext.equals("Completed", ignoreCase = true)) {
                            context.getString(R.string.policy_completed_success)
                        } else {
                            context.getString(R.string.payment_recorded_success, newNext)
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

/**
 * Modern, minimalist white card item for LIC Policy
 */
@Composable
private fun LicPolicyCard(
    account: LicAccount,
    currentLanguage: String = SettingsManager.LANG_ENGLISH,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inkColor = MaterialTheme.colorScheme.onBackground
    val secondaryInk = MaterialTheme.colorScheme.onSurfaceVariant
    val cardBg = Color(0xFFFFFFFF)
    val outlineBorder = MaterialTheme.colorScheme.outlineVariant

    val reminderInfo = remember(account.nextPaymentDate) {
        PaymentReminderHelper.calculateReminder(account.nextPaymentDate)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, outlineBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("lic_card_${account.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = account.getDisplayName(currentLanguage),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = inkColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Policy Number Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = (if (currentLanguage == SettingsManager.LANG_KANNADA) "ಪಾಲಿಸಿ: " else "No: ") + account.policyNumber,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            color = inkColor
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Policy Number", account.policyNumber)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, context.getString(R.string.policy_number_copied), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("copy_policy_no_${account.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.copy),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // Next Payment Reminder Badge (if set)
            if (account.nextPaymentDate.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (reminderInfo.status) {
                        PaymentReminderStatus.OVERDUE -> Color(0xFFFFEBEE)
                        PaymentReminderStatus.DUE_TODAY,
                        PaymentReminderStatus.DUE_SOON -> Color(0xFFFFF8E1)
                        PaymentReminderStatus.UPCOMING,
                        PaymentReminderStatus.COMPLETED -> Color(0xFFE8F5E9)
                        PaymentReminderStatus.NOT_SET -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (reminderInfo.status) {
                                PaymentReminderStatus.OVERDUE -> Icons.Default.Warning
                                PaymentReminderStatus.DUE_TODAY -> Icons.Default.Alarm
                                PaymentReminderStatus.DUE_SOON -> Icons.Default.HourglassBottom
                                PaymentReminderStatus.COMPLETED -> Icons.Default.CheckCircle
                                else -> Icons.Default.CalendarToday
                            },
                            contentDescription = null,
                            tint = when (reminderInfo.status) {
                                PaymentReminderStatus.OVERDUE -> Color(0xFFC62828)
                                PaymentReminderStatus.DUE_TODAY,
                                PaymentReminderStatus.DUE_SOON -> Color(0xFFE65100)
                                PaymentReminderStatus.UPCOMING,
                                PaymentReminderStatus.COMPLETED -> Color(0xFF2E7D32)
                                PaymentReminderStatus.NOT_SET -> secondaryInk
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (reminderInfo.status == PaymentReminderStatus.COMPLETED) {
                                stringResource(R.string.policy_matured)
                            } else {
                                val duePrefix = if (currentLanguage == SettingsManager.LANG_KANNADA) "ಮುಂದಿನ ಗಡುವು" else "Next Due"
                                val localizedStatus = if (currentLanguage == SettingsManager.LANG_KANNADA) {
                                    when (reminderInfo.status) {
                                        PaymentReminderStatus.OVERDUE -> "${reminderInfo.daysDifference} ದಿನಗಳ ಗಡುವು ಮೀರಿದೆ"
                                        PaymentReminderStatus.DUE_TODAY -> "ಇಂದೇ ಬಾಕಿ"
                                        PaymentReminderStatus.DUE_SOON, PaymentReminderStatus.UPCOMING -> "${reminderInfo.daysDifference} ದಿನಗಳಲ್ಲಿ ಬಾಕಿ"
                                        PaymentReminderStatus.COMPLETED -> stringResource(R.string.policy_matured)
                                        PaymentReminderStatus.NOT_SET -> ""
                                    }
                                } else {
                                    reminderInfo.statusMessage
                                }
                                "$duePrefix: ${account.nextPaymentDate}" + if (localizedStatus.isNotBlank()) " ($localizedStatus)" else ""
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = when (reminderInfo.status) {
                                PaymentReminderStatus.OVERDUE -> Color(0xFFC62828)
                                PaymentReminderStatus.DUE_TODAY,
                                PaymentReminderStatus.DUE_SOON -> Color(0xFFE65100)
                                PaymentReminderStatus.UPCOMING,
                                PaymentReminderStatus.COMPLETED -> Color(0xFF2E7D32)
                                PaymentReminderStatus.NOT_SET -> inkColor
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Last Payment Date
            if (account.lastPaymentDate.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (if (currentLanguage == SettingsManager.LANG_KANNADA) "ಕೊನೆಯ ಪಾವತಿ: " else "Last Paid: ") + account.lastPaymentDate,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = secondaryInk
                )
            }
        }
    }
}

@Composable
fun LicProfileDialog(
    agentNumber: String = SettingsManager.DEFAULT_AGENT_CODE,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Stock Profile Avatar Placeholder Circle
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.profile),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name: Bhavana Moger
                Text(
                    text = "Bhavana Moger",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "LIC Financial Advisor",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Agent Number Card (Fixed, Copy Only)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.agent_number),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = agentNumber,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Copy Icon Button
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Agent Number", agentNumber)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, context.getString(R.string.agent_number_copied), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("copy_agent_number_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.copy),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.cancel), fontFamily = FontFamily.Serif)
                }
            }
        }
    }
}

