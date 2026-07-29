package io.github.leonhardweiler.diffusion.ui.destination

import android.os.Parcelable
import io.github.leonhardweiler.diffusion.data.index.Note
import kotlinx.parcelize.Parcelize


sealed interface AppDestination : Parcelable {
    @Parcelize
    data object Grid : AppDestination

    /**
     * Which note the editor was opened on, and nothing else.
     *
     * It used to carry an EditParams saying whether the note was being created as
     * well: the editor was where a note came into being, on a path that had no
     * file behind it yet. A note is written before it is opened now, so every note
     * the editor is given is one that exists.
     */
    @Parcelize
    data class Edit(val note: Note) : AppDestination

    @Parcelize
    data class Settings(val settingsDestination: SettingsDestination) : AppDestination

}