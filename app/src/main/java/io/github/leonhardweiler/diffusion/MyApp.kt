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
        // thing to ask for one is the theme, on the way to the first frame.
        scope.launch {
            appModule.appPreferences.preload()
        }
    }
}
