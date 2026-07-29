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


    /**
     * @param useStored show the pair the app already holds instead of making a
     * new one. The screen is the same either way — copy the key, add it as a
     * deploy key, clone — only the first step has usually happened before.
     */
    @Parcelize
    data class GenerateNewKeys(
        val url: String,
        val useStored: Boolean = false,
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
