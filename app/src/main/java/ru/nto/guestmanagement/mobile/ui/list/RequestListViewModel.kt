package ru.nto.guestmanagement.mobile.ui.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.nto.guestmanagement.mobile.data.ApiResult
import ru.nto.guestmanagement.mobile.data.RequestStatus
import ru.nto.guestmanagement.mobile.data.RequestSummary
import ru.nto.guestmanagement.mobile.data.RequestsApi
import ru.nto.guestmanagement.mobile.di.AppContainer
import ru.nto.guestmanagement.mobile.ui.refresh.RefreshGate

class RequestListViewModel(
    container: AppContainer,
) : ViewModel() {
    private val api = RequestsApi(container.httpClient)
    private val session = container.session
    private val gate = RefreshGate()

    var items by mutableStateOf<List<RequestSummary>>(emptyList())
        private set
    var selectedFilter by mutableStateOf<RequestStatus?>(null)
        private set
    /** Пока запущен любой запрос (в т. ч. первичная загрузка), обновления не дублируются. */
    var loading by mutableStateOf(false)
        private set
    /** Хотя бы одна успешная загрузка была — дальше список показываем даже при ошибках. */
    var loadedOnce by mutableStateOf(false)
        private set
    /** Первоначальная ошибка: данных нет, показываем сообщение и кнопку повтора. */
    var initialError by mutableStateOf(false)
        private set
    /** Ошибка повторного обновления при сохранённых данных: список остаётся, показываем «Нет связи». */
    var offline by mutableStateOf(false)
        private set

    /** Первое и каждое возвращение на экран — один GET (повторное нажатие во время запроса игнорируется). */
    fun resume(onSessionExpired: () -> Unit) = request(onSessionExpired)

    fun refresh(onSessionExpired: () -> Unit) = request(onSessionExpired)

    fun selectFilter(filter: RequestStatus?, onSessionExpired: () -> Unit) {
        if (selectedFilter == filter) return
        gate.cancel()
        loading = false
        selectedFilter = filter
        request(onSessionExpired)
    }

    fun logout(onLogout: () -> Unit) {
        gate.cancel()
        viewModelScope.launch {
            session.clear()
            onLogout()
        }
    }

    private fun request(onSessionExpired: () -> Unit) {
        gate.launch(viewModelScope) {
            loading = true
            try {
                val code = session.current() ?: run {
                    onSessionExpired()
                    return@launch
                }
                when (val result = api.list(code, selectedFilter)) {
                    is ApiResult.Success -> {
                        items = result.value
                        loadedOnce = true
                        initialError = false
                        offline = false
                    }
                    ApiResult.Unauthorized -> {
                        session.clear()
                        onSessionExpired()
                    }
                    else -> {
                        if (loadedOnce) offline = true else initialError = true
                    }
                }
            } finally {
                loading = false
            }
        }
    }
}
