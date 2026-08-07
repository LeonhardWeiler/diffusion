package io.github.leonhardweiler.diffusion.ui.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface SettingsDestination : Parcelable {

    @Parcelize
    data object Main : SettingsDestination

    @Parcelize
    data object Logs : SettingsDestination

    /**
     * One repository's own settings — who its commits are by, what it pushes to,
     * which key it takes, whether it syncs by itself. By id rather than by
     * session, because a destination is written into the saved state and a
     * session is an open git repository.
     */
    @Parcelize
    data class Repo(val repoId: String) : SettingsDestination

}