package app.vazovsky.permaware.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Insert
    suspend fun insert(scan: ScanEntity): Long

    @Query("SELECT * FROM scans ORDER BY finished_at DESC LIMIT 1")
    fun observeLatest(): Flow<ScanEntity?>

    @Query("SELECT * FROM scans ORDER BY finished_at DESC LIMIT 1")
    suspend fun latest(): ScanEntity?

    @Query("SELECT * FROM scans ORDER BY finished_at DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ScanEntity>

    @Query("SELECT COUNT(*) FROM scans")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM scans")
    fun observeCount(): Flow<Int>

    /** Удаляет проверки старше [cutoff], оставляя не меньше [keepAtLeast] самых свежих. */
    @Query(
        """
        DELETE FROM scans
        WHERE finished_at < :cutoff
          AND id NOT IN (SELECT id FROM scans ORDER BY finished_at DESC LIMIT :keepAtLeast)
        """,
    )
    suspend fun deleteOlderThan(cutoff: Long, keepAtLeast: Int): Int

    @Query("DELETE FROM scans")
    suspend fun deleteAll()
}

@Dao
interface SnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(snapshots: List<AppSnapshotEntity>)

    @Query("SELECT * FROM app_snapshots WHERE scan_id = :scanId")
    suspend fun forScan(scanId: Long): List<AppSnapshotEntity>

    @Query("SELECT * FROM app_snapshots WHERE scan_id = :scanId AND package_name = :packageName")
    suspend fun forApp(scanId: Long, packageName: String): AppSnapshotEntity?

    /**
     * Снимки существуют только затем, чтобы сравнить с ними следующую проверку, поэтому всё, кроме
     * [keep] последних, выбрасывается. История, которую читает пользователь, живёт в
     * `change_events` и хранится куда дольше.
     */
    @Query(
        """
        DELETE FROM app_snapshots
        WHERE scan_id NOT IN (SELECT id FROM scans ORDER BY finished_at DESC LIMIT :keep)
        """,
    )
    suspend fun trimToRecentScans(keep: Int): Int

    @Query("SELECT COUNT(*) FROM app_snapshots")
    suspend fun count(): Int
}

@Dao
interface ChangeDao {

    @Insert
    suspend fun insertAll(events: List<ChangeEventEntity>)

    @Query("SELECT * FROM change_events ORDER BY timestamp DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ChangeEventEntity>>

    @Query("SELECT * FROM change_events WHERE package_name = :packageName ORDER BY timestamp DESC, id DESC")
    fun observeForApp(packageName: String): Flow<List<ChangeEventEntity>>

    @Query("SELECT * FROM change_events WHERE scan_id = :scanId ORDER BY id")
    suspend fun forScan(scanId: Long): List<ChangeEventEntity>

    @Query("SELECT COUNT(*) FROM change_events")
    suspend fun count(): Int

    @Query("DELETE FROM change_events WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int

    @Query("DELETE FROM change_events")
    suspend fun deleteAll()
}
