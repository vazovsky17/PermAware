package app.vazovsky.permaware.domain

import app.vazovsky.permaware.domain.model.PermissionCategory
import app.vazovsky.permaware.domain.model.Sensitivity
import app.vazovsky.permaware.domain.permission.PermissionCatalog
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PermissionCatalogTest {

    @Test
    fun `every dangerous permission group is represented`() {
        val required = listOf(
            "android.permission.CAMERA" to PermissionCategory.CAMERA,
            "android.permission.RECORD_AUDIO" to PermissionCategory.MICROPHONE,
            "android.permission.ACCESS_FINE_LOCATION" to PermissionCategory.LOCATION,
            "android.permission.ACCESS_BACKGROUND_LOCATION" to PermissionCategory.LOCATION,
            "android.permission.READ_CONTACTS" to PermissionCategory.CONTACTS,
            "android.permission.READ_SMS" to PermissionCategory.SMS,
            "android.permission.READ_CALL_LOG" to PermissionCategory.CALL_LOG,
            "android.permission.READ_CALENDAR" to PermissionCategory.CALENDAR,
            "android.permission.BODY_SENSORS" to PermissionCategory.SENSORS,
            "android.permission.ACTIVITY_RECOGNITION" to PermissionCategory.ACTIVITY,
            "android.permission.BLUETOOTH_SCAN" to PermissionCategory.NEARBY,
            "android.permission.READ_MEDIA_IMAGES" to PermissionCategory.STORAGE,
        )
        required.forEach { (permission, category) ->
            val entry = PermissionCatalog.find(permission)
            assertThat(entry).isNotNull()
            assertThat(entry!!.category).isEqualTo(category)
        }
    }

    @Test
    fun `every entry has a non-zero string resource for all three texts`() {
        PermissionCatalog.entries.values.forEach { entry ->
            assertThat(entry.titleRes).isNotEqualTo(0)
            assertThat(entry.descriptionRes).isNotEqualTo(0)
            assertThat(entry.attentionRes).isNotEqualTo(0)
        }
    }

    @Test
    fun `appop-gated permissions are marked as such`() {
        val appOpGated = listOf(
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.REQUEST_INSTALL_PACKAGES",
            "android.permission.PACKAGE_USAGE_STATS",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.WRITE_SETTINGS",
        )
        appOpGated.forEach { permission ->
            assertThat(PermissionCatalog.find(permission)?.appOpGated).isTrue()
        }
    }

    @Test
    fun `background location outweighs foreground location`() {
        val background = PermissionCatalog.find("android.permission.ACCESS_BACKGROUND_LOCATION")!!
        val fine = PermissionCatalog.find("android.permission.ACCESS_FINE_LOCATION")!!
        val coarse = PermissionCatalog.find("android.permission.ACCESS_COARSE_LOCATION")!!
        assertThat(background.attentionWeight).isGreaterThan(fine.attentionWeight)
        assertThat(fine.attentionWeight).isGreaterThan(coarse.attentionWeight)
    }

    @Test
    fun `common permissions are not treated as sensitive`() {
        // INTERNET и POST_NOTIFICATIONS есть почти у каждого приложения; помечать их значило бы
        // утопить сигнал в шуме.
        listOf(
            "android.permission.INTERNET",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.RECEIVE_BOOT_COMPLETED",
        ).forEach { permission ->
            assertThat(PermissionCatalog.find(permission)?.sensitivity).isEqualTo(Sensitivity.NONE)
        }
    }

    @Test
    fun `non-sensitive entries carry no attention weight`() {
        PermissionCatalog.entries.values
            .filter { it.sensitivity == Sensitivity.NONE }
            .forEach { assertThat(it.attentionWeight).isEqualTo(0) }
    }

    @Test
    fun `unknown permissions are simply absent rather than throwing`() {
        assertThat(PermissionCatalog.find("com.example.unknown.PERMISSION")).isNull()
        assertThat(PermissionCatalog.isKnown("com.example.unknown.PERMISSION")).isFalse()
    }

    @Test
    fun `headline categories all have permissions behind them`() {
        PermissionCatalog.headlineCategories.forEach { category ->
            assertThat(PermissionCatalog.permissionsOf(category)).isNotEmpty()
        }
    }

    @Test
    fun `catalog keys are well-formed permission names`() {
        PermissionCatalog.entries.forEach { (key, entry) ->
            assertThat(key).isEqualTo(entry.permission)
            assertThat(key).contains(".")
        }
    }
}
