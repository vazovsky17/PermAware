package app.vazovsky.permaware.data.platform

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import app.vazovsky.permaware.domain.model.SpecialAccessState
import app.vazovsky.permaware.domain.model.SpecialAccessStatus
import app.vazovsky.permaware.domain.model.SpecialAccessType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Состояние особого доступа по всему устройству: читается один раз за проверку, дальше сверяется по
 * каждому пакету.
 *
 * Читать один раз здесь принципиально: часть этих вызовов — походы через binder, и если делать их
 * на каждый пакет, проверка четырёхсот пакетов превратится в тысячи IPC.
 */
data class SpecialAccessEnvironment(
    val activeAccessibilityPackages: Set<String>,
    val enabledNotificationListenerPackages: Set<String>,
    val activeDeviceAdminPackages: Set<String>,
    val batteryUnrestrictedPackages: Set<String>,
    val declaresAccessibilityService: Set<String>,
    val declaresNotificationListener: Set<String>,
    val declaresVpnService: Set<String>,
    val declaresDeviceAdmin: Set<String>,
    val supportsPictureInPicture: Set<String>,
    /** Ложь, если платформа отказалась рассказывать про включённые слушатели уведомлений. */
    val notificationListenerStateKnown: Boolean,
) {
    companion object {
        val Empty = SpecialAccessEnvironment(
            activeAccessibilityPackages = emptySet(),
            enabledNotificationListenerPackages = emptySet(),
            activeDeviceAdminPackages = emptySet(),
            batteryUnrestrictedPackages = emptySet(),
            declaresAccessibilityService = emptySet(),
            declaresNotificationListener = emptySet(),
            declaresVpnService = emptySet(),
            declaresDeviceAdmin = emptySet(),
            supportsPictureInPicture = emptySet(),
            notificationListenerStateKnown = false,
        )
    }
}

interface SpecialAccessInspector {
    suspend fun readEnvironment(forceRefresh: Boolean = false): SpecialAccessEnvironment

    fun statesFor(
        packageName: String,
        uid: Int,
        requestedPermissions: Set<String>,
        environment: SpecialAccessEnvironment,
    ): List<SpecialAccessState>
}

/**
 * Что Android 16 на самом деле позволяет обычному приложению узнать про особый доступ
 * другого приложения:
 *
 * | Возможность               | Проверяемо? | Чем                                          |
 * |---------------------------|-------------|----------------------------------------------|
 * | Специальные возможности   | да          | AccessibilityManager.getEnabledAccessibilityServiceList |
 * | Слушатель уведомлений     | да          | Settings.Secure enabled_notification_listeners |
 * | Администратор устройства  | да          | DevicePolicyManager.getActiveAdmins          |
 * | Батарея без ограничений   | да          | PowerManager.isIgnoringBatteryOptimizations  |
 * | Overlay / install / usage / all-files / write-settings | **нет** | AppOps, чужой пакет не прочитать |
 * | Активная VPN-служба       | нет         | видно только объявление в манифесте           |
 *
 * Всё, что стоит в строках с «нет», уходит как [SpecialAccessStatus.DECLARED] и никогда как
 * выданное.
 */
@Singleton
class PlatformSpecialAccessInspector @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val packageManager: PackageManager,
    private val appOpsReader: AppOpsSpecialAccessReader,
) : SpecialAccessInspector {

    @Volatile
    private var cached: SpecialAccessEnvironment? = null

    @Volatile
    private var cachedAt: Long = 0

    /**
     * На эталонном устройстве измерено около 250 мс, и почти всё это — второй полный перебор
     * пакетов ради определения картинки в картинке.
     */
    override suspend fun readEnvironment(forceRefresh: Boolean): SpecialAccessEnvironment {
        if (!forceRefresh) {
            val existing = cached
            if (existing != null && SystemClock.elapsedRealtime() - cachedAt < CACHE_TTL_MS) {
                return existing
            }
        }
        return read().also {
            cached = it
            cachedAt = SystemClock.elapsedRealtime()
        }
    }

    private suspend fun read(): SpecialAccessEnvironment {
        val accessibilityActive = runCatching {
            context.getSystemService(AccessibilityManager::class.java)
                ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                ?.mapNotNull { it.resolveInfo?.serviceInfo?.packageName }
                ?.toSet()
                .orEmpty()
        }.getOrDefault(emptySet())

        val listenersRaw = runCatching {
            Settings.Secure.getString(context.contentResolver, ENABLED_NOTIFICATION_LISTENERS)
        }.getOrNull()
        val listenerPackages = listenersRaw
            ?.split(':')
            ?.mapNotNull { ComponentName.unflattenFromString(it)?.packageName }
            ?.toSet()
            .orEmpty()

        val activeAdmins = runCatching {
            @Suppress("DEPRECATION")
            context.getSystemService(DevicePolicyManager::class.java)
                ?.activeAdmins
                ?.map { it.packageName }
                ?.toSet()
                .orEmpty()
        }.getOrDefault(emptySet())

        val declaresAccessibility = servicePackages(android.accessibilityservice.AccessibilityService.SERVICE_INTERFACE)
        val declaresListener = servicePackages(NOTIFICATION_LISTENER_SERVICE)
        val declaresVpn = servicePackages(VPN_SERVICE)
        val declaresAdmin = runCatching {
            packageManager
                .queryBroadcastReceivers(
                    Intent(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED),
                    PackageManager.MATCH_ALL,
                )
                .map { it.activityInfo.packageName }
                .toSet()
        }.getOrDefault(emptySet())

        // Белый список батареи спрашивается только про те пакеты, которые в нём правдоподобно могут
        // быть, — чтобы число binder-вызовов оставалось соразмерным тому, что мы реально
        // показываем.
        val batteryUnrestricted = runCatching {
            val power = context.getSystemService(PowerManager::class.java) ?: return@runCatching emptySet()
            val candidates = declaresAccessibility + declaresListener + declaresVpn + declaresAdmin +
                accessibilityActive + listenerPackages + activeAdmins
            candidates.filterTo(mutableSetOf()) { pkg ->
                runCatching { power.isIgnoringBatteryOptimizations(pkg) }.getOrDefault(false)
            }
        }.getOrDefault(emptySet())

        val pipPackages = runCatching {
            packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES)
                .filter { info ->
                    info.activities?.any { (it.flags and FLAG_SUPPORTS_PICTURE_IN_PICTURE) != 0 } == true
                }
                .map { it.packageName }
                .toSet()
        }.getOrDefault(emptySet())

        return SpecialAccessEnvironment(
            activeAccessibilityPackages = accessibilityActive,
            enabledNotificationListenerPackages = listenerPackages,
            activeDeviceAdminPackages = activeAdmins,
            batteryUnrestrictedPackages = batteryUnrestricted,
            declaresAccessibilityService = declaresAccessibility,
            declaresNotificationListener = declaresListener,
            declaresVpnService = declaresVpn,
            declaresDeviceAdmin = declaresAdmin,
            supportsPictureInPicture = pipPackages,
            notificationListenerStateKnown = listenersRaw != null,
        )
    }

    override fun statesFor(
        packageName: String,
        uid: Int,
        requestedPermissions: Set<String>,
        environment: SpecialAccessEnvironment,
    ): List<SpecialAccessState> = buildList {
        // ---- Состояния, которые можно проверить ----
        if (packageName in environment.declaresAccessibilityService) {
            add(
                SpecialAccessState(
                    SpecialAccessType.ACCESSIBILITY,
                    if (packageName in environment.activeAccessibilityPackages) {
                        SpecialAccessStatus.ACTIVE
                    } else {
                        SpecialAccessStatus.INACTIVE
                    },
                ),
            )
        }
        if (packageName in environment.declaresNotificationListener) {
            add(
                SpecialAccessState(
                    SpecialAccessType.NOTIFICATION_LISTENER,
                    when {
                        !environment.notificationListenerStateKnown -> SpecialAccessStatus.DECLARED
                        packageName in environment.enabledNotificationListenerPackages -> SpecialAccessStatus.ACTIVE
                        else -> SpecialAccessStatus.INACTIVE
                    },
                ),
            )
        }
        if (packageName in environment.declaresDeviceAdmin) {
            add(
                SpecialAccessState(
                    SpecialAccessType.DEVICE_ADMIN,
                    if (packageName in environment.activeDeviceAdminPackages) {
                        SpecialAccessStatus.ACTIVE
                    } else {
                        SpecialAccessStatus.INACTIVE
                    },
                ),
            )
        }
        if (packageName in environment.batteryUnrestrictedPackages) {
            add(SpecialAccessState(SpecialAccessType.BATTERY_UNRESTRICTED, SpecialAccessStatus.ACTIVE))
        }

        // ---- Только заявлено: состояние выдачи Android от нас прячет ----
        if (packageName in environment.declaresVpnService) {
            add(SpecialAccessState(SpecialAccessType.VPN_SERVICE, SpecialAccessStatus.DECLARED))
        }
        AppOpPermissionMapping.PERMISSION_TO_SPECIAL_ACCESS.forEach { (permission, type) ->
            if (permission in requestedPermissions) {
                // На части устройств AppOps на это отвечает; там, где не отвечает, честное слово —
                // «заявлено».
                val status = appOpsReader.statusOf(type, uid, packageName) ?: SpecialAccessStatus.DECLARED
                add(SpecialAccessState(type, status))
            }
        }
        if (packageName in environment.supportsPictureInPicture) {
            add(SpecialAccessState(SpecialAccessType.PICTURE_IN_PICTURE, SpecialAccessStatus.DECLARED))
        }
    }.distinctBy { it.type }

    private fun servicePackages(action: String): Set<String> = runCatching {
        packageManager.queryIntentServices(Intent(action), PackageManager.MATCH_ALL)
            .map { it.serviceInfo.packageName }
            .toSet()
    }.getOrDefault(emptySet())

    private companion object {
        const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
        const val NOTIFICATION_LISTENER_SERVICE = "android.service.notification.NotificationListenerService"
        const val VPN_SERVICE = "android.net.VpnService"

        /**
         * Достаточно коротко, чтобы экран приложения, открытый через несколько минут, перечитал
         * состояние заново.
         */
        const val CACHE_TTL_MS = 30_000L

        /** [ActivityInfo.FLAG_SUPPORTS_PICTURE_IN_PICTURE]; как константа в SDK не выставлен. */
        const val FLAG_SUPPORTS_PICTURE_IN_PICTURE = 0x400000
    }
}
