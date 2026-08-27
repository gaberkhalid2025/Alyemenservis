plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.devtools.ksp)
  id("com.google.gms.google-services")
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.alyemenservices"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("releaseConfig") {
      val customKeystore = file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-release-key.jks")
      val releaseKeystore = file("${rootDir}/release.keystore")
      val debugKeystore = file("${rootDir}/debug.keystore")

      if (customKeystore.exists()) {
        storeFile = customKeystore
        storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD") ?: System.getenv("STORE_PASSWORD") ?: "Maher@@--@@736462##"
        keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: System.getenv("KEY_ALIAS") ?: "Maher"
        keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: System.getenv("KEY_PASSWORD") ?: "Maher@@--@@736462##"
      } else if (releaseKeystore.exists()) {
        storeFile = releaseKeystore
        storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD") ?: System.getenv("STORE_PASSWORD") ?: "Maher@@--@@736462##"
        keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: System.getenv("KEY_ALIAS") ?: "Maher"
        keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: System.getenv("KEY_PASSWORD") ?: "Maher@@--@@736462##"
      } else if (debugKeystore.exists()) {
        storeFile = debugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
    create("debugConfig") {
      val debugKeystore = file("${rootDir}/debug.keystore")
      if (debugKeystore.exists()) {
        storeFile = debugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  lint {
    checkReleaseBuilds = false
    abortOnError = false
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      isShrinkResources = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("releaseConfig")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation("androidx.fragment:fragment-ktx:1.8.6")
  implementation(platform(libs.firebase.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.auth)
  implementation(libs.firebase.appcheck)
  implementation(libs.firebase.appcheck.playintegrity)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.storage)
  implementation(libs.firebase.messaging)
  implementation("com.google.firebase:firebase-analytics")
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
