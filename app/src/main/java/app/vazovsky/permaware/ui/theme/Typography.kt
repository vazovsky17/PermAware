package app.vazovsky.permaware.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private val lineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

object PermAwareType {
    val score = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 46.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.02).em,
    )
    val display = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.01).em,
        lineHeightStyle = lineHeight,
    )

    /** Заголовок экрана. */
    val headline = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.005).em,
        lineHeightStyle = lineHeight,
    )

    /** Заголовок карточки. */
    val title = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        lineHeightStyle = lineHeight,
    )

    /** Заголовок строки. */
    val titleSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        lineHeightStyle = lineHeight,
    )
    val body = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        lineHeightStyle = lineHeight,
    )

    /** Подпись секции; читается она подписью, а не мелким текстом, именно за счёт разрядки. */
    val label = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.04.em,
    )
    val labelSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.em,
    )

    /** Метаданные: версии, время, имена пакетов. */
    val caption = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
}

internal val PermAwareTypography = Typography(
    displaySmall = PermAwareType.display,
    headlineLarge = PermAwareType.display,
    headlineMedium = PermAwareType.headline,
    headlineSmall = PermAwareType.headline,
    titleLarge = PermAwareType.title,
    titleMedium = PermAwareType.title,
    titleSmall = PermAwareType.titleSmall,
    bodyLarge = PermAwareType.body,
    bodyMedium = PermAwareType.body,
    bodySmall = PermAwareType.caption,
    labelLarge = PermAwareType.label,
    labelMedium = PermAwareType.label,
    labelSmall = PermAwareType.labelSmall,
)
