package com.chiranth7.regibook.features.lic.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chiranth7.regibook.R
import com.chiranth7.regibook.features.drawer.RegisterDrawerContent
import com.chiranth7.regibook.features.lic.data.LicAccount
import com.chiranth7.regibook.features.lic.util.PaymentReminderHelper
import com.chiranth7.regibook.features.lic.util.PaymentReminderStatus
import com.chiranth7.regibook.features.lic.viewmodel.LicViewModel
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
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            kotlinx.coroutines.delay(100)
            runCatching { focusRequester.requestFocus() }
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val inkColor = MaterialTheme.colorScheme.onBackground
    val secondaryInk = MaterialTheme.colorScheme.onSurfaceVariant

    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        viewModel.updateSearchQuery("")
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
                AnimatedContent(
                    targetState = isSearchActive,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(150))
                    },
                    label = "LicTopBarSearchTransition"
                ) { searchActive ->
                    if (searchActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    isSearchActive = false
                                    viewModel.updateSearchQuery("")
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("back_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = inkColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .padding(end = 4.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(21.dp)
                                    )
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = secondaryInk,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.updateSearchQuery(it) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester)
                                        .testTag("search_input"),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = inkColor,
                                        fontSize = 16.sp
                                    ),
                                    cursorBrush = SolidColor(inkColor),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    text = stringResource(R.string.search_hint),
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = secondaryInk.copy(alpha = 0.5f),
                                                        fontSize = 15.sp
                                                    )
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )

                                IconButton(
                                    onClick = {
                                        if (searchQuery.isNotEmpty()) {
                                            viewModel.updateSearchQuery("")
                                        } else {
                                            isSearchActive = false
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("close_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.cancel),
                                        tint = secondaryInk,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    } else {
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

                            IconButton(
                                onClick = { isSearchActive = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("search_icon_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.search),
                                    tint = inkColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
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
                                text = if (searchQuery.isNotEmpty()) {
                                    stringResource(R.string.search) + ": " + stringResource(R.string.empty_lic)
                                } else {
                                    stringResource(R.string.empty_lic)
                                },
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
                                    onClick = {
                                        viewModel.selectAccount(account.id)
                                        onNavigateToDetails(account.id)
                                    },
                                    onCall = if (account.phoneNumber.isNotEmpty()) {
                                        {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${account.phoneNumber.trim()}")
                                            }
                                            context.startActivity(intent)
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern, minimalist white card item for LIC Policy
 */
@Composable
private fun LicPolicyCard(
    account: LicAccount,
    onClick: () -> Unit,
    onCall: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
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
            // Top Row: Name and Call action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = inkColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (account.policyName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = account.policyName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (onCall != null) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("call_policy_${account.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = stringResource(R.string.call),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tags row: Policy Number & Total Years
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "No: ${account.policyNumber}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = inkColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (account.totalYears.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Term: ${account.totalYears}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp
                            ),
                            color = secondaryInk,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Next Payment Reminder Badge (if set)
            if (account.nextPaymentDate.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (reminderInfo.status) {
                        PaymentReminderStatus.OVERDUE -> Color(0xFFFFEBEE)
                        PaymentReminderStatus.DUE_TODAY,
                        PaymentReminderStatus.DUE_SOON -> Color(0xFFFFF8E1)
                        PaymentReminderStatus.UPCOMING -> Color(0xFFE8F5E9)
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
                                else -> Icons.Default.CalendarToday
                            },
                            contentDescription = null,
                            tint = when (reminderInfo.status) {
                                PaymentReminderStatus.OVERDUE -> Color(0xFFC62828)
                                PaymentReminderStatus.DUE_TODAY,
                                PaymentReminderStatus.DUE_SOON -> Color(0xFFE65100)
                                PaymentReminderStatus.UPCOMING -> Color(0xFF2E7D32)
                                PaymentReminderStatus.NOT_SET -> secondaryInk
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Next Due: ${account.nextPaymentDate}" + if (reminderInfo.statusMessage.isNotBlank()) " (${reminderInfo.statusMessage})" else "",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = when (reminderInfo.status) {
                                PaymentReminderStatus.OVERDUE -> Color(0xFFC62828)
                                PaymentReminderStatus.DUE_TODAY,
                                PaymentReminderStatus.DUE_SOON -> Color(0xFFE65100)
                                PaymentReminderStatus.UPCOMING -> Color(0xFF2E7D32)
                                PaymentReminderStatus.NOT_SET -> inkColor
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Last Payment Date row
            if (account.lastPaymentDate.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Last Paid: ${account.lastPaymentDate}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                        color = secondaryInk
                    )
                }
            }
        }
    }
}
