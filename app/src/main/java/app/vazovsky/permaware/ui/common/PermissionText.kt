package app.vazovsky.permaware.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.vazovsky.permaware.R
import app.vazovsky.permaware.domain.model.AppPermission
import app.vazovsky.permaware.domain.permission.PermissionCatalog

/**
 * Выбирает текст, который показать для разрешения, в таком порядке:
 *
 *  1. собственная база знаний — человеческий язык, написанный под этот продукт;
 *  2. название и описание, которые даёт самому разрешению Android;
 *  3. голое техническое имя — чтобы незнакомое или чужое разрешение всё равно нарисовало
 *     что-то осмысленное, а не пустую строку.
 */
@Composable
fun AppPermission.displayTitle(): String {
    PermissionCatalog.find(name)?.let { return stringResource(it.titleRes) }
    platformLabel?.takeIf { it.isNotBlank() }?.let { return it }
    return name.substringAfterLast('.')
}

@Composable
fun AppPermission.displayDescription(): String? {
    PermissionCatalog.find(name)?.let { return stringResource(it.descriptionRes) }
    return platformDescription?.takeIf { it.isNotBlank() }
}

@Composable
fun AppPermission.displayAttentionNote(): String? {
    PermissionCatalog.find(name)?.let { return stringResource(it.attentionRes) }
    return null
}

val AppPermission.isUnexplained: Boolean
    get() = !PermissionCatalog.isKnown(name) && platformDescription.isNullOrBlank()

@Composable
fun unexplainedPermissionNote(): String = stringResource(R.string.permission_no_description)
