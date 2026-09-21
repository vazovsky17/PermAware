package app.vazovsky.permaware.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vazovsky.permaware.R
import app.vazovsky.permaware.ui.components.PermCard
import app.vazovsky.permaware.ui.components.PermSurfaceStyle
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 3

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val spacing = PermAwareTheme.spacing

    LaunchedEffect(state.finished) { if (state.finished) onFinished() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PermAwareTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.xs),
            horizontalArrangement = Arrangement.End,
        ) {
            val skipAlpha by animateFloatAsState(
                targetValue = if (pagerState.currentPage < PAGE_COUNT - 1) 1f else 0f,
                label = "skipAlpha",
            )
            TextButton(
                onClick = { scope.launch { pagerState.animateScrollToPage(PAGE_COUNT - 1) } },
                enabled = skipAlpha > 0f,
                modifier = Modifier.alpha(skipAlpha),
            ) {
                Text(stringResource(R.string.onboarding_skip))
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> OnboardingPage(
                    title = stringResource(R.string.onboarding_1_title),
                    body = stringResource(R.string.onboarding_1_body),
                    image = R.drawable.onboarding_1,
                )

                1 -> PrivacyPage()
                else -> FirstScanPage(
                    isScanning = state.isScanning,
                    failed = state.failed,
                    onStart = viewModel::startFirstScan,
                    onSkip = viewModel::skipToApp,
                )
            }
        }

        val pageIndicatorDescription = stringResource(
            R.string.onboarding_page_indicator,
            pagerState.currentPage + 1,
            PAGE_COUNT,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
                .semantics { contentDescription = pageIndicatorDescription },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(PAGE_COUNT) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = spacing.xxs)
                        .size(if (selected) 9.dp else 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        ),
                )
            }
        }

        if (pagerState.currentPage < PAGE_COUNT - 1) {
            Button(
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                shape = PermAwareTheme.shapes.md,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PermAwareTheme.colors.primary,
                    contentColor = PermAwareTheme.colors.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg)
                    .padding(bottom = spacing.xl),
            ) {
                Text(stringResource(R.string.onboarding_next))
            }
        }
    }
}

@Composable
private fun OnboardingArtwork(
    @DrawableRes image: Int,
    modifier: Modifier = Modifier,
    maxWidth: Dp = OnboardingArtworkMaxWidth,
) {
    val scaled =
        (maxWidth / LocalDensity.current.fontScale).coerceAtLeast(OnboardingArtworkMinWidth)
    Image(
        painter = painterResource(image),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .widthIn(max = scaled)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(PermAwareTheme.shapes.artwork),
    )
}

private val OnboardingArtworkMinWidth = 80.dp
private val OnboardingArtworkMaxWidth = 120.dp
private val OnboardingDisclosureArtworkMaxWidth = 120.dp

@Composable
private fun OnboardingPage(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    @DrawableRes image: Int? = null,
    content: @Composable () -> Unit = {},
) {
    val spacing = PermAwareTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (image != null) OnboardingArtwork(image)
        Text(
            text = title,
            style = PermAwareType.headline,
            color = PermAwareTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.xl),
        )
        Text(
            text = body,
            style = PermAwareType.body,
            color = PermAwareTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.sm),
        )
        content()
        Spacer(Modifier.height(PermAwareTheme.spacing.md))
    }
}

@Composable
private fun PrivacyPage(modifier: Modifier = Modifier) {
    val spacing = PermAwareTheme.spacing
    OnboardingPage(
        image = R.drawable.onboarding_2,
        title = stringResource(R.string.onboarding_2_title),
        body = stringResource(R.string.onboarding_2_body),
        modifier = modifier,
    ) {
        PermCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.xl),
        ) {
            PrivacyPoint(Icons.Outlined.Shield, stringResource(R.string.onboarding_2_point_local))
            Spacer(Modifier.height(spacing.sm))
            PrivacyPoint(
                Icons.Outlined.CloudOff,
                stringResource(R.string.onboarding_2_point_no_internet),
            )
            Spacer(Modifier.height(spacing.sm))
            PrivacyPoint(
                Icons.Outlined.PersonOff,
                stringResource(R.string.onboarding_2_point_no_account),
            )
        }
    }
}

@Composable
private fun PrivacyPoint(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    val spacing = PermAwareTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PermAwareTheme.colors.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = PermAwareType.body,
            color = PermAwareTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun FirstScanPage(
    isScanning: Boolean,
    failed: Boolean,
    onStart: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = PermAwareTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OnboardingArtwork(R.drawable.onboarding_3, maxWidth = OnboardingDisclosureArtworkMaxWidth)
        Text(
            text = stringResource(R.string.onboarding_3_title),
            style = PermAwareType.headline,
            color = PermAwareTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.xl),
        )
        Text(
            text = stringResource(R.string.onboarding_3_body),
            style = PermAwareType.body,
            color = PermAwareTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.sm),
        )

        PermCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.lg),
            style = PermSurfaceStyle.Outlined,
        ) {
            Text(
                text = stringResource(R.string.onboarding_disclosure_title),
                style = PermAwareType.titleSmall,
                color = PermAwareTheme.colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.onboarding_disclosure_body),
                style = PermAwareType.caption,
                color = PermAwareTheme.colors.textSecondary,
                modifier = Modifier.padding(top = spacing.xs),
            )
        }

        Button(
            onClick = onStart,
            enabled = !isScanning,
            shape = PermAwareTheme.shapes.md,
            colors = ButtonDefaults.buttonColors(
                containerColor = PermAwareTheme.colors.primary,
                contentColor = PermAwareTheme.colors.onPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.xl),
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = stringResource(R.string.dashboard_scanning),
                    modifier = Modifier.padding(start = spacing.xs),
                )
            } else {
                Text(stringResource(R.string.onboarding_3_action))
            }
        }

        if (failed) {
            Text(
                text = stringResource(R.string.dashboard_scan_failed_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.sm),
            )
            TextButton(onClick = onSkip) { Text(stringResource(R.string.onboarding_skip)) }
        }
        Spacer(Modifier.height(spacing.md))
    }
}

@Preview(showBackground = true)
@Composable
private fun PrivacyPagePreview() {
    PermAwareTheme { PrivacyPage() }
}
