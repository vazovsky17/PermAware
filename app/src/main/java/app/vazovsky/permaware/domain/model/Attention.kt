package app.vazovsky.permaware.domain.model

enum class AttentionLevel {
    LOW,
    MODERATE,
    ELEVATED,
}

/** Что именно добавило баллов к оценке. */
enum class AttentionFactorKind {
    /** Выданное разрешение из особо чувствительной категории. */
    GRANTED_SENSITIVE_PERMISSION,

    /** Доступ к местоположению, который продолжается, когда приложением не пользуются. */
    BACKGROUND_LOCATION,

    /** Подтверждённый особый доступ, дающий широкие полномочия на устройстве. */
    ACTIVE_SPECIAL_ACCESS,

    /** Серьёзная возможность, заявленная в манифесте, но выдана она или нет — Android не скажет. */
    DECLARED_SPECIAL_ACCESS,

    /** Несколько особо чувствительных категорий выданы разом. */
    SENSITIVE_COMBINATION,

    /** Установлено не из известного магазина приложений. */
    UNKNOWN_INSTALL_SOURCE,
}

/**
 * Одна причина, стоящая за оценкой. [categoryRef]/[permissionRef]/[specialAccessRef] дают
 * интерфейсу собрать конкретное локализованное объяснение, а доменному слою — ничего не
 * знать про ресурсы.
 */
data class AttentionFactor(
    val kind: AttentionFactorKind,
    val points: Int,
    val categoryRef: PermissionCategory? = null,
    val permissionRef: String? = null,
    val specialAccessRef: SpecialAccessType? = null,
    val count: Int = 1,
)

data class AttentionAssessment(val level: AttentionLevel, val score: Int, val factors: List<AttentionFactor>) {
    companion object {
        val None = AttentionAssessment(AttentionLevel.LOW, 0, emptyList())
    }
}
