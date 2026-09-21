package app.vazovsky.permaware.data.platform

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import app.vazovsky.permaware.domain.model.AttentionAssessment
import app.vazovsky.permaware.domain.model.InstallSource
import app.vazovsky.permaware.domain.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Единственное место, где читается `PackageManager`. Всё, что выше этой границы, работает с
 * [InstalledApp] и типов фреймворка не видит.
 *
 * Внимание здесь намеренно *не* считается: источник данных сообщает факты, а толкует их конвейер
 * проверки.
 */
interface InstalledAppsDataSource {
    suspend fun loadApps(): List<InstalledApp>
    suspend fun loadApp(packageName: String): InstalledApp?
    suspend fun labelOf(packageName: String): String?
}

@Singleton
class PlatformInstalledAppsDataSource @Inject constructor(
    private val packageManager: PackageManager,
    private val permissionInspector: PermissionInspector,
    private val specialAccessInspector: SpecialAccessInspector,
) : InstalledAppsDataSource {

    override suspend fun loadApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val packages = runCatching {
            packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }.getOrElse { emptyList() }

        if (packages.isEmpty()) return@withContext emptyList()

        val environment = specialAccessInspector.readEnvironment(forceRefresh = true)

        coroutineScope {
            packages
                .chunked(CHUNK_SIZE)
                .map { chunk -> async { chunk.mapNotNull { build(it, environment) } } }
                .awaitAll()
                .flatten()
        }
    }

    override suspend fun loadApp(packageName: String): InstalledApp? = withContext(Dispatchers.IO) {
        val info = runCatching {
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        }.getOrNull() ?: return@withContext null
        build(info, specialAccessInspector.readEnvironment(forceRefresh = false))
    }

    override suspend fun labelOf(packageName: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager).toString()
        }.getOrNull()
    }

    /**
     * Собирает одно приложение. Каждый шаг здесь оборонительный: пакет могут удалить *прямо во
     * время проверки*, вендорский пакет может бросить исключение на загрузке названия, а у битого
     * APK бывает null вместо ApplicationInfo. Ничто из этого не имеет права крашнуть проверку 473
     * приложений, поэтому сбойный пакет просто пропускается.
     */
    private fun build(info: PackageInfo, environment: SpecialAccessEnvironment): InstalledApp? = try {
        val appInfo = info.applicationInfo
        if (appInfo == null) {
            null
        } else {
            @Suppress("DEPRECATION") // getRequestedPermissionsFlags() does not exist on API 36.
            val grantFlags = info.requestedPermissionsFlags
            val requested = info.requestedPermissions?.toSet().orEmpty()
            val specialAccess = specialAccessInspector.statesFor(
                packageName = info.packageName,
                uid = appInfo.uid,
                requestedPermissions = requested,
                environment = environment,
            )
            val permissions = AppOpPermissionMapping.reconcile(
                permissions = permissionInspector.inspect(info.requestedPermissions, grantFlags),
                specialAccess = specialAccess,
            )

            InstalledApp(
                packageName = info.packageName,
                label = loadLabel(appInfo, info.packageName),
                versionName = info.versionName,
                versionCode = info.longVersionCode,
                firstInstallTime = info.firstInstallTime,
                lastUpdateTime = info.lastUpdateTime,
                targetSdk = appInfo.targetSdkVersion,
                isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
                isEnabled = appInfo.enabled,
                installSource = installSourceOf(info.packageName),
                permissions = permissions,
                specialAccess = specialAccess,
                attention = AttentionAssessment.None,
            )
        }
    } catch (e: Exception) {
        // Сюда же SecurityException и NameNotFoundException от пакета, исчезнувшего посреди
        // проверки.
        null
    }

    /**
     * Пропавшее или нечитаемое название не должно оборачиваться пустой строкой в списке, поэтому
     * вместо него встаёт имя пакета.
     */
    private fun loadLabel(appInfo: ApplicationInfo, packageName: String): String =
        runCatching { appInfo.loadLabel(packageManager).toString() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: packageName

    private fun installSourceOf(packageName: String): InstallSource {
        // getInstallSourceInfo — это API 30 и выше; на 28 и 29 остаётся только устаревший
        // getInstallerPackageName. Оба имеют полное право вернуть null для сайдлоада, и тогда мы
        // сообщаем "источник не определён".
        val installer = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
        }.getOrNull()
        return InstallSource(
            installerPackage = installer,
            installerLabel = installer?.let { known ->
                KNOWN_INSTALLERS[known] ?: runCatching {
                    packageManager.getApplicationInfo(known, 0).loadLabel(packageManager).toString()
                }.getOrNull()
            },
        )
    }

    private companion object {
        const val CHUNK_SIZE = 60

        /**
         * Человеческие названия магазинов, которые мы умеем опознавать. Всё остальное откатывается
         * к названию самого установщика, а если установщика нет — сообщаем "неизвестно", не гадая.
         */
        val KNOWN_INSTALLERS = mapOf(
            "com.android.vending" to "Google Play",
            "com.google.android.packageinstaller" to "Android",
            "com.android.packageinstaller" to "Android",
            "ru.vk.store" to "RuStore",
            "com.huawei.appmarket" to "AppGallery",
            "com.sec.android.app.samsungapps" to "Galaxy Store",
            "com.amazon.venezia" to "Amazon Appstore",
            "org.fdroid.fdroid" to "F-Droid",
        )
    }
}
