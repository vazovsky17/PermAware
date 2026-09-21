package app.vazovsky.permaware.domain.model

/**
 * Крупная группировка, на которой держится весь интерфейс. Грубая намеренно: человек
 * думает про «камеру» и «контакты», а не про сорок отдельных констант разрешений.
 */
enum class PermissionCategory {
    LOCATION,
    CAMERA,
    MICROPHONE,
    CONTACTS,
    CALENDAR,
    SMS,
    PHONE,
    CALL_LOG,
    STORAGE,
    SENSORS,
    ACTIVITY,
    NEARBY,
    NOTIFICATIONS,
    ACCOUNTS,
    SPECIAL_ACCESS,
    SYSTEM,
    OTHER,
}

/**
 * Чем платформа защищает разрешение. От этого зависит, осмысленно ли вообще говорить о том,
 * выдано оно или нет, — см. [PermissionState.UNDETERMINED].
 */
enum class PermissionProtection {
    /** Выдаётся автоматически при установке, отозвать нельзя. */
    NORMAL,

    /** Runtime-разрешение. Состояние настоящее: им управляет пользователь, и его видно. */
    DANGEROUS,

    /** Достаётся только приложениям, подписанным тем же сертификатом, что и объявившее его. */
    SIGNATURE,

    /**
     * Объявлено как разрешение, но фактическим доступом управляет запись в AppOps, которую
     * платформа для сторонних приложений не показывает (SYSTEM_ALERT_WINDOW, WRITE_SETTINGS,
     * REQUEST_INSTALL_PACKAGES, PACKAGE_USAGE_STATS, MANAGE_EXTERNAL_STORAGE…).
     */
    APPOP_GATED,

    UNKNOWN,
}

/**
 * Состояние одного разрешения у одного приложения.
 *
 * [UNDETERMINED] — полноценное значение, и это сознательно: Android не даёт обычному
 * приложению прочитать состояние AppOps чужого пакета, поэтому про доступ, закрытый AppOps,
 * мы обязаны сказать «определить нельзя», а не выдумать ответ.
 */
enum class PermissionState {
    GRANTED,
    DENIED,
    UNDETERMINED,
}

/**
 * Насколько разрешение заслуживает внимания. Отсюда питаются и движок внимания, и акценты
 * в интерфейсе.
 */
enum class Sensitivity {
    /** Для обзора приватности неинтересно (INTERNET, VIBRATE, …). */
    NONE,

    /** Показать стоит, вес умеренный. */
    NOTABLE,

    /** Доступ к датчику, к личным данным или к серьёзной системной возможности. */
    HIGH,
}

/** Одно разрешение так, как его запросило одно установленное приложение. */
data class AppPermission(
    val name: String,
    val category: PermissionCategory,
    val protection: PermissionProtection,
    val state: PermissionState,
    val sensitivity: Sensitivity,
    /** Истина, когда разрешение объявлено приложением с устройства, а не самой платформой. */
    val isCustom: Boolean = false,
    /** Название от платформы. Нужно, когда разрешения нет в нашей базе знаний. */
    val platformLabel: String? = null,
    /** Описание от платформы. Нужно, когда разрешения нет в нашей базе знаний. */
    val platformDescription: String? = null,
) {
    val isSensitive: Boolean get() = sensitivity != Sensitivity.NONE

    /**
     * Истина, когда пользователь действительно может что-то с этим сделать из системных настроек.
     * В штатном интерфейсе разрешений по каждому приложению отзываются только runtime-разрешения.
     */
    val isUserControllable: Boolean get() = protection == PermissionProtection.DANGEROUS
}
