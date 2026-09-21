package app.vazovsky.permaware.domain

import app.vazovsky.permaware.domain.diff.SnapshotDiffer
import app.vazovsky.permaware.domain.model.ChangeType
import app.vazovsky.permaware.domain.model.PermissionState
import app.vazovsky.permaware.domain.model.SpecialAccessState
import app.vazovsky.permaware.domain.model.SpecialAccessStatus
import app.vazovsky.permaware.domain.model.SpecialAccessType
import app.vazovsky.permaware.fixtures.AppFixtures
import app.vazovsky.permaware.fixtures.AppFixtures.PERM_BACKGROUND_LOCATION
import app.vazovsky.permaware.fixtures.AppFixtures.PERM_CAMERA
import app.vazovsky.permaware.fixtures.AppFixtures.PERM_CONTACTS
import app.vazovsky.permaware.fixtures.AppFixtures.PERM_MIC
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private const val NOW = 1_757_100_000_000

class SnapshotDifferTest {

    @Test
    fun `first scan produces no changes`() {
        val current = AppFixtures.snapshots(AppFixtures.messenger, AppFixtures.calculator)
        val changes = SnapshotDiffer.diff(previous = emptyList(), current = current, timestamp = NOW)
        assertThat(changes).isEmpty()
    }

    @Test
    fun `identical scans produce no changes`() {
        val snapshots = AppFixtures.snapshots(AppFixtures.messenger, AppFixtures.calculator)
        assertThat(SnapshotDiffer.diff(snapshots, snapshots, NOW)).isEmpty()
    }

    @Test
    fun `installed app is detected`() {
        val before = AppFixtures.snapshots(AppFixtures.calculator)
        val after = AppFixtures.snapshots(AppFixtures.calculator, AppFixtures.messenger)
        val changes = SnapshotDiffer.diff(before, after, NOW)
        assertThat(changes).hasSize(1)
        assertThat(changes.first().type).isEqualTo(ChangeType.APP_INSTALLED)
        assertThat(changes.first().packageName).isEqualTo(AppFixtures.messenger.packageName)
    }

    @Test
    fun `removed app is detected and keeps its label`() {
        val before = AppFixtures.snapshots(AppFixtures.calculator, AppFixtures.messenger)
        val after = AppFixtures.snapshots(AppFixtures.calculator)
        val changes = SnapshotDiffer.diff(before, after, NOW)
        assertThat(changes).hasSize(1)
        assertThat(changes.first().type).isEqualTo(ChangeType.APP_REMOVED)
        // Приложения больше нет, значит название обязано прийти из прошлого снимка.
        assertThat(changes.first().appLabel).isEqualTo(AppFixtures.messenger.label)
    }

    @Test
    fun `version change is reported with both versions`() {
        val before = AppFixtures.snapshots(AppFixtures.messenger)
        val updated = AppFixtures.messenger.copy(versionName = "5.9.0", versionCode = 590)
        val changes = SnapshotDiffer.diff(before, AppFixtures.snapshots(updated), NOW)
        val update = changes.single { it.type == ChangeType.APP_UPDATED }
        assertThat(update.previousVersion).isEqualTo("5.8.0")
        assertThat(update.newVersion).isEqualTo("5.9.0")
    }

    @Test
    fun `a new permission after an update is reported`() {
        val before = AppFixtures.snapshots(AppFixtures.messenger)
        val updated = AppFixtures.messenger.copy(
            versionName = "5.9.0",
            versionCode = 590,
            permissions = AppFixtures.messenger.permissions +
                AppFixtures.permission(PERM_BACKGROUND_LOCATION),
        )
        val changes = SnapshotDiffer.diff(before, AppFixtures.snapshots(updated), NOW)

        assertThat(changes.map { it.type })
            .containsAtLeast(ChangeType.APP_UPDATED, ChangeType.PERMISSION_REQUEST_ADDED)
        val added = changes.single { it.type == ChangeType.PERMISSION_REQUEST_ADDED }
        assertThat(added.permission).isEqualTo(PERM_BACKGROUND_LOCATION)
        assertThat(added.category)
            .isEqualTo(app.vazovsky.permaware.domain.model.PermissionCategory.LOCATION)
    }

    @Test
    fun `a removed permission request is reported`() {
        val before = AppFixtures.snapshots(AppFixtures.messenger)
        val trimmed = AppFixtures.messenger.copy(
            permissions = AppFixtures.messenger.permissions.filterNot { it.name == PERM_MIC },
        )
        val changes = SnapshotDiffer.diff(before, AppFixtures.snapshots(trimmed), NOW)
        val removed = changes.single { it.type == ChangeType.PERMISSION_REQUEST_REMOVED }
        assertThat(removed.permission).isEqualTo(PERM_MIC)
    }

    @Test
    fun `granting a permission is reported`() {
        val before = AppFixtures.snapshots(
            AppFixtures.messenger.copy(
                permissions = listOf(AppFixtures.permission(PERM_CAMERA, PermissionState.DENIED)),
            ),
        )
        val after = AppFixtures.snapshots(
            AppFixtures.messenger.copy(
                permissions = listOf(AppFixtures.permission(PERM_CAMERA, PermissionState.GRANTED)),
            ),
        )
        val changes = SnapshotDiffer.diff(before, after, NOW)
        assertThat(changes.single().type).isEqualTo(ChangeType.PERMISSION_GRANTED)
    }

    @Test
    fun `revoking a permission is reported`() {
        val before = AppFixtures.snapshots(
            AppFixtures.messenger.copy(
                permissions = listOf(AppFixtures.permission(PERM_CONTACTS, PermissionState.GRANTED)),
            ),
        )
        val after = AppFixtures.snapshots(
            AppFixtures.messenger.copy(
                permissions = listOf(AppFixtures.permission(PERM_CONTACTS, PermissionState.DENIED)),
            ),
        )
        val changes = SnapshotDiffer.diff(before, after, NOW)
        assertThat(changes.single().type).isEqualTo(ChangeType.PERMISSION_REVOKED)
    }

    @Test
    fun `transitions involving UNDETERMINED are never reported as grant changes`() {
        // "Раньше мы не могли понять, а теперь можем" говорит что-то про Android, а не про
        // приложение, и появляться в истории пользователя как изменение разрешения не должно.
        val undetermined = AppFixtures.messenger.copy(
            permissions = listOf(
                AppFixtures.permission(PERM_CAMERA, PermissionState.UNDETERMINED),
            ),
        )
        val granted = AppFixtures.messenger.copy(
            permissions = listOf(AppFixtures.permission(PERM_CAMERA, PermissionState.GRANTED)),
        )
        val forward = SnapshotDiffer.diff(
            AppFixtures.snapshots(undetermined),
            AppFixtures.snapshots(granted),
            NOW,
        )
        val backward = SnapshotDiffer.diff(
            AppFixtures.snapshots(granted),
            AppFixtures.snapshots(undetermined),
            NOW,
        )
        assertThat(forward).isEmpty()
        assertThat(backward).isEmpty()
    }

    @Test
    fun `special access turning on is reported`() {
        val before = AppFixtures.snapshots(
            AppFixtures.messenger.copy(
                specialAccess = listOf(
                    SpecialAccessState(SpecialAccessType.ACCESSIBILITY, SpecialAccessStatus.INACTIVE),
                ),
            ),
        )
        val after = AppFixtures.snapshots(
            AppFixtures.messenger.copy(
                specialAccess = listOf(
                    SpecialAccessState(SpecialAccessType.ACCESSIBILITY, SpecialAccessStatus.ACTIVE),
                ),
            ),
        )
        val changes = SnapshotDiffer.diff(before, after, NOW)
        val event = changes.single()
        assertThat(event.type).isEqualTo(ChangeType.SPECIAL_ACCESS_GAINED)
        assertThat(event.specialAccess).isEqualTo(SpecialAccessType.ACCESSIBILITY)
    }

    @Test
    fun `special access turning off is reported`() {
        val before = AppFixtures.snapshots(
            AppFixtures.messenger.copy(
                specialAccess = listOf(
                    SpecialAccessState(SpecialAccessType.NOTIFICATION_LISTENER, SpecialAccessStatus.ACTIVE),
                ),
            ),
        )
        val after = AppFixtures.snapshots(
            AppFixtures.messenger.copy(
                specialAccess = listOf(
                    SpecialAccessState(SpecialAccessType.NOTIFICATION_LISTENER, SpecialAccessStatus.INACTIVE),
                ),
            ),
        )
        assertThat(SnapshotDiffer.diff(before, after, NOW).single().type)
            .isEqualTo(ChangeType.SPECIAL_ACCESS_LOST)
    }

    @Test
    fun `a declared-only special access change is not reported as gained`() {
        // Согласился ли на это человек, мы не знаем, так что заявить, будто приложение это
        // "получило", было бы ложью.
        val before = AppFixtures.snapshots(AppFixtures.messenger.copy(specialAccess = emptyList()))
        val after = AppFixtures.snapshots(
            AppFixtures.messenger.copy(
                specialAccess = listOf(
                    SpecialAccessState(SpecialAccessType.DISPLAY_OVER_OTHER_APPS, SpecialAccessStatus.DECLARED),
                ),
            ),
        )
        assertThat(SnapshotDiffer.diff(before, after, NOW)).isEmpty()
    }

    @Test
    fun `output is deterministic regardless of input order`() {
        val before = AppFixtures.snapshots(AppFixtures.messenger, AppFixtures.calculator)
        val after = AppFixtures.snapshots(
            AppFixtures.messenger.copy(versionCode = 999, versionName = "9.9"),
            AppFixtures.cameraApp,
        )
        val first = SnapshotDiffer.diff(before, after, NOW)
        val second = SnapshotDiffer.diff(before.reversed(), after.reversed(), NOW)
        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `many simultaneous changes are all reported`() {
        val before = AppFixtures.snapshots(
            AppFixtures.messenger,
            AppFixtures.calculator,
            AppFixtures.cameraApp,
        )
        val after = AppFixtures.snapshots(
            AppFixtures.messenger.copy(
                versionCode = 590,
                versionName = "5.9.0",
                permissions = AppFixtures.messenger.permissions + AppFixtures.permission(PERM_BACKGROUND_LOCATION),
            ),
            AppFixtures.cameraApp,
            AppFixtures.microphoneApp,
        )
        val changes = SnapshotDiffer.diff(before, after, NOW)
        assertThat(changes.map { it.type }).containsAtLeast(
            ChangeType.APP_INSTALLED,
            ChangeType.APP_REMOVED,
            ChangeType.APP_UPDATED,
            ChangeType.PERMISSION_REQUEST_ADDED,
        )
        assertThat(changes.all { it.timestamp == NOW }).isTrue()
    }
}
