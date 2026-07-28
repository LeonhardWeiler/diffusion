plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "io.github.wiiznokes.gitnote.baselineprofile"

    compileSdk = 37

    defaultConfig {
        // the same floor the app has; macrobenchmarks need 28 at the least
        minSdk = 30
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // what is being profiled
    targetProjectPath = ":app"
}

baselineProfile {
    // A profile is recorded from the app running, so this needs a device or an
    // emulator that is attached. Nothing here runs in CI.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.test.junit.ktx)
    implementation(libs.androidx.test.runner)
    implementation(libs.uiautomator)
    implementation(libs.benchmark.macro.junit4)
}
