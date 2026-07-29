package io.github.leonhardweiler.diffusion.manager

import android.util.Log
import androidx.annotation.StringRes
import androidx.room.withTransaction
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.data.AppPreferences
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import io.github.leonhardweiler.diffusion.data.room.Note
import io.github.leonhardweiler.diffusion.data.room.NoteFolder
import io.github.leonhardweiler.diffusion.data.room.RepoDatabase
import io.github.leonhardweiler.diffusion.helper.getParentPath
import io.github.leonhardweiler.diffusion.helper.movedUnder
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

/**
 * How long a pull waits before trying a second time, when the first found no
 * network. Long enough for a resolver that is being set up to be ready, short
 * enough that somebody watching the button does not give up on it.
 */
private const val RETRY_AFTER_NETWORK_FAILURE_MS = 1_500L

class StorageManager {


    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val db: RepoDatabase = MyApp.appModule.repoDatabase

    private val uiHelper = MyApp.appModule.uiHelper

    private val networkMonitor = MyApp.appModule.networkMonitor

    private val dao = this.db.repoDatabaseDao

    private val gitManager: GitManager = MyApp.appModule.gitManager

    /** Writes must not be cancelled by the screen that started them going away. */
    private val appScope = MyApp.appModule.appScope

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
     * Asks git whether anything is still uncommitted, rather than assuming it
     * is because something was written.
     *
     * For the one write that can take a change back: a note undone to what it
     * was leaves a working tree that agrees with the repository again, and the
     * dot on the cloud button has nothing left to announce. Too expensive to do
     * after every typing pause — it walks the whole working tree — which is why
     * only that one write asks.
     */
    suspend fun refreshChangeState(): Unit = locker.withLock {
        refreshLocalChanges()
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

    /** Whether this went wrong because the far end was never reached. */
    private fun Result<*>.isNetworkFailure(): Boolean =
        (exceptionOrNull() as? GitException)?.type == GitExceptionType.NetworkUnreachable

    /**
     * What a failed pull or push leaves on the button.
     *
     * Everything but one thing leaves an error there. The exception is a sync
     * that nobody asked for failing because the network was not there: the app
     * syncs when it is opened and when it is left, which is exactly when a
     * phone comes back from sleep and wifi has not reassociated — and the wait
     * beforehand only covers the case where the system knows it is offline, not
     * the second after it says it is online while the resolver still is not.
     *
     * The dot on the button already says the notes have not gone out. An alert
     * cloud on top of it, for something the next sync will do without being
     * asked, is a warning about nothing.
     */
    private suspend fun reportSyncFailure(err: Throwable) {
        val transient = !announceSyncErrors &&
                err is GitException &&
                err.type == GitExceptionType.NetworkUnreachable

        if (transient) {
            Log.d(TAG, "sync: no network, and nobody asked: ${err.message}")
            // the button is mid-pull as far as it knows, and nothing after this
            // will take it out of that
            _syncState.emit(SyncState.Idle)
            return
        }

        failSync(err.message)
    }

    /**
     * Commits everything that has been written since the last sync and exchanges
     * it with the remote. The only thing that reaches the network.
     *
     * @param announceErrors see [SyncState.Error]. False for the syncs that run
     * on their own when the app is opened and closed.
     */
    suspend fun syncWithRemote(announceErrors: Boolean = true): Result<Unit> {
        // Leaving the app tells the editor to write what it holds and starts a
        // sync, both in the same scope on the same dispatcher. Whoever reached
        // the lock first used to win, and when that was the sync, the last thing
        // typed was not in the commit — it was not lost, it went out one sync
        // later, which is exactly one sync too late.
        lastWrite?.join()

        return syncWithRemoteLocked(announceErrors)
    }

    /**
     * Says that a sync is under way, without waiting for anything.
     *
     * For the one caller that is a finger on a button: everything before the
     * first pull — the editor's last write, the lock, the commit — takes long
     * enough on a large repository to look like the tap did nothing. Not
     * suspending on purpose, so the tap handler itself can set it and the
     * button has changed by the time the finger is lifted.
     *
     * The syncs that run on their own do not call this: they are not being
     * watched, and a cloud pulsing for eight seconds because there is no
     * network is worse than one that says nothing.
     */
    fun announceSyncStart() {
        _syncState.value = SyncState.Starting
    }

    /**
     * A note write in the app's scope, which [syncWithRemote] lets finish before
     * it commits. Every write of a note goes through here for that reason.
     */
    fun startWrite(f: suspend () -> Unit): Job =
        appScope.launch { f() }.also { lastWrite = it }

    @Volatile
    private var lastWrite: Job? = null

    /**
     * What HEAD said the last time it was asked, or null when it has to be
     * asked again.
     *
     * Every mutation begins by comparing HEAD against the commit the database
     * was built from, and that comparison used to be a JNI transition and a
     * git_revparse behind two mutexes — the second of which belongs to the sync,
     * so a note being saved could end up waiting on a pull over the network.
     * Answered with "unchanged" every time, because HEAD does not move while
     * notes are written: that is the whole reason the index can stay in step
     * incrementally.
     *
     * So it is asked once and remembered, and forgotten again by exactly the
     * things that can move HEAD — committing, pulling, and whatever happened
     * outside this class before updateDatabase was called. Read and written
     * only under [locker], or the cheap answer would be a race instead.
     */
    private var knownFsCommit: String? = null

    private suspend fun syncWithRemoteLocked(announceErrors: Boolean): Result<Unit> = locker.withLock {
        Log.d(TAG, "syncWithRemote")

        // There is nothing to sync before a repository has been opened, and
        // saying so is worse than saying nothing: the app is stopped several
        // times on the way through the setup — the folder picker and the
        // browser both do it — and the sync that runs then failed with
        // "RepoNotInit", which the cloud button was still showing when the
        // freshly cloned notes arrived.
        if (!gitManager.isRepoInitialized) {
            Log.d(TAG, "syncWithRemote: no repository open")
            // whoever announced a sync is owed the button back
            _syncState.emit(SyncState.Idle)
            return@withLock success(Unit)
        }

        announceSyncErrors = announceErrors

        // a commit is one of the two things that move HEAD
        knownFsCommit = null

        // Only a fallback: the message a commit really carries is the notes it
        // is about, and only the rust side, which stages them, knows those.
        gitManager.commitAll(
            prefs.gitAuthor(),
            fallbackMessage = "Sync from Diffusion"
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
        val fsCommit = knownFsCommit
            ?: gitManager.lastCommit().getOrElse { return failure(it) } ?: NO_COMMIT

        knownFsCommit = fsCommit

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
        knownFsCommit = null
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
            val rootPath = prefs.repoPath()

            if (renamed) {
                val previousFile = previous.toFileFs(rootPath)
                previousFile.delete().orComplain(R.string.error_delete_file, previousFile.path)
            }

            // write creates the file if it is not there and truncates it if it
            // is, so a shortened note does not keep its old tail
            val newFile = new.toFileFs(rootPath)
            newFile.write(new.content).orComplain(R.string.error_write_file)
            newFile.dateBy(new)

            // relativePath is the primary key, so as long as the note keeps its
            // name the upsert rewrites the row in place. Only a rename needs the
            // old row taken away first — and that is rare, while this runs on
            // every typing pause.
            if (renamed) {
                dao.removeNote(previous)
            }
            dao.insertNote(new)

            success(Unit)
        }

    }

    /**
     * Best effort
     */
    suspend fun createNote(note: Note): Result<Unit> = locker.withLock {
        Log.d(TAG, "createNote: $note")

        update {
            val file = note.toFileFs(prefs.repoPath())

            file.create().orComplain(R.string.error_create_file)
            file.write(note.content).orComplain(R.string.error_write_file)
            file.dateBy(note)

            dao.insertNote(note)

            success(Unit)
        }
    }


    /**
     * Takes the path rather than the note, because deleting needs nothing else
     * and the list only holds a [io.github.leonhardweiler.diffusion.ui.model.NoteHeader].
     */
    suspend fun deleteNote(relativePath: String): Result<Unit> = locker.withLock {

        Log.d(TAG, "deleteNote: $relativePath")
        update {
            val file = NodeFs.File.fromPath(prefs.repoPath(), relativePath)
            file.delete().orComplain(R.string.error_delete_file, file.path)

            dao.removeNoteAt(relativePath)
            success(Unit)
        }
    }

    suspend fun deleteNotes(relativePaths: List<String>): Result<Unit> = locker.withLock {
        Log.d(TAG, "deleteNotes: ${relativePaths.size}")

        update {
            val repoPath = prefs.repoPath()
            relativePaths.forEach { relativePath ->

                Log.d(TAG, "deleting $relativePath")
                val file = NodeFs.File.fromPath(repoPath, relativePath)

                file.delete().orComplain(R.string.error_delete_file, file.path)
            }

            // then the rows, all of them together: the screen shows the
            // database, so the whole selection disappears at once rather than
            // note by note
            relativePaths.forEach { relativePath ->
                dao.removeNoteAt(relativePath)
            }
            success(Unit)
        }
    }

    suspend fun createNoteFolder(noteFolder: NoteFolder): Result<Unit> = locker.withLock {
        Log.d(TAG, "createNoteFolder: $noteFolder")

        update {
            val folder = noteFolder.toFolderFs(prefs.repoPath())
            folder.create().orComplain(R.string.error_create_folder)

            // Git has no concept of an empty directory, so without a file in it the
            // folder would produce no commit content and never reach another device.
            folder.createFile(GIT_KEEP).onFailure {
                Log.e(TAG, "could not create $GIT_KEEP in ${folder.path}: ${it.message}")
            }

            dao.insertNoteFolder(noteFolder)

            success(Unit)
        }
    }

    /**
     * Moves a folder, with everything under it, to where [newRelativePath] says.
     *
     * Renaming and moving are one act, as they are for a note: the new path can
     * name a folder beside this one or one further up. On disk it is a single
     * rename of the directory, so the subfolders and the notes come along
     * without being touched.
     *
     * The rows are rewritten rather than rebuilt from the disk afterwards. A
     * rebuild would read every file in the repository for a change that touched
     * one subtree, and the rows already hold everything the new ones need — the
     * ids among it, so the list keeps its place and an open undo history stays
     * with its note.
     */
    suspend fun renameNoteFolder(
        noteFolder: NoteFolder,
        newRelativePath: String
    ): Result<Unit> = locker.withLock {
        Log.d(TAG, "renameNoteFolder: ${noteFolder.relativePath} -> $newRelativePath")

        val oldPath = noteFolder.relativePath
        if (oldPath == newRelativePath) return@withLock success(Unit)

        // Moving a folder into itself leaves the whole subtree with nowhere to
        // be. The filesystem refuses it too, but only after the rows are gone.
        if (newRelativePath.startsWith("$oldPath/")) {
            return@withLock complain(R.string.error_folder_into_itself)
        }

        val repoPath = prefs.repoPath()

        val target = NodeFs.Folder.fromPath(repoPath, newRelativePath)
        if (target.exist()) {
            return@withLock complain(R.string.error_folder_already_exist)
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

            // read before anything is deleted: these rows are the only place the
            // ids and the dates still are
            val notes = dao.notesIn("$oldPath/")
            val folders = dao.foldersUnder(oldPath)

            dao.internalDeleteNotesIn("$oldPath/")
            dao.internalDeleteFoldersIn("$oldPath/")
            dao.internalDeleteNoteFolder(noteFolder)

            folders.forEach { folder ->
                dao.insertNoteFolderRow(
                    folder.copy(relativePath = movedUnder(folder.relativePath, oldPath, newRelativePath))
                )
            }

            notes.forEach { note ->
                // through the constructor, so parentPath and fileName are
                // derived again instead of keeping the ones they had
                dao.insertNoteRow(
                    Note(
                        relativePath = movedUnder(note.relativePath, oldPath, newRelativePath),
                        content = note.content,
                        lastModifiedTimeMillis = note.lastModifiedTimeMillis,
                        id = note.id,
                    )
                )
            }

            success(Unit)
        }
    }

    /** Says why nothing happened, and answers as a failure. */
    private fun complain(@StringRes text: Int, vararg args: Any?): Result<Unit> {
        val message = uiHelper.getString(text, *args)
        Log.e(TAG, message)
        uiHelper.makeToast(message)
        return failure(Exception(message))
    }

    suspend fun deleteNoteFolder(noteFolder: NoteFolder): Result<Unit> =
        deleteNoteFolders(listOf(noteFolder))

    /**
     * Takes a whole selection at once rather than one folder per call: each
     * call takes the lock and rebuilds what the database owes the files, and
     * doing that per row made deleting five folders five times the work.
     */
    suspend fun deleteNoteFolders(noteFolders: List<NoteFolder>): Result<Unit> = locker.withLock {
        Log.d(TAG, "deleteNoteFolders: ${noteFolders.size}")

        update {
            val repoPath = prefs.repoPath()
            noteFolders.forEach { noteFolder ->
                val folder = noteFolder.toFolderFs(repoPath)
                folder.delete().orComplain(R.string.error_delete_folder)
            }

            // then the rows, all of them together, so that the list loses the
            // whole selection at once rather than folder by folder
            noteFolders.forEach { dao.deleteNoteFolder(it) }

            success(Unit)
        }
    }

    suspend fun closeRepo() = locker.withLock {
        knownFsCommit = null
        prefs.closeRepo()
        gitManager.closeRepo()
        dao.clearDatabase()
        // the next repository starts without the last one's sync result, which
        // otherwise greets it as an error it never had
        _syncState.emit(SyncState.Idle)
    }


    /**
     * Gives the file the date its note carries, which is usually the moment it
     * was written and the write has already said so. It is not, for a note that
     * was undone back to what it was: that one keeps the date it had, and the
     * row and the file have to agree about it — the list reads the row now and
     * the file after the next rebuild.
     *
     * Quietly: a note whose date could not be written still reads fine.
     */
    private fun NodeFs.File.dateBy(note: Note) =
        setLastModifiedTime(note.lastModifiedTimeMillis).onFailure {
            Log.w(TAG, "could not date $path: ${it.message}")
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
     * Applies a change to the files first and to the database second. Nothing
     * is committed here: the working tree carries the change until the user
     * asks for a sync, which is what [syncWithRemote] then commits and pushes
     * in one go.
     *
     * That order is the whole of it. Both are written one after the other and
     * the process can die in between, and then one of them is behind — so the
     * question is which. The files are the truth: a row left over for a file
     * that is gone is noticed the moment the note is opened ("this note is no
     * longer there"), and the next rebuild clears it. The other way round, a
     * file with no row is a note that is simply not in the list, and since
     * databaseCommit still agrees with HEAD nothing rebuilds to find it.
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
        var hasRemote = prefs.remoteUrl.get().isNotEmpty()
        val cred = prefs.cred()
        var isError = false
        var conflicted = false

        // The two syncs nobody asks for run when the app is opened and when it
        // is left, which is exactly the moment a phone comes back from being
        // asleep and wifi has not reassociated yet. libgit2 got as far as dns
        // and no further, and the cloud button was left carrying "failed to
        // resolve address for github.com" for a sync the user never started —
        // one tap later the same thing worked. Waited out here, it usually
        // does not happen at all.
        if (hasRemote && !networkMonitor.awaitOnline()) {
            hasRemote = false

            // A sync that was asked for gets an answer, in words that name the
            // thing to fix. One that ran by itself says nothing: the dot on the
            // button already says the notes have not gone out, and being out of
            // signal for a moment is not a failure worth an error icon.
            if (announceSyncErrors) {
                failSync(uiHelper.getString(R.string.error_no_network))
            } else {
                Log.d(TAG, "sync: no network, and nobody asked")
            }
        }

        if (hasRemote) {
            _syncState.emit(SyncState.Pull)

            var pulled = gitManager.pull(cred, prefs.gitAuthor())

            // The wait beforehand asks the system whether there is a network,
            // and the system answers yes a moment before this process can
            // actually use one — coming out of doze the route is up while the
            // resolver still is not, which is the whole of "failed to resolve
            // address for github.com". One more try after a breath turns most
            // of those into a sync that happened rather than one that was
            // skipped. Nothing has been merged at that point, so there is
            // nothing to undo before trying again.
            if (pulled.isNetworkFailure()) {
                Log.d(TAG, "pull: no network yet, trying once more")
                delay(RETRY_AFTER_NETWORK_FAILURE_MS)
                pulled = gitManager.pull(cred, prefs.gitAuthor())
            }

            // and the pull is the other one
            knownFsCommit = null

            pulled.onFailure { err ->
                isError = true
                conflicted = err is GitException && err.type == GitExceptionType.MergeConflict
                reportSyncFailure(err)
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
                reportSyncFailure(err)
            }
        }

        // Whatever the sync managed, the working tree is what it is: a commit
        // that went through leaves nothing behind, one that did not leaves it.
        //
        // Before the sync is called done, not after: asking git means walking
        // the whole working tree, and the dot is only hidden while the button
        // is busy — so answering afterwards left the dot standing under a
        // button that had already said the notes were sent.
        refreshLocalChanges()

        if (hasRemote && !isError) {
            _syncState.emit(SyncState.Ok)
        } else if (_syncState.value is SyncState.Starting) {
            // nothing reached the network and nothing went wrong — a repository
            // without a remote, and a button that would otherwise pulse forever
            _syncState.emit(SyncState.Idle)
        }

        return success(Unit)
    }

}
