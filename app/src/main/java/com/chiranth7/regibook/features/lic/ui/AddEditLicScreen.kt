package com.chiranth7.regibook.features.lic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chiranth7.regibook.R
import com.chiranth7.regibook.features.lic.viewmodel.LicViewModel

@Composable
fun AddEditLicScreen(
    viewModel: LicViewModel,
    onNavigateBack: () -> Unit,
    onSaved: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val isEditing = formState.editingId != null

    val inkColor = MaterialTheme.colorScheme.onBackground
    val secondaryInk = MaterialTheme.colorScheme.onSurfaceVariant
    val nameRequiredMsg = stringResource(R.string.validation_name_required)
    val policyRequiredMsg = stringResource(R.string.enter_policy_number)

    val popularPolicies = listOf("Jeevan Labh", "Jeevan Anand", "Jeevan Umang", "Bima Jyoti", "Tech Term")
    val popularTerms = listOf("10 Years", "15 Years", "20 Years", "25 Years")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
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
                    text = if (isEditing) stringResource(R.string.edit_lic_policy) else stringResource(R.string.add_lic_policy),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    ),
                    color = inkColor,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = {
                        viewModel.saveAccount(
                            nameRequiredError = nameRequiredMsg,
                            policyRequiredError = policyRequiredMsg,
                            onSuccess = onSaved
                        )
                    },
                    modifier = Modifier.testTag("save_lic_button")
                ) {
                    if (formState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = inkColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.save),
                            tint = inkColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Name
                Text(
                    text = stringResource(R.string.name) + " *",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = inkColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = formState.name,
                    onValueChange = { viewModel.onNameChanged(it) },
                    placeholder = { Text(stringResource(R.string.enter_name)) },
                    isError = formState.nameError != null,
                    supportingText = {
                        formState.nameError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lic_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Policy Number
                Text(
                    text = stringResource(R.string.policy_number) + " *",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = inkColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = formState.policyNumber,
                    onValueChange = { viewModel.onPolicyNumberChanged(it) },
                    placeholder = { Text(stringResource(R.string.enter_policy_number)) },
                    isError = formState.policyNumberError != null,
                    supportingText = {
                        formState.policyNumberError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lic_policy_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Policy Name
                Text(
                    text = stringResource(R.string.policy_name),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = inkColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = formState.policyName,
                    onValueChange = { viewModel.onPolicyNameChanged(it) },
                    placeholder = { Text(stringResource(R.string.enter_policy_name)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lic_policy_name_input")
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Quick chips for popular policy names
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    popularPolicies.forEach { policy ->
                        FilterChip(
                            selected = formState.policyName == policy,
                            onClick = { viewModel.onPolicyNameChanged(policy) },
                            label = { Text(policy, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Total Years (Policy Term)
                Text(
                    text = stringResource(R.string.total_years),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = inkColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = formState.totalYears,
                    onValueChange = { viewModel.onTotalYearsChanged(it) },
                    placeholder = { Text(stringResource(R.string.enter_total_years)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lic_total_years_input")
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    popularTerms.forEach { term ->
                        FilterChip(
                            selected = formState.totalYears == term,
                            onClick = { viewModel.onTotalYearsChanged(term) },
                            label = { Text(term, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Next Payment Date (Reminder)
                Text(
                    text = stringResource(R.string.next_payment_date) + " (Reminder)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = formState.nextPaymentDate,
                    onValueChange = { viewModel.onNextPaymentDateChanged(it) },
                    placeholder = { Text(stringResource(R.string.enter_next_payment_date)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lic_next_payment_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Last Payment Date
                Text(
                    text = stringResource(R.string.last_payment_date),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = secondaryInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = formState.lastPaymentDate,
                    onValueChange = { viewModel.onLastPaymentDateChanged(it) },
                    placeholder = { Text(stringResource(R.string.enter_last_payment_date)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lic_date_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Phone Number
                Text(
                    text = stringResource(R.string.phone_number),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = secondaryInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = formState.phoneNumber,
                    onValueChange = { viewModel.onPhoneNumberChanged(it) },
                    placeholder = { Text(stringResource(R.string.enter_phone)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lic_phone_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Premium Amount
                Text(
                    text = stringResource(R.string.premium_amount),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = secondaryInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = formState.premiumAmount,
                    onValueChange = { viewModel.onPremiumAmountChanged(it) },
                    placeholder = { Text(stringResource(R.string.enter_premium_amount)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lic_premium_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Save button
                Surface(
                    onClick = {
                        viewModel.saveAccount(
                            nameRequiredError = nameRequiredMsg,
                            policyRequiredError = policyRequiredMsg,
                            onSuccess = onSaved
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    color = inkColor,
                    contentColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_lic_action_button")
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
