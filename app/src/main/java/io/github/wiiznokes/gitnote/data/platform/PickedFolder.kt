package io.github.wiiznokes.gitnote.data.platform

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
