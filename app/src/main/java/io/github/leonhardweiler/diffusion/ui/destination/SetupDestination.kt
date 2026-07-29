package io.github.leonhardweiler.diffusion.ui.destination

import android.os.Parcelable
import io.github.leonhardweiler.diffusion.ui.model.StorageConfiguration
import kotlinx.parcelize.Parcelize


sealed interface SetupDestination : Parcelable {

    @Parcelize
    data object Main : SetupDestination

    /**
     * @param openedRemoteUrl set when the repository is already on the device and
     * only the credentials for its remote are missing, so the setup can skip
     * straight to asking for them.
     * @param alreadyOnDevice whether this repository is being opened rather than
     * cloned — with or without a remote of its own. Nothing is downloaded then,
     * so the last step syncs instead, and says so.
     */
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


