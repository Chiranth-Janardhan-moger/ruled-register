package com.chiranth7.regibook.features.pigmi.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
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
import com.chiranth7.regibook.features.pigmi.viewmodel.PigmiViewModel
import com.chiranth7.regibook.ui.components.NotebookPaperBackground
import com.chiranth7.regibook.ui.components.NotebookRowHeight

@Composable
fun AddEditPigmiScreen(
    viewModel: PigmiViewModel,
    onNavigateBack: () -> Unit,
    onSaved: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val isEditing = formState.editingId != null

    val inkColor = MaterialTheme.colorScheme.onBackground
    val secondaryInk = MaterialTheme.colorScheme.onSurfaceVariant
    val srNoRequiredMsg = stringResource(R.string.validation_sr_no_required)
    val nameRequiredMsg = stringResource(R.string.validation_name_required)

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        NotebookPaperBackground(
            lineSpacing = NotebookRowHeight,
            topStartOffset = 56.dp,
            showMarginLine = false,
            showRuledLines = false
        )

        Column(modifier = Modifier.fillMaxSize()) {
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
                    text = if (isEditing) stringResource(R.string.edit) else stringResource(R.string.add_person),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    ),
                    color = secondaryInk,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = {
                        viewModel.saveAccount(
                            srNoRequiredError = srNoRequiredMsg,
                            nameRequiredError = nameRequiredMsg,
                            onSuccess = onSaved
                        )
                    },
                    modifier = Modifier.testTag("save_pigmi_button")
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
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.sr_no),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = secondaryInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = formState.srNo,
                    onValueChange = { viewModel.onSrNoChanged(it) },
                    placeholder = { Text(stringResource(R.string.enter_sr_no)) },
                    isError = formState.srNoError != null,
                    supportingText = {
                        formState.srNoError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = inkColor,
                        unfocusedTextColor = inkColor,
                        focusedBorderColor = inkColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pigmi_sr_no_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.name),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = secondaryInk
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = inkColor,
                        unfocusedTextColor = inkColor,
                        focusedBorderColor = inkColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pigmi_name_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.phone_number),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = inkColor,
                        unfocusedTextColor = inkColor,
                        focusedBorderColor = inkColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pigmi_phone_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.address),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = secondaryInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = formState.address,
                    onValueChange = { viewModel.onAddressChanged(it) },
                    placeholder = { Text(stringResource(R.string.enter_address)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = inkColor,
                        unfocusedTextColor = inkColor,
                        focusedBorderColor = inkColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pigmi_address_input")
                )

                Spacer(modifier = Modifier.height(28.dp))

                Surface(
                    onClick = {
                        viewModel.saveAccount(
                            srNoRequiredError = srNoRequiredMsg,
                            nameRequiredError = nameRequiredMsg,
                            onSuccess = onSaved
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    color = inkColor,
                    contentColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_pigmi_action_button")
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
