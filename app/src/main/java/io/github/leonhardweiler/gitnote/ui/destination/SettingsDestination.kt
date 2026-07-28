package io.github.leonhardweiler.gitnote.ui.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface SettingsDestination : Parcelable {

    @Parcelize
    data object Main : SettingsDestination

    @Parcelize
    data object Logs : SettingsDestination

}