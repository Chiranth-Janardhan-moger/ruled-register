package com.chiranth7.regibook.features.lic.ui

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chiranth7.regibook.R
import com.chiranth7.regibook.features.lic.data.LicAccount
import com.chiranth7.regibook.features.lic.util.PaymentReminderHelper
import com.chiranth7.regibook.features.lic.util.PaymentReminderStatus
import com.chiranth7.regibook.features.lic.viewmodel.LicViewModel
import com.chiranth7.regibook.util.SettingsManager

@Composable
fun LicDetailScreen(
    accountId: Long,
    viewModel: LicViewModel,
    settingsManager: SettingsManager,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (LicAccount) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(accountId) {
        viewModel.selectAccount(accountId)
    }

    val account by viewModel.selectedAccount.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val inkColor = MaterialTheme.colorScheme.onBackground
    val secondaryInk = MaterialTheme.colorScheme.onSurfaceVariant
    val cardBg = Color(0xFFFFFFFF)
    val outlineBorder = MaterialTheme.colorScheme.outlineVariant

    BackHandler {
        onNavigateBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
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
                    text = stringResource(R.string.lic_policy_details),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    ),
                    color = inkColor,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                account?.let { currentAccount ->
                    IconButton(
                        onClick = { onNavigateToEdit(currentAccount) },
                        modifier = Modifier.testTag("edit_lic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = inkColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag("delete_lic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            account?.let { acc ->
                val reminderInfo = remember(acc.nextPaymentDate) {
                    PaymentReminderHelper.calculateReminder(acc.nextPaymentDate)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Next Payment Reminder Card (Hero Banner)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (reminderInfo.status) {
                                PaymentReminderStatus.OVERDUE -> Color(0xFFFFF0F0)
                                PaymentReminderStatus.DUE_TODAY,
                                PaymentReminderStatus.DUE_SOON -> Color(0xFFFFF8E7)
                                PaymentReminderStatus.UPCOMING -> Color(0xFFF0FDF4)
                                PaymentReminderStatus.NOT_SET -> Color(0xFFF8F9FA)
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            when (reminderInfo.status) {
                                PaymentReminderStatus.OVERDUE -> Color(0xFFFFCDD2)
                                PaymentReminderStatus.DUE_TODAY,
                                PaymentReminderStatus.DUE_SOON -> Color(0xFFFFE082)
                                PaymentReminderStatus.UPCOMING -> Color(0xFFC8E6C9)
                                PaymentReminderStatus.NOT_SET -> outlineBorder
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_reminder_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = when (reminderInfo.status) {
                                        PaymentReminderStatus.OVERDUE -> Icons.Default.Warning
                                        PaymentReminderStatus.DUE_TODAY -> Icons.Default.Alarm
                                        PaymentReminderStatus.DUE_SOON -> Icons.Default.HourglassBottom
                                        PaymentReminderStatus.UPCOMING -> Icons.Default.CheckCircle
                                        PaymentReminderStatus.NOT_SET -> Icons.Default.EventNote
                                    },
                                    contentDescription = null,
                                    tint = when (reminderInfo.status) {
                                        PaymentReminderStatus.OVERDUE -> Color(0xFFD32F2F)
                                        PaymentReminderStatus.DUE_TODAY -> Color(0xFFE65100)
                                        PaymentReminderStatus.DUE_SOON -> Color(0xFFF57C00)
                                        PaymentReminderStatus.UPCOMING -> Color(0xFF2E7D32)
                                        PaymentReminderStatus.NOT_SET -> secondaryInk
                                    },
                                    modifier = Modifier.size(22.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.next_payment_reminder),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = when (reminderInfo.status) {
                                            PaymentReminderStatus.OVERDUE -> Color(0xFFC62828)
                                            PaymentReminderStatus.DUE_TODAY -> Color(0xFFD84315)
                                            PaymentReminderStatus.DUE_SOON -> Color(0xFFE65100)
                                            PaymentReminderStatus.UPCOMING -> Color(0xFF1B5E20)
                                            PaymentReminderStatus.NOT_SET -> inkColor
                                        }
                                    )

                                    if (acc.nextPaymentDate.isNotBlank()) {
                                        Text(
                                            text = if (reminderInfo.statusMessage.isNotBlank()) {
                                                "${reminderInfo.formattedDueDate} • ${reminderInfo.statusMessage}"
                                            } else {
                                                acc.nextPaymentDate
                                            },
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            ),
                                            color = when (reminderInfo.status) {
                                                PaymentReminderStatus.OVERDUE -> Color(0xFFB71C1C)
                                                PaymentReminderStatus.DUE_TODAY -> Color(0xFFBF360C)
                                                PaymentReminderStatus.DUE_SOON -> Color(0xFFE65100)
                                                PaymentReminderStatus.UPCOMING -> Color(0xFF1B5E20)
                                                PaymentReminderStatus.NOT_SET -> inkColor
                                            }
                                        )
                                    } else {
                                        Text(
                                            text = "No next payment date set",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = secondaryInk
                                        )
                                    }
                                }
                            }

                            // Reminder Action Buttons
                            if (acc.nextPaymentDate.isNotBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Add to Calendar
                                    OutlinedButton(
                                        onClick = {
                                            val dueDate = PaymentReminderHelper.parseDate(acc.nextPaymentDate)
                                            val calIntent = Intent(Intent.ACTION_INSERT).apply {
                                                data = CalendarContract.Events.CONTENT_URI
                                                putExtra(CalendarContract.Events.TITLE, "LIC Premium Due: ${acc.name}")
                                                putExtra(
                                                    CalendarContract.Events.DESCRIPTION,
                                                    "LIC Policy ${acc.policyName} (${acc.policyNumber}) premium of ${acc.premiumAmount} is due."
                                                )
                                                if (dueDate != null) {
                                                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, dueDate.time)
                                                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, dueDate.time + 3600000L)
                                                }
                                            }
                                            context.startActivity(calIntent)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = inkColor
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.add_to_calendar),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                            maxLines = 1
                                        )
                                    }

                                    // Send Reminder message
                                    OutlinedButton(
                                        onClick = {
                                            val reminderMsg = PaymentReminderHelper.generateReminderMessage(
                                                name = acc.name,
                                                policyNumber = acc.policyNumber,
                                                policyName = acc.policyName,
                                                nextPaymentDate = acc.nextPaymentDate,
                                                premiumAmount = acc.premiumAmount
                                            )
                                            val sendIntent = if (acc.phoneNumber.isNotBlank()) {
                                                Intent(Intent.ACTION_SENDTO).apply {
                                                    data = Uri.parse("smsto:${acc.phoneNumber.trim()}")
                                                    putExtra("sms_body", reminderMsg)
                                                }
                                            } else {
                                                Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, reminderMsg)
                                                }
                                            }
                                            context.startActivity(sendIntent)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = inkColor
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.send_reminder_client),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Main Policy Details Card (Minimalist White Card)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, outlineBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Policy Holder Name
                            Text(
                                text = stringResource(R.string.name),
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryInk
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = acc.name,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = inkColor
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Policy Number & Policy Name
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.policy_number),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = secondaryInk
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = acc.policyNumber,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp
                                            ),
                                            color = inkColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                if (acc.policyName.isNotBlank()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.policy_name),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = secondaryInk
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = acc.policyName,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 15.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = outlineBorder.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))

                            // Total Year & Premium Amount
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.total_years),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = secondaryInk
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = acc.totalYears.ifBlank { "-" },
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 16.sp
                                        ),
                                        color = inkColor
                                    )
                                }

                                if (acc.premiumAmount.isNotBlank()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.premium_amount),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = secondaryInk
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = acc.premiumAmount,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = outlineBorder.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))

                            // Phone Number & Quick Action Buttons
                            Text(
                                text = stringResource(R.string.phone_number),
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryInk
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = acc.phoneNumber.ifBlank { "Not provided" },
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = if (acc.phoneNumber.isNotBlank()) inkColor else secondaryInk,
                                    modifier = Modifier.weight(1f)
                                )

                                if (acc.phoneNumber.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${acc.phoneNumber.trim()}")
                                            }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .testTag("detail_call_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = stringResource(R.string.call),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("smsto:${acc.phoneNumber.trim()}")
                                            }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .testTag("detail_message_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Message,
                                            contentDescription = stringResource(R.string.message),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = outlineBorder.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))

                            // Dates Row: Next Payment Date & Last Payment Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.next_payment_date),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = secondaryInk
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = acc.nextPaymentDate.ifBlank { "-" },
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp
                                            ),
                                            color = inkColor
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.last_payment_date),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = secondaryInk
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = secondaryInk,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = acc.lastPaymentDate.ifBlank { "-" },
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 15.sp
                                            ),
                                            color = secondaryInk
                                        )
                                    }
                                }
                            }

                            // Address
                            if (acc.address.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = outlineBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = stringResource(R.string.address),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryInk
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = acc.address,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 15.sp
                                    ),
                                    color = inkColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.confirm_delete_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.confirm_delete_msg),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        account?.let { acc ->
                            viewModel.deleteAccount(acc) {
                                showDeleteDialog = false
                                onNavigateBack()
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
