package io.github.leonhardweiler.diffusion.data.room

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import io.github.leonhardweiler.diffusion.BuildConfig
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import io.github.leonhardweiler.diffusion.helper.removeFirstAndLastSlash
import io.github.leonhardweiler.diffusion.helper.requireNotEndOrStartWithSlash
import io.github.leonhardweiler.diffusion.ui.model.FileExtension
import kotlinx.parcelize.Parcelize
import java.time.Instant


private const val TAG = "DatabaseSchema"


@Entity(
    tableName = "NoteFolders",
    primaryKeys = ["relativePath"],
)
data class NoteFolder(
    val relativePath: String,
    val id: Int,
) {

    companion object {
        fun new(
            relativePath: String,
            id: Int = RepoDatabase.generateUid()
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return id == (other as NoteFolder).id
    }

    override fun hashCode(): Int = id
}

@Entity(
    tableName = "Notes",
    primaryKeys = ["relativePath"],
    indices = [Index("parentPath"), Index("fileName")]
)
@Parcelize
data class Note(
    val relativePath: String,
    val content: String,
    val lastModifiedTimeMillis: Long,
    val id: Int,
    /**
     * Derived from [relativePath], but stored and indexed instead of computed per
     * query: the note list filters on [parentPath] and partitions on [fileName],
     * and a value SQLite has to compute for every row cannot use an index.
     */
    val parentPath: String = relativePath.substringBeforeLast("/", missingDelimiterValue = ""),
    val fileName: String = relativePath.substringAfterLast("/"),
) : Parcelable {

    companion object {
        fun new(
            relativePath: String,
            content: String = "",
            lastModifiedTimeMillis: Long = Instant.now().toEpochMilli(),
            id: Int = RepoDatabase.generateUid()
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
     * name when there is no dot. Every file in the repository is listed now,
     * and a `LICENSE` or a `Makefile` has no extension to take off; computing
     * the cut from the extension's length used to eat its last character.
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

@Fts4(contentEntity = Note::class)
@Entity(tableName = "NotesFts")
data class NoteFts(
    val relativePath: String,
    val content: String
)