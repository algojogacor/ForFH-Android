import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}

android {
    namespace = "com.aryariap.forfh"
    // DEV: brief kata 36, tapi AAR metadata (checkDebugAarMetadata) Compose BOM 2026.08.00 (1.12.0),
    // core 1.19.0, lifecycle 2.11.0 menuntut compileSdk >= 37 — platform android-37.0 dipasang via sdkmanager.
    // targetSdk tetap 36 (edge-to-edge & izin sesuai spec).
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aryariap.forfh"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false // V1: biarkan debug-size; R8 kandidat versi berikutnya (konsisten dgn T13)
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1")
    }

    // android.util.Log tidak dimock di unit test JVM murni → kembalikan default (0)
    // supaya path logging (mis. SyncRepository.syncKampusInfo) tidak crash di test.
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// Built-in Kotlin AGP 9: blok kotlin{} top-level (bukan android.kotlinOptions)
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.core)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.work.runtime)
    implementation(libs.coroutines.android)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
