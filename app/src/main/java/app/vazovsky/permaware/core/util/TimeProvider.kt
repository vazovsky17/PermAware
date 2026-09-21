package app.vazovsky.permaware.core.util

import javax.inject.Inject
import javax.inject.Singleton

/** Прослойка над системными часами, чтобы проверка, сравнение и хранение остались тестируемыми. */
interface TimeProvider {
    fun now(): Long
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
}
