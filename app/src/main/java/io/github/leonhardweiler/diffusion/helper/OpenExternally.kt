package io.github.leonhardweiler.diffusion.helper

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.R
import java.io.File

private const val TAG = "OpenExternally"

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

fun openUrlInBrowser(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.e(TAG, "nothing can open $url", e)
        MyApp.appModule.uiHelper.makeToast(
            MyApp.appModule.uiHelper.getString(R.string.error_no_app_for_link)
        )
    }
}

private fun mimeTypeOf(fileName: String): String {
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")

    return MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extension.lowercase())
        ?: "*/*"
}
