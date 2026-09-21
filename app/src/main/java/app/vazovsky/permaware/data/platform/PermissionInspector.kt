package app.vazovsky.permaware.data.platform

import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import app.vazovsky.permaware.domain.model.AppPermission
import app.vazovsky.permaware.domain.model.PermissionCategory
import app.vazovsky.permaware.domain.model.PermissionProtection
import app.vazovsky.permaware.domain.model.PermissionState
import app.vazovsky.permaware.domain.model.Sensitivity
import app.vazovsky.permaware.domain.permission.PermissionCatalog
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Превращает сырые массивы разрешений от платформы в [AppPermission].
 *
 * Всё здесь пляшет от того, что система реально показывает про *чужие* пакеты, — измерено на
 * живом устройстве с Android 16:
 *
 *  - `PackageInfo.requestedPermissions` вместе с `requestedPermissionsFlags` дают настоящий
 *    бит выдачи
 *  - `PackageManager.getPermissionInfo` даёт уровень защиты и объявивший пакет
 *  - Возможности за AppOps (поверх других окон, установка неизвестных приложений, доступ к
 *    статистике, ко всем файлам, запись настроек) для сторонних пакетов **не** читаются. Их
 *    бит выдачи бессмыслен, поэтому они уходят как [PermissionState.UNDETERMINED], а не
 *    угадываются.
 */
interface PermissionInspector {
    fun inspect(requested: Array<String>?, grantFlags: IntArray?): List<AppPermission>
}

@Singleton
class PlatformPermissionInspector @Inject constructor(private val packageManager: PackageManager) :
    PermissionInspector {

    /**
     * Метаданные разрешения одинаковы для всех приложений, поэтому разбираются один раз на процесс.
     *
     * **Многопоточно**: проверка разбирает пакеты сразу в нескольких корутинах, так что в эту карту
     * пишут несколько потоков. Обычный HashMap при одновременной вставке способен испортить
     * сам себя.
     */
    private val descriptorCache = ConcurrentHashMap<String, PermissionDescriptor>()

    override fun inspect(requested: Array<String>?, grantFlags: IntArray?): List<AppPermission> {
        if (requested == null || requested.isEmpty()) return emptyList()
        return requested.mapIndexedNotNull { index, name ->
            if (name.isBlank()) return@mapIndexedNotNull null
            val descriptor = descriptorFor(name)
            val grantedBit = grantFlags != null &&
                index < grantFlags.size &&
                (grantFlags[index] and REQUESTED_PERMISSION_GRANTED) != 0

            AppPermission(
                name = name,
                category = descriptor.category,
                protection = descriptor.protection,
                state = stateFor(descriptor.protection, grantedBit),
                sensitivity = descriptor.sensitivity,
                isCustom = descriptor.isCustom,
                platformLabel = descriptor.label,
                platformDescription = descriptor.description,
            )
        }.distinctBy { it.name }
    }

    private fun stateFor(protection: PermissionProtection, grantedBit: Boolean): PermissionState = when {
        // Бит выдачи есть и у них, только фактический доступ он не описывает: его описывает запись
        // в AppOps, а её Android для чужих пакетов не показывает.
        protection == PermissionProtection.APPOP_GATED -> PermissionState.UNDETERMINED
        grantedBit -> PermissionState.GRANTED
        else -> PermissionState.DENIED
    }

    private fun descriptorFor(name: String): PermissionDescriptor = descriptorCache.computeIfAbsent(name) {
        val knowledge = PermissionCatalog.find(name)
        val info = runCatching { packageManager.getPermissionInfo(name, 0) }.getOrNull()

        val platformProtection = info?.let { protectionOf(it) } ?: PermissionProtection.UNKNOWN
        val protection = when {
            knowledge?.appOpGated == true -> PermissionProtection.APPOP_GATED
            else -> platformProtection
        }

        val declaringPackage = info?.packageName
        val isCustom = declaringPackage != null &&
            declaringPackage != PLATFORM_PACKAGE &&
            !name.startsWith(ANDROID_PERMISSION_PREFIX)

        PermissionDescriptor(
            category = knowledge?.category ?: inferCategory(name, protection),
            protection = protection,
            sensitivity = knowledge?.sensitivity ?: inferSensitivity(protection),
            isCustom = isCustom,
            // Нужно только как запасной вариант для разрешений, которых нет в базе знаний.
            label = if (knowledge != null) null else info?.loadLabel(packageManager)?.toString(),
            description = if (knowledge != null) {
                null
            } else {
                runCatching { info?.loadDescription(packageManager)?.toString() }.getOrNull()
            },
        )
    }

    private fun protectionOf(info: PermissionInfo): PermissionProtection {
        val base = info.protection
        val flags = info.protectionFlags
        return when {
            // Сигнатурное разрешение, которое вдобавок закрыто AppOps (классика —
            // SYSTEM_ALERT_WINDOW: protection=signature, среди флагов PROTECTION_FLAG_APPOP).
            (flags and PROTECTION_FLAG_APPOP) != 0 -> PermissionProtection.APPOP_GATED
            base == PermissionInfo.PROTECTION_DANGEROUS -> PermissionProtection.DANGEROUS
            base == PermissionInfo.PROTECTION_NORMAL -> PermissionProtection.NORMAL
            base == PermissionInfo.PROTECTION_SIGNATURE ||
                base == PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM -> PermissionProtection.SIGNATURE
            else -> PermissionProtection.UNKNOWN
        }
    }

    /**
     * Незнакомые разрешения всё равно показываем.
     */
    private fun inferCategory(name: String, protection: PermissionProtection): PermissionCategory = when {
        protection == PermissionProtection.APPOP_GATED -> PermissionCategory.SPECIAL_ACCESS
        name.startsWith(ANDROID_PERMISSION_PREFIX) -> PermissionCategory.SYSTEM
        else -> PermissionCategory.OTHER
    }

    private fun inferSensitivity(protection: PermissionProtection): Sensitivity = when (protection) {
        // Опасное разрешение, про которое неизвестно, всё равно видно пользователю и всё
        // равно отзывается.
        PermissionProtection.DANGEROUS -> Sensitivity.NOTABLE
        PermissionProtection.APPOP_GATED -> Sensitivity.NOTABLE
        else -> Sensitivity.NONE
    }

    private data class PermissionDescriptor(
        val category: PermissionCategory,
        val protection: PermissionProtection,
        val sensitivity: Sensitivity,
        val isCustom: Boolean,
        val label: String?,
        val description: String?,
    )

    private companion object {
        const val PLATFORM_PACKAGE = "android"
        const val ANDROID_PERMISSION_PREFIX = "android.permission."

        /** PackageInfo.REQUESTED_PERMISSION_GRANTED */
        const val REQUESTED_PERMISSION_GRANTED = 0x00000002

        /** PermissionInfo.PROTECTION_FLAG_APPOP */
        const val PROTECTION_FLAG_APPOP = 0x40
    }
}
