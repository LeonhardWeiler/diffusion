package io.github.leonhardweiler.diffusion.data.index

import android.os.Parcelable
import io.github.leonhardweiler.diffusion.BuildConfig
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import io.github.leonhardweiler.diffusion.helper.removeFirstAndLastSlash
import io.github.leonhardweiler.diffusion.helper.requireNotEndOrStartWithSlash
import io.github.leonhardweiler.diffusion.ui.model.FileExtension
import kotlinx.parcelize.Parcelize
import java.time.Instant
import kotlin.random.Random

/**
 * What the list and the undo history know a note by, and the one thing about it
 * that is not read off the disk.
 *
 * It is a number and not the path, because a note that is renamed is the same
 * note: the row keeps its place and the history stays with it.
 */
fun generateUid() = Random.nextInt()

/**
 * A folder of the repository.
 *
 * The root is one of these too, with the empty string for a path — everything
 * else is measured from it.
 *
 * Equal to another one when it says the same thing, for the reason a
 * [io.github.leonhardweiler.diffusion.ui.model.NoteHeader] is: a renamed folder
 * keeps its id, and a row that compares equal to the one it replaces never
 * reaches the screen. Which folders are marked is asked of the id
 * ([io.github.leonhardweiler.diffusion.ui.model.holds]).
 */
data class NoteFolder(
    val relativePath: String,
    val id: Int,
) {

    companion object {
        fun new(
            relativePath: String,
            id: Int = generateUid()
        ): NoteFolder {
            return NoteFolder(
                relativePath = removeFirstAndLastSlash(relativePath),
                id = id
            )
        }
    }

    init {
        if (BuildConfig.DEBUG) {
            requireNotEndOrStartWithSlash(relativePath)
            requireNotEndOrStartWithSlash(fullName())
        }
    }

    fun fullName(): String {
        return relativePath.substringAfterLast("/")
    }

    fun toFolderFs(rootPath: String): NodeFs.Folder {
        return NodeFs.Folder.fromPath(rootPath, relativePath)
    }
}

/**
 * A note with its text, which is what the editor is given and what a write is
 * made of.
 *
 * The list does not deal in these — it holds a
 * [io.github.leonhardweiler.diffusion.ui.model.NoteHeader] per row and the text
 * of a note is read from its file when it is opened.
 */
@Parcelize
data class Note(
    val relativePath: String,
    val content: String,
    val lastModifiedTimeMillis: Long,
    val id: Int,
    /**
     * Derived from [relativePath] once rather than worked out at every place
     * that asks. Anything that builds a Note by hand — rather than through
     * [new] or a `copy` that leaves the path alone — has to keep them agreeing.
     */
    val parentPath: String = relativePath.substringBeforeLast("/", missingDelimiterValue = ""),
    val fileName: String = relativePath.substringAfterLast("/"),
) : Parcelable {

    companion object {
        fun new(
            relativePath: String,
            content: String = "",
            lastModifiedTimeMillis: Long = Instant.now().toEpochMilli(),
            id: Int = generateUid()
        ): Note {
            return Note(
                relativePath = removeFirstAndLastSlash(relativePath),
                content = content,
                lastModifiedTimeMillis = lastModifiedTimeMillis,
                id = id,
            )
        }
    }

    fun fileExtension(): FileExtension {
        return relativePath.substringAfterLast(".", missingDelimiterValue = "")
            .let { FileExtension.match(it) }
    }

    /**
     * The file name with the dot and what follows it taken off — and the whole
     * name when there is no dot. Every file in the repository is listed, and a
     * `LICENSE` or a `Makefile` has no extension to take off; computing the cut
     * from the extension's length used to eat its last character.
     */
    fun nameWithoutExtension(): String =
        fileName.substringBeforeLast(".", missingDelimiterValue = fileName)

    init {
        if (BuildConfig.DEBUG) {
            require(relativePath.isNotEmpty())
            requireNotEndOrStartWithSlash(relativePath)
            requireNotEndOrStartWithSlash(parentPath)
            requireNotEndOrStartWithSlash(fileName)
            requireNotEndOrStartWithSlash(nameWithoutExtension())
        }
    }

    fun toFileFs(rootPath: String): NodeFs.File {
        return NodeFs.File.fromPath(rootPath, relativePath)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return id == (other as Note).id
    }

    override fun hashCode(): Int = id
}
