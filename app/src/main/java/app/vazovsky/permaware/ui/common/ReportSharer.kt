package app.vazovsky.permaware.ui.common

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Пишет отчёт в приватный файл и отдаёт его системному листу "Поделиться".
 *
 * **Почему файл, а не extra в интенте.** Сначала отчёт передавался как `EXTRA_TEXT`. На живом
 * устройстве измерено: JSON-отчёт весит 125 КБ на 53 пользовательских приложения, то есть около 2,4
 * КБ на приложение, — значит, устройство с парой сотен приложений даст примерно 700 КБ. Extra в
 * интенте едет через binder-транзакцию с жёстким потолком, и отказ выглядит как
 * `TransactionTooLargeException`, брошенный внутри системы, а не там, где приложение могло бы его с
 * пользой поймать.
 *
 * Файл лежит в приватном кэше приложения и наружу выходит только через неэкспортируемый
 * `FileProvider`, причём доступ на чтение выдаётся каждому получателю отдельно и на время отправки.
 */
object ReportSharer {

    private const val DIRECTORY = "reports"
    private const val AUTHORITY_SUFFIX = ".fileprovider"

    enum class Format(val extension: String, val mimeType: String) {
        TEXT("txt", "text/plain"),
        JSON("json", "application/json"),
    }

    /**
     * @return false, когда файл не удалось записать или отправку некому обработать, — чтобы
     *   вызывающая сторона показала сообщение, а человек не нажимал в пустоту.
     */
    fun share(context: Context, content: String, format: Format, subject: String, chooserTitle: String): Boolean {
        val uri = try {
            val directory = File(context.cacheDir, DIRECTORY)
            // Прошлые выгрузки удаляются, а не копятся в кэше: отчёт — слепок момента, и
            // залежавшиеся копии это только лишняя поверхность утечки.
            if (directory.exists()) directory.listFiles()?.forEach { it.delete() } else directory.mkdirs()
            val file = File(directory, "permission-watch-report.${format.extension}")
            file.writeText(content)
            FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
        } catch (e: Exception) {
            return false
        }

        val send = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            // ClipData доносит выданный доступ до каждого адресата в списке выбора, а не только до
            // первого.
            clipData = ClipData.newRawUri(subject, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return try {
            context.startActivity(
                Intent.createChooser(send, chooserTitle).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
            )
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}
