package io.github.leonhardweiler.gitnote.ui.viewmodel

import io.github.leonhardweiler.gitnote.ui.model.Cred
import io.github.leonhardweiler.gitnote.ui.model.StorageConfiguration

/**
 * The part of [SetupViewModel] the setup screens call, so that a @Preview can be
 * handed something that does nothing instead of a view model that would reach
 * for the whole app.
 */
interface SetupViewModelI {

    fun launch(f: suspend () -> Unit) {}

    fun cloneRepo(
        storageConfig: StorageConfiguration,
        remoteUrl: String,
        cred: Cred? = null,
        onSuccess: () -> Unit
    ) {
    }

}

class SetupViewModelMock : SetupViewModelI
