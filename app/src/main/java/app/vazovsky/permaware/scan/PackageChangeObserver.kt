package app.vazovsky.permaware.scan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Обнаружение установок, удалений и обновлений **пока приложение на переднем плане**.
 *
 * Чем это не является: монитором реального времени. С Android 8 эти широковещания в большинстве
 * случаев не доходят до приёмников, объявленных в манифесте, да и фоновое приложение попросту не
 * работает, чтобы их принять. Регистрация на лету, пока интерфейс видим, — честное подмножество
 * возможности: приложение замечает, что что-то поменялось, пока человек держал его открытым, а
 * именно тогда устаревший список и бросался бы в глаза сильнее всего.
 *
 * Всё, что происходит при закрытом приложении, закрывает периодический работник. Ничто в интерфейсе
 * не утверждает обратного.
 */
@Singleton
class PackageChangeObserver @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val _events = MutableSharedFlow<PackageEvent>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: Flow<PackageEvent> = _events.asSharedFlow()

    private var receiver: BroadcastReceiver? = null

    fun start() {
        if (receiver != null) return
        val newReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val packageName = intent?.data?.schemeSpecificPart ?: return
                // ACTION_PACKAGE_REMOVED с EXTRA_REPLACING — это середина обновления, а не
                // удаление; игнорируя его, мы не получаем призрачную пару "приложение удалено", а
                // следом "приложение установлено".
                val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                val event = when (intent.action) {
                    Intent.ACTION_PACKAGE_ADDED -> if (replacing) null else PackageEvent.Added(packageName)
                    Intent.ACTION_PACKAGE_REMOVED -> if (replacing) null else PackageEvent.Removed(packageName)
                    Intent.ACTION_PACKAGE_REPLACED -> PackageEvent.Replaced(packageName)
                    else -> null
                }
                if (event != null) _events.tryEmit(event)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        // На Android 16 при targetSdk 36 проверено: вызов без флага проходит, потому что каждое
        // действие здесь — защищённое системное широковещание. RECEIVER_NOT_EXPORTED передаётся всё
        // равно: так намерение заявлено вслух, и код останется верным, если в фильтр однажды
        // добавят незащищённое действие.
        runCatching {
            ContextCompat.registerReceiver(context, newReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }.onSuccess { receiver = newReceiver }
    }

    fun stop() {
        receiver?.let { registered ->
            runCatching { context.unregisterReceiver(registered) }
            receiver = null
        }
    }
}

sealed interface PackageEvent {
    val packageName: String

    data class Added(override val packageName: String) : PackageEvent
    data class Removed(override val packageName: String) : PackageEvent
    data class Replaced(override val packageName: String) : PackageEvent
}
