package app.footyos.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.core.content.FileProvider
import java.io.File

/** App-private photos, excluded by the backup allowlist. Call on an IO dispatcher. */
class MealPhotos(private val context: Context) {
    private val directory get() = File(context.filesDir, "meal_photos").apply { mkdirs() }

    fun create(): File {
        prune()
        return File.createTempFile("meal_", ".jpg", directory)
    }

    fun prune(now: Long = System.currentTimeMillis()) {
        directory.listFiles()?.filter { now - it.lastModified() > RETENTION_MS }?.forEach { it.delete() }
    }

    fun compress(name: String) {
        val original = file(name)
        val bitmap = requireNotNull(preview(name)) { "Unreadable image" }
        val temp = File(directory, "$name.tmp")
        try {
            temp.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it)) }
            check(temp.renameTo(original))
        } finally {
            bitmap.recycle()
            temp.delete()
        }
    }

    companion object { const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000 }

    fun file(name: String): File {
        require(name == File(name).name && name.startsWith("meal_"))
        return File(directory, name)
    }

    fun uri(name: String) = FileProvider.getUriForFile(context, "${context.packageName}.mealphotos", file(name))

    fun delete(name: String?) { name?.let { file(it).delete() } }

    fun preview(name: String): Bitmap? {
        val source = file(name)
        if (System.currentTimeMillis() - source.lastModified() > RETENTION_MS) { source.delete(); return null }
        val path = source.absolutePath
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1200) sample *= 2
        val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val matrix = Matrix()
        when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(270f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
        }
        val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (oriented !== bitmap) bitmap.recycle()
        return oriented
    }
}
