package com.chiranth7.regibook.features.lic.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chiranth7.regibook.R
import com.chiranth7.regibook.features.lic.data.LicAccount
import com.chiranth7.regibook.features.lic.util.PaymentReminderHelper
import com.chiranth7.regibook.util.KannadaNameHelper
import com.chiranth7.regibook.util.SettingsManager

@Composable
fun MarkPaidDialog(
    account: LicAccount,
    currentLanguage: String,
    onDismiss: () -> Unit,
    onConfirm: (newNextPaymentDate: String, newLastPaymentDate: String) -> Unit
) {
    var isFinalPayment by remember { mutableStateOf(false) }

    val advanceResult = remember(account, isFinalPayment) {
        PaymentReminderHelper.calculateNextPaymentAdvance(
            currentNextDate = account.nextPaymentDate,
            totalYears = account.totalYears,
            forceMatured = isFinalPayment,
            lastPaymentDate = account.lastPaymentDate,
            storedLastPremiumDate = account.lastPremiumDate,
            storedCommencementDate = account.commencementDate,
            premiumAmount = account.premiumAmount
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("mark_paid_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.mark_paid_confirm_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Account summary box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = account.getDisplayName(currentLanguage),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = (if (currentLanguage == SettingsManager.LANG_KANNADA) "ಪಾಲಿಸಿ: " else "Policy: ") + account.policyNumber +
                                if (account.policyName.isNotBlank()) " • ${KannadaNameHelper.formatDisplayName(account.policyName, currentLanguage)}" else "",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (account.premiumAmount.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = (if (currentLanguage == SettingsManager.LANG_KANNADA) "ಪ್ರೀಮಿಯಂ: " else "Premium: ") + account.premiumAmount,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Date transition preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.current_due_date),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = account.nextPaymentDate.ifBlank { "-" },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(18.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.new_next_due_date),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (advanceResult.isMatured) {
                                stringResource(R.string.policy_matured)
                            } else {
                                advanceResult.newNextPaymentDate
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Recorded payment date
                Text(
                    text = "${stringResource(R.string.payment_date)}: ${advanceResult.newLastPaymentDate}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val scheduleAfter = remember(account, advanceResult) {
                    PaymentReminderHelper.calculatePolicySchedule(
                        totalYears = account.totalYears,
                        nextPaymentDate = advanceResult.newNextPaymentDate,
                        lastPaymentDate = advanceResult.newLastPaymentDate,
                        storedCommencement = account.commencementDate,
                        storedLastPremium = account.lastPremiumDate,
                        storedMaturity = account.maturityDate,
                        premiumAmount = account.premiumAmount
                    )
                }

                if (scheduleAfter != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (scheduleAfter.isFullyPaid || advanceResult.isMatured) {
                            stringResource(R.string.all_premiums_paid)
                        } else if (scheduleAfter.remainingPaymentsCount == 1) {
                            stringResource(R.string.final_premium_due)
                        } else if (scheduleAfter.remainingYears > 0 && scheduleAfter.remainingMonths > 0) {
                            stringResource(
                                R.string.years_and_months_remaining_to_pay,
                                scheduleAfter.remainingYears,
                                scheduleAfter.remainingMonths,
                                scheduleAfter.remainingPaymentsCount
                            )
                        } else if (scheduleAfter.remainingMonths > 0) {
                            stringResource(
                                R.string.months_remaining_to_pay,
                                scheduleAfter.remainingMonths,
                                scheduleAfter.remainingPaymentsCount
                            )
                        } else {
                            stringResource(
                                R.string.years_remaining_to_pay,
                                scheduleAfter.remainingYears,
                                scheduleAfter.remainingPaymentsCount
                            )
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = if (advanceResult.isMatured) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Final payment checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isFinalPayment,
                        onCheckedChange = { isFinalPayment = it },
                        modifier = Modifier.testTag("final_payment_checkbox")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.final_payment_matured),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_mark_paid_button")
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(
                                advanceResult.newNextPaymentDate,
                                advanceResult.newLastPaymentDate
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.testTag("confirm_mark_paid_button")
                    ) {
                        Text(
                            text = stringResource(R.string.confirm_payment),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
