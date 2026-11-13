package ru.nto.guestmanagement.mobile.ui.refresh

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Вызывает [onResume] при каждом возвращении на экран: первое открытие, возврат из деталей/формы (экран
 * возвращается в композицию) и из фона (`ON_RESUME`).
 *
 * Наивный `LifecycleResumeEffect` здесь не подходит: наблюдатель не получает события, которые уже произошли
 * до его добавления, а возврат на экран внутри одного Activity вообще не меняет состояние жизненного цикла —
 * экран просто входит в композицию заново. Поэтому кроме подписки на `ON_RESUME` выполняем вызов сразу при
 * входе в композицию, если жизненный цикл уже `RESUMED` — ровно один раз, без двойных срабатываний.
 */
@Composable
fun ResumeEffect(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResume = rememberUpdatedState(onResume)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentOnResume.value()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            currentOnResume.value()
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
