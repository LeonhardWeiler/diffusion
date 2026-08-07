package io.github.leonhardweiler.diffusion.helper

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "NetworkMonitor"

class NetworkMonitor(context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    fun isOnline(): Boolean {
        val manager = connectivity ?: return true
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun awaitOnline(timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS): Boolean {
        if (isOnline()) return true

        val manager = connectivity ?: return true

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        val available = CompletableDeferred<Unit>()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                available.complete(Unit)
            }
        }

        try {
            manager.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            Log.w(TAG, "could not watch the network: ${e.message}")
            return true
        }

        return try {
            withTimeoutOrNull(timeoutMillis) { available.await() } != null
        } finally {
            runCatching { manager.unregisterNetworkCallback(callback) }
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 8_000L
    }
}
