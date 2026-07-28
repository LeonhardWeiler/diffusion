package io.github.wiiznokes.gitnote.manager

import android.util.Log
import androidx.room.withTransaction
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.room.Note
import io.github.wiiznokes.gitnote.data.room.NoteFolder
import io.github.wiiznokes.gitnote.data.room.RepoDatabase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

private const val TAG = "StorageManager"

/**
 * How long a local change waits for the next one before the repo is synced with
 * the remote. Ticking a row of checkboxes should cost one sync, not one per tick.
 */
private const val REMOTE_SYNC_DEBOUNCE_MS = 3_000L

sealed interface SyncState {

    data class Ok(val isConsumed: Boolean) : SyncState

    data class Error(val msg: String?) : SyncState

    object Pull : SyncState

    object Push : SyncState

    fun isLoading(): Boolean {
        return this is Pull || this is Push
    }

    fun message(): String {
        return when (this) {
            is Error -> this.msg ?: "Unknow Error"
            is Ok -> "Sync done"
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

    private val appScope = MyApp.appModule.appScope

    /**
     * The sync that is waiting for the debounce to run out, if any.
     */
    private var pendingRemoteSync: Job? = null

    private val _syncState: MutableStateFlow<SyncState> = MutableStateFlow(SyncState.Ok(true))
    val syncState: StateFlow<SyncState> = _syncState


    /**
     * Syncs with the remote right away instead of waiting for the debounce, and
     * therefore also covers whatever [scheduleRemoteSync] still had queued.
     * Used on start up and for pull to refresh.
     */
    suspend fun updateDatabaseAndRepo(): Result<Unit> = locker.withLock {
        Log.d(TAG, "updateDatabaseAndRepo")

        pendingRemoteSync?.cancel()

        gitManager.commitAll(
            prefs.gitAuthor(),
            "commit from gitnote to update the repo of the app"
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

        update(
            commitMessage = "gitnote modified ${previous.relativePath}"
        ) {
            dao.removeNote(previous)
            dao.insertNote(new)

            val rootPath = prefs.repoPath()
            val previousFile = previous.toFileFs(rootPath)

            previousFile.delete().onFailure {
                val message =
                    uiHelper.getString(R.string.error_delete_file, previousFile.path, it.message)
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }

            val newFile = new.toFileFs(rootPath)
            newFile.create().onFailure {
                val message = uiHelper.getString(R.string.error_create_file, it.message)
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }

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

        update(
            commitMessage = "gitnote created ${note.relativePath}"
        ) {
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
        update(
            commitMessage = "gitnote deleted ${note.relativePath}"
        ) {
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

        update(
            commitMessage = "gitnote deleted ${notes.size} notes"
        ) {
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

        update(
            commitMessage = "gitnote created folder ${noteFolder.relativePath}"
        ) {
            dao.insertNoteFolder(noteFolder)

            val folder = noteFolder.toFolderFs(prefs.repoPath())
            folder.create().onFailure {
                val message = uiHelper.getString(R.string.error_create_folder, it.message)
                Log.e(TAG, message)
                uiHelper.makeToast(message)
            }

            success(Unit)
        }
    }

    suspend fun deleteNoteFolder(noteFolder: NoteFolder): Result<Unit> = locker.withLock {
        Log.d(TAG, "deleteNoteFolder: $noteFolder")

        update(
            commitMessage = "gitnote deleted folder ${noteFolder.relativePath}"
        ) {
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
        pendingRemoteSync?.cancel()
        prefs.closeRepo()
        gitManager.closeRepo()
        dao.clearDatabase()
    }


    /**
     * Applies a change and commits it locally. Committing is cheap and stays
     * on the caller's path, so the files on disk and the repo never drift apart.
     *
     * Talking to the remote is not part of this: it is handed to
     * [scheduleRemoteSync], which bundles the changes that follow shortly after.
     * Otherwise every saved note, every deleted file and every ticked checkbox
     * would be a full network roundtrip while the mutex is held.
     */
    private suspend fun <T> update(
        commitMessage: String,
        f: suspend () -> Result<T>
    ): Result<T> {

        val author = prefs.gitAuthor()

        gitManager.commitAll(
            author,
            "commit from gitnote, before doing a change"
        ).onFailure { err ->
            err.message?.let { Log.e(TAG, it) }
            _syncState.emit(SyncState.Error(err.message))
            return failure(err)
        }

        updateDatabaseWithoutLocker().onFailure { err ->
            err.message?.let { Log.e(TAG, it) }
            _syncState.emit(SyncState.Error(err.message))
            return failure(err)
        }

        val payload = f().fold(
            onFailure = { err ->
                err.message?.let { Log.e(TAG, it) }
                return failure(err)
            },
            onSuccess = {
                it
            }
        )

        gitManager.commitAll(author, commitMessage).onFailure { err ->
            err.message?.let { Log.e(TAG, it) }
            _syncState.emit(SyncState.Error(err.message))
            return failure(err)
        }

        prefs.databaseCommit.update(gitManager.lastCommit())

        scheduleRemoteSync()

        return success(payload)
    }

    /**
     * Queues a sync with the remote, replacing one that has not run yet. The
     * caller may hold [locker]: this only starts the timer, the sync itself
     * takes the lock when it runs.
     *
     * A change that is still queued when the process dies is not lost, it is
     * only not pushed yet — [updateDatabaseAndRepo] on the next start picks it up.
     */
    private fun scheduleRemoteSync() {
        pendingRemoteSync?.cancel()
        pendingRemoteSync = appScope.launch {
            delay(REMOTE_SYNC_DEBOUNCE_MS)
            syncWithRemote()
        }
    }

    private suspend fun syncWithRemote(): Result<Unit> = locker.withLock {
        syncWithRemoteWithoutLocker()
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
                _syncState.emit(SyncState.Ok(false))
        }

        return success(Unit)
    }

    suspend fun consumeOkSyncState() {
        _syncState.emit(SyncState.Ok(true))
    }
}
