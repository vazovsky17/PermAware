package app.vazovsky.permaware.data.platform

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Грузит иконки приложений лениво, вне главного потока, растрируя их до ограниченного размера.
 *
 * Иконки намеренно *не* входят в проверку и никогда не попадают в базу.
 */
@Singleton
class AppIconLoader @Inject constructor(private val packageManager: PackageManager) {
    private val cache = object : LruCache<String, Bitmap>(MAX_CACHED_ICONS) {}

    fun cached(packageName: String): Bitmap? = cache.get(packageName)

    suspend fun load(packageName: String): Bitmap? {
        cache.get(packageName)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                packageManager.getApplicationInfo(packageName, 0)
                    .loadIcon(packageManager)
                    .toBitmap()
            }.getOrNull()
        }
        if (bitmap != null) cache.put(packageName, bitmap)
        return bitmap
    }

    fun clear() = cache.evictAll()

    private fun Drawable.toBitmap(): Bitmap? {
        (this as? BitmapDrawable)?.bitmap?.let { existing ->
            if (!existing.isRecycled) {
                return if (existing.width <= ICON_SIZE_PX) {
                    existing
                } else {
                    existing.scale(ICON_SIZE_PX, ICON_SIZE_PX)
                }
            }
        }
        val width = if (intrinsicWidth > 0) minOf(intrinsicWidth, ICON_SIZE_PX) else ICON_SIZE_PX
        val height = if (intrinsicHeight > 0) minOf(intrinsicHeight, ICON_SIZE_PX) else ICON_SIZE_PX
        return runCatching {
            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
            bitmap
        }.getOrNull()
    }

    private companion object {
        /** Хватает на несколько экранов списка и при этом не держит все иконки устройства. */
        const val MAX_CACHED_ICONS = 150

        /**
         * В строках иконки показываются в 40dp; 144px покрывают самую высокую целевую плотность с
         * запасом.
         */
        const val ICON_SIZE_PX = 144
    }
}
