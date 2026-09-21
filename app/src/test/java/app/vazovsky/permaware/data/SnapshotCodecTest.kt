package app.vazovsky.permaware.data

import app.vazovsky.permaware.data.db.SnapshotCodec
import app.vazovsky.permaware.domain.model.PermissionSnapshot
import app.vazovsky.permaware.domain.model.PermissionState
import app.vazovsky.permaware.domain.model.SpecialAccessState
import app.vazovsky.permaware.domain.model.SpecialAccessStatus
import app.vazovsky.permaware.domain.model.SpecialAccessType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SnapshotCodecTest {

    @Test
    fun `permissions round-trip through the codec`() {
        val permissions = listOf(
            PermissionSnapshot("android.permission.CAMERA", PermissionState.GRANTED),
            PermissionSnapshot("android.permission.RECORD_AUDIO", PermissionState.DENIED),
            PermissionSnapshot("android.permission.SYSTEM_ALERT_WINDOW", PermissionState.UNDETERMINED),
        )
        val decoded = SnapshotCodec.decodePermissions(SnapshotCodec.encodePermissions(permissions))
        assertThat(decoded).isEqualTo(permissions)
    }

    @Test
    fun `special access round-trips through the codec`() {
        val access = listOf(
            SpecialAccessState(SpecialAccessType.ACCESSIBILITY, SpecialAccessStatus.ACTIVE),
            SpecialAccessState(SpecialAccessType.VPN_SERVICE, SpecialAccessStatus.DECLARED),
            SpecialAccessState(SpecialAccessType.DEVICE_ADMIN, SpecialAccessStatus.INACTIVE),
        )
        val decoded = SnapshotCodec.decodeSpecialAccess(SnapshotCodec.encodeSpecialAccess(access))
        assertThat(decoded).isEqualTo(access)
    }

    @Test
    fun `empty lists round-trip`() {
        assertThat(SnapshotCodec.decodePermissions(SnapshotCodec.encodePermissions(emptyList())))
            .isEmpty()
        assertThat(SnapshotCodec.decodeSpecialAccess(SnapshotCodec.encodeSpecialAccess(emptyList())))
            .isEmpty()
    }

    @Test
    fun `a corrupt row degrades to no data instead of throwing`() {
        listOf("garbage", "a=Z;;;", "=G", "name=", ";;;", "android.permission.CAMERA").forEach { input ->
            val decoded = SnapshotCodec.decodePermissions(input)
            assertThat(decoded.none { it.name.isBlank() }).isTrue()
        }
        assertThat(SnapshotCodec.decodeSpecialAccess("NOT_A_TYPE=A;ACCESSIBILITY=Z")).isEmpty()
    }

    @Test
    fun `permission names containing separators survive`() {
        val permissions = listOf(
            PermissionSnapshot("com.vendor.permission.READ=WRITE", PermissionState.GRANTED),
        )
        val decoded = SnapshotCodec.decodePermissions(SnapshotCodec.encodePermissions(permissions))
        assertThat(decoded).isEqualTo(permissions)
    }

    @Test
    fun `encoding stays compact for a realistic app`() {
        val permissions = (1..20).map {
            PermissionSnapshot("android.permission.PERMISSION_$it", PermissionState.GRANTED)
        }
        val encoded = SnapshotCodec.encodePermissions(permissions)
        assertThat(encoded.length).isLessThan(1024)
    }
}
