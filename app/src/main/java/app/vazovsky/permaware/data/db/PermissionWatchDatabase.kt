package app.vazovsky.permaware.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Локальное хранилище проверок, снимков и истории — всё на устройстве. Отсюда ничего никогда не
 * синхронизируется и никуда не выгружается; база лежит в приватном хранилище приложения и исключена
 * из облачного бэкапа.
 *
 * Схемы экспортируются в `app/schemas`. Любое будущее изменение сущности обязано поднять [VERSION],
 * привезти `Migration` и добавить случай в `MigrationTest` — `fallbackToDestructiveMigration` не
 * используется намеренно: молча стереть историю значило бы уничтожить ту единственную возможность,
 * которой нужна непрерывность.
 */
@Database(
    entities = [ScanEntity::class, AppSnapshotEntity::class, ChangeEventEntity::class],
    version = PermissionWatchDatabase.VERSION,
    exportSchema = true,
)
abstract class PermissionWatchDatabase : RoomDatabase() {

    abstract fun scanDao(): ScanDao
    abstract fun snapshotDao(): SnapshotDao
    abstract fun changeDao(): ChangeDao

    companion object {
        const val VERSION = 1
        const val NAME = "permission_watch.db"
    }
}
