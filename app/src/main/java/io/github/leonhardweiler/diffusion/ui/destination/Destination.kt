package io.github.leonhardweiler.diffusion.ui.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface Destination : Parcelable {

    @Parcelize
    data class Setup(val setupDestination: SetupDestination) : Destination

    @Parcelize
    data class App(val appDestination: AppDestination) : Destination

    /**
     * A repository that is set up and cannot be reached: everything is stored,
     * the permission to read the files is not there. Not part of the setup —
     * there is nothing here to set up.
     */
    @Parcelize
    data class MissingPermission(val repoPath: String) : Destination

}