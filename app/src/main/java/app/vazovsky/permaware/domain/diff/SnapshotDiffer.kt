package app.vazovsky.permaware.domain.diff

import app.vazovsky.permaware.domain.model.AppSnapshot
import app.vazovsky.permaware.domain.model.ChangeEvent
import app.vazovsky.permaware.domain.model.ChangeType
import app.vazovsky.permaware.domain.model.PermissionState
import app.vazovsky.permaware.domain.model.SpecialAccessStatus
import app.vazovsky.permaware.domain.permission.PermissionCatalog

/**
 * Сравнивает две проверки и составляет список изменений. Это та самая возможность, ради
 * которой PermAware имеет смысл держать установленным, поэтому правила здесь осторожные:
 * ложное «приложение получило доступ к камере» хуже, чем промолчать.
 *
 * Правила:
 *
 *  - **Установка, удаление, обновление** берутся из набора пакетов и кода версии.
 *  - **Запрошенные разрешения** сравниваются как множества. Разрешение, появившееся после
 *    обновления, отмечается один раз, как `PERMISSION_REQUEST_ADDED`; мы не сообщаем про него
 *    ещё и «выдано», даже если оно приехало уже выданным, — иначе на один факт приходилось бы
 *    две строки.
 *  - **Переходы выдачи** отмечаются только между теми двумя состояниями, о которых Android
 *    сообщает надёжно. Всё, где участвует [PermissionState.UNDETERMINED] — то есть
 *    возможности за AppOps, — игнорируется: «раньше мы не могли понять, а теперь можем» это
 *    не изменение в приложении.
 *  - **Переходы особого доступа** отмечаются, только когда включается или выключается
 *    возможность, которую мы умеем *проверить*. Возможность, которая просто появилась в
 *    манифесте, доедет до пользователя по правилам про запрошенные разрешения выше.
 *
 * Чисто и детерминированно: результат отсортирован, поэтому и тесты, и интерфейс видят один
 * и тот же порядок.
 */
object SnapshotDiffer {

    fun diff(previous: List<AppSnapshot>, current: List<AppSnapshot>, timestamp: Long): List<ChangeEvent> {
        if (previous.isEmpty()) return emptyList() // Первая проверка — точка отсчёта, а не список изменений.

        val previousByPackage = previous.associateBy { it.packageName }
        val currentByPackage = current.associateBy { it.packageName }
        val events = mutableListOf<ChangeEvent>()

        // --- Установлено ---
        currentByPackage.keys.subtract(previousByPackage.keys).forEach { packageName ->
            val app = currentByPackage.getValue(packageName)
            events += ChangeEvent(
                timestamp = timestamp,
                packageName = packageName,
                appLabel = app.label,
                type = ChangeType.APP_INSTALLED,
                newVersion = app.versionName,
            )
        }

        // --- Удалено ---
        previousByPackage.keys.subtract(currentByPackage.keys).forEach { packageName ->
            val app = previousByPackage.getValue(packageName)
            events += ChangeEvent(
                timestamp = timestamp,
                packageName = packageName,
                appLabel = app.label,
                type = ChangeType.APP_REMOVED,
                previousVersion = app.versionName,
            )
        }

        // --- Изменилось на месте ---
        currentByPackage.keys.intersect(previousByPackage.keys).forEach { packageName ->
            val before = previousByPackage.getValue(packageName)
            val after = currentByPackage.getValue(packageName)
            events += diffApp(before, after, timestamp)
        }

        return events.sortedWith(
            compareBy({ it.type.ordinal }, { it.appLabel.lowercase() }, { it.permission.orEmpty() }),
        )
    }

    private fun diffApp(before: AppSnapshot, after: AppSnapshot, timestamp: Long): List<ChangeEvent> {
        val events = mutableListOf<ChangeEvent>()
        val label = after.label

        if (before.versionCode != after.versionCode) {
            events += ChangeEvent(
                timestamp = timestamp,
                packageName = after.packageName,
                appLabel = label,
                type = ChangeType.APP_UPDATED,
                previousVersion = before.versionName,
                newVersion = after.versionName,
            )
        }

        val beforePermissions = before.permissions.associate { it.name to it.state }
        val afterPermissions = after.permissions.associate { it.name to it.state }

        afterPermissions.keys.subtract(beforePermissions.keys).forEach { permission ->
            events += permissionEvent(after, label, timestamp, permission, ChangeType.PERMISSION_REQUEST_ADDED)
        }
        beforePermissions.keys.subtract(afterPermissions.keys).forEach { permission ->
            events += permissionEvent(after, label, timestamp, permission, ChangeType.PERMISSION_REQUEST_REMOVED)
        }

        afterPermissions.keys.intersect(beforePermissions.keys).forEach { permission ->
            val was = beforePermissions.getValue(permission)
            val now = afterPermissions.getValue(permission)
            if (was == now) return@forEach
            val type = when {
                was == PermissionState.DENIED && now == PermissionState.GRANTED -> ChangeType.PERMISSION_GRANTED
                was == PermissionState.GRANTED && now == PermissionState.DENIED -> ChangeType.PERMISSION_REVOKED
                // Любой переход, задевающий UNDETERMINED, говорит больше о том, что нам сообщил
                // Android, чем о том, что сделало приложение.
                else -> return@forEach
            }
            events += permissionEvent(after, label, timestamp, permission, type)
        }

        val beforeAccess = before.specialAccess.associate { it.type to it.status }
        val afterAccess = after.specialAccess.associate { it.type to it.status }
        (beforeAccess.keys + afterAccess.keys).forEach { type ->
            val wasActive = beforeAccess[type] == SpecialAccessStatus.ACTIVE
            val isActive = afterAccess[type] == SpecialAccessStatus.ACTIVE
            when {
                !wasActive && isActive -> events += ChangeEvent(
                    timestamp = timestamp,
                    packageName = after.packageName,
                    appLabel = label,
                    type = ChangeType.SPECIAL_ACCESS_GAINED,
                    specialAccess = type,
                )
                wasActive && !isActive -> events += ChangeEvent(
                    timestamp = timestamp,
                    packageName = after.packageName,
                    appLabel = label,
                    type = ChangeType.SPECIAL_ACCESS_LOST,
                    specialAccess = type,
                )
            }
        }

        return events
    }

    private fun permissionEvent(
        app: AppSnapshot,
        label: String,
        timestamp: Long,
        permission: String,
        type: ChangeType,
    ) = ChangeEvent(
        timestamp = timestamp,
        packageName = app.packageName,
        appLabel = label,
        type = type,
        permission = permission,
        category = PermissionCatalog.find(permission)?.category,
    )
}
