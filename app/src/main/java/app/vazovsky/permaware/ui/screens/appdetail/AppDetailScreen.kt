package app.vazovsky.permaware.ui.screens.appdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vazovsky.permaware.BuildConfig
import app.vazovsky.permaware.R
import app.vazovsky.permaware.core.config.AppLinks
import app.vazovsky.permaware.domain.model.AppPermission
import app.vazovsky.permaware.domain.model.InstalledApp
import app.vazovsky.permaware.domain.model.SpecialAccessState
import app.vazovsky.permaware.domain.model.SpecialAccessStatus
import app.vazovsky.permaware.domain.model.groupToAppChanges
import app.vazovsky.permaware.ui.common.IntentLaunchers
import app.vazovsky.permaware.ui.common.RelativeTime
import app.vazovsky.permaware.ui.common.labelRes
import app.vazovsky.permaware.ui.components.AppIcon
import app.vazovsky.permaware.ui.components.AttentionChip
import app.vazovsky.permaware.ui.components.ChangeEventCard
import app.vazovsky.permaware.ui.components.EmptyState
import app.vazovsky.permaware.ui.components.PermCard
import app.vazovsky.permaware.ui.components.PermDetailHeader
import app.vazovsky.permaware.ui.components.PermDivider
import app.vazovsky.permaware.ui.components.PermSurface
import app.vazovsky.permaware.ui.components.PermSurfaceStyle
import app.vazovsky.permaware.ui.components.PermissionRow
import app.vazovsky.permaware.ui.components.PermissionRowTextInset
import app.vazovsky.permaware.ui.components.ScanProgress
import app.vazovsky.permaware.ui.components.SectionHeader
import app.vazovsky.permaware.ui.components.icon
import app.vazovsky.permaware.ui.components.permSurface
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType
import kotlinx.coroutines.launch

@Composable
fun AppDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedPermission by remember { mutableStateOf<AppPermission?>(null) }
    var showAttentionDetails by remember { mutableStateOf(false) }
    val settingsUnavailable = stringResource(R.string.app_details_settings_unavailable)
    val packageCopied = stringResource(R.string.app_details_package_copied)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PermAwareTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PermDetailHeader(
                title = (state as? AppDetailState.Content)?.app?.label
                    ?: stringResource(R.string.app_details_title),
                onBack = onBack,
            )

            when (val current = state) {
                AppDetailState.Loading -> ScanProgress(
                    Modifier.padding(horizontal = PermAwareTheme.spacing.screenHorizontal),
                )

                AppDetailState.NotFound -> EmptyState(
                    title = stringResource(R.string.app_details_not_found),
                    actionLabel = stringResource(R.string.action_back),
                    onActionClick = onBack,
                )

                is AppDetailState.Content -> AppDetailContent(
                    state = current,
                    onPermissionClick = { selectedPermission = it },
                    onAttentionClick = { showAttentionDetails = true },
                    onManagePermissions = {
                        if (!IntentLaunchers.openAppSettings(context, current.app.packageName)) {
                            scope.launch { snackbarHostState.showSnackbar(settingsUnavailable) }
                        }
                    },
                    onPackageCopied = {
                        scope.launch { snackbarHostState.showSnackbar(packageCopied) }
                    },
                    onOpenSource = {
                        val url = AppLinks.GITHUB_URL
                        if (url == null || !IntentLaunchers.openUrl(context, url)) {
                            scope.launch { snackbarHostState.showSnackbar(settingsUnavailable) }
                        }
                    },
                )
            }
        }
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    selectedPermission?.let { permission ->
        PermissionDetailSheet(permission = permission, onDismiss = { selectedPermission = null })
    }

    if (showAttentionDetails) {
        (state as? AppDetailState.Content)?.let { content ->
            AttentionDetailSheet(
                assessment = content.app.attention,
                onDismiss = { showAttentionDetails = false },
            )
        }
    }
}

@Composable
private fun AppDetailContent(
    state: AppDetailState.Content,
    onPermissionClick: (AppPermission) -> Unit,
    onAttentionClick: () -> Unit,
    onManagePermissions: () -> Unit,
    onPackageCopied: () -> Unit,
    onOpenSource: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = PermAwareTheme.spacing
    val context = LocalContext.current
    val app = state.app
    var showOtherPermissions by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = spacing.xxl),
    ) {
        item(key = "header") { AppHeader(app = app, onAttentionClick = onAttentionClick) }

        if (app.packageName == BuildConfig.APPLICATION_ID && AppLinks.isSourceConfigured) {
            item(key = "source") {
                SourceInvitation(
                    onClick = onOpenSource,
                    modifier = Modifier.padding(
                        horizontal = spacing.screenHorizontal,
                        vertical = spacing.xs,
                    ),
                )
            }
        }

        item(key = "manage") {
            Column(modifier = Modifier.padding(horizontal = spacing.screenHorizontal)) {
                OutlinedButton(
                    onClick = onManagePermissions,
                    modifier = Modifier.fillMaxWidth(),
                    shape = PermAwareTheme.shapes.md,
                    border = BorderStroke(
                        PermAwareTheme.elevation.hairline,
                        PermAwareTheme.colors.outline,
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PermAwareTheme.colors.surface,
                        contentColor = PermAwareTheme.colors.primary,
                    ),
                    contentPadding = PaddingValues(vertical = PermAwareTheme.spacing.sm),
                ) {
                    Text(
                        stringResource(R.string.app_details_manage_permissions),
                        style = PermAwareType.titleSmall,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = spacing.xs)
                            .size(16.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.app_details_manage_hint),
                    style = PermAwareType.caption,
                    color = PermAwareTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = spacing.xs),
                )
            }
        }

        // ---- Чувствительные разрешения ----
        item(key = "sensitive_header") {
            SectionHeader(
                title = stringResource(R.string.app_details_sensitive_permissions),
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal),
            )
        }
        if (state.sensitivePermissions.isEmpty()) {
            item(key = "sensitive_empty") {
                Text(
                    text = stringResource(R.string.app_details_no_sensitive),
                    style = PermAwareType.body,
                    color = PermAwareTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = spacing.screenHorizontal),
                )
            }
        } else {
            itemsIndexed(state.sensitivePermissions, key = { _, it -> "perm_" + it.name }) { index, permission ->
                PermissionGroupItem(
                    isFirst = index == 0,
                    isLast = index == state.sensitivePermissions.lastIndex,
                ) {
                    PermissionRow(permission = permission, onClick = { onPermissionClick(permission) })
                }
            }
        }

        // ---- Особый доступ ----
        item(key = "special_header") {
            SectionHeader(
                title = stringResource(R.string.app_details_special_access),
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal),
            )
        }
        if (app.specialAccess.isEmpty()) {
            item(key = "special_empty") {
                Text(
                    text = stringResource(R.string.special_access_none),
                    style = PermAwareType.body,
                    color = PermAwareTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = spacing.screenHorizontal),
                )
            }
        } else {
            itemsIndexed(app.specialAccess, key = { _, it -> "special_" + it.type.name }) { index, access ->
                PermissionGroupItem(
                    isFirst = index == 0,
                    isLast = index == app.specialAccess.lastIndex,
                    style = PermSurfaceStyle.Outlined,
                ) {
                    SpecialAccessRow(access)
                }
            }
            if (app.specialAccess.any { it.status == SpecialAccessStatus.DECLARED }) {
                item(key = "special_note") {
                    Text(
                        text = stringResource(R.string.special_declared_explanation),
                        style = PermAwareType.caption,
                        color = PermAwareTheme.colors.textSecondary,
                        modifier = Modifier.padding(
                            horizontal = spacing.screenHorizontal,
                            vertical = spacing.xs,
                        ),
                    )
                }
            }
        }

        // ---- Прочие разрешения, по умолчанию свёрнуты ----
        if (state.otherPermissions.isNotEmpty()) {
            item(key = "other_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showOtherPermissions = !showOtherPermissions }
                        .padding(horizontal = spacing.screenHorizontal),
                ) {
                    SectionHeader(
                        title = stringResource(
                            R.string.app_details_other_permissions_count,
                            state.otherPermissions.size,
                        ),
                        actionLabel = stringResource(
                            if (showOtherPermissions) R.string.cd_collapse else R.string.cd_expand,
                        ),
                        onActionClick = { showOtherPermissions = !showOtherPermissions },
                    )
                }
            }
            if (showOtherPermissions) {
                itemsIndexed(state.otherPermissions, key = { _, it -> "other_" + it.name }) { index, permission ->
                    PermissionGroupItem(
                        isFirst = index == 0,
                        isLast = index == state.otherPermissions.lastIndex,
                    ) {
                        PermissionRow(permission = permission, onClick = { onPermissionClick(permission) })
                    }
                }
            }
        }

        // ---- Сведения ----
        item(key = "info_header") {
            SectionHeader(
                title = stringResource(R.string.app_details_information),
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal),
            )
        }
        item(key = "info") {
            PermSurface(
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal),
                style = PermSurfaceStyle.Flat,
            ) {
                AppInformation(app = app, onPackageCopied = onPackageCopied)
            }
        }

        // ---- История по приложению ----
        item(key = "history_header") {
            SectionHeader(
                title = stringResource(R.string.app_details_history),
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal),
            )
        }
        if (state.history.isEmpty()) {
            item(key = "history_empty") {
                Text(
                    text = stringResource(R.string.app_details_no_history),
                    style = PermAwareType.body,
                    color = PermAwareTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = spacing.screenHorizontal),
                )
            }
        } else {
            items(
                state.history.groupToAppChanges(),
                key = { "hist_" + it.timestamp },
            ) { change ->
                ChangeEventCard(
                    change = change,
                    timeLabel = RelativeTime.ago(context, change.timestamp),
                    modifier = Modifier.padding(
                        horizontal = spacing.screenHorizontal,
                        vertical = spacing.xxs,
                    ),
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun SourceInvitation(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(PermAwareTheme.shapes.md)
            .background(colors.surfaceMuted)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.Code,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(PermAwareTheme.icons.lg),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_details_source_question),
                style = PermAwareType.body,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.app_details_source_action),
                style = PermAwareType.titleSmall,
                color = colors.primary,
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

@Composable
private fun AppHeader(app: InstalledApp, onAttentionClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = PermAwareTheme.spacing
    val grantedCount = app.grantedSensitivePermissions.size

    Column(modifier = modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            AppIcon(packageName = app.packageName, label = app.label, size = 56.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = PermAwareType.headline,
                    color = PermAwareTheme.colors.textPrimary,
                )
                Text(
                    text = app.packageName,
                    style = PermAwareType.caption,
                    color = PermAwareTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                app.versionName?.let { version ->
                    Text(
                        text = version,
                        style = PermAwareType.caption,
                        color = PermAwareTheme.colors.textSecondary,
                    )
                }
            }
        }

        PermCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.sm)
                .clip(PermAwareTheme.shapes.lg)
                .clickable(onClick = onAttentionClick),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                AttentionChip(level = app.attention.level)
                Text(
                    text = pluralStringResource(
                        R.plurals.plural_sensitive_permissions,
                        grantedCount,
                        grantedCount,
                    ),
                    style = PermAwareType.body,
                    color = PermAwareTheme.colors.textSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.attention_why_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpecialAccessRow(access: SpecialAccessState, modifier: Modifier = Modifier) {
    val spacing = PermAwareTheme.spacing
    val attentionColors = PermAwareTheme.colors
    val statusColor = when (access.status) {
        SpecialAccessStatus.ACTIVE -> attentionColors.attentionModerateContent
        SpecialAccessStatus.INACTIVE -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> attentionColors.textSecondary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = spacing.listRowHeight)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Icon(
            imageVector = access.type.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(access.type.labelRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(access.status.labelRes),
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
            )
        }
    }
}

@Composable
private fun AppInformation(app: InstalledApp, onPackageCopied: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = PermAwareTheme.spacing
    val context = LocalContext.current
    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)

    Column(modifier = modifier.padding(horizontal = spacing.md)) {
        InfoRow(stringResource(R.string.app_details_version), app.versionName ?: "—")
        PermDivider()
        InfoRow(
            label = stringResource(R.string.app_details_package),
            value = app.packageName,
            clickLabel = stringResource(R.string.app_details_copy_package),
            onClick = {
                clipboard?.setPrimaryClip(
                    android.content.ClipData.newPlainText(app.packageName, app.packageName),
                )
                onPackageCopied()
            },
        )
        PermDivider()
        InfoRow(
            stringResource(R.string.app_details_installed),
            RelativeTime.date(context, app.firstInstallTime),
        )
        PermDivider()
        InfoRow(
            stringResource(R.string.app_details_updated),
            RelativeTime.date(context, app.lastUpdateTime),
        )
        PermDivider()
        InfoRow(
            label = stringResource(R.string.app_details_installer),
            value = app.installSource.installerLabel
                ?: app.installSource.installerPackage
                ?: stringResource(R.string.app_details_installer_unknown),
        )
        PermDivider()
        InfoRow(
            label = stringResource(R.string.app_details_type),
            value = when {
                app.isUpdatedSystemApp -> stringResource(R.string.app_details_type_system_updated)
                app.isSystem -> stringResource(R.string.app_details_type_system)
                else -> stringResource(R.string.app_details_type_user)
            },
        )
        PermDivider()
        InfoRow(
            label = stringResource(R.string.app_details_target_sdk, app.targetSdk),
            value = app.targetSdk.toString(),
        )
        if (!app.isEnabled) {
            PermDivider()
            InfoRow(
                label = stringResource(R.string.app_details_type),
                value = stringResource(R.string.app_details_disabled),
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    clickLabel: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val spacing = PermAwareTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClickLabel = clickLabel, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            text = label,
            style = PermAwareType.caption,
            color = PermAwareTheme.colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = PermAwareType.body,
            color = PermAwareTheme.colors.textPrimary,
            modifier = Modifier.weight(1.4f),
        )
    }
}

@Composable
private fun PermissionGroupItem(
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
    style: PermSurfaceStyle = PermSurfaceStyle.Flat,
    content: @Composable () -> Unit,
) {
    val spacing = PermAwareTheme.spacing
    val radius = PermAwareTheme.shapes.lgRadius
    val shape = RoundedCornerShape(
        topStart = if (isFirst) radius else 0.dp,
        topEnd = if (isFirst) radius else 0.dp,
        bottomStart = if (isLast) radius else 0.dp,
        bottomEnd = if (isLast) radius else 0.dp,
    )
    Column(
        modifier = modifier
            .padding(horizontal = spacing.screenHorizontal)
            .permSurface(style, shape),
    ) {
        if (!isFirst) PermDivider(Modifier.padding(start = PermissionRowTextInset))
        content()
    }
}
