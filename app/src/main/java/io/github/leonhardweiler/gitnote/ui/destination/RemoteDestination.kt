package io.github.leonhardweiler.gitnote.ui.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface RemoteDestination : Parcelable {

    @Parcelize
    data object EnterUrl : RemoteDestination


    @Parcelize
    data class SelectGenerateNewSshKeys(
        val url: String
    ) : RemoteDestination


    @Parcelize
    data class GenerateNewKeys(
        val url: String,
    ) : RemoteDestination

    @Parcelize
    data class Credentials(
        val url: String,
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
