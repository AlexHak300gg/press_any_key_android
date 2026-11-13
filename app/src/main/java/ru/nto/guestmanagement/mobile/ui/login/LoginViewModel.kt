package ru.nto.guestmanagement.mobile.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import ru.nto.guestmanagement.mobile.di.AppContainer
import ru.nto.guestmanagement.mobile.data.ApiResult
import ru.nto.guestmanagement.mobile.data.RequestsApi

private val CODE_REGEX = Regex("^[A-Za-z0-9]{4}$")

class LoginViewModel(
    container: AppContainer,
) : ViewModel() {
    private val api = RequestsApi(container.httpClient)
    private val session = container.session

    var code by mutableStateOf("")
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set

    private var skipChecked = false

    val codeValid: Boolean get() = CODE_REGEX.matches(code)

    /** Проверяется один раз за жизнь экрана: если код сохранён — вход пропускается без запроса /auth. */
    fun checkSavedCode(onLoggedIn: () -> Unit) {
        if (skipChecked) return
        skipChecked = true
        viewModelScope.launch {
            if (session.current() != null) onLoggedIn()
        }
    }

    fun onCodeChange(value: String) {
        code = value
        error = null
    }

    fun submit(onLoggedIn: () -> Unit) {
        if (!codeValid || loading) return
        loading = true
        error = null
        viewModelScope.launch {
            when (api.auth(code)) {
                is ApiResult.Success -> {
                    session.save(code)
                    loading = false
                    onLoggedIn()
                }
                ApiResult.Unauthorized -> {
                    error = "Неверный код"
                    loading = false
                }
                ApiResult.Network -> {
                    error = "Нет связи с сервером. Попробуйте ещё раз"
                    loading = false
                }
                else -> {
                    error = "Не удалось войти. Попробуйте ещё раз"
                    loading = false
                }
            }
        }
    }
}
