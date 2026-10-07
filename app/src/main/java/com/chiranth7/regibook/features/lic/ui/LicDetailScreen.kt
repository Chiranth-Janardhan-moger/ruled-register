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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.chiranth7.regibook.util.KannadaNameHelper
import com.chiranth7.regibook.util.SettingsManager

@Composable
fun LicDetailScreen(
    accountId: Long,
    viewModel: LicViewModel,
    settingsManager: SettingsManager,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (LicAccount) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LaunchedEffect(accountId) {
        viewModel.selectAccount(accountId)
    }

    val account by viewModel.selectedAccount.collectAsStateWithLifecycle()
    val currentLang by settingsManager.currentLanguage.collectAsStateWithLifecycle()
    var showMarkPaidDialog by remember { mutableStateOf(false) }

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
                                PaymentReminderStatus.UPCOMING,
                                PaymentReminderStatus.COMPLETED -> Color(0xFFF0FDF4)
                                PaymentReminderStatus.NOT_SET -> Color(0xFFF8F9FA)
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            when (reminderInfo.status) {
                                PaymentReminderStatus.OVERDUE -> Color(0xFFFFCDD2)
                                PaymentReminderStatus.DUE_TODAY,
                                PaymentReminderStatus.DUE_SOON -> Color(0xFFFFE082)
                                PaymentReminderStatus.UPCOMING,
                                PaymentReminderStatus.COMPLETED -> Color(0xFFC8E6C9)
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
                                        PaymentReminderStatus.UPCOMING,
                                        PaymentReminderStatus.COMPLETED -> Icons.Default.CheckCircle
                                        PaymentReminderStatus.NOT_SET -> Icons.Default.EventNote
                                    },
                                    contentDescription = null,
                                    tint = when (reminderInfo.status) {
                                        PaymentReminderStatus.OVERDUE -> Color(0xFFD32F2F)
                                        PaymentReminderStatus.DUE_TODAY -> Color(0xFFE65100)
                                        PaymentReminderStatus.DUE_SOON -> Color(0xFFF57C00)
                                        PaymentReminderStatus.UPCOMING,
                                        PaymentReminderStatus.COMPLETED -> Color(0xFF2E7D32)
                                        PaymentReminderStatus.NOT_SET -> secondaryInk
                                    },
                                    modifier = Modifier.size(22.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (reminderInfo.status == PaymentReminderStatus.COMPLETED) {
                                            stringResource(R.string.policy_matured)
                                        } else {
                                            stringResource(R.string.next_payment_reminder)
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = when (reminderInfo.status) {
                                            PaymentReminderStatus.OVERDUE -> Color(0xFFC62828)
                                            PaymentReminderStatus.DUE_TODAY -> Color(0xFFD84315)
                                            PaymentReminderStatus.DUE_SOON -> Color(0xFFE65100)
                                            PaymentReminderStatus.UPCOMING,
                                            PaymentReminderStatus.COMPLETED -> Color(0xFF1B5E20)
                                            PaymentReminderStatus.NOT_SET -> inkColor
                                        }
                                    )

                                    if (acc.nextPaymentDate.isNotBlank()) {
                                        Text(
                                            text = if (reminderInfo.status == PaymentReminderStatus.COMPLETED) {
                                                stringResource(R.string.policy_matured)
                                            } else {
                                                val localizedStatus = if (currentLang == SettingsManager.LANG_KANNADA) {
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
                                                if (localizedStatus.isNotBlank()) {
                                                    "${reminderInfo.formattedDueDate} • $localizedStatus"
                                                } else {
                                                    acc.nextPaymentDate
                                                }
                                            },
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            ),
                                            color = when (reminderInfo.status) {
                                                PaymentReminderStatus.OVERDUE -> Color(0xFFB71C1C)
                                                PaymentReminderStatus.DUE_TODAY -> Color(0xFFBF360C)
                                                PaymentReminderStatus.DUE_SOON -> Color(0xFFE65100)
                                                PaymentReminderStatus.UPCOMING,
                                                PaymentReminderStatus.COMPLETED -> Color(0xFF1B5E20)
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

                            if (reminderInfo.status != PaymentReminderStatus.COMPLETED && acc.nextPaymentDate.isNotBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { showMarkPaidDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E7D32)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("mark_paid_detail_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.mark_as_paid),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
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
                                text = KannadaNameHelper.formatDisplayName(acc.name, currentLang),
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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                                        ) {
                                            Text(
                                                text = acc.policyNumber,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 15.sp
                                                ),
                                                color = inkColor
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText("Policy Number", acc.policyNumber)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, context.getString(R.string.policy_number_copied), Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("copy_policy_number_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = stringResource(R.string.copy),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
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
                                                text = KannadaNameHelper.formatDisplayName(acc.policyName, currentLang),
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

                            // Sum Assured
                            if (acc.address.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = outlineBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = stringResource(R.string.sum_assured),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryInk
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                ) {
                                    Text(
                                        text = acc.address,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showMarkPaidDialog && account != null) {
            MarkPaidDialog(
                account = account!!,
                currentLanguage = currentLang,
                onDismiss = { showMarkPaidDialog = false },
                onConfirm = { newNext, newLast ->
                    showMarkPaidDialog = false
                    viewModel.markPolicyAsPaid(account!!, newNext, newLast) {
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

