package io.github.leonhardweiler.diffusion.ui.destination

import android.os.Parcelable
import io.github.leonhardweiler.diffusion.data.room.Note
import io.github.leonhardweiler.diffusion.ui.model.EditType
import io.github.leonhardweiler.diffusion.ui.model.FileExtension
import kotlinx.parcelize.Parcelize


sealed interface AppDestination : Parcelable {
    @Parcelize
    data object Grid : AppDestination

    @Parcelize
    data class Edit(val params: EditParams) : AppDestination

    @Parcelize
    data class Settings(val settingsDestination: SettingsDestination) : AppDestination

}

/**
 * Which note the editor was opened on, and whether it is being created.
 *
 * Only that. What is being typed lives in the editor's own saved state, which
 * is where a half written note survives the process dying — there used to be a
 * second variant here for that, fed from a file in the app's private directory.
 */
@Parcelize
data class EditParams(
    val note: Note,
    val editType: EditType,
) : Parcelable {
    fun fileExtension(): FileExtension = note.fileExtension()
}