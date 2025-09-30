plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") // Make sure KSP plugin is applied
}

android {
    namespace = "com.aught.wakawaka"
    compileSdk = 35

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    defaultConfig {
        applicationId = "com.aught.wakawaka"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // glance
    // For AppWidgets support
    implementation(libs.androidx.glance.appwidget)

    // For interop APIs with Material 3
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.navigation.compose) // Use the latest version

    // For interop APIs with Material 2
    implementation(libs.androidx.glance.material)

    // Retrofit for API calls (Check for latest versions)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi) // Or converter-gson

    // Moshi for JSON parsing (If using converter-moshi)
    implementation(libs.moshi.kotlin) // Use latest
    ksp(libs.moshi.kotlin.codegen) // Use latest (kapt or ksp)
//    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")

    // WorkManager for background tasks (Check for latest version)
    implementation(libs.androidx.work.runtime.ktx) // Use latest ktx version

    // OkHttp Logging Interceptor (Check for the latest version)
    implementation(libs.logging.interceptor) // Use latest

    // SharedPreferences (or DataStore)
    implementation(libs.androidx.preference.ktx) // For SharedPreferences simplicity, or use DataStore


    // koin
    // https://mvnrepository.com/artifact/io.insert-koin/koin-androidx-compose
    implementation(libs.insert.koin.koin.androidx.compose)
    // https://mvnrepository.com/artifact/io.insert-koin/koin-core
    implementation(libs.insert.koin.koin.core)
}
