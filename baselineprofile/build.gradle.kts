plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "io.github.leonhardweiler.diffusion.baselineprofile"

    compileSdk = 37

    defaultConfig {
        // the same floor the app has; macrobenchmarks need 28 at the least
        minSdk = 30
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // What a profile is recorded for is a build type of the app, and the two
    // sides are matched by name. The app has three, so a nightly build asked
    // this module for a variant it did not have and the whole
    // generateBaselineProfile failed to resolve — "No matching variant of
    // project ':baselineprofile'".
    //
    // Nothing is configured in them: this module has no code of its own to
    // build differently, it only has to answer for the name.
    buildTypes {
        create("nightly") { }
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
