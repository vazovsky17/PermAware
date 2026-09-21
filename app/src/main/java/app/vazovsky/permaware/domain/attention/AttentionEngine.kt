package app.vazovsky.permaware.domain.attention

import app.vazovsky.permaware.domain.model.AppPermission
import app.vazovsky.permaware.domain.model.AttentionAssessment
import app.vazovsky.permaware.domain.model.AttentionFactor
import app.vazovsky.permaware.domain.model.AttentionFactorKind
import app.vazovsky.permaware.domain.model.AttentionLevel
import app.vazovsky.permaware.domain.model.PermissionCategory
import app.vazovsky.permaware.domain.model.PermissionState
import app.vazovsky.permaware.domain.model.Sensitivity
import app.vazovsky.permaware.domain.model.SpecialAccessState
import app.vazovsky.permaware.domain.model.SpecialAccessStatus
import app.vazovsky.permaware.domain.model.SpecialAccessType
import app.vazovsky.permaware.domain.permission.PermissionCatalog
import kotlin.math.roundToInt

object AttentionEngine {

    /** Множители для первой, второй, третьей… по силе выданной категории. */
    private val DECAY = doubleArrayOf(1.0, 0.7, 0.5, 0.35, 0.25)
    private const val TAIL_DECAY = 0.15

    val SCORED_AS_SPECIAL_ACCESS = setOf(
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.WRITE_SETTINGS",
        "android.permission.SCHEDULE_EXACT_ALARM",
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
        "android.permission.BIND_VPN_SERVICE",
        "android.permission.BIND_DEVICE_ADMIN",
    )

    private const val BACKGROUND_LOCATION_POINTS = 25
    private const val SENSITIVE_COMBINATION_POINTS = 8
    private const val UNKNOWN_SOURCE_POINTS = 5

    /** Сколько разных выданных категорий высокой чувствительности включают фактор сочетания. */
    private const val SENSITIVE_COMBINATION_THRESHOLD = 3

    const val MODERATE_THRESHOLD = 22
    const val ELEVATED_THRESHOLD = 50
    const val MAX_SCORE = 100

    /** Баллы за особый доступ, который мы смогли проверить и убедиться, что он включён. */
    private val ACTIVE_SPECIAL_ACCESS_POINTS = mapOf(
        SpecialAccessType.ACCESSIBILITY to 30,
        SpecialAccessType.NOTIFICATION_LISTENER to 22,
        SpecialAccessType.DEVICE_ADMIN to 22,
        SpecialAccessType.BATTERY_UNRESTRICTED to 4,
    )

    /**
     * Баллы за возможности, которые приложение заявляет, но выданы они или нет — Android не
     * раскрывает. Половина того, что та же возможность набрала бы, если бы мы знали точно.
     */
    private val DECLARED_SPECIAL_ACCESS_POINTS = mapOf(
        SpecialAccessType.INSTALL_UNKNOWN_APPS to 12,
        SpecialAccessType.ALL_FILES_ACCESS to 10,
        SpecialAccessType.DISPLAY_OVER_OTHER_APPS to 8,
        SpecialAccessType.USAGE_ACCESS to 8,
        SpecialAccessType.ACCESSIBILITY to 8,
        SpecialAccessType.NOTIFICATION_LISTENER to 8,
        SpecialAccessType.VPN_SERVICE to 8,
        SpecialAccessType.DEVICE_ADMIN to 8,
        SpecialAccessType.WRITE_SYSTEM_SETTINGS to 5,
        SpecialAccessType.EXACT_ALARM to 2,
        SpecialAccessType.PICTURE_IN_PICTURE to 0,
    )

    fun assess(
        permissions: List<AppPermission>,
        specialAccess: List<SpecialAccessState>,
        isSystemApp: Boolean,
        hasKnownInstallSource: Boolean,
    ): AttentionAssessment {
        val factors = mutableListOf<AttentionFactor>()

        // --- 1. Выданные чувствительные разрешения, по сильнейшему на категорию ---
        val granted = permissions.filter {
            it.state == PermissionState.GRANTED &&
                it.sensitivity != Sensitivity.NONE &&
                it.name !in SCORED_AS_SPECIAL_ACCESS &&
                // Фоновое местоположение считается один раз — своим усилителем ниже. Оставь мы его
                // ещё и в проходе по категориям, один и тот же факт учитывался бы дважды и
                // отправлял бы каждое такое приложение прямиком в ELEVATED.
                it.name != BACKGROUND_LOCATION
        }

        val perCategory: List<Triple<PermissionCategory, String, Int>> = granted
            .groupBy { it.category }
            .mapNotNull { (category, perms) ->
                val strongest = perms.maxByOrNull { weightOf(it) } ?: return@mapNotNull null
                val weight = weightOf(strongest)
                if (weight <= 0) null else Triple(category, strongest.name, weight)
            }
            .sortedByDescending { it.third }

        perCategory.forEachIndexed { index, (category, permission, weight) ->
            val multiplier = DECAY.getOrElse(index) { TAIL_DECAY }
            val points = (weight * multiplier).roundToInt()
            if (points > 0) {
                factors += AttentionFactor(
                    kind = AttentionFactorKind.GRANTED_SENSITIVE_PERMISSION,
                    points = points,
                    categoryRef = category,
                    permissionRef = permission,
                )
            }
        }

        // --- 2. Фоновое местоположение: доступ, который продолжается без пользователя ---
        val backgroundLocation = permissions.firstOrNull {
            it.name == BACKGROUND_LOCATION && it.state == PermissionState.GRANTED
        }
        if (backgroundLocation != null) {
            factors += AttentionFactor(
                kind = AttentionFactorKind.BACKGROUND_LOCATION,
                points = BACKGROUND_LOCATION_POINTS,
                categoryRef = PermissionCategory.LOCATION,
                permissionRef = BACKGROUND_LOCATION,
            )
        }

        // --- 3. Особый доступ ---
        specialAccess.forEach { access ->
            when (access.status) {
                SpecialAccessStatus.ACTIVE -> {
                    val points = ACTIVE_SPECIAL_ACCESS_POINTS[access.type] ?: 0
                    if (points > 0) {
                        factors += AttentionFactor(
                            kind = AttentionFactorKind.ACTIVE_SPECIAL_ACCESS,
                            points = points,
                            specialAccessRef = access.type,
                        )
                    }
                }
                SpecialAccessStatus.DECLARED -> {
                    val points = DECLARED_SPECIAL_ACCESS_POINTS[access.type] ?: 0
                    if (points > 0) {
                        factors += AttentionFactor(
                            kind = AttentionFactorKind.DECLARED_SPECIAL_ACCESS,
                            points = points,
                            specialAccessRef = access.type,
                        )
                    }
                }
                SpecialAccessStatus.INACTIVE, SpecialAccessStatus.UNDETERMINED -> Unit
            }
        }

        // --- 4. Несколько особо чувствительных категорий разом ---
        val highCategories = granted.filter { it.sensitivity == Sensitivity.HIGH }
            .map { it.category }
            .toSet()
        if (highCategories.size >= SENSITIVE_COMBINATION_THRESHOLD) {
            factors += AttentionFactor(
                kind = AttentionFactorKind.SENSITIVE_COMBINATION,
                points = SENSITIVE_COMBINATION_POINTS,
                count = highCategories.size,
            )
        }

        // --- 5. Установлено из неопознанного источника (только пользовательские) ---
        if (!isSystemApp && !hasKnownInstallSource && factors.isNotEmpty()) {
            factors += AttentionFactor(
                kind = AttentionFactorKind.UNKNOWN_INSTALL_SOURCE,
                points = UNKNOWN_SOURCE_POINTS,
            )
        }

        val score = factors.sumOf { it.points }.coerceIn(0, MAX_SCORE)
        return AttentionAssessment(
            level = levelFor(score),
            score = score,
            factors = factors.sortedByDescending { it.points },
        )
    }

    fun levelFor(score: Int): AttentionLevel = when {
        score >= ELEVATED_THRESHOLD -> AttentionLevel.ELEVATED
        score >= MODERATE_THRESHOLD -> AttentionLevel.MODERATE
        else -> AttentionLevel.LOW
    }

    private fun weightOf(permission: AppPermission): Int = PermissionCatalog.find(permission.name)?.attentionWeight
        ?: when (permission.sensitivity) {
            Sensitivity.HIGH -> 10
            Sensitivity.NOTABLE -> 5
            Sensitivity.NONE -> 0
        }

    private const val BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION"
}
