plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
    kotlin("plugin.serialization") version "1.9.23"
    id("com.google.gms.google-services")
}

android {
    namespace = "com.craftflowtechnologies.guidelens"
    compileSdk = 36 // Updated to Android 14 (API 34)

    defaultConfig {
        applicationId = "com.craftflowtechnologies.guidelens"
        minSdk = 26 // Android 8.0 (Oreo) - covers 95%+ devices
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Enable code shrinking for release
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
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
            excludes += "mozilla/public-suffix-list.txt"
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.navigation:navigation-compose:2.7.4")
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.view)
    implementation(libs.firebase.crashlytics.buildtools)
    // Room database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // CameraX
    val camerax_version = "1.4.0-alpha02"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")
    implementation("androidx.camera:camera-extensions:${camerax_version}")
    implementation("androidx.camera:camera-video:${camerax_version}") // For video recording with AudioConfig

    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Networking (for API calls)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("org.json:json:20230227")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.4.0")

    // Ktor dependencies removed to avoid conflicts - using WebSocket-based approach instead
    
    // Supabase (commented out for build stability - using direct REST API calls)
    // implementation("io.github.jan-tennert.supabase:postgrest-kt:2.5.4")
    // implementation("io.github.jan-tennert.supabase:auth-kt:2.5.4")
    // implementation("io.github.jan-tennert.supabase:realtime-kt:2.5.4")
    // implementation("io.ktor:ktor-utils:2.3.12")
    // Firebase AI Logic for Gemini Live API (Production Ready)
    implementation(platform("com.google.firebase:firebase-bom:34.2.0"))
    implementation("com.google.firebase:firebase-ai")
    
    // Gemini API dependencies removed - using direct WebSocket API instead to avoid all Ktor conflicts
    // implementation("dev.shreyaspatil:generative-ai-kmp:0.9.0-1.1.0")
    
    // WebSocket for Gemini Live API (kept for custom implementation if needed)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Audio processing
    implementation("androidx.media3:media3-common:1.2.0")
    implementation("androidx.media3:media3-session:1.2.0")
    
    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Basic serialization for API work
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // TensorFlow Lite for local AI model inference
    implementation("org.tensorflow:tensorflow-lite:2.13.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.13.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    
    // MediaPipe for local AI tasks (alternative approach)
    implementation("com.google.mediapipe:tasks-text:0.10.5")

    // Google Sign-In
    implementation("androidx.credentials:credentials:1.6.0-alpha05")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0-alpha05")
    implementation ("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("com.google.android.gms:play-services-identity:18.1.0")
    
    // Google Auth ID Token validation  
    implementation("com.google.auth:google-auth-library-oauth2-http:1.30.0")

    // Data security
    implementation("androidx.security:security-crypto:1.1.0")

    // Clerk auth with Google SSO
    implementation("com.clerk:clerk-android:0.1.8")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    
    // Credential Manager for modern auth flows
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}