package io.github.leonhardweiler.diffusion.data.room

import android.os.SystemClock
import android.util.Log
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import io.github.leonhardweiler.diffusion.manager.Progress
import io.github.leonhardweiler.diffusion.manager.isExtensionSupported
import io.github.leonhardweiler.diffusion.ui.model.GridNote
import io.github.leonhardweiler.diffusion.ui.model.NoteHeader
import io.github.leonhardweiler.diffusion.ui.model.SortOrder
import io.github.leonhardweiler.diffusion.ui.model.FolderModel
import kotlinx.coroutines.flow.Flow


private const val TAG = "Dao"

/**
 * The largest file whose text is read into the index — and, because the editor
 * is handed what the index holds, the largest note that can be opened here.
 */
const val LIMIT_FILE_SIZE_DB = 2 * 1024 * 1024

/**
 * What of a file goes into the index.
 *
 * Every file in the repository gets a row, so that the list is the repository
 * and not a filtered view of it — a photo next to the note that mentions it is
 * the thing a user came looking for. Only what the app can show itself is read:
 * a jpeg is not text, and a note above the size limit is one the search would
 * choke on rather than help with. Everything else is a row with a name, a date
 * and nothing to search in.
 */
private fun NodeFs.File.contentForIndex(): String {
    if (!isExtensionSupported(extension.text)) return ""

    val fileSize = fileSize()
    if (fileSize > LIMIT_FILE_SIZE_DB) {
        Log.d(TAG, "not reading $path: $fileSize is above $LIMIT_FILE_SIZE_DB")
        return ""
    }

    return readText()
}

/**
 * What a row of the note list is made of: what it shows, plus whether its file
 * name is enough to tell it apart from the others. The content is left out on
 * purpose — see [io.github.leonhardweiler.diffusion.ui.model.NoteHeader].
 */
private const val NOTE_HEADER_COLUMNS = """
    relativePath, lastModifiedTimeMillis, id, fileName,
    CASE WHEN COUNT(*) OVER (PARTITION BY fileName) = 1 THEN 1 ELSE 0 END AS isUnique
"""

/**
 * How a sort order reads in SQL. Which columns carry the name and the date
 * differs between notes and folders, so the caller names them.
 */
private fun SortOrder.orderBy(nameColumn: String, dateColumn: String): String = when (this) {
    SortOrder.AZ -> "$nameColumn ASC"
    SortOrder.ZA -> "$nameColumn DESC"
    SortOrder.MostRecent -> "$dateColumn DESC"
    SortOrder.Oldest -> "$dateColumn ASC"
}

/**
 * Room only supports FTS4, whose MATCH syntax gives some characters a meaning
 * the user did not type. Quote the whole query as soon as one turns up.
 */
internal fun ftsEscape(query: String): String {
    val specialChars =
        listOf("\"", "*", "-", "(", ")", "<", ">", ":", "^", "~", "'", "AND", "OR", "NOT")

    if (specialChars.none { query.contains(it) }) return "$query*"

    return "\"${query.replace("\"", "\"\"")}\" * "
}


@Dao
interface RepoDatabaseDao {

    /**
     * Rebuilds the whole index from the files, which are the source of truth.
     * Nothing here is worth keeping: a row that disagrees with its file is
     * exactly what this is meant to get rid of.
     */
    @Transaction
    suspend fun clearAndInit(
        rootPath: String,
        progressCb: ((Progress) -> Unit)? = null
    ) {
        // What this costs is the question behind every idea of doing without
        // the index and reading the repository at each start instead: the walk
        // is the same one, it would just have to happen every time rather than
        // when HEAD has moved. So it says how long it took and over how much —
        // `adb logcat -s StorageManager Dao` on a real repository
        // on a real phone answers it.
        val startedAt = SystemClock.elapsedRealtime()
        var files = 0
        var folders = 0

        Log.d(TAG, "clearAndInit")
        clearDatabase()

        val rootFs = NodeFs.Folder.fromPath(rootPath)
        val rootFolder = NoteFolder.new(
            relativePath = "",
        )
        insertNoteFolderRow(rootFolder)

        val rootLength = rootFs.path.length + 1

        suspend fun initRec(folder: NodeFs.Folder) {

            folder.forEachNodeFs { nodeFs ->

                when (nodeFs) {
                    is NodeFs.File -> {
                        // .gitkeep, .gitignore and whatever else a repository
                        // keeps for itself are not files anybody came here to
                        // read — and .gitkeep is one this app writes.
                        if (nodeFs.isHidden() || nodeFs.isSym()) {
                            return@forEachNodeFs
                        }

                        // The file's own date, and nothing else. It is the only
                        // one that is right for a note that was written and not
                        // committed yet, and a checkout hands the others theirs
                        // (see applyCommitTimestamps) rather than the moment it
                        // ran — so the commit history has nothing to add here.
                        val note = Note.new(
                            relativePath = nodeFs.path.substring(startIndex = rootLength),
                            lastModifiedTimeMillis = nodeFs.lastModifiedTime().toMillis(),
                            content = nodeFs.contentForIndex(),
                        )
                        // straight in: the table was cleared a moment ago, so
                        // there is nothing here that could already be there
                        insertNoteRow(note)
                        files++
                    }

                    is NodeFs.Folder -> {
                        if (nodeFs.isHidden() || nodeFs.isSym()) {
                            return@forEachNodeFs
                        }
                        val noteFolder = NoteFolder.new(
                            relativePath = nodeFs.path.substring(startIndex = rootLength),
                        )
                        insertNoteFolderRow(noteFolder)
                        folders++
                        progressCb?.invoke(Progress.GeneratingDatabase(noteFolder.relativePath))
                        initRec(nodeFs)
                    }
                }
            }
        }

        initRec(rootFs)

        Log.i(
            TAG,
            "clearAndInit: $files files in $folders folders, " +
                    "${SystemClock.elapsedRealtime() - startedAt} ms"
        )
    }


    @Query(
        """
    SELECT EXISTS(
        SELECT 1 FROM Notes WHERE relativePath = :relativePath
    )
    """
    )
    suspend fun isNoteExist(relativePath: String): Boolean

    /**
     * The notes of one folder. parentPath is a stored column with an index, so
     * the filter does not have to be computed for every row.
     */
    private fun gridNotesQuery(
        currentNoteFolderRelativePath: String,
        sortOrder: SortOrder,
    ): SupportSQLiteQuery {
        val sql = """
            SELECT $NOTE_HEADER_COLUMNS
            FROM Notes
            WHERE parentPath = ?
            ORDER BY ${sortOrder.orderBy("fileName", "lastModifiedTimeMillis")}
        """.trimIndent()

        return SimpleSQLiteQuery(sql, arrayOf(currentNoteFolderRelativePath))
    }

    /** The search, which unlike the list reaches into subfolders and the text. */
    private fun gridNotesWithQueryQuery(
        currentNoteFolderRelativePath: String,
        sortOrder: SortOrder,
        query: String,
    ): SupportSQLiteQuery {
        val sql = """
            WITH matches AS (
                SELECT Notes.relativePath, Notes.lastModifiedTimeMillis, Notes.id,
                       Notes.fileName,
                       rank(matchinfo(NotesFts, 'pcx')) AS score
                FROM Notes
                JOIN NotesFts ON NotesFts.rowid = Notes.rowid
                WHERE Notes.relativePath LIKE ? || '%' AND NotesFts MATCH ?
            )
            SELECT $NOTE_HEADER_COLUMNS
            FROM matches
            ORDER BY score DESC, ${sortOrder.orderBy("fileName", "lastModifiedTimeMillis")}
        """.trimIndent()

        return SimpleSQLiteQuery(
            sql,
            arrayOf(currentNoteFolderRelativePath, ftsEscape(query))
        )
    }

    @RawQuery(observedEntities = [Note::class])
    fun gridNotesRaw(query: SupportSQLiteQuery): PagingSource<Int, GridNote>

    /**
     * The same rows the list is paging through, all at once. Only "select all"
     * asks for this — everything else has no business holding the whole folder.
     */
    @RawQuery
    suspend fun gridNoteListRaw(query: SupportSQLiteQuery): List<GridNote>

    fun gridNotes(
        currentNoteFolderRelativePath: String,
        sortOrder: SortOrder,
    ): PagingSource<Int, GridNote> =
        gridNotesRaw(gridNotesQuery(currentNoteFolderRelativePath, sortOrder))

    fun gridNotesWithQuery(
        currentNoteFolderRelativePath: String,
        sortOrder: SortOrder,
        query: String,
    ): PagingSource<Int, GridNote> =
        gridNotesRaw(gridNotesWithQueryQuery(currentNoteFolderRelativePath, sortOrder, query))

    suspend fun gridNoteList(
        currentNoteFolderRelativePath: String,
        sortOrder: SortOrder,
        query: String,
    ): List<GridNote> = gridNoteListRaw(
        if (query.isEmpty()) {
            gridNotesQuery(currentNoteFolderRelativePath, sortOrder)
        } else {
            gridNotesWithQueryQuery(currentNoteFolderRelativePath, sortOrder, query)
        }
    )

    private fun foldersQuery(
        currentNoteFolderRelativePath: String,
        sortOrder: SortOrder,
    ): SupportSQLiteQuery {
        val sql = """
            SELECT f.relativePath, f.id, COUNT(n.relativePath) as noteCount,
                   fullName(f.relativePath) as folderName
            FROM NoteFolders AS f
            LEFT JOIN Notes AS n ON n.relativePath LIKE f.relativePath || '%'
            WHERE parentPath(f.relativePath) = ?
            GROUP BY f.relativePath, f.id, folderName
            ORDER BY ${sortOrder.orderBy("folderName", "MAX(n.lastModifiedTimeMillis)")}
        """.trimIndent()

        return SimpleSQLiteQuery(sql, arrayOf(currentNoteFolderRelativePath))
    }

    @RawQuery(observedEntities = [Note::class, NoteFolder::class])
    fun foldersRaw(query: SupportSQLiteQuery): Flow<List<FolderModel>>

    @RawQuery
    suspend fun folderListRaw(query: SupportSQLiteQuery): List<FolderModel>

    fun folders(
        currentNoteFolderRelativePath: String,
        sortOrder: SortOrder,
    ): Flow<List<FolderModel>> =
        foldersRaw(foldersQuery(currentNoteFolderRelativePath, sortOrder))

    suspend fun folderList(
        currentNoteFolderRelativePath: String,
        sortOrder: SortOrder,
    ): List<FolderModel> =
        folderListRaw(foldersQuery(currentNoteFolderRelativePath, sortOrder))

    /**
     * The row goes in, whether or not one is already there.
     *
     * Not `@Upsert`, which Room implements by inserting and catching the
     * constraint violation when that fails. Every note write did that, so
     * logcat carried a "UNIQUE constraint failed: Notes.relativePath" twice a
     * second while somebody was typing — and behind each one an exception
     * thrown and caught, and the whole row, content and all, bound twice.
     * Updating first costs one statement in the case that actually happens: a
     * note that is being written again.
     */
    @Transaction
    suspend fun insertNoteFolder(noteFolder: NoteFolder) {
        if (updateNoteFolderRow(noteFolder) == 0) insertNoteFolderRow(noteFolder)
    }

    @Update
    suspend fun updateNoteFolderRow(noteFolder: NoteFolder): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteFolderRow(noteFolder: NoteFolder)

    /**
     * Delete all notes inside the note folder, its subfolders, and the note
     * folder itself.
     *
     * The subfolders matter: the directory goes recursively on disk, and a row
     * left behind for one of them is a folder in the list that no longer has
     * anything to open.
     */
    @Transaction
    suspend fun deleteNoteFolder(noteFolder: NoteFolder) {
        internalDeleteNotesIn(noteFolder.relativePath + '/')
        internalDeleteFoldersIn(noteFolder.relativePath + '/')
        internalDeleteNoteFolder(noteFolder)
    }

    /**
     * The notes standing anywhere under a folder, that folder's own path with a
     * slash after it. For moving one: the rows have to be written again under
     * the new path, and they carry everything that a rebuild would otherwise
     * have to read off the disk again.
     */
    @Query("SELECT * FROM Notes WHERE relativePath LIKE :relativePath || '%'")
    suspend fun notesIn(relativePath: String): List<Note>

    /** The folder itself and everything under it, for the same reason. */
    @Query("SELECT * FROM NoteFolders WHERE relativePath = :relativePath OR relativePath LIKE :relativePath || '/%'")
    suspend fun foldersUnder(relativePath: String): List<NoteFolder>

    /**
     * Private
     * Note: always add a '/' at the end of relativePath param
     */
    @Query("DELETE FROM Notes WHERE relativePath LIKE :relativePath || '%'")
    suspend fun internalDeleteNotesIn(relativePath: String)

    /**
     * Private
     * Note: always add a '/' at the end of relativePath param
     */
    @Query("DELETE FROM NoteFolders WHERE relativePath LIKE :relativePath || '%'")
    suspend fun internalDeleteFoldersIn(relativePath: String)

    /**
     * Private
     */
    @Delete
    suspend fun internalDeleteNoteFolder(noteFolder: NoteFolder)

    /** See [insertNoteFolder] for why this is not an `@Upsert`. */
    @Transaction
    suspend fun insertNote(note: Note) {
        if (updateNoteRow(note) == 0) insertNoteRow(note)
    }

    @Update
    suspend fun updateNoteRow(note: Note): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteRow(note: Note)

    @Delete
    suspend fun removeNote(note: Note)

    /**
     * The note behind a row of the list, which only carries a [NoteHeader].
     */
    @Query("SELECT * FROM Notes WHERE relativePath = :relativePath")
    suspend fun note(relativePath: String): Note?

    @Query("DELETE FROM Notes WHERE relativePath = :relativePath")
    suspend fun removeNoteAt(relativePath: String)

    @Query("DELETE  FROM NoteFolders")
    fun removeAllNoteFolder()

    @Query("DELETE  FROM Notes")
    fun removeAllNote()

    fun clearDatabase() {
        removeAllNoteFolder()
        removeAllNote()
    }
}
