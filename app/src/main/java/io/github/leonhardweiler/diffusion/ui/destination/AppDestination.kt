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

@Parcelize
sealed class EditParams : Parcelable {
    data class Saved(
        val note: Note,
        val editType: EditType,
        val name: String,
        val content: String,
    ) : EditParams()

    data class Idle(
        val note: Note,
        val editType: EditType
    ) : EditParams()

    fun fileExtension(): FileExtension {
        return when (this) {
            is Idle -> this.note.fileExtension()
            is Saved -> this.note.fileExtension()
        }
    }
}