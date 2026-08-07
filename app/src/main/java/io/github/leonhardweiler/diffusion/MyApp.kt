package io.github.leonhardweiler.diffusion

import android.app.Application
import android.util.Log
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private const val TAG = "MyApp"

class MyApp : Application() {

    companion object {
        lateinit var appModule: AppModule
    }

    private val scope = MainScope()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        appModule = AppModuleImpl(this)

        // The first read of a preference is a read of the disk, and the first
        // two things to ask for one are the theme and the list of repositories,
        // both on the way to the first frame — the second of them from a
        // runBlocking, which is the main thread waiting on that disk.
        scope.launch {
            appModule.appPreferences.preload()
        }
        scope.launch {
            appModule.repoStore.preload()
        }
    }
}
