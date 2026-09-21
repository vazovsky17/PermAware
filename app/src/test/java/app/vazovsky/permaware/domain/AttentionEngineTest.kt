package app.vazovsky.permaware.domain

import app.vazovsky.permaware.domain.attention.AttentionEngine
import app.vazovsky.permaware.domain.model.AttentionFactorKind
import app.vazovsky.permaware.domain.model.AttentionLevel
import app.vazovsky.permaware.domain.model.PermissionCategory
import app.vazovsky.permaware.domain.model.PermissionState
import app.vazovsky.permaware.domain.model.SpecialAccessState
import app.vazovsky.permaware.domain.model.SpecialAccessStatus
import app.vazovsky.permaware.domain.model.SpecialAccessType
import app.vazovsky.permaware.domain.permission.PermissionCatalog
import app.vazovsky.permaware.fixtures.AppFixtures
import app.vazovsky.permaware.fixtures.AppFixtures.PERM_BACKGROUND_LOCATION
import app.vazovsky.permaware.fixtures.AppFixtures.PERM_CAMERA
import app.vazovsky.permaware.fixtures.AppFixtures.PERM_CONTACTS
import app.vazovsky.permaware.fixtures.AppFixtures.PERM_FINE_LOCATION
import app.vazovsky.permaware.fixtures.AppFixtures.PERM_INSTALL
import app.vazovsky.permaware.fixtures.AppFixtures.PERM_MIC
import app.vazovsky.permaware.fixtures.AppFixtures.PERM_SMS
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AttentionEngineTest {

    private fun assess(
        permissions: List<app.vazovsky.permaware.domain.model.AppPermission>,
        specialAccess: List<SpecialAccessState> = emptyList(),
        isSystem: Boolean = false,
        knownSource: Boolean = true,
    ) = AttentionEngine.assess(permissions, specialAccess, isSystem, knownSource)

    @Test
    fun `app with no sensitive permissions scores zero`() {
        val result = assess(AppFixtures.calculator.permissions)
        assertThat(result.score).isEqualTo(0)
        assertThat(result.level).isEqualTo(AttentionLevel.LOW)
        assertThat(result.factors).isEmpty()
    }

    @Test
    fun `an app that reads the installed app list is flagged for it`() {
        val permission = AppFixtures.permission("android.permission.QUERY_ALL_PACKAGES")
        assertThat(permission.isSensitive).isTrue()
        assertThat(permission.state).isEqualTo(PermissionState.GRANTED)

        val result = assess(listOf(permission))

        assertThat(result.factors).isNotEmpty()
        assertThat(result.score).isGreaterThan(0)
    }

    @Test
    fun `every special access permission is either scored as one or flagged as a permission`() {
        val unreachable = PermissionCatalog.entries.values
            .filter { it.category == PermissionCategory.SPECIAL_ACCESS }
            .filter { it.attentionWeight > 0 }
            .filterNot { it.permission in AttentionEngine.SCORED_AS_SPECIAL_ACCESS }
            .filter { assess(listOf(AppFixtures.permission(it.permission))).factors.isEmpty() }
            .map { it.permission }

        assertThat(unreachable).isEmpty()
    }

    @Test
    fun `denied permissions contribute nothing`() {
        val granted = assess(listOf(AppFixtures.permission(PERM_CAMERA)))
        val denied = assess(
            listOf(AppFixtures.permission(PERM_CAMERA, state = PermissionState.DENIED)),
        )
        assertThat(granted.score).isGreaterThan(0)
        assertThat(denied.score).isEqualTo(0)
    }

    @Test
    fun `undetermined permissions do not count as granted`() {
        val result = assess(
            listOf(AppFixtures.permission(PERM_MIC, state = PermissionState.UNDETERMINED)),
        )
        assertThat(result.factors.none { it.kind == AttentionFactorKind.GRANTED_SENSITIVE_PERMISSION })
            .isTrue()
    }

    @Test
    fun `a popular messenger is not pushed to elevated by breadth alone`() {
        val result = assess(AppFixtures.messenger.permissions)
        assertThat(result.level).isNotEqualTo(AttentionLevel.ELEVATED)
    }

    @Test
    fun `breadth has diminishing returns`() {
        val single = assess(listOf(AppFixtures.permission(PERM_CAMERA)))
        val many = assess(
            listOf(
                AppFixtures.permission(PERM_CAMERA),
                AppFixtures.permission(PERM_MIC),
                AppFixtures.permission(PERM_CONTACTS),
                AppFixtures.permission(PERM_SMS),
            ),
        )
        assertThat(many.score).isLessThan(single.score * 4)
        assertThat(many.score).isGreaterThan(single.score)
    }

    @Test
    fun `only the strongest permission in a category counts`() {
        val fineOnly = assess(listOf(AppFixtures.permission(PERM_FINE_LOCATION)))
        val fineAndCoarse = assess(
            listOf(
                AppFixtures.permission(PERM_FINE_LOCATION),
                AppFixtures.permission("android.permission.ACCESS_COARSE_LOCATION"),
            ),
        )
        assertThat(fineAndCoarse.score).isEqualTo(fineOnly.score)
    }

    @Test
    fun `background location is scored once, not twice`() {
        val result = assess(
            listOf(
                AppFixtures.permission(PERM_FINE_LOCATION),
                AppFixtures.permission(PERM_BACKGROUND_LOCATION),
            ),
        )
        val locationFactors = result.factors.filter {
            it.categoryRef == PermissionCategory.LOCATION
        }
        assertThat(locationFactors).hasSize(2)
        assertThat(locationFactors.count { it.kind == AttentionFactorKind.BACKGROUND_LOCATION })
            .isEqualTo(1)
        assertThat(
            locationFactors.none {
                it.permissionRef == PERM_BACKGROUND_LOCATION &&
                    it.kind == AttentionFactorKind.GRANTED_SENSITIVE_PERMISSION
            },
        )
            .isTrue()
    }

    @Test
    fun `verified active special access outweighs a merely declared one`() {
        val declared = assess(
            permissions = emptyList(),
            specialAccess = listOf(
                SpecialAccessState(SpecialAccessType.ACCESSIBILITY, SpecialAccessStatus.DECLARED),
            ),
        )
        val active = assess(
            permissions = emptyList(),
            specialAccess = listOf(
                SpecialAccessState(SpecialAccessType.ACCESSIBILITY, SpecialAccessStatus.ACTIVE),
            ),
        )
        assertThat(active.score).isGreaterThan(declared.score)
        assertThat(active.factors.first().kind).isEqualTo(AttentionFactorKind.ACTIVE_SPECIAL_ACCESS)
    }

    @Test
    fun `inactive special access contributes nothing`() {
        val result = assess(
            permissions = emptyList(),
            specialAccess = listOf(
                SpecialAccessState(SpecialAccessType.ACCESSIBILITY, SpecialAccessStatus.INACTIVE),
                SpecialAccessState(SpecialAccessType.DEVICE_ADMIN, SpecialAccessStatus.UNDETERMINED),
            ),
        )
        assertThat(result.score).isEqualTo(0)
    }

    @Test
    fun `a sideloaded location-hoarding app reaches elevated`() {
        val result = assess(
            permissions = AppFixtures.locationHeavySideload.permissions,
            specialAccess = AppFixtures.locationHeavySideload.specialAccess,
            knownSource = false,
        )
        assertThat(result.level).isEqualTo(AttentionLevel.ELEVATED)
        assertThat(result.factors.map { it.kind })
            .contains(AttentionFactorKind.UNKNOWN_INSTALL_SOURCE)
    }

    @Test
    fun `unknown install source alone does not create attention`() {
        val result = assess(permissions = AppFixtures.calculator.permissions, knownSource = false)
        assertThat(result.score).isEqualTo(0)
    }

    @Test
    fun `sensitive combination factor appears for three high categories`() {
        val result = assess(
            listOf(
                AppFixtures.permission(PERM_CAMERA),
                AppFixtures.permission(PERM_MIC),
                AppFixtures.permission(PERM_CONTACTS),
            ),
        )
        assertThat(result.factors.map { it.kind })
            .contains(AttentionFactorKind.SENSITIVE_COMBINATION)
    }

    @Test
    fun `score is capped and levels follow thresholds`() {
        val everything = assess(
            permissions = listOf(
                AppFixtures.permission(PERM_CAMERA),
                AppFixtures.permission(PERM_MIC),
                AppFixtures.permission(PERM_CONTACTS),
                AppFixtures.permission(PERM_SMS),
                AppFixtures.permission(PERM_FINE_LOCATION),
                AppFixtures.permission(PERM_BACKGROUND_LOCATION),
            ),
            specialAccess = listOf(
                SpecialAccessState(SpecialAccessType.ACCESSIBILITY, SpecialAccessStatus.ACTIVE),
                SpecialAccessState(SpecialAccessType.NOTIFICATION_LISTENER, SpecialAccessStatus.ACTIVE),
                SpecialAccessState(SpecialAccessType.INSTALL_UNKNOWN_APPS, SpecialAccessStatus.DECLARED),
            ),
            knownSource = false,
        )
        assertThat(everything.score).isAtMost(AttentionEngine.MAX_SCORE)
        assertThat(everything.level).isEqualTo(AttentionLevel.ELEVATED)
    }

    @Test
    fun `assessment is deterministic`() {
        val first = assess(AppFixtures.messenger.permissions)
        val second = assess(AppFixtures.messenger.permissions.reversed())
        assertThat(first.score).isEqualTo(second.score)
        assertThat(first.level).isEqualTo(second.level)
    }

    @Test
    fun `factors are ordered by weight so the explanation leads with the strongest reason`() {
        val result = assess(
            listOf(
                AppFixtures.permission(PERM_CONTACTS),
                AppFixtures.permission(PERM_CAMERA),
                AppFixtures.permission(PERM_BACKGROUND_LOCATION),
            ),
        )
        assertThat(result.factors.map { it.points })
            .isInOrder(Comparator.reverseOrder<Int>())
    }

    @Test
    fun `level boundaries match the documented thresholds`() {
        assertThat(AttentionEngine.levelFor(0)).isEqualTo(AttentionLevel.LOW)
        assertThat(AttentionEngine.levelFor(AttentionEngine.MODERATE_THRESHOLD - 1))
            .isEqualTo(AttentionLevel.LOW)
        assertThat(AttentionEngine.levelFor(AttentionEngine.MODERATE_THRESHOLD))
            .isEqualTo(AttentionLevel.MODERATE)
        assertThat(AttentionEngine.levelFor(AttentionEngine.ELEVATED_THRESHOLD - 1))
            .isEqualTo(AttentionLevel.MODERATE)
        assertThat(AttentionEngine.levelFor(AttentionEngine.ELEVATED_THRESHOLD))
            .isEqualTo(AttentionLevel.ELEVATED)
    }

    @Test
    fun `appop-gated declared install rights are weighted but not treated as granted`() {
        val result = assess(
            permissions = listOf(
                AppFixtures.permission(
                    PERM_INSTALL,
                    state = PermissionState.UNDETERMINED,
                    protection = app.vazovsky.permaware.domain.model.PermissionProtection.APPOP_GATED,
                ),
            ),
            specialAccess = listOf(
                SpecialAccessState(SpecialAccessType.INSTALL_UNKNOWN_APPS, SpecialAccessStatus.DECLARED),
            ),
        )
        assertThat(result.factors.map { it.kind })
            .containsExactly(AttentionFactorKind.DECLARED_SPECIAL_ACCESS)
    }
}
