package io.github.leonhardweiler.diffusion.manager

import android.util.Log
import androidx.annotation.StringRes
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import io.github.leonhardweiler.diffusion.data.index.Note
import io.github.leonhardweiler.diffusion.data.index.NoteFolder
import io.github.leonhardweiler.diffusion.data.index.NoteIndex
import io.github.leonhardweiler.diffusion.helper.getParentPath
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

private const val TAG = "StorageManager"

private const val GIT_KEEP = ".gitkeep"

/**
 * The one write path into a repository: its files, its note index and its git
 * repository, in that order and behind one lock. One per [RepoSession], because
 * a repository nobody is looking at still commits, pulls and pushes, and does
 * that to its own files with its own credentials.
 */
class StorageManager(private val repo: RepoSession) {
    private val uiHelper = MyApp.appModule.uiHelper

    private val index: NoteIndex get() = repo.noteIndex

    private val gitManager: GitManager get() = repo.gitManager

    private val repoPath: String get() = repo.path

    private val appScope = MyApp.appModule.appScope

    private val locker = Mutex()

    private val sync = RepoSync(
        repo = repo,
        refreshLocalChanges = ::refreshLocalChanges,
        rebuildIndex = { rebuildIndexWithoutLocker() },
    )

    val syncState: StateFlow<SyncState> = sync.state

    private val _hasLocalChanges: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * Whether there is anything the remote has not been told about. Writing a
     * note does not commit it, so this is the only thing that says the notes
     * here and the notes on the remote have drifted apart.
     */
    val hasLocalChanges: StateFlow<Boolean> = _hasLocalChanges

    private suspend fun refreshLocalChanges() {
        _hasLocalChanges.value = gitManager.isChange().getOrDefault(false)
    }

    /**
     * Asks git rather than assuming, for the one write that can take a change
     * back: a note undone to what it was leaves a working tree that agrees with
     * the repository again. Too expensive after every typing pause — it walks
     * the whole working tree — which is why only that one write asks.
     */
    suspend fun refreshChangeState(): Unit = locker.withLock {
        refreshLocalChanges()
    }

    /** Says a sync is under way, without waiting for anything. */
    fun announceSyncStart() = sync.announceStart()

    /**
     * Commits everything written since the last sync and exchanges it with the
     * remote. The only thing here that reaches the network.
     *
     * @param announceErrors false for the syncs that run on their own when the
     * app is opened and closed.
     */
    suspend fun syncWithRemote(announceErrors: Boolean = true): Result<Unit> {
        // leaving the app writes the open note and syncs in the same scope, and
        // whichever reached the lock first won: the last thing typed then went
        // out one sync late
        lastWrite?.join()

        return locker.withLock { sync.run(announceErrors) }
    }

    /** Every note write goes through here, for the join above. */
    fun startWrite(f: suspend () -> Unit): Job =
        appScope.launch { f() }.also { lastWrite = it }

    @Volatile
    private var lastWrite: Job? = null

    suspend fun rebuildIndex(
        progressCb: ((Progress) -> Unit)? = null
    ): Result<Unit> = locker.withLock {
        refreshLocalChanges()

        rebuildIndexWithoutLocker(progressCb)
    }

    private suspend fun rebuildIndexWithoutLocker(
        progressCb: ((Progress) -> Unit)? = null
    ): Result<Unit> {
        if (!repo.exists) return success(Unit)

        index.rebuild(repoPath, progressCb)

        return success(Unit)
    }

    suspend fun updateNote(new: Note, previous: Note): Result<Unit> = locker.withLock {
        Log.d(TAG, "updateNote: previous = $previous")
        Log.d(TAG, "updateNote: new = $new")

        val renamed = new.relativePath != previous.relativePath

        update {
            val rootPath = repoPath
            if (renamed) {
                val previousFile = previous.toFileFs(rootPath)
                previousFile.delete().orComplain(R.string.error_delete_file, previousFile.path)
            }

            val newFile = new.toFileFs(rootPath)
            newFile.write(new.content).orComplain(R.string.error_write_file)
            newFile.dateBy(new)

            if (renamed) {
                index.moveNote(previous.relativePath, new)
            } else {
                index.putNote(new)
            }

            success(Unit)
        }
    }

    suspend fun createNote(note: Note): Result<Unit> = locker.withLock {
        Log.d(TAG, "createNote: $note")

        update {
            val file = note.toFileFs(repoPath)

            file.create().orComplain(R.string.error_create_file)
            file.write(note.content).orComplain(R.string.error_write_file)
            file.dateBy(note)

            index.putNote(note)

            success(Unit)
        }
    }

    suspend fun deleteNote(relativePath: String): Result<Unit> = locker.withLock {
        Log.d(TAG, "deleteNote: $relativePath")
        update {
            val file = NodeFs.File.fromPath(repoPath, relativePath)
            file.delete().orComplain(R.string.error_delete_file, file.path)

            index.removeNoteAt(relativePath)
            success(Unit)
        }
    }

    suspend fun deleteNotes(relativePaths: List<String>): Result<Unit> = locker.withLock {
        Log.d(TAG, "deleteNotes: ${relativePaths.size}")

        update {
            relativePaths.forEach { relativePath ->

                Log.d(TAG, "deleting $relativePath")
                val file = NodeFs.File.fromPath(repoPath, relativePath)

                file.delete().orComplain(R.string.error_delete_file, file.path)
            }

            index.removeNotesAt(relativePaths)
            success(Unit)
        }
    }

    suspend fun renameNote(note: Note, newRelativePath: String): Result<Unit> = locker.withLock {
        Log.d(TAG, "renameNote: ${note.relativePath} -> $newRelativePath")

        if (note.relativePath == newRelativePath) return@withLock success(Unit)

        val target = NodeFs.File.fromPath(repoPath, newRelativePath)
        if (target.exist()) {
            return@withLock complain(R.string.error_file_already_exist, newRelativePath)
        }

        val parentPath = getParentPath(newRelativePath)
        if (parentPath.isNotEmpty() &&
            !NodeFs.Folder.fromPath(repoPath, parentPath).exist()
        ) {
            return@withLock complain(R.string.error_folder_not_found, parentPath)
        }

        update {
            NodeFs.File.fromPath(repoPath, note.relativePath).moveTo(target.path)
                .onFailure { return@update failure(it) }

            index.moveNote(
                oldRelativePath = note.relativePath,
                note = Note(
                    relativePath = newRelativePath,
                    content = note.content,
                    lastModifiedTimeMillis = note.lastModifiedTimeMillis,
                    id = note.id,
                )
            )

            success(Unit)
        }
    }

    suspend fun createNoteFolder(noteFolder: NoteFolder): Result<Unit> = locker.withLock {
        Log.d(TAG, "createNoteFolder: $noteFolder")

        update {
            val folder = noteFolder.toFolderFs(repoPath)
            folder.create().orComplain(R.string.error_create_folder)

            // git has no empty directory, so without a file in it the folder
            // would never reach another device
            folder.createFile(GIT_KEEP).onFailure {
                Log.e(TAG, "could not create $GIT_KEEP in ${folder.path}: ${it.message}")
            }

            index.putFolder(noteFolder)

            success(Unit)
        }
    }

    suspend fun renameNoteFolder(
        noteFolder: NoteFolder,
        newRelativePath: String
    ): Result<Unit> = locker.withLock {
        Log.d(TAG, "renameNoteFolder: ${noteFolder.relativePath} -> $newRelativePath")

        val oldPath = noteFolder.relativePath
        if (oldPath == newRelativePath) return@withLock success(Unit)

        if (newRelativePath.startsWith("$oldPath/")) {
            return@withLock complain(R.string.error_folder_into_itself)
        }

        val target = NodeFs.Folder.fromPath(repoPath, newRelativePath)
        if (target.exist()) {
            return@withLock complain(R.string.error_folder_already_exist, newRelativePath)
        }

        val parentPath = getParentPath(newRelativePath)
        if (parentPath.isNotEmpty() &&
            !NodeFs.Folder.fromPath(repoPath, parentPath).exist()
        ) {
            return@withLock complain(R.string.error_folder_not_found, parentPath)
        }

        update {
            NodeFs.Folder.fromPath(repoPath, oldPath).moveTo(target.path)
                .onFailure { return@update failure(it) }

            index.moveFolder(noteFolder, newRelativePath)

            success(Unit)
        }
    }

    private fun complain(@StringRes text: Int, vararg args: Any?): Result<Unit> {
        val message = uiHelper.getString(text, *args)
        Log.e(TAG, message)
        uiHelper.makeToast(message)
        return failure(Exception(message))
    }

    suspend fun deleteNoteFolder(noteFolder: NoteFolder): Result<Unit> =
        deleteNoteFolders(listOf(noteFolder))

    suspend fun deleteNoteFolders(noteFolders: List<NoteFolder>): Result<Unit> = locker.withLock {
        Log.d(TAG, "deleteNoteFolders: ${noteFolders.size}")

        update {
            noteFolders.forEach { noteFolder ->
                val folder = noteFolder.toFolderFs(repoPath)
                folder.delete().orComplain(R.string.error_delete_folder)
            }

            index.removeFolders(noteFolders)

            success(Unit)
        }
    }

    private fun NodeFs.File.dateBy(note: Note) =
        setLastModifiedTime(note.lastModifiedTimeMillis).onFailure {
            Log.w(TAG, "could not date $path: ${it.message}")
        }

    private fun Result<*>.orComplain(@StringRes text: Int, vararg args: Any?): Result<*> =
        onFailure { cause ->
            val message = uiHelper.getString(text, *args, cause.message)
            Log.e(TAG, message)
            uiHelper.makeToast(message)
        }

    /**
     * The files first and the index second. Both are written one after the
     * other and the process can die between them, so the only question is which
     * may be behind: a row left over for a file that is gone is noticed when
     * the note is opened, while a file with no row is simply not in the list —
     * and the next start reads the files again either way.
     */
    private suspend fun <T> update(
        f: suspend () -> Result<T>
    ): Result<T> {
        return f().onSuccess {
            _hasLocalChanges.value = true
        }.onFailure { err ->
            err.message?.let { Log.e(TAG, it) }
        }
    }
}
