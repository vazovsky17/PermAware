package app.vazovsky.permaware.data

import app.vazovsky.permaware.data.platform.AppOpPermissionMapping
import app.vazovsky.permaware.domain.model.PermissionProtection
import app.vazovsky.permaware.domain.model.PermissionState
import app.vazovsky.permaware.domain.model.SpecialAccessState
import app.vazovsky.permaware.domain.model.SpecialAccessStatus
import app.vazovsky.permaware.domain.model.SpecialAccessType
import app.vazovsky.permaware.fixtures.AppFixtures
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppOpPermissionMappingTest {

    private fun overlayPermission(state: PermissionState = PermissionState.UNDETERMINED) = AppFixtures.permission(
        AppFixtures.PERM_OVERLAY,
        state = state,
        protection = PermissionProtection.APPOP_GATED,
    )

    @Test
    fun `a verified active capability marks its permission as granted`() {
        val result = AppOpPermissionMapping.reconcile(
            permissions = listOf(overlayPermission()),
            specialAccess = listOf(
                SpecialAccessState(SpecialAccessType.DISPLAY_OVER_OTHER_APPS, SpecialAccessStatus.ACTIVE),
            ),
        )
        assertThat(result.single().state).isEqualTo(PermissionState.GRANTED)
    }

    @Test
    fun `a verified inactive capability marks its permission as denied`() {
        val result = AppOpPermissionMapping.reconcile(
            permissions = listOf(overlayPermission()),
            specialAccess = listOf(
                SpecialAccessState(SpecialAccessType.DISPLAY_OVER_OTHER_APPS, SpecialAccessStatus.INACTIVE),
            ),
        )
        assertThat(result.single().state).isEqualTo(PermissionState.DENIED)
    }

    @Test
    fun `a merely declared capability leaves the permission undetermined`() {
        val result = AppOpPermissionMapping.reconcile(
            permissions = listOf(overlayPermission()),
            specialAccess = listOf(
                SpecialAccessState(SpecialAccessType.DISPLAY_OVER_OTHER_APPS, SpecialAccessStatus.DECLARED),
            ),
        )
        assertThat(result.single().state).isEqualTo(PermissionState.UNDETERMINED)
    }

    @Test
    fun `no matching special access leaves the permission untouched`() {
        val permissions = listOf(overlayPermission())
        assertThat(AppOpPermissionMapping.reconcile(permissions, emptyList())).isEqualTo(permissions)
    }

    @Test
    fun `runtime permissions are never rewritten`() {
        val camera = AppFixtures.permission(AppFixtures.PERM_CAMERA, PermissionState.GRANTED)
        val result = AppOpPermissionMapping.reconcile(
            permissions = listOf(camera),
            specialAccess = listOf(
                SpecialAccessState(SpecialAccessType.DISPLAY_OVER_OTHER_APPS, SpecialAccessStatus.INACTIVE),
            ),
        )
        assertThat(result.single()).isEqualTo(camera)
    }

    @Test
    fun `a list without appop-gated permissions is returned unchanged`() {
        val permissions = AppFixtures.messenger.permissions
        assertThat(AppOpPermissionMapping.reconcile(permissions, emptyList())).isSameInstanceAs(permissions)
    }

    @Test
    fun `every mapped permission has a corresponding special access type`() {
        AppOpPermissionMapping.PERMISSION_TO_SPECIAL_ACCESS.forEach { (permission, type) ->
            assertThat(permission).startsWith("android.permission.")
            assertThat(SpecialAccessType.entries).contains(type)
        }
    }

    @Test
    fun `mapping covers the capabilities the knowledge base marks appop-gated`() {
        val catalogAppOps = app.vazovsky.permaware.domain.permission.PermissionCatalog.entries
            .values.filter { it.appOpGated }.map { it.permission }.toSet()
        val mapped = AppOpPermissionMapping.PERMISSION_TO_SPECIAL_ACCESS.keys
        val unmapped = catalogAppOps - mapped
        assertThat(unmapped).containsExactly("android.permission.MANAGE_MEDIA")
    }
}
