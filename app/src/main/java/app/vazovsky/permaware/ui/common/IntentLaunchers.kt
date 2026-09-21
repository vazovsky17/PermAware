package app.vazovsky.permaware.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Уход из приложения наружу. Два интента, и ни один не везёт пользовательских данных. (Отправка
 * отчёта живёт в [ReportSharer] — ей приходится прикладывать URI файла.)
 *
 * PermAware никогда не меняет разрешение сам — он отводит человека на системный экран, который это
 * делает. Это и единственный поддерживаемый путь, и правильная модель доверия для такого
 * инструмента.
 */
object IntentLaunchers {

    /** Открывает системную страницу "О приложении", где разрешения можно поменять. */
    fun openAppSettings(context: Context, packageName: String): Boolean = tryStart(context) {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Открывает ссылку в браузере пользователя.
     *
     * `CATEGORY_BROWSABLE` вместе с `FLAG_ACTIVITY_NEW_TASK` держат это за пределами нашей задачи,
     * так что внешняя страница никогда не выглядит частью PermAware. К ссылке ничего не
     * дописывается.
     */
    fun openUrl(context: Context, url: String): Boolean = tryStart(context) {
        Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * @return false, когда интент некому обработать, — чтобы вызывающая сторона показала
     *   сообщение, а не приложение падало на устройстве без браузера или экрана настроек.
     */
    private inline fun tryStart(context: Context, build: () -> Intent): Boolean = try {
        context.startActivity(build())
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    private fun String.toUri(): Uri = Uri.parse(this)
}
