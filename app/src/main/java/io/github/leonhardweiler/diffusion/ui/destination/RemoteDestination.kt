package io.github.leonhardweiler.diffusion.ui.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface RemoteDestination : Parcelable {
    @Parcelize
    data class EnterUrl(val defaultUrl: String = "") : RemoteDestination

    @Parcelize
    data class SelectGenerateNewSshKeys(
        val url: String
    ) : RemoteDestination

    @Parcelize
    data class GenerateNewKeys(
        val url: String,
        val storedKeyId: String? = null,
    ) : RemoteDestination

    @Parcelize
    data object Cloning : RemoteDestination

    @Parcelize
    data class LoadKeysFromDevice(
        val url: String
    ) : RemoteDestination

    @Parcelize
    data object Logs: RemoteDestination
}
