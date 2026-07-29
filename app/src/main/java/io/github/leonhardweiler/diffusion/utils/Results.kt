package io.github.leonhardweiler.diffusion.utils

import android.util.Log
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

private const val TAG = "Results"

/**
 * Runs something that throws and answers with a [Result] instead.
 *
 * Everything that touches the filesystem goes through here: the java.nio api
 * reports by throwing, and the rest of the app decides what to do with a
 * failure rather than being unwound by it.
 */
fun <T> toResult(fn: () -> T): Result<T> {
    return try {
        success(fn())
    } catch (e: Exception) {
        Log.e(TAG, e.message ?: "call failed", e)
        failure(e)
    }
}
