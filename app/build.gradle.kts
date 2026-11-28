import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/**
 * Signing material, from a properties file that is never committed or from the
 * environment, in that order. Absent both, the release build is simply
 * unsigned — which is what lets anyone clone this and run `bundleRelease`
 * without being handed a key.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) load(FileInputStream(file))
}

fun signingValue(key: String, environment: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(environment)

val keystorePath = signingValue("storeFile", "ZAKA_KEYSTORE")

android {
    namespace = "com.jabulile.zaka"

    // Compose 1.13 refuses to be consumed by anything compiled against less
    // than 37.1, so the minor matters here and is not cosmetic.
    compileSdk = 37
    compileSdkMinor = 1

    defaultConfig {
        applicationId = "com.jabulile.zaka"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = signingValue("storePassword", "ZAKA_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "ZAKA_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "ZAKA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            // Null when no key is configured, which leaves the bundle unsigned
            // rather than failing the build.
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // Robolectric needs the merged resources and the manifest to stand
        // up an Application, which is how the Room tests get a Context
        // without an emulator.
        unitTests.isIncludeAndroidResources = true
    }
}

/**
 * The version the release workflow checks a tag against. A task rather than a
 * grep, so the single source of truth stays `versionName` above.
 */
tasks.register("printVersionName") {
    val version = android.defaultConfig.versionName
    doLast { println(version) }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    // Compose tests run under Robolectric too, so the semantics a screen
    // reader sees are asserted without an emulator.
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
