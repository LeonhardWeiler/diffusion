package io.github.leonhardweiler.diffusion.ui.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface RemoteDestination : Parcelable {

    /**
     * @param defaultUrl what the field starts out holding. An opened repository
     * whose remote is https comes through here so that it can be given an ssh
     * one, rather than being a dead end.
     */
    @Parcelize
    data class EnterUrl(val defaultUrl: String = "") : RemoteDestination


    @Parcelize
    data class SelectGenerateNewSshKeys(
        val url: String
    ) : RemoteDestination


    @Parcelize
    data class GenerateNewKeys(
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
