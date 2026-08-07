package io.github.leonhardweiler.diffusion.ui.destination

import android.os.Parcelable
import io.github.leonhardweiler.diffusion.ui.model.StorageConfiguration
import kotlinx.parcelize.Parcelize

sealed interface SetupDestination : Parcelable {
    @Parcelize
    data object Main : SetupDestination

    @Parcelize
    data class Remote(
        val storageConfig: StorageConfiguration,
        val openedRemoteUrl: String? = null,
        val alreadyOnDevice: Boolean = false,
    ) : SetupDestination
}

enum class NewRepoMethod {
    Open,
    Clone,
}

