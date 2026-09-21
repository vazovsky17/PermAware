package app.vazovsky.permaware.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.R
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

@Composable
fun PermSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(PermAwareTheme.shapes.pill)
            .background(colors.surface)
            .heightIn(min = spacing.minTouchTarget)
            .padding(start = spacing.md, end = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = stringResource(R.string.apps_search),
            tint = colors.textSecondary,
            modifier = Modifier.size(PermAwareTheme.icons.md),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = spacing.xs),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(text = placeholder, style = PermAwareType.body, color = colors.textDisabled)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.merge(PermAwareType.body).copy(color = colors.textPrimary),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                // У самодельного поля нет подписи, которую объявил бы скринридер, а плейсхолдер —
                // это соседний Text, а не часть поля. Без этого TalkBack натыкается на
                // неподписанное поле ввода.
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = placeholder },
            )
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.apps_clear_search),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(PermAwareTheme.icons.md),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEDE7FD)
@Composable
private fun PermSearchFieldPreview() {
    PermAwareTheme {
        PermSearchField("", {}, "Поиск по названию или пакету", Modifier.padding(20.dp))
    }
}
