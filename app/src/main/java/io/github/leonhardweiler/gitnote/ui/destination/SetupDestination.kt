package io.github.leonhardweiler.gitnote.ui.destination

import android.os.Parcelable
import io.github.leonhardweiler.gitnote.ui.model.StorageConfiguration
import kotlinx.parcelize.Parcelize


sealed interface SetupDestination : Parcelable {

    @Parcelize
    data object Main : SetupDestination

    /**
     * @param openedRemoteUrl set when the repository is already on the device and
     * only the credentials for its remote are missing, so the setup can skip
     * straight to asking for them.
     */
    @Parcelize
    data class Remote(
        val storageConfig: StorageConfiguration,
        val openedRemoteUrl: String? = null,
    ) : SetupDestination

}

enum class NewRepoMethod {
    Open,
    Clone,
}


