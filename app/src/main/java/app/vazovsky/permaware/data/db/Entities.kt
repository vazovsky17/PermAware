package app.vazovsky.permaware.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long,
    @ColumnInfo(name = "app_count") val appCount: Int,
    @ColumnInfo(name = "user_app_count") val userAppCount: Int,
    @ColumnInfo(name = "change_count") val changeCount: Int,
    @ColumnInfo(name = "privacy_score") val privacyScore: Int,
    @ColumnInfo(name = "trigger") val trigger: String,
)

/**
 * Одно приложение глазами одной проверки.
 *
 * Разрешения и особый доступ лежат компактными закодированными строками, а не дочерними таблицами:
 * устройство с 473 приложениями даёт около 7000 строк разрешений на проверку, а единственный
 * запрос, который мы к ним вообще делаем, — «отдай снимок целиком, буду сравнивать». См.
 * [SnapshotCodec].
 *
 * Иконки здесь не хранятся никогда — они грузятся по требованию из `PackageManager`.
 */
@Entity(
    tableName = "app_snapshots",
    primaryKeys = ["scan_id", "package_name"],
    foreignKeys = [
        ForeignKey(
            entity = ScanEntity::class,
            parentColumns = ["id"],
            childColumns = ["scan_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("scan_id"), Index("package_name")],
)
data class AppSnapshotEntity(
    @ColumnInfo(name = "scan_id") val scanId: Long,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "version_name") val versionName: String?,
    @ColumnInfo(name = "version_code") val versionCode: Long,
    @ColumnInfo(name = "is_system") val isSystem: Boolean,
    @ColumnInfo(name = "last_update_time") val lastUpdateTime: Long,
    @ColumnInfo(name = "permissions") val permissions: String,
    @ColumnInfo(name = "special_access") val specialAccess: String,
)

@Entity(
    tableName = "change_events",
    foreignKeys = [
        ForeignKey(
            entity = ScanEntity::class,
            parentColumns = ["id"],
            childColumns = ["scan_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("scan_id"), Index("package_name"), Index("timestamp")],
)
data class ChangeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "scan_id") val scanId: Long,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "app_label") val appLabel: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "permission") val permission: String?,
    @ColumnInfo(name = "category") val category: String?,
    @ColumnInfo(name = "special_access") val specialAccess: String?,
    @ColumnInfo(name = "previous_version") val previousVersion: String?,
    @ColumnInfo(name = "new_version") val newVersion: String?,
)
