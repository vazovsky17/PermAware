package app.vazovsky.permaware.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vazovsky.permaware.BuildConfig
import app.vazovsky.permaware.R
import app.vazovsky.permaware.core.config.AppLinks
import app.vazovsky.permaware.data.prefs.HistoryRetention
import app.vazovsky.permaware.data.prefs.ScanInterval
import app.vazovsky.permaware.data.prefs.ThemeMode
import app.vazovsky.permaware.ui.common.RelativeTime
import app.vazovsky.permaware.ui.common.ReportSharer
import app.vazovsky.permaware.ui.common.labelRes
import app.vazovsky.permaware.ui.components.LocalContentBottomInset
import app.vazovsky.permaware.ui.components.PermCard
import app.vazovsky.permaware.ui.components.PermDivider
import app.vazovsky.permaware.ui.components.PermScreenHeader
import app.vazovsky.permaware.ui.components.PermSectionLabel
import app.vazovsky.permaware.ui.components.PermSurface
import app.vazovsky.permaware.ui.components.SupportDeveloperCard
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenAbout: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenLicenses: () -> Unit,
    onSupportClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.preferencesState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val spacing = PermAwareTheme.spacing
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomInset = LocalContentBottomInset.current

    var confirmClearHistory by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    val historyCleared = stringResource(R.string.settings_clear_history_done)
    val resetDone = stringResource(R.string.settings_reset_done)
    val exportEmpty = stringResource(R.string.settings_export_empty)
    val exportFailed = stringResource(R.string.settings_export_failed)
    val chooserTitle = stringResource(R.string.export_chooser)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.HistoryCleared -> snackbarHostState.showSnackbar(historyCleared)
                SettingsEffect.ResetDone -> snackbarHostState.showSnackbar(resetDone)
                SettingsEffect.ExportEmpty -> snackbarHostState.showSnackbar(exportEmpty)
                is SettingsEffect.ShareReport -> {
                    val shared = ReportSharer.share(
                        context = context,
                        content = effect.body,
                        format = when (effect.format) {
                            ExportFormat.TEXT -> ReportSharer.Format.TEXT
                            ExportFormat.JSON -> ReportSharer.Format.JSON
                        },
                        subject = effect.subject,
                        chooserTitle = chooserTitle,
                    )
                    if (!shared) snackbarHostState.showSnackbar(exportFailed)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PermAwareTheme.colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            PermScreenHeader(title = stringResource(R.string.settings_title))

            val gutter = Modifier.padding(horizontal = spacing.screenHorizontal)

            // ---------------- Проверка ---------------
            SettingsGroup(stringResource(R.string.settings_section_scanning), gutter) {
                SwitchRow(
                    title = stringResource(R.string.settings_auto_scan),
                    summary = stringResource(R.string.settings_auto_scan_summary),
                    checked = prefs.autoScanEnabled,
                    onCheckedChange = viewModel::setAutoScan,
                )
                if (prefs.autoScanEnabled) {
                    PermDivider()
                    ChoiceRow(
                        title = stringResource(R.string.settings_scan_interval),
                        current = stringResource(prefs.scanInterval.labelRes),
                        options = ScanInterval.entries,
                        optionLabel = { stringResource(it.labelRes) },
                        onSelect = viewModel::setScanInterval,
                    )
                }
                PermDivider()
                ChoiceRow(
                    title = stringResource(R.string.settings_history_retention),
                    current = stringResource(prefs.historyRetention.labelRes),
                    options = HistoryRetention.entries,
                    optionLabel = { stringResource(it.labelRes) },
                    onSelect = viewModel::setHistoryRetention,
                )
            }
            if (prefs.autoScanEnabled) {
                Text(
                    text = stringResource(R.string.settings_background_note),
                    style = PermAwareType.caption,
                    color = PermAwareTheme.colors.textDisabled,
                    modifier = gutter.padding(top = spacing.xs),
                )
            }

            // ---------------- Оформление -------------
            SettingsGroup(stringResource(R.string.settings_section_appearance), gutter) {
                ChoiceRow(
                    title = stringResource(R.string.settings_theme),
                    current = stringResource(prefs.themeMode.labelRes),
                    options = ThemeMode.entries,
                    optionLabel = { stringResource(it.labelRes) },
                    onSelect = viewModel::setTheme,
                )
            }

            // ---------------- Данные -----------------
            val exportStrings = SettingsViewModel.ExportStrings(
                header = stringResource(R.string.export_header),
                generatedLabel = stringResource(
                    R.string.export_generated_at,
                    RelativeTime.dateTime(context, System.currentTimeMillis()),
                ),
                footer = stringResource(R.string.export_note),
            )
            SettingsGroup(stringResource(R.string.settings_section_data), gutter) {
                ActionRow(
                    title = stringResource(R.string.settings_export_text),
                    summary = stringResource(R.string.settings_export_summary),
                    onClick = { viewModel.export(ExportFormat.TEXT, exportStrings) },
                )
                PermDivider()
                ActionRow(
                    title = stringResource(R.string.settings_export_json),
                    onClick = { viewModel.export(ExportFormat.JSON, exportStrings) },
                )
                PermDivider()
                ActionRow(
                    title = stringResource(R.string.settings_clear_history),
                    summary = stringResource(R.string.settings_clear_history_summary),
                    onClick = { confirmClearHistory = true },
                )
                PermDivider()
                ActionRow(
                    title = stringResource(R.string.settings_reset),
                    summary = stringResource(R.string.settings_reset_summary),
                    destructive = true,
                    onClick = { confirmReset = true },
                )
            }
            Text(
                text = stringResource(R.string.settings_local_note),
                style = PermAwareType.caption,
                color = PermAwareTheme.colors.textDisabled,
                modifier = gutter.padding(top = spacing.xs),
            )

            // ---------------- О приложении -----------
            SettingsGroup(stringResource(R.string.settings_section_about), gutter) {
                ActionRow(
                    title = stringResource(R.string.settings_version),
                    summary = BuildConfig.VERSION_NAME + " · " + stringResource(R.string.app_tagline),
                    onClick = {},
                    clickable = false,
                )
                PermDivider()
                ActionRow(
                    title = stringResource(R.string.settings_about),
                    summary = stringResource(R.string.settings_about_summary),
                    onClick = onOpenAbout,
                )
                PermDivider()
                ActionRow(
                    title = stringResource(R.string.settings_privacy),
                    summary = stringResource(R.string.settings_privacy_summary),
                    onClick = onOpenPrivacy,
                )
                PermDivider()
                ActionRow(
                    title = stringResource(R.string.settings_licenses),
                    summary = stringResource(R.string.settings_licenses_summary),
                    onClick = onOpenLicenses,
                )
            }

            PermCard(modifier = gutter.padding(top = spacing.sm)) {
                Text(
                    text = stringResource(R.string.settings_what_app_cannot),
                    style = PermAwareType.titleSmall,
                    color = PermAwareTheme.colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.settings_disclaimer),
                    style = PermAwareType.body,
                    color = PermAwareTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = spacing.xxs),
                )
            }

            // ---------------- Поддержка --------------
            if (AppLinks.isSupportConfigured) {
                SupportDeveloperCard(
                    modifier = gutter.padding(top = spacing.md),
                    dismissible = false,
                    onSupportClick = onSupportClick,
                )
            } else if (BuildConfig.DEBUG) {
                // Видно только в отладочных сборках, так что ненастроенный релиз трудно не
                // заметить.
                Text(
                    text = stringResource(R.string.support_not_configured),
                    style = PermAwareType.caption,
                    color = PermAwareTheme.colors.danger,
                    modifier = gutter.padding(top = spacing.md),
                )
            }

            Spacer(Modifier.height(bottomInset + spacing.md))
        }
    }

    if (confirmClearHistory) {
        ConfirmDialog(
            text = stringResource(R.string.settings_clear_history_confirm),
            confirmLabel = stringResource(R.string.settings_action_delete),
            onConfirm = {
                viewModel.clearHistory()
                confirmClearHistory = false
            },
            onDismiss = { confirmClearHistory = false },
        )
    }
    if (confirmReset) {
        ConfirmDialog(
            text = stringResource(R.string.settings_reset_confirm),
            confirmLabel = stringResource(R.string.settings_action_delete),
            onConfirm = {
                viewModel.resetEverything()
                confirmReset = false
            },
            onDismiss = { confirmReset = false },
        )
    }
}

/** Секция настроек: разреженная подпись на холсте, строки внутри одной карточки. */
@Composable
private fun SettingsGroup(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val spacing = PermAwareTheme.spacing
    Column(modifier = modifier.padding(top = spacing.xl)) {
        PermSectionLabel(text = title, modifier = Modifier.padding(bottom = spacing.md))
        PermSurface(content = content)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    val spacing = PermAwareTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .heightIn(min = spacing.minTouchTarget)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = PermAwareType.titleSmall,
                color = PermAwareTheme.colors.textPrimary,
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = PermAwareType.caption,
                    color = PermAwareTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = spacing.xxxs),
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> ChoiceRow(
    title: String,
    current: String,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = PermAwareTheme.spacing
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .heightIn(min = spacing.minTouchTarget)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = PermAwareType.titleSmall,
            color = PermAwareTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = current,
            style = PermAwareType.body,
            color = PermAwareTheme.colors.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text(title) },
            text = {
                Column {
                    options.forEach { option ->
                        val label = optionLabel(option)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(option)
                                    expanded = false
                                }
                                .heightIn(min = spacing.minTouchTarget)
                                .padding(vertical = spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = label == current,
                                onClick = {
                                    onSelect(option)
                                    expanded = false
                                },
                            )
                            Text(text = label, modifier = Modifier.padding(start = spacing.xs))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { expanded = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun ActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    destructive: Boolean = false,
    clickable: Boolean = true,
) {
    val spacing = PermAwareTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = spacing.minTouchTarget)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
    ) {
        Text(
            text = title,
            style = PermAwareType.titleSmall,
            color = if (destructive) {
                PermAwareTheme.colors.danger
            } else {
                PermAwareTheme.colors.textPrimary
            },
        )
        if (summary != null) {
            Text(
                text = summary,
                style = PermAwareType.caption,
                color = PermAwareTheme.colors.textSecondary,
                modifier = Modifier.padding(top = spacing.xxxs),
            )
        }
    }
}

@Composable
private fun ConfirmDialog(text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
