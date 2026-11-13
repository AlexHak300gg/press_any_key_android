package ru.nto.guestmanagement.mobile.ui.list

import androidx.compose.runtime.Composable
import ru.nto.guestmanagement.mobile.di.AppContainer

/**
 * Список заявок. Реализовать: фильтры, сортировку (как отдаёт API), пустое состояние, ошибки и «Нет связи»,
 * ручное обновление, перечитывание при возврате на экран, выход.
 * [onSessionExpired] вызывается, когда сервер ответил `401` (код при этом нужно очистить).
 */
@Composable
fun RequestListScreen(
    container: AppContainer,
    onOpenDetails: (Long) -> Unit,
    onAddRequest: () -> Unit,
    onLogout: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    // TODO
}
