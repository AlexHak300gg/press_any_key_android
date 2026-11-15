package ru.nto.guestmanagement.mobile.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.nto.guestmanagement.mobile.R
import ru.nto.guestmanagement.mobile.data.RequestStatus
import ru.nto.guestmanagement.mobile.data.formatDateTime
import ru.nto.guestmanagement.mobile.di.AppContainer
import ru.nto.guestmanagement.mobile.ui.TestIds
import ru.nto.guestmanagement.mobile.ui.common.StatusLabel
import ru.nto.guestmanagement.mobile.ui.common.containerViewModel
import ru.nto.guestmanagement.mobile.ui.refresh.ResumeEffect

@Composable
fun RequestDetailsScreen(
    container: AppContainer,
    requestId: Long,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val viewModel: RequestDetailsViewModel = containerViewModel {
        RequestDetailsViewModel(container, requestId)
    }

    ResumeEffect { viewModel.resume(onSessionExpired) }

    var confirmVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            // Кнопка отмены закреплена снизу: видна без прокрутки; только у заявки на рассмотрении.
            if (viewModel.details?.status == RequestStatus.PENDING) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Button(
                        onClick = { confirmVisible = true },
                        enabled = !viewModel.cancelling,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestIds.DETAILS_CANCEL),
                    ) {
                        Text(stringResource(R.string.details_cancel))
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.testTag(TestIds.DETAILS_BACK),
                ) {
                    Text(stringResource(R.string.action_back))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { viewModel.refresh(onSessionExpired) },
                    modifier = Modifier.testTag(TestIds.DETAILS_REFRESH),
                ) {
                    Text(stringResource(R.string.action_refresh))
                }
            }

            if (viewModel.initialError) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.details_error),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag(TestIds.DETAILS_ERROR),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.refresh(onSessionExpired) },
                        modifier = Modifier.testTag(TestIds.DETAILS_RETRY),
                    ) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
                return@Column
            }

            Text(
                text = stringResource(R.string.details_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            if (viewModel.offline) {
                Text(
                    text = stringResource(R.string.offline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag(TestIds.DETAILS_OFFLINE),
                )
            }

            if (viewModel.loading && viewModel.details == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            viewModel.details?.let { d ->
                DetailRow(R.string.details_label_id, d.id.toString(), TestIds.DETAILS_ID)
                DetailRow(R.string.details_label_name, d.fullName, TestIds.DETAILS_NAME)
                DetailRow(R.string.details_label_phone, d.phone, TestIds.DETAILS_PHONE)
                if (!d.organization.isNullOrBlank()) {
                    DetailRow(R.string.details_label_organization, d.organization, TestIds.DETAILS_ORGANIZATION)
                }
                DetailRow(R.string.details_label_purpose, d.purpose, TestIds.DETAILS_PURPOSE)
                DetailRow(R.string.details_label_from, formatDateTime(d.validFrom), TestIds.DETAILS_VALID_FROM)
                DetailRow(R.string.details_label_to, formatDateTime(d.validTo), TestIds.DETAILS_VALID_TO)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.details_label_status) + ": ",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    StatusLabel(status = d.status, tag = TestIds.DETAILS_STATUS)
                }
                DetailRow(R.string.details_label_created, formatDateTime(d.createdAt), TestIds.DETAILS_CREATED_AT)
                DetailRow(R.string.details_label_updated, formatDateTime(d.updatedAt), TestIds.DETAILS_UPDATED_AT)

                if (d.status == RequestStatus.REJECTED) {
                    Text(
                        text = stringResource(R.string.details_label_comment) + ":",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        text = d.comment.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(top = 2.dp, bottom = 8.dp)
                            .testTag(TestIds.DETAILS_COMMENT),
                    )
                }
            }

            viewModel.cancelError?.let { messageRes ->
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag(TestIds.DETAILS_CANCEL_ERROR),
                )
            }
        }
    }

    if (confirmVisible) {
        AlertDialog(
            onDismissRequest = { confirmVisible = false },
            title = { Text(stringResource(R.string.details_cancel_title)) },
            text = { Text(stringResource(R.string.details_cancel_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmVisible = false
                        viewModel.cancel(onSessionExpired)
                    },
                    modifier = Modifier.testTag(TestIds.DETAILS_CANCEL_CONFIRM),
                ) {
                    Text(stringResource(R.string.details_cancel))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmVisible = false },
                    modifier = Modifier.testTag(TestIds.DETAILS_CANCEL_DISMISS),
                ) {
                    Text(stringResource(R.string.action_back))
                }
            },
        )
    }
}

@Composable
private fun DetailRow(labelRes: Int, value: String, valueTag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = stringResource(labelRes) + ": ",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.6f).testTag(valueTag),
        )
    }
}
