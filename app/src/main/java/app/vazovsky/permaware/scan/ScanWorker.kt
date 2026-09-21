package app.vazovsky.permaware.scan

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.vazovsky.permaware.data.repository.ScanRepository
import app.vazovsky.permaware.domain.model.ScanTrigger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Периодическая фоновая проверка.
 *
 * Нарочито скромная: ни службы переднего плана, ни wake lock, ни точных будильников. Проверка — это
 * несколько секунд процессора и ноль сети, так что она достаточно дешёвая, чтобы запускаться при
 * любом окне, которое Android решит нам выдать. А если он не выдаст его никогда, ничего не
 * сломается: человек увидит изменения при следующей ручной проверке.
 */
@HiltWorker
class ScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val scanRepository: ScanRepository,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val outcome = scanRepository.scan(ScanTrigger.BACKGROUND)
        return if (outcome.isSuccess) {
            Result.success()
        } else {
            // Повтор вместо провала: проверка может проиграть гонку менеджеру пакетов во время
            // обновления системы, а в следующее окно почти наверняка пройдёт.
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "permission_watch_periodic_scan"
        private const val MAX_ATTEMPTS = 3
    }
}
