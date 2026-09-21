package app.vazovsky.permaware.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.R
import app.vazovsky.permaware.data.platform.AppIconLoader

val LocalAppIconLoader = staticCompositionLocalOf<AppIconLoader?> { null }

/**
 * Иконка приложения, которая грузится асинхронно и откатывается к первой букве названия.
 */
@Composable
fun AppIcon(packageName: String, label: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val loader = LocalAppIconLoader.current
    var bitmap by remember(packageName) { mutableStateOf(loader?.cached(packageName)) }

    LaunchedEffect(packageName, loader) {
        if (bitmap == null && loader != null) {
            bitmap = loader.load(packageName)
        }
    }

    val shape = RoundedCornerShape(size / 3)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = stringResource(R.string.cd_app_icon, label),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size),
            )
        } else {
            Text(
                text = label.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
