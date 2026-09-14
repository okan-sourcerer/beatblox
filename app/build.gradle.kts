import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // No org.jetbrains.kotlin.android: AGP 9 has built-in Kotlin support.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Deployment knobs live outside the repo: `keystore.properties` next to this
// file (see README "Release build"), and -P / gradle.properties values baked
// into BuildConfig. Empty URL = link hidden; empty hub key = feedback stays
// queued on the device. The hub key is write-only for this app, so embedding
// it in the APK is fine (docs/integration.md).
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// local.properties (git-ignored) wins over gradle.properties / -P, so the hub
// key and other per-machine values never end up in a commit.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun urlProp(name: String): String =
    localProps.getProperty(name)?.takeIf { it.isNotBlank() } ?: (project.findProperty(name) as String?).orEmpty()

android {
    namespace = "com.okan.beatblox"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.okan.beatblox"
        minSdk = 26
        targetSdk = 37
        // Bump versionCode for EVERY APK you host: Android refuses to update an
        // installed app to the same or a lower code. versionName is just for display.
        versionCode = 2
        versionName = "0.2.0"

        buildConfigField("String", "HUB_URL", "\"${urlProp("beatblox.hubUrl").ifBlank { "https://coreworkbench.com" }}\"")
        buildConfigField("String", "HUB_KEY", "\"${urlProp("beatblox.hubKey")}\"")
        buildConfigField("String", "SOURCE_URL", "\"${urlProp("beatblox.sourceUrl")}\"")
        buildConfigField("String", "DOWNLOAD_URL", "\"${urlProp("beatblox.downloadUrl")}\"")
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "ENVIRONMENT", "\"dev\"")
        }
        release {
            buildConfigField("String", "ENVIRONMENT", "\"prod\"")
            // Left off on purpose: the JS bridge (@JavascriptInterface) and
            // kotlinx.serialization would need keep rules, and the APK is small anyway.
            isMinifyEnabled = false
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.webkit)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
