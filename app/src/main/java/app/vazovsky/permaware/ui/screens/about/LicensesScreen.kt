package app.vazovsky.permaware.ui.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.vazovsky.permaware.R
import app.vazovsky.permaware.ui.components.PermCard
import app.vazovsky.permaware.ui.components.PermDetailHeader
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

@Immutable
data class LicenseEntry(val name: String, val license: String, val copyright: String)

/** Уведомления о стороннем коде. */
private val LICENSES = listOf(
    LicenseEntry(
        "AndroidX (Core, Lifecycle, Activity, Navigation)",
        "Apache License 2.0",
        "The Android Open Source Project",
    ),
    LicenseEntry("Jetpack Compose, Material 3", "Apache License 2.0", "The Android Open Source Project"),
    LicenseEntry("AndroidX Room", "Apache License 2.0", "The Android Open Source Project"),
    LicenseEntry("AndroidX DataStore", "Apache License 2.0", "The Android Open Source Project"),
    LicenseEntry("AndroidX WorkManager", "Apache License 2.0", "The Android Open Source Project"),
    LicenseEntry("Dagger / Hilt", "Apache License 2.0", "Google LLC"),
    LicenseEntry("Kotlin standard library, Coroutines, Serialization", "Apache License 2.0", "JetBrains s.r.o."),
)

@Composable
fun LicensesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = PermAwareTheme.spacing
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PermAwareTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PermDetailHeader(title = stringResource(R.string.settings_licenses), onBack = onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = spacing.screenHorizontal,
                    end = spacing.screenHorizontal,
                    bottom = spacing.xxl,
                ),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(spacing.xs),
            ) {
                items(LICENSES, key = { it.name }) { entry ->
                    PermCard {
                        Text(
                            text = entry.name,
                            style = PermAwareType.titleSmall,
                            color = PermAwareTheme.colors.textPrimary,
                        )
                        Text(
                            text = entry.license,
                            style = PermAwareType.caption,
                            color = PermAwareTheme.colors.textSecondary,
                            modifier = Modifier.padding(top = spacing.xxs),
                        )
                        Text(
                            text = entry.copyright,
                            style = PermAwareType.caption,
                            color = PermAwareTheme.colors.textDisabled,
                        )
                    }
                }
            }
        }
    }
}
