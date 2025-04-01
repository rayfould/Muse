import java.util.Properties // for ImgurAPI

// for local.properties keeping ImgurAPI key secret
val imgurClientId: String by lazy {
    val properties = Properties()
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        properties.load(localPropsFile.inputStream())
    }
    properties.getProperty("IMGUR_CLIENT_ID") ?: "temp-imgur-id"
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.creativecommunity"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.creativecommunity"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "IMGUR_CLIENT_ID", "\"$imgurClientId\"") // for ImgurAPI key
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        buildConfig = true // added for local.properties
    }
}

dependencies {
    implementation("androidx.navigation:navigation-compose:2.7.6") // for navigation between different pages
    implementation("io.coil-kt:coil-compose:2.5.0") // For previewing images
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // For HTTP uploading to Imgur using ImgurAPI

    // Use the Supabase BOM to set the version for all modules
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.1"))

    // Supabase modules
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")

    // Ktor dependencies for Supabase networking
    implementation("io.ktor:ktor-client-core:3.0.0")
    implementation("io.ktor:ktor-client-okhttp:3.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

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
}