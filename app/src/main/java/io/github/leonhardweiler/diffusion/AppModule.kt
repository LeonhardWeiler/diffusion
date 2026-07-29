package io.github.leonhardweiler.diffusion

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import io.github.leonhardweiler.diffusion.data.AppPreferences
import io.github.leonhardweiler.diffusion.data.index.NoteIndex
import io.github.leonhardweiler.diffusion.helper.EditHistoryStore
import io.github.leonhardweiler.diffusion.helper.NetworkMonitor
import io.github.leonhardweiler.diffusion.helper.UiHelper
import io.github.leonhardweiler.diffusion.manager.GitManager
import io.github.leonhardweiler.diffusion.manager.StorageManager


interface AppModule {
    val appScope: CoroutineScope
    val noteIndex: NoteIndex
    val uiHelper: UiHelper
    val storageManager: StorageManager
    val gitManager: GitManager
    val appPreferences: AppPreferences
    val editHistoryStore: EditHistoryStore
    val networkMonitor: NetworkMonitor
    val context: Context

}

class AppModuleImpl(
    override val context: Context
) : AppModule {

    /** What the note list reads, built from the files at every start. */
    override val noteIndex: NoteIndex by lazy {
        NoteIndex()
    }

    override val uiHelper: UiHelper by lazy {
        UiHelper(context)
    }
    override val storageManager: StorageManager by lazy {
        StorageManager()
    }
    override val gitManager: GitManager by lazy {
        GitManager()
    }
    override val appPreferences: AppPreferences by lazy {
        AppPreferences(context)
    }
    /** Storage and git writes must not be cancelled when a screen goes away. */
    override val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val editHistoryStore: EditHistoryStore by lazy {
        EditHistoryStore()
    }

    override val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(context)
    }
}