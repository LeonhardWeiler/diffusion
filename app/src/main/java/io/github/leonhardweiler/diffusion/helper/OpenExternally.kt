package io.github.leonhardweiler.diffusion.helper

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.R
import java.io.File

private const val TAG = "OpenExternally"

/**
 * Hands a file in the repository to whatever app on the device knows what to do
 * with it.
 *
 * The list shows every file, not only the ones this app can read: a photo, a
 * pdf, a spreadsheet next to the notes about it. Tapping one of those opens the
 * system chooser rather than an editor that would show its bytes as text.
 *
 * It goes out as a `content://` uri from [FileProvider] and not as the path we
 * have: another app has no business with our storage permission, and since
 * Android 7 handing over a `file://` uri is refused outright.
 */
fun openFileWithAnotherApp(context: Context, path: String) {
    val file = File(path)

    val uri = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrElse {
        Log.e(TAG, "no uri for $path", it)
        MyApp.appModule.uiHelper.makeToast(
            MyApp.appModule.uiHelper.getString(R.string.error_open_externally, file.name)
        )
        return
    }

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeTypeOf(file.name))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(
        intent,
        MyApp.appModule.uiHelper.getString(R.string.open_with_another_app)
    )

    try {
        context.startActivity(chooser)
    } catch (e: ActivityNotFoundException) {
        Log.e(TAG, "nothing can open $path", e)
        MyApp.appModule.uiHelper.makeToast(
            MyApp.appModule.uiHelper.getString(R.string.error_no_app_for_file, file.name)
        )
    }
}

/**
 * What the system thinks a name of that shape holds. The wildcard type when it
 * has no idea, which leaves the choice to the user instead of ending the tap in
 * nothing.
 */
private fun mimeTypeOf(fileName: String): String {
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")

    return MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extension.lowercase())
        ?: "*/*"
}
