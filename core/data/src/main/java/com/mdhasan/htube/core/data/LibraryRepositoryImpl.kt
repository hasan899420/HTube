package com.mdhasan.htube.core.data

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.mdhasan.htube.core.database.DeletedHistoryDao
import com.mdhasan.htube.core.database.DeletedHistoryEntity
import com.mdhasan.htube.core.database.DownloadDao
import com.mdhasan.htube.core.database.DownloadEntity
import com.mdhasan.htube.core.database.asEntity
import com.mdhasan.htube.core.database.asModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

internal class LibraryRepositoryImpl(
    private val context: Context,
    private val dao: DownloadDao,
    private val history: DeletedHistoryDao,
) : LibraryRepository {
    // --- Gallery hiding -------------------------------------------------
    // Vault and Recycle Bin used to flip only a database flag, leaving the
    // file at its public path, so Gallery/Album kept showing it. Now a
    // locked/trashed file is MOVED to the app's private filesDir, which
    // MediaStore never scans, and its original path is kept in a small
    // sidecar file next to it so Restore/Unlock can put it back exactly
    // where it was. Delete-forever also tells MediaScanner the old path
    // is gone, so Gallery does not keep a stale entry.
    private fun hiddenDir(kind: String): File = File(context.filesDir, "hidden/$kind").apply { mkdirs() }

    private fun moveFile(source: File, target: File): Boolean {
        if (source.renameTo(target)) return true
        return runCatching {
            source.copyTo(target, overwrite = true)
            source.delete()
            target.exists()
        }.getOrDefault(false)
    }

    private fun uniqueFile(dir: File, name: String): File {
        var f = File(dir, name)
        if (!f.exists()) return f
        val base = name.substringBeforeLast('.', name)
        val ext = if ('.' in name) ".${name.substringAfterLast('.')}" else ""
        var n = 1
        while (f.exists()) f = File(dir, "$base ($n)$ext").also { n++ }
        return f
    }

    /** Moves a public file into the private hidden folder. Returns the new path, or null. */
    private fun hideFile(e: DownloadEntity, kind: String): String? {
        val old = File(e.filePath)
        if (!old.exists()) return null
        val dir = hiddenDir(kind)
        val target = uniqueFile(dir, "${e.id}_${old.name}")
        if (!moveFile(old, target)) return null
        runCatching { File(dir, "${e.id}.path").writeText(old.absolutePath) }
        // Purge the MediaStore entry for the old public path.
        MediaScannerConnection.scanFile(context, arrayOf(old.absolutePath), null, null)
        return target.absolutePath
    }

    /** Moves a hidden file back to its original public path. Returns the new path, or null. */
    private fun unhideFile(e: DownloadEntity, kind: String): String? {
        val hidden = File(e.filePath)
        if (!hidden.exists()) return null
        val dir = hiddenDir(kind)
        val sidecar = File(dir, "${e.id}.path")
        val originalName = hidden.name.substringAfter('_', hidden.name)
        val originalPath = runCatching { sidecar.readText().trim() }.getOrNull()?.takeIf { it.isNotBlank() }
        val target = if (originalPath != null) {
            val t = File(originalPath)
            t.parentFile?.mkdirs()
            if (t.exists() && t != hidden) uniqueFile(t.parentFile!!, t.name) else t
        } else {
            val pub = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "HTube").apply { mkdirs() }
            uniqueFile(pub, originalName)
        }
        if (!moveFile(hidden, target)) return null
        runCatching { sidecar.delete() }
        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(e.mimeType), null)
        return target.absolutePath
    }

    private fun deleteSidecar(e: DownloadEntity) {
        listOf("trash", "vault").forEach { kind ->
            runCatching { File(hiddenDir(kind), "${e.id}.path").delete() }
        }
    }
    override val downloads = dao.observeAll().map { list -> list.map { it.asModel() } }
    override val trash = dao.observeTrash().map { list -> list.map { it.asModel() } }
    override fun observe(id: Long) = dao.observe(id).map { it?.asModel() }
    override suspend fun get(id: Long) = dao.get(id)?.asModel()

    override suspend fun moveToTrash(ids: List<Long>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        ids.forEach { id ->
            dao.get(id)?.let { e ->
                // Hiding the file from Gallery is the point of deleting it
                // from the app, even though it stays recoverable for 30 days.
                val newPath = if (e.status == com.mdhasan.htube.core.model.DownloadStatus.COMPLETED) hideFile(e, "trash") else null
                dao.update(e.copy(deletedAt = now, filePath = newPath ?: e.filePath))
            }
        }
    }

    override suspend fun restore(ids: List<Long>) = withContext(Dispatchers.IO) {
        ids.forEach { id ->
            dao.get(id)?.let { e ->
                val newPath = unhideFile(e, "trash")
                dao.update(e.copy(deletedAt = 0, filePath = newPath ?: e.filePath))
            }
        }
    }

    override val deletedHistory = history.observe().map { list -> list.map { it.asModel() } }
    override suspend fun removeDeletedHistory(ids: List<Long>) = history.delete(ids)
    override suspend fun clearDeletedHistory() = history.clear()

    override suspend fun deleteForever(ids: List<Long>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        // Remember what was deleted so it can be downloaded again later.
        history.insertAll(
            ids.mapNotNull { dao.get(it) }.filter { it.status == com.mdhasan.htube.core.model.DownloadStatus.COMPLETED }.map { e ->
                DeletedHistoryEntity(
                    title = e.title, pageUrl = e.pageUrl, url = e.url, thumbnail = e.thumbnail, mimeType = e.mimeType, quality = e.quality,
                    sizeBytes = e.totalBytes, durationSec = e.durationSec, deletedAt = now,
                )
            },
        )
        ids.forEach { id ->
            dao.get(id)?.let { e ->
                val main = File(e.filePath)
                main.delete()
                File(e.filePath + ".part").delete()
                File(e.filePath + ".audio.part").delete()
                File(e.filePath + ".part.idx").delete()
                File(e.filePath + ".audio.part.idx").delete()
                deleteSidecar(e)
                // Without this, Gallery keeps a stale MediaStore entry and
                // the deleted file still appears in the album.
                MediaScannerConnection.scanFile(context, arrayOf(main.absolutePath), null, null)
            }
        }
        dao.delete(ids)
    }

    override suspend fun purgeExpiredTrash() {
        val before = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(TRASH_DAYS)
        deleteForever(dao.trashOlderThan(before).map { it.id })
    }

    override suspend fun setVault(ids: List<Long>, inVault: Boolean) = withContext(Dispatchers.IO) {
        ids.forEach { id ->
            dao.get(id)?.let { e ->
                if (inVault) {
                    val newPath = hideFile(e, "vault")
                    dao.update(e.copy(inVault = true, filePath = newPath ?: e.filePath))
                } else {
                    val newPath = unhideFile(e, "vault")
                    dao.update(e.copy(inVault = false, filePath = newPath ?: e.filePath))
                }
            }
        }
    }

    override suspend fun rename(id: Long, newTitle: String): Boolean = withContext(Dispatchers.IO) {
        val e = dao.get(id) ?: return@withContext false
        val old = File(e.filePath)
        val ext = old.extension
        val safe = newTitle.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifBlank { return@withContext false }
        val target = File(old.parentFile, if (ext.isBlank()) safe else "$safe.$ext")
        if (target.exists() && target != old) return@withContext false
        if (old.exists() && !old.renameTo(target)) return@withContext false
        // If the file is hidden (vault/trash), its sidecar holds the public
        // original path: rename that too, so Restore/Unlock keeps the new name.
        val isHidden = "/hidden/" in old.absolutePath
        if (isHidden) {
            runCatching {
                val sidecar = File(old.parentFile, "${e.id}.path")
                val orig = sidecar.readText().trim()
                val origFile = File(orig)
                sidecar.writeText(File(origFile.parentFile, target.name.substringAfter('_', target.name)).absolutePath)
            }
        } else {
            MediaScannerConnection.scanFile(context, arrayOf(old.absolutePath), null, null)
            MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(e.mimeType), null)
        }
        dao.update(e.copy(title = safe, filePath = target.absolutePath))
        true
    }

    override suspend fun importFolder(dir: String): Int = withContext(Dispatchers.IO) {
        val root = File(dir)
        if (!root.isDirectory) return@withContext 0
        val known = dao.allPaths().toHashSet()
        val media = root.walkTopDown().maxDepth(2).filter { f ->
            f.isFile && !f.name.startsWith(".") && !f.name.endsWith(".video.mp4") && f.extension.lowercase() in MEDIA_EXT && f.absolutePath !in known
        }.toList()
        media.forEach { f ->
            val ext = f.extension.lowercase()
            val mime = when (ext) {
                "m4a", "aac" -> "audio/mp4"; "mp3" -> "audio/mpeg"; "opus", "ogg" -> "audio/ogg"; "wav" -> "audio/wav"
                "webm" -> "video/webm"; "mkv" -> "video/x-matroska"; "ts" -> "video/mp2t"; "jpg", "jpeg" -> "image/jpeg"; "png" -> "image/png"
                else -> "video/mp4"
            }
            // File names look like "Title(720p).mp4": split the quality back out.
            val base = f.nameWithoutExtension
            val q = Regex("""\(([^()]{1,12})\)(?: \(\d+\))?$""").find(base)?.groupValues?.get(1)
            val title = if (q != null) base.substringBeforeLast("($q)").trim() else base
            val duration = runCatching {
                val r = android.media.MediaMetadataRetriever()
                try {
                    r.setDataSource(f.absolutePath)
                    (r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: -1000) / 1000
                } finally { r.release() }
            }.getOrDefault(-1)
            dao.insert(
                com.mdhasan.htube.core.database.DownloadEntity(
                    title = title, pageUrl = null, sourceHost = null, thumbnail = null, url = f.toURI().toString(), audioUrl = null,
                    filePath = f.absolutePath, mimeType = mime, type = com.mdhasan.htube.core.model.ContentType.fromMime(mime), quality = q,
                    totalBytes = f.length(), downloadedBytes = f.length(), audioTotalBytes = -1, audioDownloadedBytes = 0,
                    status = com.mdhasan.htube.core.model.DownloadStatus.COMPLETED, error = null, durationSec = duration,
                    createdAt = f.lastModified(), completedAt = f.lastModified(), inVault = false, deletedAt = 0, lastPlayedPositionMs = 0,
                ),
            )
        }
        media.size
    }

    override suspend fun savePosition(id: Long, positionMs: Long) = dao.updatePosition(id, positionMs)

    override suspend fun addLocalFile(task: com.mdhasan.htube.core.model.DownloadTask) =
        dao.insert(task.asEntity())

    companion object {
        private val MEDIA_EXT = setOf("mp4", "m4a", "mp3", "webm", "mkv", "mov", "3gp", "aac", "opus", "ogg", "wav", "ts", "jpg", "jpeg", "png")
        const val TRASH_DAYS = 30L
    }
}
