package io.github.leonhardweiler.diffusion.ui.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface Destination : Parcelable {
    @Parcelize
    data class Setup(val setupDestination: SetupDestination) : Destination

    /**
     * The repository's id is part of the destination, so switching is not a
     * change to a screen but a different one: the note list gets a new
     * ViewModelStore and a new SaveableStateProvider, and its selection and
     * scroll position stay with the repository they were made for.
     */
    @Parcelize
    data class App(val repoId: String, val appDestination: AppDestination) : Destination

    @Parcelize
    data class MissingPermission(val repoPath: String) : Destination
}
