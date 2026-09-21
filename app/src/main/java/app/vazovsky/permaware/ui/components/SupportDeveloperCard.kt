package app.vazovsky.permaware.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.R
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupportDeveloperCard(
    modifier: Modifier = Modifier,
    dismissible: Boolean = false,
    onSupportClick: () -> Unit,
    onDismiss: () -> Unit = {},
) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing
    val externalHint = stringResource(R.string.cd_open_external)

    PermCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(PermAwareTheme.spacing.cardPaddingLarge),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(PermAwareTheme.icons.disc)
                    .clip(CircleShape)
                    .background(colors.surfaceMuted),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(PermAwareTheme.icons.md),
                )
            }
            Text(
                text = stringResource(R.string.support_title),
                style = PermAwareType.title,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = stringResource(R.string.support_body),
            style = PermAwareType.body,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = spacing.sm),
        )
        Text(
            text = stringResource(R.string.support_voluntary_note),
            style = PermAwareType.caption,
            color = colors.textDisabled,
            modifier = Modifier.padding(top = spacing.xxs),
        )

        // FlowRow, а не Row, и причину стоит сохранить.
        //
        // В Row обе кнопки меряются по очереди против того, что осталось от ширины, так что кнопке
        // «закрыть» — её меряли последней — доставался остаток после того, как длинная основная
        // подпись забрала своё. На тех русских строках, что уехали в релиз, этого остатка уже не
        // хватало на устройстве 1080 × 2340 при масштабе шрифта 1,0, и Compose сделал единственное,
        // что можно сделать с одним не влезающим словом: разорвал его посередине, нарисовав
        // «Скрыт», а под ним «ь».
        //
        // Никакого меню переполнения здесь нет и не было — залётные символы были хвостом
        // разорванного слова, а не элементом управления. Поэтому чинится раскладка, а не подпись и
        // не размер шрифта: FlowRow меряет каждую кнопку против полной ширины и уводит вторую на
        // отдельную строку, когда вдвоём им не поместиться. Так или иначе каждая подпись остаётся
        // целой и в одну строку — при любом масштабе шрифта и на обоих языках.
        FlowRow(
            modifier = Modifier.padding(top = spacing.md),
            // Своего горизонтального зазора нет: у TextButton и так по 12dp отступа с каждой
            // стороны, так что spacedBy здесь — это второй зазор поверх оптического. На устройстве
            // 1080 × 2340 измерено: именно этот удвоенный зазор и выталкивал пару на две строки при
            // масштабе 1,0 — стоит его убрать, и обе подписи встают бок о бок ещё и с запасом.
            // Вертикальное значение по-прежнему в силе, для тех масштабов, где им и правда не
            // поместиться в одну строку.
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onSupportClick,
                shape = PermAwareTheme.shapes.md,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                modifier = Modifier.semantics { contentDescription = externalHint },
            ) {
                Text(
                    text = stringResource(R.string.support_action),
                    style = PermAwareType.titleSmall,
                    maxLines = 1,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = spacing.xs)
                        .size(PermAwareTheme.icons.sm),
                )
            }
            if (dismissible) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.support_dismiss),
                        style = PermAwareType.titleSmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEDE7FD)
@Composable
private fun SupportDeveloperCardPreview() {
    PermAwareTheme {
        SupportDeveloperCard(dismissible = true, onSupportClick = {}, modifier = Modifier.padding(20.dp))
    }
}
