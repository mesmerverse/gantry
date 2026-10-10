import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// Release signing from the gitignored signing.properties at the repo root;
// absent (e.g. a fresh clone) the release build is simply unsigned.
val signingProperties = Properties().apply {
    rootProject.file("signing.properties").takeIf { it.exists() }
        ?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.mesmerverse.gantry"
    compileSdk { version = release(36) }

    // Disable dependency metadata in APK signing block (required for F-Droid)
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    defaultConfig {
        applicationId = "com.mesmerverse.gantry"
        minSdk = 34
        targetSdk = 36
        versionCode = 61
        versionName = "1.25.2"
    }

    if (!signingProperties.isEmpty) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(signingProperties.getProperty("ANDROID_KEYSTORE_PATH"))
                storePassword = signingProperties.getProperty("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = signingProperties.getProperty("ANDROID_KEY_ALIAS")
                keyPassword = signingProperties.getProperty("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.datastore.preferences)
}
