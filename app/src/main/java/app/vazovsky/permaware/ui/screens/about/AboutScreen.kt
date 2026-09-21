package app.vazovsky.permaware.ui.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.BuildConfig
import app.vazovsky.permaware.R
import app.vazovsky.permaware.core.config.AppLinks
import app.vazovsky.permaware.ui.components.PermCard
import app.vazovsky.permaware.ui.components.PermDetailHeader
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenSource: () -> Unit,
    onOpenContact: () -> Unit,
    onSupportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = PermAwareTheme.spacing
    val colors = PermAwareTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PermDetailHeader(title = stringResource(R.string.about_title), onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenHorizontal)
                    .padding(bottom = spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_author),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .padding(top = spacing.sm)
                        .widthIn(max = PortraitSize)
                        .fillMaxWidth()
                        .clip(PermAwareTheme.shapes.artwork),
                )
                Text(
                    text = stringResource(R.string.about_studio),
                    style = PermAwareType.display,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = spacing.sm),
                )
                Text(
                    text = stringResource(R.string.about_tagline),
                    style = PermAwareType.body,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = spacing.xxs, bottom = spacing.lg),
                )
                PermCard(modifier = Modifier.padding(bottom = spacing.sm)) {
                    Text(
                        text = stringResource(R.string.about_body),
                        style = PermAwareType.body,
                        color = colors.textSecondary,
                    )
                }
                AboutSection(
                    title = stringResource(R.string.about_craft_title),
                    body = stringResource(R.string.about_craft_body),
                )
                if (AppLinks.isSourceConfigured) {
                    AboutLink(
                        icon = Icons.Outlined.Code,
                        title = stringResource(R.string.about_source_title),
                        body = stringResource(R.string.about_source_body),
                        onClick = onOpenSource,
                    )
                }
                if (AppLinks.isContactConfigured) {
                    AboutLink(
                        icon = Icons.Outlined.BugReport,
                        title = stringResource(R.string.about_contact_title),
                        body = stringResource(R.string.about_contact_body),
                        onClick = onOpenContact,
                    )
                }
                if (AppLinks.isSupportConfigured) {
                    AboutLink(
                        icon = Icons.Outlined.Favorite,
                        title = stringResource(R.string.about_support_title),
                        body = stringResource(R.string.about_support_body),
                        onClick = onSupportClick,
                    )
                }
                Text(
                    text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = PermAwareType.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = spacing.lg),
                )
            }
        }
    }
}

@Composable
private fun AboutSection(title: String, body: String, modifier: Modifier = Modifier) {
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

@Composable
private fun AboutLink(
    icon: ImageVector,
    title: String,
    body: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = PermAwareTheme.spacing
    val colors = PermAwareTheme.colors
    PermCard(
        modifier = modifier
            .padding(bottom = spacing.sm)
            .clip(PermAwareTheme.shapes.md)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(PermAwareTheme.icons.lg),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = PermAwareType.titleSmall,
                    color = colors.textPrimary,
                )
                Text(
                    text = body,
                    style = PermAwareType.body,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = spacing.xxxs),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(PermAwareTheme.icons.md),
            )
        }
    }
}

private val PortraitSize = 180.dp

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    PermAwareTheme {
        AboutScreen(onBack = {}, onOpenSource = {}, onOpenContact = {}, onSupportClick = {})
    }
}
