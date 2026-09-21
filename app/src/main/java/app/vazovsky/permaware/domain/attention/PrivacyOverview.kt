package app.vazovsky.permaware.domain.attention

import app.vazovsky.permaware.domain.model.InstalledApp
import app.vazovsky.permaware.domain.model.PermissionCategory
import app.vazovsky.permaware.domain.model.PermissionState
import app.vazovsky.permaware.domain.model.SpecialAccessStatus
import app.vazovsky.permaware.domain.model.SpecialAccessType
import app.vazovsky.permaware.domain.permission.PermissionCatalog
import kotlin.math.roundToInt

data class PrivacyReason(
    val kind: PrivacyReasonKind,
    val appCount: Int,
    val penalty: Int,
    val category: PermissionCategory? = null,
)

enum class PrivacyReasonKind {
    APPS_WITH_CATEGORY,
    APPS_WITH_BACKGROUND_LOCATION,
    APPS_WITH_ACTIVE_SPECIAL_ACCESS,
    APPS_NEEDING_ATTENTION,
}

data class PrivacyOverview(
    val score: Int,
    val reasons: List<PrivacyReason>,
    val appsNeedingAttention: Int,
    val userAppCount: Int,
    val categoryCounts: Map<PermissionCategory, Int>,
)

object PrivacyOverviewCalculator {

    private const val MAX_SCORE = 100

    /** Во сколько баллов обходится категория, если её держат *все* пользовательские приложения. */
    private data class Rule(val category: PermissionCategory, val weight: Int)

    private val CATEGORY_RULES = listOf(
        Rule(PermissionCategory.LOCATION, weight = 18),
        Rule(PermissionCategory.MICROPHONE, weight = 16),
        Rule(PermissionCategory.CAMERA, weight = 14),
        Rule(PermissionCategory.SMS, weight = 12),
        Rule(PermissionCategory.CONTACTS, weight = 10),
        Rule(PermissionCategory.CALL_LOG, weight = 8),
    )

    private const val BACKGROUND_LOCATION_WEIGHT = 14
    private const val SPECIAL_ACCESS_WEIGHT = 10
    private const val ATTENTION_WEIGHT = 10

    private const val BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION"

    private val NON_REPORTABLE = setOf(
        PermissionCategory.SPECIAL_ACCESS,
        PermissionCategory.SYSTEM,
        PermissionCategory.OTHER,
    )

    val reportableCategories: List<PermissionCategory> =
        (PermissionCatalog.headlineCategories + PermissionCategory.entries)
            .distinct()
            .filterNot { it in NON_REPORTABLE }

    /** Особый доступ настолько широкий, что сам факт его включения стоит отдельного вычета. */
    private val FAR_REACHING_ACCESS = setOf(
        SpecialAccessType.ACCESSIBILITY,
        SpecialAccessType.NOTIFICATION_LISTENER,
        SpecialAccessType.DEVICE_ADMIN,
        SpecialAccessType.INSTALL_UNKNOWN_APPS,
        SpecialAccessType.ALL_FILES_ACCESS,
    )

    fun calculate(apps: List<InstalledApp>): PrivacyOverview {
        val userApps = apps.filter { it.isUserApp }
        val categoryCounts = categoryCounts(userApps)

        if (userApps.isEmpty()) {
            return PrivacyOverview(
                score = MAX_SCORE,
                reasons = emptyList(),
                appsNeedingAttention = 0,
                userAppCount = 0,
                categoryCounts = categoryCounts,
            )
        }

        val total = userApps.size
        val reasons = mutableListOf<PrivacyReason>()
        var score = MAX_SCORE

        CATEGORY_RULES.forEach { rule ->
            val count = categoryCounts[rule.category] ?: 0
            if (count > 0) {
                val penalty = penaltyFor(rule.weight, count, total)
                score -= penalty
                reasons += PrivacyReason(PrivacyReasonKind.APPS_WITH_CATEGORY, count, penalty, rule.category)
            }
        }

        val backgroundLocationApps = userApps.count { app ->
            app.permissions.any { it.name == BACKGROUND_LOCATION && it.state == PermissionState.GRANTED }
        }
        if (backgroundLocationApps > 0) {
            val penalty = penaltyFor(BACKGROUND_LOCATION_WEIGHT, backgroundLocationApps, total)
            score -= penalty
            reasons += PrivacyReason(
                PrivacyReasonKind.APPS_WITH_BACKGROUND_LOCATION,
                backgroundLocationApps,
                penalty,
            )
        }

        val specialAccessApps = apps.count { app ->
            app.specialAccess.any { it.status == SpecialAccessStatus.ACTIVE && it.type in FAR_REACHING_ACCESS }
        }
        if (specialAccessApps > 0) {
            val penalty = penaltyFor(SPECIAL_ACCESS_WEIGHT, specialAccessApps, total)
            score -= penalty
            reasons += PrivacyReason(
                PrivacyReasonKind.APPS_WITH_ACTIVE_SPECIAL_ACCESS,
                specialAccessApps,
                penalty,
            )
        }

        val attentionApps = userApps.count { it.needsAttention }
        if (attentionApps > 0) {
            val penalty = penaltyFor(ATTENTION_WEIGHT, attentionApps, total)
            score -= penalty
            reasons += PrivacyReason(PrivacyReasonKind.APPS_NEEDING_ATTENTION, attentionApps, penalty)
        }

        return PrivacyOverview(
            score = score.coerceIn(0, MAX_SCORE),
            reasons = reasons.filter { it.penalty > 0 }.sortedByDescending { it.penalty },
            appsNeedingAttention = attentionApps,
            userAppCount = total,
            categoryCounts = categoryCounts,
        )
    }

    private fun penaltyFor(weight: Int, count: Int, total: Int): Int =
        (weight * (count.toDouble() / total)).roundToInt().coerceAtLeast(1).coerceAtMost(weight)

    fun categoryCounts(userApps: List<InstalledApp>): Map<PermissionCategory, Int> =
        reportableCategories.associateWith { category ->
            userApps.count { it.hasAccessTo(category) }
        }.filterValues { it > 0 }
}
