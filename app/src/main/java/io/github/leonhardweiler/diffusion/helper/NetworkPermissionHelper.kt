package io.github.leonhardweiler.diffusion.helper

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.core.content.PermissionChecker
import java.net.InetAddress
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NetworkPermissionHelper {
    companion object {

        /**
         * What a repository on the local network needs, from Android 37 on.
         *
         * The constant is a plain string and is copied into this class file
         * rather than looked up on the platform — which is what makes naming it
         * on an older device harmless, and it is never asked for there anyway:
         * [isPermissionGranted] answers true below 37.
         */
        @SuppressLint("InlinedApi")
        val PERMISSION: String = Manifest.permission.ACCESS_LOCAL_NETWORK

        fun isPermissionGranted(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < 37) return true
            return PermissionChecker.checkSelfPermission(context, PERMISSION) ==
                    PermissionChecker.PERMISSION_GRANTED
        }

        suspend fun requiresLocalNetworkPermission(urlString: String): Boolean = withContext(Dispatchers.IO) {
            try {
                val host = if (urlString.contains("://")) {
                    URI(urlString).host
                } else {
                    val afterAt = urlString.substringAfter('@', urlString)
                    afterAt.substringBefore(':')
                }

                if (host == null) return@withContext false

                if (host.endsWith(".local", ignoreCase = true)) {
                    return@withContext true
                }

                val addresses = InetAddress.getAllByName(host)

                addresses.any { address ->
                    address.isSiteLocalAddress ||
                    address.isLinkLocalAddress
                }
            } catch (_: Exception) {
                false
            }
        }
    }
}
