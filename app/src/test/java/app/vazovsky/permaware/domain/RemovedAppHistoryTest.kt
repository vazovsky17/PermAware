package app.vazovsky.permaware.domain

import app.vazovsky.permaware.domain.diff.SnapshotDiffer
import app.vazovsky.permaware.domain.model.ChangeType
import app.vazovsky.permaware.domain.model.groupToAppChanges
import app.vazovsky.permaware.fixtures.AppFixtures
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Как только приложение удалили, у `PackageManager` уже нечего взять — ни названия, ни иконки, ни
 * версии. Всё, что история о нём показывает, обязано прийти из снимка, снятого пока оно ещё стояло,
 * и эти тесты это закрепляют.
 */
class RemovedAppHistoryTest {

    private val now = 1_757_100_000_000L

    @Test
    fun `removal carries the label captured before the app disappeared`() {
        val before = AppFixtures.snapshots(AppFixtures.messenger, AppFixtures.calculator)
        val after = AppFixtures.snapshots(AppFixtures.calculator)

        val removal = SnapshotDiffer.diff(before, after, now).single()

        assertThat(removal.type).isEqualTo(ChangeType.APP_REMOVED)
        assertThat(removal.appLabel).isEqualTo(AppFixtures.messenger.label)
        assertThat(removal.packageName).isEqualTo(AppFixtures.messenger.packageName)
        assertThat(removal.previousVersion).isEqualTo(AppFixtures.messenger.versionName)
    }

    @Test
    fun `a removal entry knows it is a removal`() {
        val before = AppFixtures.snapshots(AppFixtures.messenger)
        val after = emptyList<app.vazovsky.permaware.domain.model.AppSnapshot>()
        val change = SnapshotDiffer.diff(before, after, now).groupToAppChanges().single()

        assertThat(change.isRemoval).isTrue()
        assertThat(change.displayLabel).isEqualTo(AppFixtures.messenger.label)
    }

    @Test
    fun `a label that is only the package name is not passed off as a name`() {
        val nameless = AppFixtures.app(
            packageName = "com.example.ghost",
            label = "com.example.ghost",
        )
        val change = SnapshotDiffer.diff(
            AppFixtures.snapshots(nameless),
            emptyList(),
            now,
        ).groupToAppChanges().single()

        assertThat(change.isRemoval).isTrue()
        assertThat(change.displayLabel).isNull()
    }

    @Test
    fun `a blank label is treated as unknown`() {
        val blank = AppFixtures.app(packageName = "com.example.blank", label = "  ")
        val change = SnapshotDiffer.diff(
            AppFixtures.snapshots(blank),
            emptyList(),
            now,
        ).groupToAppChanges().single()
        assertThat(change.displayLabel).isNull()
    }

    @Test
    fun `an ordinary change is not marked as a removal`() {
        val before = AppFixtures.snapshots(AppFixtures.messenger)
        val after = AppFixtures.snapshots(
            AppFixtures.messenger.copy(versionCode = 999, versionName = "9.9"),
        )
        val change = SnapshotDiffer.diff(before, after, now).groupToAppChanges().single()
        assertThat(change.isRemoval).isFalse()
    }

    @Test
    fun `preferences expose no wallpaper colour flag`() {
        val fields = app.vazovsky.permaware.data.prefs.UserPreferences::class.java.declaredFields
            .map { it.name.lowercase() }
        assertThat(fields.none { "dynamic" in it || "wallpaper" in it }).isTrue()
    }
}
