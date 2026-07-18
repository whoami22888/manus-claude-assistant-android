plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Read local.properties so developers can set groq.api.key there without
// touching the build file.  CI uses the GROQ_API_KEY environment variable.
val localProperties = java.util.Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) props.load(f.inputStream())
}
val groqApiKey: String =
    System.getenv("GROQ_API_KEY") ?: localProperties.getProperty("groq.api.key", "")

android {
    namespace = "com.manus.assistant"
    compileSdk = 34
    ndkVersion = "27.3.13750724"

    defaultConfig {
        applicationId = "com.manus.assistant"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Expose the Groq API key to the app via BuildConfig (empty string when unset)
        buildConfigField("String", "GROQ_API_KEY", "\"$groqApiKey\"")

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }
    }

    signingConfigs {
        // Release signing uses environment variables injected by CI.
        // When the env vars are absent the block is a no-op and the build
        // falls back to the debug key in the buildTypes block below.
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
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
            // Use the real keystore when KEYSTORE_PATH is available; otherwise
            // fall back to the debug key so local/CI builds remain installable.
            signingConfig = if (System.getenv("KEYSTORE_PATH") != null)
                signingConfigs.getByName("release")
            else
                signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.31.5"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // HTTP client for Groq API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Coroutines for off-main-thread API calls
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
