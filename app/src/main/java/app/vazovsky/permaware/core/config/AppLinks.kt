package app.vazovsky.permaware.core.config

import app.vazovsky.permaware.BuildConfig

/**
 * Все исходящие ссылки приложения. Больше нигде в коде URL зашивать нельзя.
 *
 * Обе приезжают на этапе сборки, чтобы менять их можно было не трогая код:
 *
 * ```
 * # gradle.properties или -P в командной строке
 * permaware.boostyUrl=https://boosty.to/your-page
 * permaware.githubUrl=https://github.com/you/YourApp
 * permaware.contactUrl=https://t.me/you
 * ```
 *
 * Если ссылку не задать, значение будет `null`, и то, что на неё опирается, просто не
 * показывается — приложение никогда не выводит выдуманную или мёртвую ссылку. Отладочные
 * сборки показывают заметную заглушку, чтобы ненастроенный релиз не проскочил незамеченным.
 */
object AppLinks {

    val BOOSTY_URL: String? = BuildConfig.BOOSTY_URL

    /** Репозиторий с исходниками. На него ведёт кнопка на странице самого PermAware. */
    val GITHUB_URL: String? = BuildConfig.GITHUB_URL

    val CONTACT_URL: String? = BuildConfig.CONTACT_URL

    val isSupportConfigured: Boolean get() = !BOOSTY_URL.isNullOrBlank()

    val isSourceConfigured: Boolean get() = !GITHUB_URL.isNullOrBlank()

    val isContactConfigured: Boolean get() = !CONTACT_URL.isNullOrBlank()
}
