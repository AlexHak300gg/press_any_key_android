package ru.nto.guestmanagement.mobile.ui.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.nto.guestmanagement.mobile.R
import ru.nto.guestmanagement.mobile.data.ApiResult
import ru.nto.guestmanagement.mobile.data.CreateRequestBody
import ru.nto.guestmanagement.mobile.data.RequestsApi
import ru.nto.guestmanagement.mobile.data.toApi
import ru.nto.guestmanagement.mobile.di.AppContainer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

private val DATE_REGEX = Regex("^\\d{2}\\.\\d{2}\\.\\d{4}$")
private val TIME_REGEX = Regex("^\\d{2}:\\d{2}$")
private val PHONE_REGEX = Regex("^\\+7\\d{10}$")
private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.ROOT)
    .withResolverStyle(ResolverStyle.STRICT)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    .withResolverStyle(ResolverStyle.STRICT)

class RequestFormViewModel(
    container: AppContainer,
) : ViewModel() {
    private val api = RequestsApi(container.httpClient)
    private val session = container.session
    private val timeProvider = container.timeProvider

    var fullName by mutableStateOf("")
        private set
    var phone by mutableStateOf("")
        private set
    var organization by mutableStateOf("")
        private set
    var purpose by mutableStateOf("")
        private set
    var date by mutableStateOf("")
        private set
    var timeFrom by mutableStateOf("")
        private set
    var timeTo by mutableStateOf("")
        private set

    /** Ошибки сервера по ключам контракта; исчезают при редактировании своего поля (у даты — не сбрасывают validFrom/validTo). */
    var serverErrors by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    /** Общее сообщение: дубликат (409) или сеть/5xx. */
    var commonError by mutableStateOf<Int?>(null)
        private set

    /** Клиентские ошибки показываются только после первой попытки отправки. */
    var attempted by mutableStateOf(false)
        private set

    var loading by mutableStateOf(false)
        private set

    fun onFullNameChange(value: String) {
        fullName = value
        serverErrors = serverErrors - "fullName"
    }

    fun onPhoneChange(value: String) {
        phone = value
        serverErrors = serverErrors - "phone"
    }

    fun onOrganizationChange(value: String) {
        organization = value
        serverErrors = serverErrors - "organization"
    }

    fun onPurposeChange(value: String) {
        purpose = value
        serverErrors = serverErrors - "purpose"
    }

    fun onDateChange(value: String) {
        date = value
        // Серверные ошибки validFrom/validTo правкой даты не сбрасываются — по заданию.
    }

    fun onTimeFromChange(value: String) {
        timeFrom = value
        serverErrors = serverErrors - "validFrom"
    }

    fun onTimeToChange(value: String) {
        timeTo = value
        serverErrors = serverErrors - "validTo"
    }

    private data class Parsed(
        val validFrom: LocalDateTime?,
        val validTo: LocalDateTime?,
    )

    private fun parseDate(): LocalDate? {
        if (!DATE_REGEX.matches(date.trim())) return null
        return try {
            LocalDate.parse(date.trim(), dateFormatter)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTime(raw: String): LocalTime? {
        if (!TIME_REGEX.matches(raw.trim())) return null
        return try {
            LocalTime.parse(raw.trim(), timeFormatter)
        } catch (e: Exception) {
            null
        }
    }

    private fun parse(): Parsed {
        val day = parseDate()
        val fromTime = parseTime(timeFrom)
        val toTime = parseTime(timeTo)
        return Parsed(
            validFrom = if (day != null && fromTime != null) LocalDateTime.of(day, fromTime) else null,
            validTo = if (day != null && toTime != null) LocalDateTime.of(day, toTime) else null,
        )
    }

    /** Клиентские ошибки по полям; вызываются при отправке и для живого обновления после первой попытки. */
    fun clientError(field: String): Int? {
        if (!attempted) return null
        return when (field) {
            "fullName" -> {
                val v = fullName.trim()
                if (v.length < 5 || v.length > 150) R.string.form_error_full_name else null
            }
            "phone" -> if (!PHONE_REGEX.matches(phone.trim())) R.string.form_error_phone else null
            "organization" -> if (organization.trim().length > 150) R.string.form_error_organization else null
            "purpose" -> {
                val v = purpose.trim()
                if (v.length < 5 || v.length > 200) R.string.form_error_purpose else null
            }
            "date" -> if (parseDate() == null) R.string.form_error_date else null
            "validFrom" -> {
                if (parseTime(timeFrom) == null) {
                    R.string.form_error_time_from_format
                } else {
                    val parsed = parse()
                    if (parsed.validFrom == null) null // ошибка даты показана в поле даты
                    else if (parsed.validFrom.isBefore(timeProvider.now())) R.string.form_error_time_from_past else null
                }
            }
            "validTo" -> {
                if (parseTime(timeTo) == null) {
                    R.string.form_error_time_to_format
                } else {
                    val parsed = parse()
                    if (parsed.validTo == null || parsed.validFrom == null) null
                    else if (!parsed.validTo.isAfter(parsed.validFrom)) R.string.form_error_time_to_order else null
                }
            }
            else -> null
        }
    }

    private fun hasClientErrors(): Boolean =
        listOf("fullName", "phone", "organization", "purpose", "date", "validFrom", "validTo")
            .any { clientError(it) != null }

    /** Ошибка под полем: серверная — если есть (и не сброшена редактированием), иначе клиентская. */
    fun fieldErrorRes(field: String, serverKey: String? = field): Pair<Int?, String?>? {
        val server = serverKey?.let { serverErrors[it] }
        if (server != null) return null to server
        return clientError(field)?.let { it to null }
    }

    fun submit(onCreated: () -> Unit, onSessionExpired: () -> Unit) {
        if (loading) return
        serverErrors = emptyMap()
        attempted = true
        commonError = null
        if (hasClientErrors()) return

        val parsed = parse()
        val from = parsed.validFrom ?: return
        val to = parsed.validTo ?: return
        val body = CreateRequestBody(
            fullName = fullName.trim(),
            phone = phone.trim(),
            organization = organization.trim().takeIf { it.isNotEmpty() },
            purpose = purpose.trim(),
            validFrom = from.withSecond(0).withNano(0).toApi(),
            validTo = to.withSecond(0).withNano(0).toApi(),
        )

        loading = true
        viewModelScope.launch {
            val code = session.current()
            if (code == null) {
                loading = false
                onSessionExpired()
                return@launch
            }
            when (val result = api.create(code, body)) {
                is ApiResult.Success -> {
                    loading = false
                    onCreated()
                }
                ApiResult.Unauthorized -> {
                    session.clear()
                    loading = false
                    onSessionExpired()
                }
                is ApiResult.FieldErrors -> {
                    serverErrors = result.errors
                    loading = false
                }
                is ApiResult.Conflict -> {
                    commonError = R.string.form_error_duplicate
                    loading = false
                }
                is ApiResult.Failure, ApiResult.Network -> {
                    commonError = R.string.form_error_network
                    loading = false
                }
                else -> {
                    commonError = R.string.form_error_other
                    loading = false
                }
            }
        }
    }
}
