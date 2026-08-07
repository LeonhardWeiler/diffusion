package io.github.leonhardweiler.diffusion.utils

import android.util.Log
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

private const val TAG = "Results"

fun <T> toResult(fn: () -> T): Result<T> {
    return try {
        success(fn())
    } catch (e: Exception) {
        Log.e(TAG, e.message ?: "call failed", e)
        failure(e)
    }
}
