package io.github.leonhardweiler.diffusion.helper

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.net.toUri
import io.github.leonhardweiler.diffusion.BuildConfig

object StoragePermissionHelper {
    fun isPermissionGranted(): Boolean = Environment.isExternalStorageManager()

    val contract: ActivityResultContract<Unit, Boolean> =
        object : ActivityResultContract<Unit, Boolean>() {
            override fun createIntent(context: Context, input: Unit): Intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                "package:${BuildConfig.APPLICATION_ID}".toUri()
            )

            override fun parseResult(resultCode: Int, intent: Intent?) = isPermissionGranted()

            override fun getSynchronousResult(context: Context, input: Unit) =
                if (isPermissionGranted()) SynchronousResult(true) else null
        }
}
