package io.github.wiiznokes.gitnote.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.platform.FileSystem
import io.github.wiiznokes.gitnote.data.platform.NodeFs
import io.github.wiiznokes.gitnote.helper.UiHelper
import kotlinx.coroutines.runBlocking


class FileExplorerViewModel(val path: String?) : ViewModel() {

    companion object {
        const val TAG = "FileExplorerViewModel"
    }

    val uiHelper: UiHelper = MyApp.appModule.uiHelper
    val prefs: AppPreferences = MyApp.appModule.appPreferences


    var currentDir: NodeFs.Folder = path?.let { path ->
        NodeFs.Folder.fromPath(path).let {
            if (it.exist()) it else null
        }
    } ?: FileSystem.defaultDir

    val folders: SnapshotStateList<NodeFs.Folder> = mutableStateListOf()

    init {

        val foldersList = runBlocking {
            currentDir.filterMapNodeFs {
                it as? NodeFs.Folder
            }
        }


        folders.addAll(foldersList)
    }

    fun createDir(name: String): Boolean {

        currentDir.createFolder(name).onFailure {
            uiHelper.makeToast(it.message)
            return false
        }

        // the list is read once when the screen opens, so a folder created
        // afterwards would only show up on the way back into this directory
        folders.add(NodeFs.Folder.fromPath(currentDir.path, name))

        Log.d(TAG, "$name created")
        return true
    }
}