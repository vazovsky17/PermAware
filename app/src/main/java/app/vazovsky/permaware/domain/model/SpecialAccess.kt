package app.vazovsky.permaware.domain.model

/**
 * Возможности, живущие за пределами обычной модели runtime-разрешений («особый доступ» в
 * системных настройках). Про большинство из них Android намеренно не раскрывает, выдан ли
 * доступ стороннему приложению. Что именно проверяемо, перечислено в
 * PlatformSpecialAccessInspector.
 */
enum class SpecialAccessType {
    ACCESSIBILITY,
    NOTIFICATION_LISTENER,
    DEVICE_ADMIN,
    BATTERY_UNRESTRICTED,
    VPN_SERVICE,
    DISPLAY_OVER_OTHER_APPS,
    INSTALL_UNKNOWN_APPS,
    USAGE_ACCESS,
    EXACT_ALARM,
    ALL_FILES_ACCESS,
    WRITE_SYSTEM_SETTINGS,
    PICTURE_IN_PICTURE,
}

/**
 * Что мы можем честно сказать про запись об особом доступе.
 *
 * [ACTIVE]/[INACTIVE] ставятся только тогда, когда на вопрос отвечает документированный
 * публичный API. [DECLARED] означает вот что: возможность заявлена в манифесте и приложение
 * может попросить её у пользователя, но согласился тот или нет — платформа не сообщает.
 */
enum class SpecialAccessStatus {
    ACTIVE,
    INACTIVE,
    DECLARED,
    UNDETERMINED,
}

data class SpecialAccessState(val type: SpecialAccessType, val status: SpecialAccessStatus) {
    /** Статус — это факт про нынешнее устройство или всего лишь запись в манифесте. */
    val isVerified: Boolean
        get() = status == SpecialAccessStatus.ACTIVE || status == SpecialAccessStatus.INACTIVE
}
