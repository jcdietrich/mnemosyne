plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.objectbox)
}

android {
    namespace = "com.mnemosyne"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mnemosyne.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Avoid conflicts from google-api-client, guava, and auth
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.navigation.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // ObjectBox (vector store)
    implementation(libs.objectbox.android)
    implementation(libs.objectbox.kotlin)

    // WorkManager (background backup)
    implementation(libs.work.runtime.ktx)
    implementation(libs.work.hilt)
    ksp(libs.work.hilt.compiler)

    // MediaPipe (text embedding)
    implementation(libs.mediapipe.tasks.text)

    // sherpa-onnx (on-device STT via local AAR)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    // OkHttp (model download)
    implementation(libs.okhttp)

    // Coil (Image loading)
    implementation(libs.coil.compose)

    // Google Credential Manager + Drive
    implementation(libs.credentials)
    implementation(libs.credentials.play.auth)
    implementation(libs.google.id)
    implementation(libs.google.api.drive)
    implementation(libs.google.api.client.android)
    implementation(libs.google.http.client.android)
    implementation(libs.google.http.client.gson)
    implementation(libs.google.auth.oauth2)

    // Location
    implementation(libs.play.services.location)

    // Coroutines
    implementation(libs.coroutines.android)

    // JSON (backup serialization)
    implementation(libs.moshi.kotlin)

    // Debug tooling
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)

    // Instrumented tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
