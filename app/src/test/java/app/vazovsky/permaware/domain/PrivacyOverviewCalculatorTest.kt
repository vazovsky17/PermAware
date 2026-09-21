package app.vazovsky.permaware.domain

import app.vazovsky.permaware.domain.attention.PrivacyOverviewCalculator
import app.vazovsky.permaware.domain.attention.PrivacyReasonKind
import app.vazovsky.permaware.domain.model.PermissionCategory
import app.vazovsky.permaware.domain.model.SpecialAccessState
import app.vazovsky.permaware.domain.model.SpecialAccessStatus
import app.vazovsky.permaware.domain.model.SpecialAccessType
import app.vazovsky.permaware.fixtures.AppFixtures
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PrivacyOverviewCalculatorTest {

    @Test
    fun `a device with no apps scores full marks`() {
        val overview = PrivacyOverviewCalculator.calculate(emptyList())
        assertThat(overview.score).isEqualTo(100)
        assertThat(overview.reasons).isEmpty()
    }

    @Test
    fun `harmless apps do not reduce the score`() {
        val overview = PrivacyOverviewCalculator.calculate(listOf(AppFixtures.calculator))
        assertThat(overview.score).isEqualTo(100)
    }

    @Test
    fun `system apps are excluded from the permission shares`() {
        // Телефон предоставляется с десятками предустановленных приложений, у которых уже есть камера и
        // микрофон. Считать их значило бы выдать каждому устройству один и тот же плохой балл за
        // то, чего человек изменить не может, — ровно тот провал, ради ухода от которого
        // калькулятор и переписали.
        val systemOnly = PrivacyOverviewCalculator.calculate(
            listOf(AppFixtures.systemApp, AppFixtures.updatedSystemApp),
        )
        assertThat(systemOnly.score).isEqualTo(100)
        assertThat(systemOnly.userAppCount).isEqualTo(0)
    }

    @Test
    fun `an updated system app still counts as a system app`() {
        val overview = PrivacyOverviewCalculator.calculate(listOf(AppFixtures.updatedSystemApp))
        assertThat(overview.userAppCount).isEqualTo(0)
    }

    @Test
    fun `score reflects the share of user apps, not their absolute count`() {
        // Два устройства с одинаковой *долей* приложений, держащих камеру, должны получить близкий
        // балл, чтобы число оставалось сопоставимым между телефоном с четырьмя приложениями и
        // телефоном с сорока.
        val small = PrivacyOverviewCalculator.calculate(
            listOf(AppFixtures.cameraApp, AppFixtures.calculator),
        )
        val large = PrivacyOverviewCalculator.calculate(
            List(10) { index -> AppFixtures.cameraApp.copy(packageName = "cam$index") } +
                List(10) { index -> AppFixtures.calculator.copy(packageName = "calc$index") },
        )
        assertThat(small.score).isEqualTo(large.score)
    }

    @Test
    fun `a realistic device lands in a believable range rather than at zero`() {
        val apps = AppFixtures.all
        val overview = PrivacyOverviewCalculator.calculate(apps)
        assertThat(overview.score).isGreaterThan(0)
        assertThat(overview.score).isLessThan(100)
    }

    @Test
    fun `every deduction is explained`() {
        val overview = PrivacyOverviewCalculator.calculate(AppFixtures.all)
        val deducted = overview.reasons.sumOf { it.penalty }
        assertThat(100 - overview.score).isEqualTo(deducted)
        assertThat(overview.reasons.all { it.penalty > 0 }).isTrue()
    }

    @Test
    fun `category reasons carry the category they refer to`() {
        val overview = PrivacyOverviewCalculator.calculate(listOf(AppFixtures.cameraApp))
        val reason = overview.reasons.single { it.kind == PrivacyReasonKind.APPS_WITH_CATEGORY }
        assertThat(reason.category).isEqualTo(PermissionCategory.CAMERA)
        assertThat(reason.appCount).isEqualTo(1)
    }

    @Test
    fun `background location is called out separately`() {
        val overview = PrivacyOverviewCalculator.calculate(
            listOf(AppFixtures.locationHeavySideload, AppFixtures.calculator),
        )
        assertThat(overview.reasons.map { it.kind })
            .contains(PrivacyReasonKind.APPS_WITH_BACKGROUND_LOCATION)
    }

    @Test
    fun `active special access counts even for a system app`() {
        val systemWithAccessibility = AppFixtures.systemApp.copy(
            specialAccess = listOf(
                SpecialAccessState(SpecialAccessType.ACCESSIBILITY, SpecialAccessStatus.ACTIVE),
            ),
        )
        val overview = PrivacyOverviewCalculator.calculate(
            listOf(systemWithAccessibility, AppFixtures.calculator),
        )
        assertThat(overview.reasons.map { it.kind })
            .contains(PrivacyReasonKind.APPS_WITH_ACTIVE_SPECIAL_ACCESS)
    }

    @Test
    fun `score never leaves the 0 to 100 range`() {
        val everything = List(30) { index ->
            AppFixtures.locationHeavySideload.copy(packageName = "app$index")
        }
        val overview = PrivacyOverviewCalculator.calculate(everything)
        assertThat(overview.score).isAtLeast(0)
        assertThat(overview.score).isAtMost(100)
    }

    @Test
    fun `category counts exclude plumbing categories`() {
        val counts = PrivacyOverviewCalculator.categoryCounts(AppFixtures.all.filter { it.isUserApp })
        assertThat(counts.keys).doesNotContain(PermissionCategory.SPECIAL_ACCESS)
        assertThat(counts.keys).doesNotContain(PermissionCategory.SYSTEM)
        assertThat(counts.keys).doesNotContain(PermissionCategory.OTHER)
    }

    @Test
    fun `category counts only include granted access`() {
        // recentlyUpdated запрашивает местоположение, но получает отказ, значит считаться не
        // должно.
        val counts = PrivacyOverviewCalculator.categoryCounts(listOf(AppFixtures.recentlyUpdated))
        assertThat(counts).doesNotContainKey(PermissionCategory.LOCATION)
    }
}
