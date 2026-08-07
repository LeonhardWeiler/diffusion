package io.github.leonhardweiler.diffusion.ui.destination

import android.os.Parcelable
import io.github.leonhardweiler.diffusion.data.index.Note
import kotlinx.parcelize.Parcelize

sealed interface AppDestination : Parcelable {
    @Parcelize
    data object Grid : AppDestination

    @Parcelize
    data class Edit(val note: Note) : AppDestination

    @Parcelize
    data class Settings(val settingsDestination: SettingsDestination) : AppDestination
}
