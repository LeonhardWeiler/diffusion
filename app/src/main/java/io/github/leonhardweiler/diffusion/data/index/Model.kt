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

fun generateUid() = Random.nextInt()

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

@Parcelize
data class Note(
    val relativePath: String,
    val content: String,
    val lastModifiedTimeMillis: Long,
    val id: Int,
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
