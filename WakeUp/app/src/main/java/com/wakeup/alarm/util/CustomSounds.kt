package com.wakeup.alarm.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.io.File

/**
 * The user's own alarm sounds.
 *
 * A picked audio file is COPIED into the app's private storage (device-protected, so it can also play before the
 * first unlock after a reboot). That is why the app can list it and delete it again later: it owns the copy. The
 * original file is never touched. An alarm stores the copy as a `file://` URI string.
 */
object CustomSounds {

    const val MAX_BYTES: Long = 20L * 1024 * 1024
    const val MAX_MEGABYTES: Int = 20
    const val MAX_COUNT: Int = 20

    data class Entry(val name: String, val uriString: String)

    sealed interface ImportResult {
        data class Added(val uriString: String, val name: String) : ImportResult
        data object TooBig : ImportResult
        data object TooMany : ImportResult
        data object Unusable : ImportResult
    }

    private fun folder(context: Context): File {
        val storage = ContextCompat.createDeviceProtectedStorageContext(context) ?: context
        return File(storage.filesDir, "sounds").also { it.mkdirs() }
    }

    private fun files(context: Context): List<File> =
        folder(context).listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") }.orEmpty()

    /** The user's sounds, newest first. Blocking: call from a background dispatcher. */
    fun list(context: Context): List<Entry> =
        files(context).sortedByDescending { it.lastModified() }.map { Entry(displayName(it), Uri.fromFile(it).toString()) }

    /** True if [uriString] points at one of our own copies. */
    fun isCustom(context: Context, uriString: String): Boolean = fileFor(context, uriString) != null

    /** The readable name of a custom sound, or null if [uriString] is not one of ours. */
    fun nameOf(context: Context, uriString: String): String? = fileFor(context, uriString)?.let { displayName(it) }

    /** Deletes our copy. Returns true if the file is gone afterwards. Blocking. */
    fun delete(context: Context, uriString: String): Boolean {
        val file = fileFor(context, uriString) ?: return false
        return !file.exists() || file.delete()
    }

    /** Copies [source] (a document the user picked) into our storage after checking it is real, playable audio. */
    fun import(context: Context, source: Uri): ImportResult {
        val dir = folder(context)
        if (files(context).size >= MAX_COUNT) return ImportResult.TooMany

        val displayName = queryDisplayName(context, source) ?: "sound"
        val base = displayName.substringBeforeLast('.', displayName)
        val extension = displayName.substringAfterLast('.', "").lowercase().filter { it.isLetterOrDigit() }.take(5)
            .ifEmpty { "audio" }
        val safeBase = base.map { if (it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_') it else '_' }
            .joinToString("").trim().take(40).ifEmpty { "sound" }

        val target = File(dir, "${System.currentTimeMillis()}_$safeBase.$extension")
        val temp = File(dir, target.name + ".tmp")
        try {
            var total = 0L
            var tooBig = false
            val input = context.contentResolver.openInputStream(source) ?: return ImportResult.Unusable
            input.use { stream ->
                temp.outputStream().use { out ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_BYTES) {
                            tooBig = true
                            break
                        }
                        out.write(buffer, 0, read)
                    }
                }
            }
            if (tooBig) return ImportResult.TooBig
            if (total == 0L || !temp.renameTo(target)) return ImportResult.Unusable
            if (!looksLikeAudio(target)) {
                target.delete()
                return ImportResult.Unusable
            }
            return ImportResult.Added(Uri.fromFile(target).toString(), displayName(target))
        } catch (_: Exception) {
            target.delete()
            return ImportResult.Unusable
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    private fun looksLikeAudio(file: File): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            hasAudio && duration > 0
        } catch (_: Exception) {
            false
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (_: Exception) {
        null
    }

    /** "1736000000000_My song.mp3" -> "My song" */
    private fun displayName(file: File): String =
        file.nameWithoutExtension.replace(Regex("^\\d+_"), "").ifBlank { "Custom sound" }

    /** Only files directly inside our sounds folder count; anything else is never touched. */
    private fun fileFor(context: Context, uriString: String): File? {
        if (uriString.isBlank()) return null
        return try {
            val uri = uriString.toUri()
            if (uri.scheme != "file") return null
            val file = File(uri.path ?: return null).canonicalFile
            if (file.parentFile == folder(context).canonicalFile) file else null
        } catch (_: Exception) {
            null
        }
    }
}
