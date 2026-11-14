package ru.nto.guestmanagement.mobile.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.nto.guestmanagement.mobile.R
import ru.nto.guestmanagement.mobile.data.RequestStatus
import ru.nto.guestmanagement.mobile.data.RequestSummary
import ru.nto.guestmanagement.mobile.data.formatDate
import ru.nto.guestmanagement.mobile.data.formatInterval
import ru.nto.guestmanagement.mobile.di.AppContainer
import ru.nto.guestmanagement.mobile.ui.TestIds
import ru.nto.guestmanagement.mobile.ui.common.StatusLabel
import ru.nto.guestmanagement.mobile.ui.common.containerViewModel
import ru.nto.guestmanagement.mobile.ui.refresh.ResumeEffect

private data class FilterOption(val tag: String, val label: String, val status: RequestStatus?)

@Composable
private fun filterOptions() = listOf(
    FilterOption(TestIds.REQUESTS_FILTER_ALL, stringResource(R.string.filter_all), null),
    FilterOption(TestIds.REQUESTS_FILTER_PENDING, stringResource(R.string.filter_pending), RequestStatus.PENDING),
    FilterOption(TestIds.REQUESTS_FILTER_APPROVED, stringResource(R.string.filter_approved), RequestStatus.APPROVED),
    FilterOption(TestIds.REQUESTS_FILTER_REJECTED, stringResource(R.string.filter_rejected), RequestStatus.REJECTED),
    FilterOption(TestIds.REQUESTS_FILTER_CANCELLED, stringResource(R.string.filter_cancelled), RequestStatus.CANCELLED),
)

@Composable
fun RequestListScreen(
    container: AppContainer,
    onOpenDetails: (Long) -> Unit,
    onAddRequest: () -> Unit,
    onLogout: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val viewModel: RequestListViewModel = containerViewModel { RequestListViewModel(container) }

    ResumeEffect { viewModel.resume(onSessionExpired) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.list_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Row {
                    if (!viewModel.initialError) {
                        TextButton(
                            onClick = { viewModel.refresh(onSessionExpired) },
                            modifier = Modifier.testTag(TestIds.REQUESTS_REFRESH),
                        ) {
                            Text(stringResource(R.string.action_refresh))
                        }
                    }
                    TextButton(
                        onClick = { viewModel.logout(onLogout) },
                        modifier = Modifier.testTag(TestIds.REQUESTS_LOGOUT),
                    ) {
                        Text(stringResource(R.string.action_logout))
                    }
                }
            }

            if (viewModel.initialError) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.list_error),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag(TestIds.REQUESTS_ERROR),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.refresh(onSessionExpired) },
                        modifier = Modifier.testTag(TestIds.REQUESTS_REFRESH),
                    ) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            } else {

            val filters = filterOptions()
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                filters.forEach { option ->
                    FilterChip(
                        selected = viewModel.selectedFilter == option.status,
                        onClick = { viewModel.selectFilter(option.status, onSessionExpired) },
                        label = { Text(option.label) },
                        modifier = Modifier.testTag(option.tag),
                    )
                }
            }

            Button(
                onClick = onAddRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag(TestIds.REQUESTS_ADD),
            ) {
                Text(stringResource(R.string.action_add))
            }

            if (viewModel.offline) {
                Text(
                    text = stringResource(R.string.offline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag(TestIds.REQUESTS_OFFLINE),
                )
            }

            when {
                viewModel.loading && !viewModel.loadedOnce -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                viewModel.loadedOnce && viewModel.items.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.list_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag(TestIds.REQUESTS_EMPTY),
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(viewModel.items) { index, item ->
                            RequestCard(
                                item = item,
                                index = index,
                                onOpenDetails = onOpenDetails,
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun RequestCard(
    item: RequestSummary,
    index: Int,
    onOpenDetails: (Long) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestIds.requestsPos(index))
            // Собственное onClick-действие в семантике вместо clickable:
            // семантика потомков не сливается, вложенные теги остаются доступны тестам.
            .semantics {
                onClick(label = "Открыть заявку") {
                    onOpenDetails(item.id)
                    true
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = item.fullName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag(TestIds.REQUEST_NAME),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatDate(item.validFrom),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag(TestIds.REQUEST_DATE),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatInterval(item.validFrom, item.validTo),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag(TestIds.REQUEST_TIME),
                )
                Spacer(modifier = Modifier.weight(1f))
                StatusLabel(status = item.status, tag = TestIds.REQUEST_STATUS)
            }
        }
    }
}
