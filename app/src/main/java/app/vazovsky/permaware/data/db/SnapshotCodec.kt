package app.vazovsky.permaware.data.db

import app.vazovsky.permaware.domain.model.PermissionSnapshot
import app.vazovsky.permaware.domain.model.PermissionState
import app.vazovsky.permaware.domain.model.SpecialAccessState
import app.vazovsky.permaware.domain.model.SpecialAccessStatus
import app.vazovsky.permaware.domain.model.SpecialAccessType

/**
 * Компактная кодировка для списков разрешений и особого доступа внутри строки снимка.
 *
 * Формат: `name=S;name=S`, где `S` — один символ состояния. Выбран вместо JSON потому, что полная
 * проверка устройства сохраняет около 7000 записей о разрешениях, а так строка снимка укладывается
 * в несколько сотен байт. Незнакомые символы и покалеченные записи пропускаются, а не бросают
 * исключение: испорченная строка обязана выродиться в «прошлых данных нет», но никак не в падение
 * на старте.
 */
object SnapshotCodec {

    private const val ENTRY_SEPARATOR = ';'
    private const val VALUE_SEPARATOR = '='

    fun encodePermissions(permissions: List<PermissionSnapshot>): String =
        permissions.joinToString(ENTRY_SEPARATOR.toString()) { "${it.name}$VALUE_SEPARATOR${it.state.code}" }

    fun decodePermissions(encoded: String): List<PermissionSnapshot> = encoded.splitEntries().mapNotNull { entry ->
        val index = entry.lastIndexOf(VALUE_SEPARATOR)
        if (index <= 0 || index == entry.lastIndex) return@mapNotNull null
        val state = permissionStateOf(entry[index + 1]) ?: return@mapNotNull null
        PermissionSnapshot(entry.substring(0, index), state)
    }

    fun encodeSpecialAccess(access: List<SpecialAccessState>): String =
        access.joinToString(ENTRY_SEPARATOR.toString()) { "${it.type.name}$VALUE_SEPARATOR${it.status.code}" }

    fun decodeSpecialAccess(encoded: String): List<SpecialAccessState> = encoded.splitEntries().mapNotNull { entry ->
        val index = entry.lastIndexOf(VALUE_SEPARATOR)
        if (index <= 0 || index == entry.lastIndex) return@mapNotNull null
        val type = runCatching { SpecialAccessType.valueOf(entry.substring(0, index)) }.getOrNull()
            ?: return@mapNotNull null
        val status = specialAccessStatusOf(entry[index + 1]) ?: return@mapNotNull null
        SpecialAccessState(type, status)
    }

    private fun String.splitEntries(): List<String> =
        if (isEmpty()) emptyList() else split(ENTRY_SEPARATOR).filter { it.isNotBlank() }

    private val PermissionState.code: Char
        get() = when (this) {
            PermissionState.GRANTED -> 'G'
            PermissionState.DENIED -> 'D'
            PermissionState.UNDETERMINED -> 'U'
        }

    private fun permissionStateOf(code: Char): PermissionState? = when (code) {
        'G' -> PermissionState.GRANTED
        'D' -> PermissionState.DENIED
        'U' -> PermissionState.UNDETERMINED
        else -> null
    }

    private val SpecialAccessStatus.code: Char
        get() = when (this) {
            SpecialAccessStatus.ACTIVE -> 'A'
            SpecialAccessStatus.INACTIVE -> 'I'
            SpecialAccessStatus.DECLARED -> 'D'
            SpecialAccessStatus.UNDETERMINED -> 'U'
        }

    private fun specialAccessStatusOf(code: Char): SpecialAccessStatus? = when (code) {
        'A' -> SpecialAccessStatus.ACTIVE
        'I' -> SpecialAccessStatus.INACTIVE
        'D' -> SpecialAccessStatus.DECLARED
        'U' -> SpecialAccessStatus.UNDETERMINED
        else -> null
    }
}
