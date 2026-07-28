package io.github.wiiznokes.gitnote.helper

import android.util.Log
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.ui.model.EditType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

private const val TAG = "NoteSaver"

private const val DRAFT = "edit_draft"
private const val DRAFT_TMP = "edit_draft.tmp"

private val LEGACY_FILES = listOf(
    "EDIT_IS_UNSAVED",
    "EDIT_EDIT_TYPE",
    "EDIT_PREVIOUS_NOTE",
    "EDIT_NAME",
    "EDIT_CONTENT",
)

data class SaveInfo(
    val name: String,
    val content: String,
    val previousNote: Note,
    val editType: EditType
)

// the note is stored field by field, so that a change to the Note entity does
// not make the drafts written by an older version unreadable
private data class StoredDraft(
    val name: String,
    val content: String,
    val editType: EditType,
    val noteRelativePath: String,
    val noteContent: String,
    val noteLastModifiedTimeMillis: Long,
    val noteId: Int,
) : Serializable

/**
 * The draft is written to a temporary file and renamed, so a draft on disk is
 * never partial, even if the process dies during a write.
 *
 * Writing and clearing are suspending and run on the io dispatcher: they happen
 * on every typing pause and once more when the editor is left, and doing that
 * on the main thread is a stall in the middle of typing. The lock keeps two
 * writes from meeting over the one temporary file.
 */
class NoteSaver(private val dir: File) {

    private val locker = Mutex()

    suspend fun save(
        shouldSave: Boolean,
        name: String,
        content: String,
        previousNote: Note,
        editType: EditType
    ) {
        if (!shouldSave) {
            clear()
            return
        }

        val draft = StoredDraft(
            name = name,
            content = content,
            editType = editType,
            noteRelativePath = previousNote.relativePath,
            noteContent = previousNote.content,
            noteLastModifiedTimeMillis = previousNote.lastModifiedTimeMillis,
            noteId = previousNote.id,
        )

        onIo {
            try {
                val tmp = file(DRAFT_TMP)
                tmp.outputStream().use { out ->
                    ObjectOutputStream(out).use { it.writeObject(draft) }
                }
                if (!tmp.renameTo(file(DRAFT))) {
                    tmp.delete()
                    Log.e(TAG, "can't rename ${tmp.path}")
                    return@onIo
                }
                Log.d(TAG, "draft saved")
            } catch (e: Exception) {
                Log.e(TAG, "can't save the draft", e)
            }
        }
    }

    suspend fun clear() = onIo {
        file(DRAFT_TMP).delete()
        if (file(DRAFT).delete()) {
            Log.d(TAG, "draft cleared")
        }
    }

    private suspend fun <T> onIo(f: () -> T): T = locker.withLock {
        withContext(Dispatchers.IO) { f() }
    }

    fun getSaveState(): SaveInfo? {
        val draftFile = file(DRAFT)
        if (!draftFile.exists()) return null

        return try {
            val draft = draftFile.inputStream().use { input ->
                ObjectInputStream(input).use { it.readObject() as StoredDraft }
            }
            SaveInfo(
                name = draft.name,
                content = draft.content,
                previousNote = Note(
                    relativePath = draft.noteRelativePath,
                    content = draft.noteContent,
                    lastModifiedTimeMillis = draft.noteLastModifiedTimeMillis,
                    id = draft.noteId,
                ),
                editType = draft.editType,
            )
        } catch (e: Exception) {
            Log.e(TAG, "can't read the draft, dropping it", e)
            draftFile.delete()
            null
        }
    }

    fun deleteLegacyFiles() {
        LEGACY_FILES.forEach { file(it).delete() }
    }

    private fun file(name: String): File = File(dir, name)
}
