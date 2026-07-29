package io.github.leonhardweiler.diffusion.manager

import android.util.Log
import androidx.annotation.StringRes
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.data.AppPreferences
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import io.github.leonhardweiler.diffusion.data.index.Note
import io.github.leonhardweiler.diffusion.data.index.NoteFolder
import io.github.leonhardweiler.diffusion.data.index.NoteIndex
import io.github.leonhardweiler.diffusion.helper.getParentPath
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
 * How long a pull waits before trying a second time, when the first found no
 * network. Long enough for a resolver that is being set up to be ready, short
 * enough that somebody watching the button does not give up on it.
 */
private const val RETRY_AFTER_NETWORK_FAILURE_MS = 1_500L

class StorageManager {


    val prefs: AppPreferences = MyApp.appModule.appPreferences

    private val uiHelper = MyApp.appModule.uiHelper

    private val networkMonitor = MyApp.appModule.networkMonitor

    private val index: NoteIndex = MyApp.appModule.noteIndex

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
     * Reads the whole repository into the index, which is the only thing that
     * says what is in it.
     *
     * There is nothing to compare against anymore. The index lives in memory,
     * so it is built when a repository is opened and read again whenever
     * something other than this app may have written to it — a pull, and the
     * reload in the menu. A note written here is put in as it is written, and
     * committing does not touch the working tree.
     */
    suspend fun rebuildIndex(
        progressCb: ((Progress) -> Unit)? = null
    ): Result<Unit> = locker.withLock {
        refreshLocalChanges()

        rebuildIndexWithoutLocker(progressCb)
    }

    private suspend fun rebuildIndexWithoutLocker(
        progressCb: ((Progress) -> Unit)? = null
    ): Result<Unit> {
        val repoPath = runCatching { prefs.repoPath() }.getOrElse { return failure(it) }

        index.rebuild(repoPath, progressCb)

        return success(Unit)
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

            // A note that kept its name writes over the row it already had.
            // Only a rename leaves an old one behind, and that is rare while
            // this runs on every typing pause.
            if (renamed) {
                index.moveNote(previous.relativePath, new)
            } else {
                index.putNote(new)
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
            val file = note.toFileFs(prefs.repoPath())

            file.create().orComplain(R.string.error_create_file)
            file.write(note.content).orComplain(R.string.error_write_file)
            file.dateBy(note)

            index.putNote(note)

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

            index.removeNoteAt(relativePath)
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

            // then the rows, all of them together: the screen shows the index,
            // so the whole selection disappears at once rather than note by note
            index.removeNotesAt(relativePaths)
            success(Unit)
        }
    }

    /**
     * Moves a note to where [newRelativePath] says.
     *
     * Renaming and moving are one act here, the way they are for a folder, and
     * the file is moved rather than written somewhere else and deleted: the
     * bytes are never read, so a note that is only renamed keeps its date and a
     * file too large to be indexed is moved like any other.
     *
     * The row is rewritten with the id and the date it already held, so the
     * list keeps its place and an open undo history stays with its note.
     */
    suspend fun renameNote(note: Note, newRelativePath: String): Result<Unit> = locker.withLock {
        Log.d(TAG, "renameNote: ${note.relativePath} -> $newRelativePath")

        if (note.relativePath == newRelativePath) return@withLock success(Unit)

        val repoPath = prefs.repoPath()

        val target = NodeFs.File.fromPath(repoPath, newRelativePath)
        if (target.exist()) {
            return@withLock complain(R.string.error_file_already_exist)
        }

        // the way mv wants it: the folder is moved into has to be there already
        val parentPath = getParentPath(newRelativePath)
        if (parentPath.isNotEmpty() &&
            !NodeFs.Folder.fromPath(repoPath, parentPath).exist()
        ) {
            return@withLock complain(R.string.error_folder_not_found, parentPath)
        }

        update {
            NodeFs.File.fromPath(repoPath, note.relativePath).moveTo(target.path)
                .onFailure { return@update failure(it) }

            // through the constructor, so parentPath and fileName are derived
            // from the new path rather than kept from the old one
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
            val folder = noteFolder.toFolderFs(prefs.repoPath())
            folder.create().orComplain(R.string.error_create_folder)

            // Git has no concept of an empty directory, so without a file in it the
            // folder would produce no commit content and never reach another device.
            folder.createFile(GIT_KEEP).onFailure {
                Log.e(TAG, "could not create $GIT_KEEP in ${folder.path}: ${it.message}")
            }

            index.putFolder(noteFolder)

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
     * The rows are rewritten rather than read from the disk afterwards. A
     * rebuild would walk every file in the repository for a change that touched
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

            index.moveFolder(noteFolder, newRelativePath)

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
            index.removeFolders(noteFolders)

            success(Unit)
        }
    }

    suspend fun closeRepo() = locker.withLock {
        prefs.closeRepo()
        gitManager.closeRepo()
        index.clear()
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
     * Applies a change to the files first and to the index second. Nothing is
     * committed here: the working tree carries the change until the user asks
     * for a sync, which is what [syncWithRemote] then commits and pushes in one
     * go.
     *
     * That order is the whole of it. Both are written one after the other and
     * the process can die in between, and then one of them is behind — so the
     * question is which. The files are the truth: a row left over for a file
     * that is gone is noticed the moment the note is opened ("this note is no
     * longer there"), and the index is read from the files again at the next
     * start anyway. The other way round, a file with no row is a note that is
     * simply not in the list until then.
     */
    private suspend fun <T> update(
        f: suspend () -> Result<T>
    ): Result<T> {

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

        // Whether the working tree was written to, which only a pull does here.
        var pulledFiles = false

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

            pulled.onSuccess {
                pulledFiles = true
            }.onFailure { err ->
                isError = true
                // a conflict is written into the notes, so it is something the
                // list has to be told about like any other pull
                pulledFiles = err is GitException && err.type == GitExceptionType.MergeConflict
                reportSyncFailure(err)
            }
        }

        // Only what a pull wrote is worth reading again: it is the one thing
        // here that touches the working tree. A conflict counts as well — it is
        // written into the notes without HEAD moving, and without this it would
        // be in the files and nowhere on screen.
        if (pulledFiles) {
            rebuildIndexWithoutLocker().onFailure { err ->
                failSync(err.message)
                return failure(err)
            }
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
