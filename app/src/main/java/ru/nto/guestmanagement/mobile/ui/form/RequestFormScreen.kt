package ru.nto.guestmanagement.mobile.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import ru.nto.guestmanagement.mobile.R
import ru.nto.guestmanagement.mobile.di.AppContainer
import ru.nto.guestmanagement.mobile.ui.TestIds
import ru.nto.guestmanagement.mobile.ui.common.containerViewModel

@Composable
fun RequestFormScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onCreated: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val viewModel: RequestFormViewModel = containerViewModel { RequestFormViewModel(container) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.testTag(TestIds.FORM_BACK),
                ) {
                    Text(stringResource(R.string.action_back))
                }
            }
            Text(
                text = stringResource(R.string.form_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            FormField(
                value = viewModel.fullName,
                onValueChange = viewModel::onFullNameChange,
                label = stringResource(R.string.form_full_name),
                fieldTag = TestIds.FORM_FULL_NAME,
                errorTag = TestIds.FORM_ERROR_FULL_NAME,
                errorRes = viewModel.fieldErrorRes("fullName"),
            )
            FormField(
                value = viewModel.phone,
                onValueChange = viewModel::onPhoneChange,
                label = stringResource(R.string.form_phone),
                fieldTag = TestIds.FORM_PHONE,
                errorTag = TestIds.FORM_ERROR_PHONE,
                errorRes = viewModel.fieldErrorRes("phone"),
                keyboardType = KeyboardType.Phone,
            )
            FormField(
                value = viewModel.organization,
                onValueChange = viewModel::onOrganizationChange,
                label = stringResource(R.string.form_organization),
                fieldTag = TestIds.FORM_ORGANIZATION,
                errorTag = TestIds.FORM_ERROR_ORGANIZATION,
                errorRes = viewModel.fieldErrorRes("organization"),
            )
            FormField(
                value = viewModel.purpose,
                onValueChange = viewModel::onPurposeChange,
                label = stringResource(R.string.form_purpose),
                fieldTag = TestIds.FORM_PURPOSE,
                errorTag = TestIds.FORM_ERROR_PURPOSE,
                errorRes = viewModel.fieldErrorRes("purpose"),
            )
            FormField(
                value = viewModel.date,
                onValueChange = viewModel::onDateChange,
                label = stringResource(R.string.form_date),
                fieldTag = TestIds.FORM_DATE,
                errorTag = TestIds.FORM_ERROR_DATE,
                errorRes = viewModel.fieldErrorRes("date", null),
                keyboardType = KeyboardType.Number,
            )
            FormField(
                value = viewModel.timeFrom,
                onValueChange = viewModel::onTimeFromChange,
                label = stringResource(R.string.form_time_from),
                fieldTag = TestIds.FORM_TIME_FROM,
                errorTag = TestIds.FORM_ERROR_VALID_FROM,
                errorRes = viewModel.fieldErrorRes("validFrom", "validFrom"),
                keyboardType = KeyboardType.Number,
            )
            FormField(
                value = viewModel.timeTo,
                onValueChange = viewModel::onTimeToChange,
                label = stringResource(R.string.form_time_to),
                fieldTag = TestIds.FORM_TIME_TO,
                errorTag = TestIds.FORM_ERROR_VALID_TO,
                errorRes = viewModel.fieldErrorRes("validTo", "validTo"),
                keyboardType = KeyboardType.Number,
            )

            viewModel.commonError?.let { messageRes ->
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .testTag(TestIds.FORM_ERROR_COMMON),
                )
            }

            if (viewModel.loading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.testTag(TestIds.FORM_LOADING),
                    )
                }
            }

            Button(
                onClick = { viewModel.submit(onCreated, onSessionExpired) },
                enabled = !viewModel.loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 16.dp)
                    .testTag(TestIds.FORM_SUBMIT),
            ) {
                Text(stringResource(R.string.form_submit))
            }
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    fieldTag: String,
    errorTag: String,
    errorRes: Pair<Int?, String?>?,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        isError = errorRes != null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .testTag(fieldTag),
    )
    errorRes?.let { (clientRes, serverMessage) ->
        Text(
            text = serverMessage ?: stringResource(clientRes!!),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .testTag(errorTag),
        )
    }
}
