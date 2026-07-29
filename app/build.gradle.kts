import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.BufferedReader
import java.io.InputStreamReader


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    // for room
    alias(libs.plugins.ksp)
    // @Parcelize on the navigation destinations, which is what lets the
    // backstack survive the process being killed
    id("kotlin-parcelize")
    // consumes what :baselineprofile records
    alias(libs.plugins.baselineprofile)
}

android {
    // changing this version require to also change it in CI.
    // link: https://developer.android.com/ndk/downloads
    // Note that we should always take an lts version (end in d, ex: "r27d"), because the dl link
    // could be removed otherwise
    ndkVersion = "27.3.13750724"

    namespace = "io.github.leonhardweiler.diffusion"

    // https://developer.android.com/about/versions
    compileSdk = 37
    
    dependenciesInfo {
        // Disables dependency metadata when building APKs (for IzzyOnDroid/F-Droid)
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles (for Google Play)
        includeInBundle = false
    }

    defaultConfig {

        fun getGitHash(): String {
            val command = arrayOf("git", "rev-parse", "HEAD")
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            return reader.readLine()
        }

        applicationId = "io.github.leonhardweiler.diffusion"
        minSdk = 30

        versionCode = 18
        versionName = "26.05.1"

        buildConfigField(
            "String",
            "GIT_HASH",
            "\"${getGitHash()}\""
        )

        vectorDrawables.useSupportLibrary = true

        // android.util.Log throws in JVM tests unless its methods return a default
        testOptions.unitTests.isReturnDefaultValues = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

    }

    signingConfigs {
        create("release") {
            // on powershell
            // $env:KEY_ALIAS = "var"
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
            storeFile = file("key.jks")
            storePassword = System.getenv("STORE_PASSWORD")
        }

        // need this because debug key is machine dependent
        create("nightly") {
            keyAlias = "key0"
            keyPassword = "123456"
            storeFile = file("nightly-signing-key.jks")
            storePassword = "123456"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")
        }


        create("nightly") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("nightly")
            applicationIdSuffix = ".nightly"
        }

        debug {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("nightly")
        }
    }



    buildFeatures {
        buildConfig = true
        compose = true
    }

    lint {
        // A lint error stops the build. It could not before, which meant a new
        // critical finding would have gone past in the same silence the nine
        // harmless warnings do.
        abortOnError = true
        checkReleaseBuilds = false

        // "a newer AGP is available" is not something the code can be wrong
        // about, and it would break the build on its own schedule.
        disable += "AndroidGradlePluginVersion"
    }

}

/**
 * The largest a `libgit_wrapper.so` that is about to be released may be.
 *
 * A release build of the rust side is about 7 MB per architecture, a debug one
 * about 68 — it carries the debug info of libgit2, openssl and libssh2. The two
 * are the same file in the same place (`jniLibs` is gitignored and filled by
 * `make build_install`), so a release apk built after a debug `make` took the
 * debug library with it and said nothing about it. Ten times the download, and
 * nothing to see in the build log.
 *
 * Anywhere between the two does as a line, so this is far enough above the one
 * and below the other that a release growing does not start failing builds.
 */
val maxReleasedJniLibBytes = 20L * 1024 * 1024

val checkJniLibsAreRelease = tasks.register("checkJniLibsAreRelease") {
    group = "verification"
    description = "Fails when the bundled libgit_wrapper.so is a debug build."

    val jniLibs = fileTree(layout.projectDirectory.dir("src/main/jniLibs")) {
        include("**/*.so")
    }
    inputs.files(jniLibs).withPropertyName("jniLibs")

    doLast {
        val libraries = jniLibs.files.sortedBy { it.path }

        if (libraries.isEmpty()) {
            throw GradleException(
                "No native library in app/src/main/jniLibs — run `make build_install " +
                        "DEBUG=0` in app/src/main/rust first."
            )
        }

        val tooLarge = libraries.filter { it.length() > maxReleasedJniLibBytes }
        if (tooLarge.isEmpty()) return@doLast

        throw GradleException(
            tooLarge.joinToString(
                prefix = "These native libraries look like debug builds:\n",
                separator = "\n",
                postfix = "\nRun `make build_install DEBUG=0` in app/src/main/rust.",
            ) { "  ${it.name} in ${it.parentFile.name} is ${it.length() / 1024 / 1024} MB" }
        )
    }
}

androidComponents {
    onVariants { variant ->

        // The variants the baseline profile plugin adds are built from release
        // and inherit its signing config, whose keystore is not in the
        // repository — so recording a profile asked for the release key, which
        // has nothing to do with recording one. They are never published, so
        // they are signed like the nightly, whose key is committed.
        if (variant.name.startsWith("nonMinified") || variant.name.startsWith("benchmark")) {
            variant.signingConfig.setConfig(android.signingConfigs.getByName("nightly"))
        }

        // debug is the one build that is meant to carry a debug library
        if (variant.buildType == "debug") return@onVariants

        val capitalized = variant.name.replaceFirstChar { it.uppercase() }

        // Packaging rather than assembling: the bundle is packaged too, and
        // both of them are where the library would go out.
        tasks.matching {
            it.name == "package$capitalized" || it.name == "package${capitalized}Bundle"
        }.configureEach {
            dependsOn(checkJniLibsAreRelease)
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    // set what version of the jdk will be used to compile the code
    jvmToolchain(21)

    compilerOptions {
        // set the target JVM bytecode
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {

    // AndroidX Core
    implementation(libs.core.ktx)
    // installs the recorded baseline profile on first run
    implementation(libs.profileinstaller)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    // viewModel() and LocalViewModelStoreOwner, which ui/navigation scopes per screen
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(libs.datastore.preferences)

    val composeBom = platform(libs.compose.bom)

    // Compose
    implementation(composeBom)
    // the previews are compiled everywhere, see below
    compileOnly(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // Compose Debug
    // A preview is written next to the thing it previews, so the annotation has
    // to be on the compile path of every variant — but only the debug build has
    // anything to do with it, and the release should not carry it.
    compileOnly(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    annotationProcessor(libs.room.compiler)
    ksp(libs.room.compiler)
    implementation(libs.sqlite)
    implementation(libs.paging)
    implementation(libs.paging.compose)
    implementation(libs.room.paging)

    // Markdown
    implementation(libs.compose.markdown.core)
    implementation(libs.compose.markdown.renderer)
    implementation(libs.compose.markdown.android)

    // unit test
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))

    // the profile itself, recorded on a device
    baselineProfile(project(":baselineprofile"))

    // integration test
    androidTestImplementation(libs.test.junit.ktx)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)
}