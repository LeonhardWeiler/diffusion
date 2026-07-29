package io.github.leonhardweiler.diffusion.data.room

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
import io.github.leonhardweiler.diffusion.manager.isExtensionSupportedLib
import io.github.leonhardweiler.diffusion.ui.model.GridNote
import io.github.leonhardweiler.diffusion.ui.model.NoteHeader
import io.github.leonhardweiler.diffusion.ui.model.SortOrder
import io.github.leonhardweiler.diffusion.ui.model.FolderModel
import kotlinx.coroutines.flow.Flow


private const val TAG = "Dao"

private const val LIMIT_FILE_SIZE_DB = 2 * 1024 * 1024

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
private fun ftsEscape(query: String): String {
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
                        if (!isExtensionSupportedLib(nodeFs.extension.text)) {
                            //Log.d(TAG, "skipped ${nodeFs.path} because extension not supported")
                            return@forEachNodeFs
                        }

                        val fileSize = nodeFs.fileSize()
                        if (fileSize > LIMIT_FILE_SIZE_DB) {
                            Log.d(
                                TAG,
                                "skipped ${nodeFs.path} because size was above $LIMIT_FILE_SIZE_DB ($fileSize)"
                            )
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
                            content = nodeFs.readText(),
                        )
                        // straight in: the table was cleared a moment ago, so
                        // there is nothing here that could already be there
                        insertNoteRow(note)
                        //Log.d(TAG, "add note: $note")
                    }

                    is NodeFs.Folder -> {
                        if (nodeFs.isHidden() || nodeFs.isSym()) {
                            return@forEachNodeFs
                        }
                        val noteFolder = NoteFolder.new(
                            relativePath = nodeFs.path.substring(startIndex = rootLength),
                        )
                        //Log.d(TAG, "add noteFolder: $noteFolder")
                        insertNoteFolderRow(noteFolder)
                        progressCb?.invoke(Progress.GeneratingDatabase(noteFolder.relativePath))
                        initRec(nodeFs)
                    }
                }
            }
        }

        initRec(rootFs)
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
