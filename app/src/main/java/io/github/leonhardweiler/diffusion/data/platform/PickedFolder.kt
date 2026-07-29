package io.github.leonhardweiler.diffusion.data.platform

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract

/**
 * The real path behind a folder the system picker handed back.
 *
 * The picker answers with a `content://` tree uri, and libgit2 works on files,
 * not on documents — so the uri has to be turned back into a path. That only
 * works for the shared internal storage, whose documents are named after their
 * place in it. A memory card or a usb stick has no path we are allowed to use,
 * and those return null rather than a guess.
 */
fun pickedFolderPath(uri: Uri): String? {
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        ?: return null

    val (volume, relativePath) = documentId.split(':', limit = 2)
        .let { it.first() to it.getOrElse(1) { "" } }

    if (volume != PRIMARY_VOLUME) return null

    val root = Environment.getExternalStorageDirectory().path
    return if (relativePath.isEmpty()) root else "$root/$relativePath"
}

/**
 * The real path behind a file another app asked this one to open.
 *
 * The same problem as [pickedFolderPath] and the same answer: a note is a file
 * in the repository, and the repository is a path — so a uri that cannot be
 * turned back into one names nothing this app can open, and says so by
 * returning null rather than by guessing.
 *
 * A `file://` uri is already a path. A `content://` one is only readable this
 * way when it comes from the document provider of the shared storage, whose
 * document ids are the place in it.
 */
fun openedFilePath(uri: Uri): String? = when (uri.scheme) {
    "file" -> uri.path

    "content" -> {
        if (uri.authority != EXTERNAL_STORAGE_PROVIDER) {
            null
        } else {
            val documentId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
            val (volume, relativePath) = documentId
                ?.split(':', limit = 2)
                ?.let { it.first() to it.getOrElse(1) { "" } }
                ?: (null to "")

            if (volume != PRIMARY_VOLUME || relativePath.isEmpty()) {
                null
            } else {
                "${Environment.getExternalStorageDirectory().path}/$relativePath"
            }
        }
    }

    else -> null
}

/**
 * Where the picker should open: the shared storage, which is the only place a
 * repository can live. Providers are free to ignore the hint.
 */
fun primaryStorageUri(): Uri = DocumentsContract.buildDocumentUri(
    EXTERNAL_STORAGE_PROVIDER,
    "$PRIMARY_VOLUME:"
)

/** What the document provider calls the storage every device has. */
private const val PRIMARY_VOLUME = "primary"

private const val EXTERNAL_STORAGE_PROVIDER = "com.android.externalstorage.documents"
