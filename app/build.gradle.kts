import java.util.Properties // for ImgurAPI

// for local.properties keeping ImgurAPI key secret
val imgurClientId: String by lazy {
    val properties = Properties()
    val localPropsFile = rootProject.file("local.properties")
    if (!localPropsFile.exists()) {
        throw GradleException("local.properties not found. Please create it with IMGUR_CLIENT_ID.")
    }
    properties.load(localPropsFile.inputStream())
    val clientId = properties.getProperty("IMGUR_CLIENT_ID")
    if (clientId.isNullOrBlank()) {
        throw GradleException("IMGUR_CLIENT_ID missing in local.properties. Please add it.")
    }
    clientId
}

// Load keystore properties
val keystoreProperties = Properties()
val keystorePropsFile = rootProject.file("local.properties")
if (keystorePropsFile.exists()) {
    keystoreProperties.load(keystorePropsFile.inputStream())
} else {
    // Handle missing local.properties for signing if needed, or throw error
    println("Warning: local.properties not found. Release signing may fail.")
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.riss.muse"
    compileSdk = 35

    // Define Signing Configs
    signingConfigs {
        create("release") {
            // Read properties, providing default empty string if not found
            val storeFilePassword = keystoreProperties.getProperty("muse.keystore.password", "")
            val keyAliasName = keystoreProperties.getProperty("muse.keystore.key.alias", "")
            val keyPasswordValue = keystoreProperties.getProperty("muse.keystore.key.password", "")
            val keystorePath = "../../../../Important/keystores/keystore1.jks" // Use the calculated relative path

            // Check if properties were loaded - gradle sync might pass but build will fail
            if (storeFilePassword.isNotEmpty() && keyAliasName.isNotEmpty() && keyPasswordValue.isNotEmpty()) {
                storeFile = file(keystorePath)
                storePassword = storeFilePassword
                keyAlias = keyAliasName
                keyPassword = keyPasswordValue
            } else {
                println("Warning: Keystore properties not fully defined in local.properties. Release signing disabled.")
                // Optionally prevent build failure here if needed for debug/CI without credentials
            }
        }
    }

    defaultConfig {
        applicationId = "dev.riss.muse"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "IMGUR_CLIENT_ID", "\"$imgurClientId\"") // for ImgurAPI key
    }

    buildTypes {
        release {
            // Enable code shrinking
            isMinifyEnabled = true 
            // Enable resource shrinking
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Assign the signing config to the release build type
            signingConfig = signingConfigs.getByName("release")
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

    implementation("androidx.compose.material:material:1.6.8") // 

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("androidx.navigation:navigation-compose:2.7.6") // for navigation between different pages
    implementation("io.coil-kt:coil-compose:2.5.0") // For previewing images
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // For HTTP uploading to Imgur using ImgurAPI

    implementation("androidx.compose.material:material-icons-extended")
    
    // DataStore for persisting likes
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Use the Supabase BOM to set the version for all modules
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.1"))

    // Supabase modules
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt") // Added for real-time like updates

    // Ktor dependencies for Supabase networking
    implementation("io.ktor:ktor-client-core:3.0.0")
    implementation("io.ktor:ktor-client-okhttp:3.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("androidx.core:core-splashscreen:1.0.1")

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