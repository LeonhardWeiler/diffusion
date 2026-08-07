package io.github.leonhardweiler.diffusion.data

import android.content.Context
import io.github.leonhardweiler.diffusion.manager.PreferencesManager
import io.github.leonhardweiler.diffusion.ui.model.Cred
import io.github.leonhardweiler.diffusion.ui.model.CredType
import io.github.leonhardweiler.diffusion.ui.theme.Theme

/**
 * What belongs to the app rather than to one of its repositories: the theme, and
 * which notes were last left being read.
 *
 * Everything about a repository — where it is, what it pushes to, who its
 * commits are by, which key it takes — moved to
 * [io.github.leonhardweiler.diffusion.data.repo.RepoStore] when there could be
 * more than one of them. What is left of it here is what
 * [io.github.leonhardweiler.diffusion.data.repo.RepoStore.migrateFrom] reads
 * once, to carry the single repository this app used to hold into that list.
 */
class AppPreferences(
    context: Context
) : PreferencesManager(context, "settings") {

    companion object {
        /**
         * How many notes are remembered as being read rather than written. A
         * repository can hold ten thousand of them, and this is one preference
         * that is read whenever a note is opened.
         */
        const val MAX_REMEMBERED_READING_NOTES = 200
    }

    val theme = enumPreference("theme", Theme.SYSTEM)

    // ---------------------------------------------------------------------
    // The one repository there used to be. Read once, by the migration, and
    // written by nothing anymore.
    // ---------------------------------------------------------------------

    val isInit = booleanPreference("isInit", false)

    private val repoPath = stringPreference("repoPath")

    suspend fun repoPath(): String = repoPath.get()

    val remoteUrl = stringPreference("remoteUrl", "")

    private val credType = enumPreference("credType", CredType.None)

    val gitAuthorName = stringPreference("gitAuthorName", "")
    val gitAuthorEmail = stringPreference("gitAuthorEmail", "")

    private val publicKey = stringPreference("publicKey", "")
    private val privateKey = stringPreference("privateKey", "")
    private val passphrase = stringPreference("passphrase", "")

    suspend fun cred(): Cred? {
        return when (credType.get()) {
            CredType.None -> null

            CredType.Ssh -> Cred.Ssh(
                publicKey = this.publicKey.get(),
                privateKey = this.privateKey.get(),
                passphrase = this.passphrase.get().ifEmpty { null }
            )
        }
    }

    /**
     * Whether the app syncs by itself when it is opened and when it is left.
     * A per-repository setting now; this is what the one repository there was
     * had it set to.
     */
    val syncOnOpenAndClose = booleanPreference("syncOnOpenAndClose", true)

    /**
     * The notes that were last left in reading mode, one per line, each written
     * as the repository's id and the note's path with a slash between them.
     *
     * Reading or writing belongs to the note, not to the app: one note is a page
     * to read and the next is a list to tick off, and the switch in the search
     * bar's menu was a single answer for all of them — turned on to read one note
     * and then in the way of every note after it. There is no setting anymore.
     * The eye above an open note is the whole of it, and what it decides is
     * remembered for that note.
     *
     * By path, because that is the only name a note has between two runs: an id
     * is made up again every time the repository is read. A note that is renamed
     * is therefore one this has never seen and opens for writing again, which
     * costs one tap. The repository's id is in front of it because two
     * repositories can both hold a `todo.md`, and they are not the same note. A
     * path cannot hold a newline (NameValidation refuses it), so one per line is
     * a list that reads back.
     *
     * The most recently switched are at the end, and only
     * [MAX_REMEMBERED_READING_NOTES] of them are kept.
     */
    private val readingModeNotes = stringPreference("readingModeNotes")

    private fun noteKey(repoId: String, relativePath: String) = "$repoId/$relativePath"

    /** Whether this note was left being read. Written notes are not listed. */
    fun opensInReadingMode(repoId: String, relativePath: String): Boolean {
        val key = noteKey(repoId, relativePath)
        return readingModeNotes.getBlocking().lineSequence().any { it == key }
    }

    suspend fun setReadingMode(repoId: String, relativePath: String, reading: Boolean) {
        val key = noteKey(repoId, relativePath)

        val kept = readingModeNotes.get()
            .lineSequence()
            .filter { it.isNotEmpty() && it != key }
            .toMutableList()

        if (reading) kept += key

        readingModeNotes.update(
            kept.takeLast(MAX_REMEMBERED_READING_NOTES).joinToString("\n")
        )
    }
}
