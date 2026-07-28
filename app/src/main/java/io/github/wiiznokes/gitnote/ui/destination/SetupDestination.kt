package io.github.wiiznokes.gitnote.ui.destination

import android.os.Parcelable
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import kotlinx.parcelize.Parcelize


sealed interface SetupDestination : Parcelable {

    @Parcelize
    data object Main : SetupDestination

    @Parcelize
    data class Remote(val storageConfig: StorageConfiguration) : SetupDestination

}

enum class NewRepoMethod {
    Open,
    Clone,
}


