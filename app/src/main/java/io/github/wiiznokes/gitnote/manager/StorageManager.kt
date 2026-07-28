package io.github.wiiznokes.gitnote.manager

import android.util.Log
import androidx.annotation.StringRes
import androidx.room.withTransaction
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.platform.NodeFs
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

/**
 * Stands in [AppPreferences.databaseCommit] for a repository that has no commit
 * yet. It has to be something other than the empty string, which is what the
 * preference holds before the database has ever been built: a freshly cloned
 * repository that reports no HEAD would otherwise look like one whose commit is
 * already loaded, and the note list would stay empty until a reload.
 */
private const val NO_COMMIT = "none"

class StorageManager {


    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val db: RepoDatabase = MyApp.appModule.repoDatabase

    private val uiHelper = MyApp.appModule.uiHelper

    private val dao = this.db.repoDatabaseDao

    private val gitManager: GitManager = MyApp.appModule.gitManager

    private val locker = Mutex()

    private val _syncState: MutableStateFlow<SyncState> = MutableStateFlow(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _hasLocalChanges: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * Whether there is anything the remote has not been told about. Writing a
     * note does not commit it, so this is the only thing that says the notes on
     * this device and the notes on the remote have drifted apart.
     */
    val hasLocalChanges: StateFlow<Boolean> = _hasLocalChanges

    /**
     * Asks git. Only worth it when something other than this app may have
     * written to the repository — a write of our own is known to be a change
     * without asking, and asking means walking the whole working tree.
     */
    private suspend fun refreshLocalChanges() {
        _hasLocalChanges.value = gitManager.isChange().getOrDefault(false)
    }


    /**
     * Whether a failure of the sync that is running should say so by itself.
     * Read only under [locker], which is also the only thing that runs a sync.
     */
    private var announceSyncErrors = true

    private suspend fun failSync(message: String?) {
        message?.let { Log.e(TAG, it) }
        _syncState.emit(SyncState.Error(message, announce = announceSyncErrors))
    }

    /**
     * Commits everything that has been written since the last sync and exchanges
     * it with the remote. The only thing that reaches the network.
     *
     * @param announceErrors see [SyncState.Error]. False for the syncs that run
     * on their own when the app is opened and closed.
     */
    suspend fun syncWithRemote(announceErrors: Boolean = true): Result<Unit> = locker.withLock {
        Log.d(TAG, "syncWithRemote")

        announceSyncErrors = announceErrors

        gitManager.commitAll(
            prefs.gitAuthor(),
            "commit from gitnote"
        ).onFailure { err ->
            failSync(err.message)
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

        // A repository that cannot be read is not one that has nothing new: the
        // failure has to travel, or the rebuild is skipped for a reason nobody
        // ever sees.
        val fsCommit = gitManager.lastCommit().getOrElse { return failure(it) } ?: NO_COMMIT
        val databaseCommit = prefs.databaseCommit.get()

        Log.d(TAG, "fsCommit: $fsCommit, databaseCommit: $databaseCommit")
        if (!force && fsCommit == databaseCommit) {
            Log.d(TAG, "last commit is already loaded in data base")
            return success(Unit)
        }

        val repoPath = prefs.repoPath()
        Log.d(TAG, "repoPath = $repoPath")

        db.withTransaction {
            dao.clearAndInit(repoPath, progressCb)
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
        // The three places this is called from — start up, the reload and the
        // end of a setup — are exactly the ones where the repository may have
        // been written to by something that is not this app.
        refreshLocalChanges()

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
                previousFile.delete().orComplain(R.string.error_delete_file, previousFile.path)
            }

            // write creates the file if it is not there and truncates it if it
            // is, so a shortened note does not keep its old tail
            val newFile = new.toFileFs(rootPath)
            newFile.write(new.content).orComplain(R.string.error_write_file)

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

            file.create().orComplain(R.string.error_create_file)
            file.write(note.content).orComplain(R.string.error_write_file)

            success(Unit)
        }
    }


    /**
     * Takes the path rather than the note, because deleting needs nothing else
     * and the list only holds a [io.github.wiiznokes.gitnote.ui.model.NoteHeader].
     */
    suspend fun deleteNote(relativePath: String): Result<Unit> = locker.withLock {

        Log.d(TAG, "deleteNote: $relativePath")
        update {
            dao.removeNoteAt(relativePath)

            val file = NodeFs.File.fromPath(prefs.repoPath(), relativePath)
            file.delete().orComplain(R.string.error_delete_file, file.path)
            success(Unit)
        }
    }

    suspend fun deleteNotes(relativePaths: List<String>): Result<Unit> = locker.withLock {
        Log.d(TAG, "deleteNotes: ${relativePaths.size}")

        update {
            // the rows go first, all of them: the screen shows the database, so
            // the whole selection disappears at once instead of note by note
            relativePaths.forEach { relativePath ->
                dao.removeNoteAt(relativePath)
            }

            val repoPath = prefs.repoPath()
            relativePaths.forEach { relativePath ->

                Log.d(TAG, "deleting $relativePath")
                val file = NodeFs.File.fromPath(repoPath, relativePath)

                file.delete().orComplain(R.string.error_delete_file, file.path)
            }
            success(Unit)
        }
    }

    suspend fun createNoteFolder(noteFolder: NoteFolder): Result<Unit> = locker.withLock {
        Log.d(TAG, "createNoteFolder: $noteFolder")

        update {
            dao.insertNoteFolder(noteFolder)

            val folder = noteFolder.toFolderFs(prefs.repoPath())
            folder.create().orComplain(R.string.error_create_folder)

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
            folder.delete().orComplain(R.string.error_delete_folder)

            success(Unit)
        }
    }

    suspend fun closeRepo() = locker.withLock {
        prefs.closeRepo()
        gitManager.closeRepo()
        dao.clearDatabase()
        // the next repository starts without the last one's sync result, which
        // otherwise greets it as an error it never had
        _syncState.emit(SyncState.Idle)
    }


    /**
     * Says out loud that a step of a best effort change did not work. The reason
     * is always the last thing the message mentions, so [args] carries whatever
     * comes before it.
     */
    private fun Result<*>.orComplain(@StringRes text: Int, vararg args: Any?): Result<*> =
        onFailure { cause ->
            val message = uiHelper.getString(text, *args, cause.message)
            Log.e(TAG, message)
            uiHelper.makeToast(message)
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
            failSync(err.message)
            return failure(err)
        }

        return f().onSuccess {
            // a note was written, renamed or deleted, and nothing commits by
            // itself — so the repository is behind the working tree from here
            // until the next sync
            _hasLocalChanges.value = true
        }.onFailure { err ->
            err.message?.let { Log.e(TAG, it) }
        }
    }

    private suspend fun syncWithRemoteWithoutLocker(): Result<Unit> {
        val hasRemote = prefs.remoteUrl.get().isNotEmpty()
        val cred = prefs.cred()
        var isError = false
        var conflicted = false

        if (hasRemote) {
            _syncState.emit(SyncState.Pull)
            gitManager.pull(cred, prefs.gitAuthor()).onFailure { err ->
                isError = true
                conflicted = err is GitException && err.type == GitExceptionType.MergeConflict
                failSync(err.message)
            }
        }

        // Also runs without a remote: the local commits still have to reach the
        // database. A conflict is written into the notes without HEAD moving,
        // so it takes a rebuild that does not ask whether the commit changed —
        // otherwise the conflict would be in the files and nowhere on screen.
        updateDatabaseWithoutLocker(force = conflicted).onFailure { err ->
            failSync(err.message)
            return failure(err)
        }

        // Not after a pull that did not go through: the push would be refused
        // for being behind, and its failure would be the last thing said —
        // replacing the one explanation that tells the user what to do.
        if (hasRemote && !isError) {
            _syncState.emit(SyncState.Push)
            gitManager.push(cred).onFailure { err ->
                isError = true
                failSync(err.message)
            }

            if (!isError)
                _syncState.emit(SyncState.Ok)
        }

        // whatever the sync managed, the working tree is what it is: a commit
        // that went through leaves nothing behind, one that did not leaves it
        refreshLocalChanges()

        return success(Unit)
    }

}
