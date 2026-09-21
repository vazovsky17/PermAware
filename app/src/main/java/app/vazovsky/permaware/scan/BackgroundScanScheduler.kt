package app.vazovsky.permaware.scan

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.vazovsky.permaware.data.prefs.PreferencesRepository
import app.vazovsky.permaware.data.prefs.ScanInterval
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Планирует периодическую проверку — честно.
 *
 * Что Android гарантирует на самом деле и что поэтому говорит интерфейс:
 *
 *  - Минимальный период периодической работы — 15 минут, а самый частый наш вариант — 6
 *    часов.
 *  - Doze, корзины App Standby и вендорские менеджеры батареи (самсунговский особенно)
 *    способны задержать периодического работника на часы или вовсе пропустить его, пока
 *    устройство простаивает.
 *  - Узнать о смене разрешения в тот же миг нельзя никак, кроме постоянной службы переднего
 *    плана, а её этот продукт не использует.
 *
 * Поэтому экран настроек говорит "проверяет изменения, когда Android разрешает выполнить
 * фоновую задачу", а не обещает расписание, которого платформа не выдержит.
 */
@Singleton
class BackgroundScanScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: PreferencesRepository,
) {

    private val workManager: WorkManager?
        get() = runCatching { WorkManager.getInstance(context) }.getOrNull()

    /**
     * Вызывается на каждом запуске приложения. Использует [ExistingPeriodicWorkPolicy.KEEP], чтобы
     * уже работающее расписание осталось нетронутым, — см. [schedule].
     */
    suspend fun syncWithPreferences() {
        val prefs = preferences.current()
        if (prefs.autoScanEnabled && prefs.onboardingComplete) {
            schedule(prefs.scanInterval, replaceExisting = false)
        } else {
            cancel()
        }
    }

    /**
     * @param replaceExisting true только тогда, когда пользователь поменял интервал в настройках.
     *
     * Разница тут важнее, чем кажется. Первая версия ставила начальную задержку в целый интервал
     * и всегда звала `UPDATE`, а `syncWithPreferences` отрабатывает на каждом запуске приложения
     * — так что у всякого, кто открывает приложение чаще интервала, периодическая проверка
     * отодвигалась каждый раз и **не запустилась бы вообще никогда**. Начальной задержки теперь
     * нет, а рядовая синхронизация сохраняет существующее расписание, а не сбрасывает период.
     */
    fun schedule(interval: ScanInterval, replaceExisting: Boolean = true) {
        val manager = workManager ?: return
        val request = PeriodicWorkRequestBuilder<ScanWorker>(interval.hours, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .build()

        runCatching {
            manager.enqueueUniquePeriodicWork(
                ScanWorker.WORK_NAME,
                if (replaceExisting) {
                    ExistingPeriodicWorkPolicy.UPDATE
                } else {
                    ExistingPeriodicWorkPolicy.KEEP
                },
                request,
            )
        }
    }

    fun cancel() {
        runCatching { workManager?.cancelUniqueWork(ScanWorker.WORK_NAME) }
    }

    /** Удалось ли вообще зарегистрировать периодическую проверку в платформе. */
    val isAvailable: Boolean get() = workManager != null
}
