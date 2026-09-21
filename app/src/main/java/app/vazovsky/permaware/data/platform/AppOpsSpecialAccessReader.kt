package app.vazovsky.permaware.data.platform

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import app.vazovsky.permaware.domain.model.SpecialAccessStatus
import app.vazovsky.permaware.domain.model.SpecialAccessType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Читатель особого доступа, закрытого AppOps.
 *
 * Принято считать, что `unsafeCheckOpNoThrow` бросает исключение для чужих пакетов. На
 * Android 16 (Samsung SM-A175F) выяснено: не бросает, и возвращает по-настоящему разные
 * значения — 13 из 91 приложения, запрашивающих `SYSTEM_ALERT_WINDOW`, ответили
 * `MODE_ALLOWED`.
 *
 * Поскольку на всех вендорах и версиях такое поведение не гарантировано, этот читатель —
 *  *дополнение*:
 *
 *  - `MODE_ALLOWED` — единственное значение, которое считается фактом выдачи
 *    ([SpecialAccessStatus.ACTIVE]).
 *  - `MODE_DEFAULT` означает «явной записи нет, смотри проверку разрешения». Это **не** то же
 *    самое, что отказ, поэтому оно опускается до [SpecialAccessStatus.DECLARED] — «попросить
 *    может, а подтвердить, что имеет, мы не умеем».
 *  - Что угодно неожиданное (SecurityException, отсутствующий сервис) выключает читателя на
 *    всю проверку, и каждая возможность откатывается к [SpecialAccessStatus.DECLARED].
 */
@Singleton
class AppOpsSpecialAccessReader @Inject constructor(@param:ApplicationContext private val context: Context) {

    private val appOps: AppOpsManager? =
        runCatching { context.getSystemService(AppOpsManager::class.java) }.getOrNull()

    /** Выключается для всего процесса навсегда, как только платформа впервые откажет в запросе. */
    @Volatile
    private var usable: Boolean = appOps != null

    val isUsable: Boolean get() = usable

    /**
     * @return [SpecialAccessStatus.ACTIVE] / [SpecialAccessStatus.INACTIVE], когда платформа
     *   ответила определённо, или `null`, когда состояние установить не удалось и вызывающей
     *   стороне следует откатиться к [SpecialAccessStatus.DECLARED].
     */
    fun statusOf(type: SpecialAccessType, uid: Int, packageName: String): SpecialAccessStatus? {
        val ops = appOps ?: return null
        if (!usable) return null
        val op = OPS[type] ?: return null
        return try {
            when (checkOp(ops, op, uid, packageName)) {
                AppOpsManager.MODE_ALLOWED -> SpecialAccessStatus.ACTIVE
                AppOpsManager.MODE_IGNORED, AppOpsManager.MODE_ERRORED -> SpecialAccessStatus.INACTIVE
                // MODE_DEFAULT: явной записи нет. Свидетельством отказа это не является.
                else -> null
            }
        } catch (e: SecurityException) {
            // Этот вендор или эта версия не дают читать AppOps чужих пакетов. Больше не спрашиваем.
            usable = false
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * `unsafeCheckOpNoThrow` существует только с API 29; на API 28 тот же вызов — ныне устаревший
     * `checkOpNoThrow`. Без этой развилки приложение падает на Android 9 с NoSuchMethodError, и
     * поймал это lint раньше, чем хоть одно устройство с Android 9.
     */
    private fun checkOp(ops: AppOpsManager, op: String, uid: Int, packageName: String): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(op, uid, packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(op, uid, packageName)
        }

    private companion object {
        /**
         * Строковые имена операций. Константами в SDK заведены только `OPSTR_SYSTEM_ALERT_WINDOW`,
         * `OPSTR_GET_USAGE_STATS` и `OPSTR_WRITE_SETTINGS`; остальные — стабильные публичные имена,
         * которые SDK наружу не отдаёт (наличие проверено на API 36).
         */
        val OPS: Map<SpecialAccessType, String> = mapOf(
            SpecialAccessType.DISPLAY_OVER_OTHER_APPS to AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
            SpecialAccessType.USAGE_ACCESS to AppOpsManager.OPSTR_GET_USAGE_STATS,
            SpecialAccessType.WRITE_SYSTEM_SETTINGS to AppOpsManager.OPSTR_WRITE_SETTINGS,
            SpecialAccessType.INSTALL_UNKNOWN_APPS to PERMISSION_REQUEST_INSTALL_PACKAGE,
            SpecialAccessType.ALL_FILES_ACCESS to PERMISSION_MANAGE_EXTERNAL_STORAGE,
        )

        private const val PERMISSION_REQUEST_INSTALL_PACKAGE = "android:request_install_packages"
        private const val PERMISSION_MANAGE_EXTERNAL_STORAGE = "android:manage_external_storage"
    }
}
