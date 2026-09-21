package app.vazovsky.permaware.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.R
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

@Composable
fun PermDetailHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = spacing.sm,
                end = spacing.screenHorizontal,
                top = spacing.xs,
                bottom = spacing.xs,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = colors.textPrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color.Transparent)
                    .size(PermAwareTheme.icons.lg),
            )
        }
        Text(
            text = title,
            style = PermAwareType.title,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEDE7FD)
@Composable
private fun PermDetailHeaderPreview() {
    PermAwareTheme { PermDetailHeader(title = "Telegram", onBack = {}) }
}
