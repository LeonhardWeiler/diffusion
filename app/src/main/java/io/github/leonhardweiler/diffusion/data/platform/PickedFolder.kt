package io.github.leonhardweiler.diffusion.data.platform

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract

fun pickedFolderPath(uri: Uri): String? {
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        ?: return null

    val (volume, relativePath) = documentId.split(':', limit = 2)
        .let { it.first() to it.getOrElse(1) { "" } }

    if (volume != PRIMARY_VOLUME) return null

    val root = Environment.getExternalStorageDirectory().path
    return if (relativePath.isEmpty()) root else "$root/$relativePath"
}

fun primaryStorageUri(): Uri = DocumentsContract.buildDocumentUri(
    EXTERNAL_STORAGE_PROVIDER,
    "$PRIMARY_VOLUME:"
)

private const val PRIMARY_VOLUME = "primary"

private const val EXTERNAL_STORAGE_PROVIDER = "com.android.externalstorage.documents"
