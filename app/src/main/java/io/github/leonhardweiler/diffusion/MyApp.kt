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

        appModule = AppModule(this)

        scope.launch {
            appModule.appPreferences.preload()
        }
        scope.launch {
            appModule.repoStore.preload()
        }
    }
}
