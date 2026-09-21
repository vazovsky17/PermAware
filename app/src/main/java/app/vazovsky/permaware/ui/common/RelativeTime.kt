package app.vazovsky.permaware.ui.common

import android.content.Context
import android.text.format.DateFormat
import app.vazovsky.permaware.R
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Время по-человечески. Намеренно не `DateUtils.getRelativeTimeSpanString`: на нескольких
 * диапазонах он выдаёт корявый русский, а строке про последнюю проверку нужно "сегодня, 12:45", а
 * не "5 часов назад".
 */
object RelativeTime {

    /** "сегодня, 12:45" / "вчера, 09:10" / "13 сентября 2026". */
    fun dateTime(context: Context, timestamp: Long): String {
        val time = DateFormat.getTimeFormat(context).format(Date(timestamp))
        return when {
            isSameDay(timestamp, System.currentTimeMillis()) ->
                context.getString(R.string.time_today_at, time)
            isYesterday(timestamp) -> context.getString(R.string.time_yesterday_at, time)
            else -> DateFormat.getMediumDateFormat(context).format(Date(timestamp))
        }
    }

    /** "только что" / "12 минут назад" / "3 часа назад" / "5 дней назад" / absolute date. */
    fun ago(context: Context, timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val delta = (now - timestamp).coerceAtLeast(0)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
        val hours = TimeUnit.MILLISECONDS.toHours(delta)
        val days = TimeUnit.MILLISECONDS.toDays(delta)
        return when {
            minutes < 1 -> context.getString(R.string.time_just_now)
            minutes < 60 -> context.resources.getQuantityString(
                R.plurals.plural_minutes_ago,
                minutes.toInt(),
                minutes.toInt(),
            )
            hours < 24 -> context.resources.getQuantityString(
                R.plurals.plural_hours_ago,
                hours.toInt(),
                hours.toInt(),
            )
            days <= 30 -> context.resources.getQuantityString(
                R.plurals.plural_days_ago,
                days.toInt(),
                days.toInt(),
            )
            else -> DateFormat.getMediumDateFormat(context).format(Date(timestamp))
        }
    }

    fun date(context: Context, timestamp: Long): String =
        DateFormat.getMediumDateFormat(context).format(Date(timestamp))

    fun isSameDay(first: Long, second: Long): Boolean {
        val a = Calendar.getInstance().apply { timeInMillis = first }
        val b = Calendar.getInstance().apply { timeInMillis = second }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    fun isYesterday(timestamp: Long): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return isSameDay(timestamp, yesterday.timeInMillis)
    }
}
