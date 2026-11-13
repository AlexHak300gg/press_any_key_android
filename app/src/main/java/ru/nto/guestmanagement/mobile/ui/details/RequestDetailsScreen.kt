package ru.nto.guestmanagement.mobile.ui.details

import androidx.compose.runtime.Composable
import ru.nto.guestmanagement.mobile.di.AppContainer

/**
 * Детали заявки [requestId]. Реализовать: все поля, комментарий охраны, отмену с подтверждением,
 * обработку `409`, обновление и повтор при ошибке, возврат.
 */
@Composable
fun RequestDetailsScreen(
    container: AppContainer,
    requestId: Long,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    // TODO
}
