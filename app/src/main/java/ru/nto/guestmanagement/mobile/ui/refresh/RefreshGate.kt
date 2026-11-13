package ru.nto.guestmanagement.mobile.ui.refresh

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Защита от повторных обновлений: пока запущенное через [launch] обновление выполняется, новый запуск игнорируется
 * (возвращает `null`), второй запрос не создаётся. [cancel] прерывает текущее обновление (например, при смене фильтра).
 */
class RefreshGate {
    private var current: Job? = null

    @Synchronized
    fun launch(scope: CoroutineScope, block: suspend () -> Unit): Job? {
        val active = current
        if (active != null && active.isActive) return null
        val job = scope.launch { block() }
        current = job
        return job
    }

    @Synchronized
    fun cancel() {
        current?.cancel()
        current = null
    }
}
