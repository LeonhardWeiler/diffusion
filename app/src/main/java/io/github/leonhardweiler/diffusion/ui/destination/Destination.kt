package io.github.leonhardweiler.diffusion.ui.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface Destination : Parcelable {

    @Parcelize
    data class Setup(val setupDestination: SetupDestination) : Destination

    /**
     * The notes of one repository.
     *
     * The id is part of the destination and not only of the app module, because
     * switching repositories has to build this screen again from nothing: the
     * note list, its view model, its selection and where it was scrolled all
     * belong to the repository they were made for, and the navigation keys every
     * one of those by the destination.
     */
    @Parcelize
    data class App(val repoId: String, val appDestination: AppDestination) : Destination

    /**
     * A repository that is set up and cannot be reached: everything is stored,
     * the permission to read the files is not there. Not part of the setup —
     * there is nothing here to set up.
     */
    @Parcelize
    data class MissingPermission(val repoPath: String) : Destination

}