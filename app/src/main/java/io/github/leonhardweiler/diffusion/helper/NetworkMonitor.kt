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

/**
 * Whether there is a network that reaches the internet, and a way to wait for
 * one.
 *
 * The app syncs when it is opened and when it is left, and both of those happen
 * at the moment a phone comes back from being asleep — before wifi has
 * reassociated. libgit2 then failed at the first thing it did, and what the
 * cloud button carried was "failed to resolve address for github.com", for a
 * sync that was never going to work and that the user had not asked for. A
 * second later the same tap went through.
 *
 * `VALIDATED` and not merely `INTERNET` is what is asked for: the first says
 * the system has actually reached something, the second only says the network
 * claims it could. A captive portal is `INTERNET` and nothing else.
 */
class NetworkMonitor(context: Context) {

    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    fun isOnline(): Boolean {
        val manager = connectivity ?: return true
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * True as soon as there is such a network, false if none turns up within
     * [timeoutMillis].
     *
     * Waiting rather than only asking: coming back from sleep, the answer is no
     * for about as long as it takes wifi to associate, and a sync that waited
     * that out is the one the user would otherwise have started by hand.
     */
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
            // too many callbacks registered, say: better to let the sync try and
            // fail than to hold it back for a reason that is not the network
            Log.w(TAG, "could not watch the network: ${e.message}")
            return true
        }

        return try {
            withTimeoutOrNull(timeoutMillis) { available.await() } != null
        } finally {
            // in every case, including the one where the wait was cancelled:
            // a callback that stays registered is one the system keeps calling
            runCatching { manager.unregisterNetworkCallback(callback) }
        }
    }

    private companion object {
        /**
         * Long enough for wifi to come back after the screen went on, short
         * enough that a sync started by hand somewhere without signal answers
         * while the user is still looking at it.
         */
        const val DEFAULT_TIMEOUT_MILLIS = 8_000L
    }
}
