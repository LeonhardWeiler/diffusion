package io.github.wiiznokes.gitnote

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.room.RepoDatabase
import io.github.wiiznokes.gitnote.helper.EditHistoryStore
import io.github.wiiznokes.gitnote.helper.NoteSaver
import io.github.wiiznokes.gitnote.helper.UiHelper
import io.github.wiiznokes.gitnote.manager.GitManager
import io.github.wiiznokes.gitnote.manager.StorageManager


interface AppModule {
    val appScope: CoroutineScope
    val repoDatabase: RepoDatabase
    val uiHelper: UiHelper
    val storageManager: StorageManager
    val gitManager: GitManager
    val appPreferences: AppPreferences
    val noteSaver: NoteSaver
    val editHistoryStore: EditHistoryStore
    val context: Context

}

class AppModuleImpl(
    override val context: Context
) : AppModule {

    override val repoDatabase: RepoDatabase by lazy {
        RepoDatabase.buildDatabase(context)
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

    override val noteSaver: NoteSaver by lazy {
        NoteSaver(context.filesDir)
    }

    override val editHistoryStore: EditHistoryStore by lazy {
        EditHistoryStore()
    }
}