plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.secrets)
}

android {

    namespace = "com.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.smartstudyfocus.ptwszq"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isCrunchPngs = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            // Android Studio will automatically use the default debug keystore
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = false
        viewBinding = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

dependencies {
  implementation("androidx.appcompat:appcompat:1.6.1")
  implementation("com.google.android.material:material:1.11.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  
  // Lifecycle components
  implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
  implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")
  
  // Room database
  implementation(libs.androidx.room.runtime)
  annotationProcessor(libs.androidx.room.compiler)
  
  // MPAndroidChart
  implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
  
  // Unit & UI testing
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
}
