package app.vazovsky.permaware.ui.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.vazovsky.permaware.R
import app.vazovsky.permaware.ui.components.PermCard
import app.vazovsky.permaware.ui.components.PermDetailHeader
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

/** Заявление о приватности, внутри приложения. */
@Composable
fun PrivacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = PermAwareTheme.spacing
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PermAwareTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PermDetailHeader(title = stringResource(R.string.privacy_title), onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenHorizontal)
                    .padding(bottom = spacing.xxl),
            ) {
                Text(
                    text = stringResource(R.string.privacy_headline),
                    style = PermAwareType.display,
                    color = PermAwareTheme.colors.textPrimary,
                    modifier = Modifier.padding(top = spacing.xs, bottom = spacing.lg),
                )
                PrivacySection(
                    title = stringResource(R.string.privacy_point_local_title),
                    body = stringResource(R.string.privacy_point_local_body),
                )
                PrivacySection(
                    title = stringResource(R.string.privacy_point_no_internet_title),
                    body = stringResource(R.string.privacy_point_no_internet_body),
                )
                PrivacySection(
                    title = stringResource(R.string.privacy_point_no_analytics_title),
                    body = stringResource(R.string.privacy_point_no_analytics_body),
                )
                PrivacySection(
                    title = stringResource(R.string.privacy_point_backup_title),
                    body = stringResource(R.string.privacy_point_backup_body),
                )
                PrivacySection(
                    title = stringResource(R.string.privacy_point_boosty_title),
                    body = stringResource(R.string.privacy_point_boosty_body),
                )
            }
        }
    }
}

@Composable
private fun PrivacySection(title: String, body: String, modifier: Modifier = Modifier) {
    val spacing = PermAwareTheme.spacing
    PermCard(modifier = modifier.padding(bottom = spacing.sm)) {
        Text(
            text = title,
            style = PermAwareType.titleSmall,
            color = PermAwareTheme.colors.textPrimary,
        )
        Text(
            text = body,
            style = PermAwareType.body,
            color = PermAwareTheme.colors.textSecondary,
            modifier = Modifier.padding(top = spacing.xxs),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PrivacyScreenPreview() {
    PermAwareTheme { PrivacyScreen(onBack = {}) }
}
