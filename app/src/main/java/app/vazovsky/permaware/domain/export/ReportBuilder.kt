package app.vazovsky.permaware.domain.export

import app.vazovsky.permaware.domain.attention.PrivacyOverview
import app.vazovsky.permaware.domain.model.AttentionLevel
import app.vazovsky.permaware.domain.model.InstalledApp
import app.vazovsky.permaware.domain.model.PermissionState
import app.vazovsky.permaware.domain.model.SpecialAccessStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Собирает отчёт, которым можно поделиться.
 *
 * В отчёте ровно то, что пользователь и так видит в приложении, и ничего сверх: ни идентификаторов
 * устройства, ни учётной записи, ни меток времени, кроме тех, что на экране. Он отдаётся системному
 * листу «Поделиться»; сам PermAware никуда ничего не выгружает.
 *
 * Чистые функции над доменными моделями, так что содержимое можно проверять юнит-тестами.
 */
object ReportBuilder {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun buildText(
        apps: List<InstalledApp>,
        overview: PrivacyOverview,
        header: String,
        generatedLabel: String,
        footer: String,
    ): String = buildString {
        appendLine(header)
        appendLine(generatedLabel)
        appendLine()
        appendLine("Privacy overview: ${overview.score}/100")
        appendLine("User-installed apps: ${overview.userAppCount}")
        appendLine("Apps needing attention: ${overview.appsNeedingAttention}")
        appendLine()

        overview.categoryCounts.entries
            .sortedByDescending { it.value }
            .forEach { (category, count) -> appendLine("${category.name}: $count") }
        appendLine()

        val reported = apps.filter { it.isUserApp }.sortedByDescending { it.attention.score }
        reported.forEach { app ->
            appendLine("— ${app.label} (${app.packageName})")
            appendLine(
                "  version: ${app.versionName ?: "?"}  attention: ${app.attention.level} (${app.attention.score})",
            )
            val granted = app.permissions.filter { it.isSensitive && it.state == PermissionState.GRANTED }
            if (granted.isNotEmpty()) {
                appendLine("  granted: " + granted.joinToString { it.name.substringAfterLast('.') })
            }
            val undetermined = app.permissions.filter { it.state == PermissionState.UNDETERMINED }
            if (undetermined.isNotEmpty()) {
                appendLine("  not determinable: " + undetermined.joinToString { it.name.substringAfterLast('.') })
            }
            val active = app.specialAccess.filter { it.status == SpecialAccessStatus.ACTIVE }
            if (active.isNotEmpty()) {
                appendLine("  special access active: " + active.joinToString { it.type.name })
            }
            appendLine()
        }

        appendLine(footer)
    }

    fun buildJson(apps: List<InstalledApp>, overview: PrivacyOverview, generatedAt: Long): String {
        val report = Report(
            generatedAt = generatedAt,
            privacyScore = overview.score,
            userAppCount = overview.userAppCount,
            appsNeedingAttention = overview.appsNeedingAttention,
            categoryCounts = overview.categoryCounts.mapKeys { it.key.name },
            apps = apps.filter { it.isUserApp }
                .sortedByDescending { it.attention.score }
                .map { app ->
                    ReportApp(
                        packageName = app.packageName,
                        label = app.label,
                        versionName = app.versionName,
                        versionCode = app.versionCode,
                        isSystem = app.isSystem,
                        firstInstallTime = app.firstInstallTime,
                        lastUpdateTime = app.lastUpdateTime,
                        installer = app.installSource.installerPackage,
                        attentionLevel = app.attention.level,
                        attentionScore = app.attention.score,
                        permissions = app.permissions
                            .filter { it.isSensitive }
                            .map { ReportPermission(it.name, it.state.name, it.category.name) },
                        specialAccess = app.specialAccess.map {
                            ReportSpecialAccess(it.type.name, it.status.name)
                        },
                    )
                },
        )
        return json.encodeToString(report)
    }

    @Serializable
    data class Report(
        val format: String = "permission-watch-report",
        val version: Int = 1,
        val generatedAt: Long,
        val privacyScore: Int,
        val userAppCount: Int,
        val appsNeedingAttention: Int,
        val categoryCounts: Map<String, Int>,
        val apps: List<ReportApp>,
    )

    @Serializable
    data class ReportApp(
        val packageName: String,
        val label: String,
        val versionName: String?,
        val versionCode: Long,
        val isSystem: Boolean,
        val firstInstallTime: Long,
        val lastUpdateTime: Long,
        val installer: String?,
        val attentionLevel: AttentionLevel,
        val attentionScore: Int,
        val permissions: List<ReportPermission>,
        val specialAccess: List<ReportSpecialAccess>,
    )

    @Serializable
    data class ReportPermission(val name: String, val state: String, val category: String)

    @Serializable
    data class ReportSpecialAccess(val type: String, val status: String)
}
