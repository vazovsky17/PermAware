package app.vazovsky.permaware.domain

import app.vazovsky.permaware.domain.model.ChangeEvent
import app.vazovsky.permaware.domain.model.ChangeFilter
import app.vazovsky.permaware.domain.model.ChangeType
import app.vazovsky.permaware.domain.model.groupToAppChanges
import app.vazovsky.permaware.domain.model.matches
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ChangeGroupingTest {

    private fun event(
        type: ChangeType,
        timestamp: Long,
        packageName: String = "com.example.a",
        permission: String? = null,
    ) = ChangeEvent(
        timestamp = timestamp,
        packageName = packageName,
        appLabel = packageName,
        type = type,
        permission = permission,
    )

    @Test
    fun `events from one scan form a single card`() {
        val grouped = listOf(
            event(ChangeType.APP_UPDATED, 1_000),
            event(ChangeType.PERMISSION_REQUEST_ADDED, 1_000, permission = "android.permission.CAMERA"),
        ).groupToAppChanges()

        assertThat(grouped).hasSize(1)
        assertThat(grouped.single().events).hasSize(2)
    }

    @Test
    fun `events from different scans stay in separate cards`() {
        // Регрессия: группировка по одному только пакету склеивала всю историю приложения в одну
        // карточку, и более позднее «приложение удалено» оказывалось внутри карточки, описывающей
        // давнее обновление.
        val grouped = listOf(
            event(ChangeType.APP_UPDATED, 1_000),
            event(ChangeType.APP_REMOVED, 2_000),
        ).groupToAppChanges()

        assertThat(grouped).hasSize(2)
        assertThat(grouped.first().timestamp).isEqualTo(2_000)
        assertThat(grouped.first().events.single().type).isEqualTo(ChangeType.APP_REMOVED)
    }

    @Test
    fun `different apps in the same scan are separate cards`() {
        val grouped = listOf(
            event(ChangeType.APP_UPDATED, 1_000, packageName = "com.example.a"),
            event(ChangeType.APP_UPDATED, 1_000, packageName = "com.example.b"),
        ).groupToAppChanges()
        assertThat(grouped).hasSize(2)
    }

    @Test
    fun `cards are ordered newest first`() {
        val grouped = listOf(
            event(ChangeType.APP_UPDATED, 1_000),
            event(ChangeType.APP_UPDATED, 3_000),
            event(ChangeType.APP_UPDATED, 2_000),
        ).groupToAppChanges()
        assertThat(grouped.map { it.timestamp }).containsExactly(3_000L, 2_000L, 1_000L).inOrder()
    }

    @Test
    fun `filters select the expected change types`() {
        assertThat(ChangeType.PERMISSION_GRANTED.matches(ChangeFilter.PERMISSIONS)).isTrue()
        assertThat(ChangeType.PERMISSION_REQUEST_ADDED.matches(ChangeFilter.PERMISSIONS)).isTrue()
        assertThat(ChangeType.APP_UPDATED.matches(ChangeFilter.PERMISSIONS)).isFalse()
        assertThat(ChangeType.APP_UPDATED.matches(ChangeFilter.UPDATES)).isTrue()
        assertThat(ChangeType.APP_INSTALLED.matches(ChangeFilter.INSTALLED)).isTrue()
        assertThat(ChangeType.APP_REMOVED.matches(ChangeFilter.REMOVED)).isTrue()
        assertThat(ChangeType.SPECIAL_ACCESS_GAINED.matches(ChangeFilter.SPECIAL_ACCESS)).isTrue()
        ChangeType.entries.forEach { assertThat(it.matches(ChangeFilter.ALL)).isTrue() }
    }
}
