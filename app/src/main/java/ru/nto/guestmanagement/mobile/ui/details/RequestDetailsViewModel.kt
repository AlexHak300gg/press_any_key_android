package ru.nto.guestmanagement.mobile.ui.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.nto.guestmanagement.mobile.R
import ru.nto.guestmanagement.mobile.di.AppContainer
import ru.nto.guestmanagement.mobile.data.ApiResult
import ru.nto.guestmanagement.mobile.data.RequestDetails
import ru.nto.guestmanagement.mobile.data.RequestStatus
import ru.nto.guestmanagement.mobile.data.RequestsApi
import ru.nto.guestmanagement.mobile.ui.refresh.RefreshGate

class RequestDetailsViewModel(
    container: AppContainer,
    private val requestId: Long,
) : ViewModel() {
    private val api = RequestsApi(container.httpClient)
    private val session = container.session
    private val gate = RefreshGate()

    var details by mutableStateOf<RequestDetails?>(null)
        private set
    /** Пока идёт обновление, повторный запрос не создаётся (RefreshGate). */
    var loading by mutableStateOf(false)
        private set
    /** Первоначальная ошибка (в т.ч. 404): данных нет, показываем сообщение и кнопку повтора. */
    var initialError by mutableStateOf(false)
        private set
    /** Ошибка повторного обновления при сохранённых данных: детали остаются, «Нет связи». */
    var offline by mutableStateOf(false)
        private set
    /** Ошибка отмены («Заявка уже рассмотрена» / сетевая); сбрасывается при успешной отмене. Хранится как id строки. */
    var cancelError by mutableStateOf<Int?>(null)
        private set
    var cancelling by mutableStateOf(false)
        private set

    fun resume(onSessionExpired: () -> Unit) = load(onSessionExpired)

    fun refresh(onSessionExpired: () -> Unit) = load(onSessionExpired)

    private fun load(onSessionExpired: () -> Unit) {
        gate.launch(viewModelScope) {
            loading = true
            try {
                val code = session.current() ?: run {
                    onSessionExpired()
                    return@launch
                }
                when (val result = api.get(code, requestId)) {
                    is ApiResult.Success -> {
                        details = result.value
                        initialError = false
                        offline = false
                    }
                    ApiResult.Unauthorized -> {
                        session.clear()
                        onSessionExpired()
                    }
                    else -> {
                        if (details == null) initialError = true else offline = true
                    }
                }
            } finally {
                loading = false
            }
        }
    }

    fun cancel(onSessionExpired: () -> Unit) {
        if (cancelling) return
        val current = details ?: return
        if (current.status != RequestStatus.PENDING) return
        cancelling = true
        viewModelScope.launch {
            val code = session.current()
            if (code == null) {
                cancelling = false
                onSessionExpired()
                return@launch
            }
            when (val result = api.cancel(code, requestId)) {
                is ApiResult.Success -> {
                    details = result.value
                    cancelError = null
                    offline = false
                }
                ApiResult.Unauthorized -> {
                    session.clear()
                    onSessionExpired()
                }
                is ApiResult.Conflict -> {
                    cancelError = R.string.details_cancel_conflict
                    // Перечитываем детали; сообщение об ошибке остаётся (оно не связано с загрузкой).
                    load(onSessionExpired)
                }
                ApiResult.Network, is ApiResult.Failure -> {
                    cancelError = R.string.details_cancel_network
                }
                else -> {
                    cancelError = R.string.details_cancel_other
                }
            }
            cancelling = false
        }
    }
}
