package io.github.wiiznokes.gitnote.manager

import android.util.Log
import androidx.room.withTransaction
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.data.room.NoteFolder
import io.github.wiiznokes.gitnote.data.room.RepoDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

private const val TAG = "StorageManager"

/**
 * Placed in a newly created folder so that git has something to track.
 */
private const val GIT_KEEP = ".gitkeep"

sealed interface SyncState {

    /** Nothing has been synced in this session yet. */
    data object Idle : SyncState

    data object Ok : SyncState

    data class Error(val msg: String?) : SyncState

    data object Pull : SyncState

    data object Push : SyncState

    fun isLoading(): Boolean {
        return this is Pull || this is Push
    }

    fun message(): String {
        return when (this) {
            is Error -> this.msg ?: "Unknow Error"
            Idle -> "Sync with the remote"
            Ok -> "Sync done"
            Pull -> "Pulling"
            Push -> "Pushing"
        }
    }
}

sealed class Progress {
    data object Timestamps : Progress()

    data class GeneratingDatabase(val path: String) : Progress()
}

class StorageManager {


    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val db: RepoDatabase = MyApp.appModule.repoDatabase

    private val uiHelper = MyApp.appModule.uiHelper

    private val dao = this.db.repoDatabaseDao

    private val gitManager: GitManager = MyApp.appModule.gitManager

    private val locker = Mutex()

    private val _syncState: MutableStateFlow<SyncState> = MutableStateFlow(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState


    /**
     * Commits everything that has been written since the last sync and exchanges
     * it with the remote. This is the only thing that reaches the network, and
     * it only ever runs because the user asked for it.
     */
    suspend fun syncWithRemote(): Result<Unit> = locker.withLock {
        Log.d(TAG, "syncWithRemote")

        gitManager.commitAll(
            prefs.gitAuthor(),
            "commit from gitnote"
        ).onFailure { err ->
            err.message?.let { Log.e(TAG, it) }
            _syncState.emit(SyncState.Error(err.message))
            return@withLock failure(err)
        }

        return syncWithRemoteWithoutLocker()
    }

    /**
     * Update the database with the last files
     * available of the fs, and update with the
     * head commit of the repo.
     *
     * /!\ Warning there can be pending file added to the database
     * that are not committed to the repo.
     * The caller must ensure that all files has been committed
     * to keep the database in sync with the remote repo
     */
    private suspend fun updateDatabaseWithoutLocker(
        force: Boolean = false,
        progressCb: ((Progress) -> Unit)? = null
    ): Result<Unit> {

        val fsCommit = gitManager.lastCommit()
        val databaseCommit = prefs.databaseCommit.get()

        Log.d(TAG, "fsCommit: $fsCommit, databaseCommit: $databaseCommit")
        if (!force && fsCommit == databaseCommit) {
            Log.d(TAG, "last commit is already loaded in data base")
            return success(Unit)
        }

        val repoPath = prefs.repoPath()
        Log.d(TAG, "repoPath = $repoPath")

        progressCb?.invoke(Progress.Timestamps)
        val timestamps = gitManager.getTimestamps().getOrThrow()

        db.withTransaction {
            dao.clearAndInit(repoPath, timestamps, progressCb)
        }
        prefs.databaseCommit.update(fsCommit)

        return success(Unit)
    }

    /**
     * See the documentation of [updateDatabaseWithoutLocker]
     */
    suspend fun updateDatabase(
        force: Boolean = false,
        progressCb: ((Progress) -> Unit)? = null
    ): Result<Unit> = locker.withLock {
        updateDatabaseWithoutLocker(force, progressCb)
    }

    /**
     * Best effort
     */
    suspend fun updateNote(new: Note, previous: Note): Result<Unit> = locker.withLock {
        Log.d(TAG, "updateNote: previous = $previous")
        Log.d(TAG, "updateNote: new = $new")

        val renamed = new.relativePath != previous.relativePath

        update {
            // relativePath is the primary key, so as long as the note keeps its
            // name the upsert rewrites the row in place. Only a rename needs the
            // old row and the old file taken away first — and that is rare,
            // while this runs on every typing pause.
            if (renamed) {
                dao.removeNote(previous)
            }
            dao.insertNote(new)

            val rootPath = prefs.repoPath()

            if (renamed) {
                val previousFile = previous.toFileFs(rootPath)
                previousFile.delete().onFailure {
                    val message =
                        uiHelper.getString(R.string.error_delete_file, previousFile.path, it.message)
                    Log.e(TAG, message)
                    uiHelper.makeToast(message)
                }
            }

            // write creates the file if it is not there and truncates it if it
            // is, so a shortened note does not keep its old tail
            val newFile = new.toFileFs(rootPath)
            newFile.write(new.content).onFailure {
                val message = uiHelper.getString(R.string.error_write_file, it.message)
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }

            success(Unit)
        }

    }

    /**
     * Best effort
     */
    suspend fun createNote(note: Note): Result<Unit> = locker.withLock {
        Log.d(TAG, "createNote: $note")

        update {
            dao.insertNote(note)

            val file = note.toFileFs(prefs.repoPath())

            file.create().onFailure {
                val message = uiHelper.getString(R.string.error_create_file, it.message)
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }
            file.write(note.content).onFailure {
                val message = uiHelper.getString(R.string.error_write_file, it.message)
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }

            success(Unit)
        }
    }


    suspend fun deleteNote(note: Note): Result<Unit> = locker.withLock {

        Log.d(TAG, "deleteNote: $note")
        update {
            dao.removeNote(note)

            val file = note.toFileFs(prefs.repoPath())
            file.delete().onFailure {
                val message = uiHelper.getString(R.string.error_delete_file, file.path, it.message)
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }
            success(Unit)
        }
    }

    suspend fun deleteNotes(notes: List<Note>): Result<Unit> = locker.withLock {
        Log.d(TAG, "deleteNotes: ${notes.size}")

        update {
            // optimization because we only see the db state on screen
            notes.forEach { note ->
                dao.removeNote(note)
            }

            val repoPath = prefs.repoPath()
            notes.forEach { note ->

                Log.d(TAG, "deleting $note")
                val file = note.toFileFs(repoPath)

                file.delete().onFailure {
                    val message =
                        uiHelper.getString(R.string.error_delete_file, file.path, it.message)
                    Log.e(TAG, message)
                    uiHelper.makeToast(message)
                }
            }
            success(Unit)
        }
    }

    suspend fun createNoteFolder(noteFolder: NoteFolder): Result<Unit> = locker.withLock {
        Log.d(TAG, "createNoteFolder: $noteFolder")

        update {
            dao.insertNoteFolder(noteFolder)

            val folder = noteFolder.toFolderFs(prefs.repoPath())
            folder.create().onFailure {
                val message = uiHelper.getString(R.string.error_create_folder, it.message)
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }

            // Git has no concept of an empty directory, so without a file in it the
            // folder would produce no commit content and never reach another device.
            folder.createFile(GIT_KEEP).onFailure {
                Log.e(TAG, "could not create $GIT_KEEP in ${folder.path}: ${it.message}")
            }

            success(Unit)
        }
    }

    suspend fun deleteNoteFolder(noteFolder: NoteFolder): Result<Unit> = locker.withLock {
        Log.d(TAG, "deleteNoteFolder: $noteFolder")

        update {
            dao.deleteNoteFolder(noteFolder)

            val folder = noteFolder.toFolderFs(prefs.repoPath())
            folder.delete().onFailure {
                val msg = uiHelper.getString(R.string.error_delete_folder, it.message)
                Log.e(TAG, msg)
                uiHelper.makeToast(msg)
            }

            success(Unit)
        }
    }

    suspend fun closeRepo() = locker.withLock {
        prefs.closeRepo()
        gitManager.closeRepo()
        dao.clearDatabase()
    }


    /**
     * Applies a change to the files and to the database. Nothing is committed
     * here: the working tree carries the change until the user asks for a sync,
     * which is what [syncWithRemote] then commits and pushes in one go.
     */
    private suspend fun <T> update(
        f: suspend () -> Result<T>
    ): Result<T> {

        updateDatabaseWithoutLocker().onFailure { err ->
            err.message?.let { Log.e(TAG, it) }
            _syncState.emit(SyncState.Error(err.message))
            return failure(err)
        }

        return f().onFailure { err ->
            err.message?.let { Log.e(TAG, it) }
        }
    }

    private suspend fun syncWithRemoteWithoutLocker(): Result<Unit> {
        val hasRemote = prefs.remoteUrl.get().isNotEmpty()
        val cred = prefs.cred()
        var isError = false

        if (hasRemote) {
            _syncState.emit(SyncState.Pull)
            gitManager.pull(cred, prefs.gitAuthor()).onFailure { err ->
                isError = true
                err.message?.let { Log.e(TAG, it) }
                _syncState.emit(SyncState.Error(err.message))
            }
        }

        // Also runs without a remote: the local commits still have to reach the database.
        updateDatabaseWithoutLocker().onFailure { err ->
            err.message?.let { Log.e(TAG, it) }
            _syncState.emit(SyncState.Error(err.message))
            return failure(err)
        }

        if (hasRemote) {
            _syncState.emit(SyncState.Push)
            gitManager.push(cred).onFailure { err ->
                isError = true
                err.message?.let { Log.e(TAG, it) }
                _syncState.emit(SyncState.Error(err.message))
            }

            if (!isError)
                _syncState.emit(SyncState.Ok)
        }

        return success(Unit)
    }

}
